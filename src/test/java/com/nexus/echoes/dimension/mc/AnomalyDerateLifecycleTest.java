package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.anomaly.AnomalyDefinition;
import com.nexus.echoes.dimension.anomaly.AnomalyInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Derate-clearing decision on anomaly expiry (Phase 5.5 audit RISK-1).
 *
 * <p>Covers: a position owned only by the expired static field must be
 * cleared even when its kinetic node was removed mid-anomaly (no provider
 * present); a position still covered by another active static field must
 * keep its derate; non-static anomalies never own derates.
 */
class AnomalyDerateLifecycleTest {

    private static final ResourceLocation STATIC_FIELD =
            new ResourceLocation("nexus_echoes", "static_field");
    private static final ResourceLocation ECHO_BURST =
            new ResourceLocation("nexus_echoes", "echo_burst");

    private final AnomalyManager manager = new AnomalyManager();

    private AnomalyInstance instance(ResourceLocation def, int x, int y, int z) {
        return new AnomalyInstance(UUID.randomUUID(), def, x, y, z, 0L, 100L, 0.5);
    }

    @Test
    void expiredFieldAloneLeavesPositionUncovered() {
        AnomalyInstance expired = instance(STATIC_FIELD, 0, 64, 0);
        // static_field radius is 12: (5,64,0) is inside, (50,64,0) is outside.
        assertTrue(manager.isUncovered(new BlockPos(5, 64, 0), expired, List.of()),
                "position owned only by the expired field must be cleared");
        assertTrue(manager.isUncovered(new BlockPos(50, 64, 0), expired, List.of()),
                "positions outside every field are trivially uncovered");
    }

    @Test
    void overlappingStaticFieldKeepsPositionCovered() {
        AnomalyInstance expired = instance(STATIC_FIELD, 0, 64, 0);
        AnomalyInstance other = instance(STATIC_FIELD, 8, 64, 0);
        BlockPos pos = new BlockPos(6, 64, 0);
        assertFalse(manager.isUncovered(pos, expired, List.of(other)),
                "position inside another active static field must keep its derate");
    }

    @Test
    void distantStaticFieldDoesNotCover() {
        AnomalyInstance expired = instance(STATIC_FIELD, 0, 64, 0);
        AnomalyInstance far = instance(STATIC_FIELD, 100, 64, 0);
        assertTrue(manager.isUncovered(new BlockPos(5, 64, 0), expired, List.of(far)),
                "a static field 100 blocks away cannot cover the position");
    }

    @Test
    void nonStaticAnomalyNeverCoversDerates() {
        AnomalyInstance expired = instance(STATIC_FIELD, 0, 64, 0);
        AnomalyInstance echo = instance(ECHO_BURST, 5, 64, 0);
        assertTrue(manager.isUncovered(new BlockPos(5, 64, 0), expired, List.of(echo)),
                "echo_burst owns no derates, so the position must be cleared");
    }

    @Test
    void unknownDefinitionCannotCover() {
        AnomalyInstance expired = instance(STATIC_FIELD, 0, 64, 0);
        AnomalyInstance unknown = instance(
                new ResourceLocation("nexus_echoes", "no_such_anomaly"), 5, 64, 0);
        assertTrue(manager.isUncovered(new BlockPos(5, 64, 0), expired, List.of(unknown)),
                "unknown definitions are ignored by the covering check");
    }

    @Test
    void builtinStaticFieldRadiusIsTwelve() {
        // Locks the geometric assumption the tests above rely on.
        AnomalyDefinition def = AnomalyDefinition.builtins("nexus_echoes").stream()
                .filter(d -> d.id().equals(STATIC_FIELD))
                .findFirst()
                .orElseThrow();
        assertEquals(12, def.radius());
    }
}
