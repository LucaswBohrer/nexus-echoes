package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.anomaly.AnomalyDefinition;
import com.nexus.echoes.dimension.anomaly.AnomalyInstance;
import com.nexus.echoes.dimension.anomaly.AnomalyService;
import com.nexus.echoes.dimension.block.WardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anomaly persistence and ward suppression (Phase 5).
 *
 * <p>Covers: ward radius suppression, add/remove of anomalies and wards,
 * and save/load round-trip.
 */
class AnomalySavedDataTest {

    private AnomalyInstance anomalyAt(int x, int y, int z) {
        List<AnomalyDefinition> defs = AnomalyDefinition.builtins("nexus_echoes");
        return AnomalyService.spawn(new Random(11), defs.get(0), x, y, z, 0L);
    }

    @Test
    void wardSuppressesNearbyAnomaly() {
        AnomalySavedData data = AnomalySavedData.createEmpty();
        BlockPos ward = new BlockPos(0, 64, 0);
        data.addWard(ward);
        BlockPos near = new BlockPos(WardBlockEntity.RADIUS - 1, 64, 0);
        BlockPos far = new BlockPos(WardBlockEntity.RADIUS + 8, 64, 0);
        assertTrue(data.isWarded(near, WardBlockEntity.RADIUS));
        assertFalse(data.isWarded(far, WardBlockEntity.RADIUS));
    }

    @Test
    void wardRemovalRestoresEffect() {
        AnomalySavedData data = AnomalySavedData.createEmpty();
        BlockPos ward = new BlockPos(0, 64, 0);
        BlockPos near = new BlockPos(4, 64, 0);
        data.addWard(ward);
        assertTrue(data.isWarded(near, WardBlockEntity.RADIUS));
        data.removeWard(ward);
        assertFalse(data.isWarded(near, WardBlockEntity.RADIUS));
    }

    @Test
    void addAndRemoveAnomaly() {
        AnomalySavedData data = AnomalySavedData.createEmpty();
        AnomalyInstance instance = anomalyAt(10, 64, 10);
        data.add(instance);
        assertEquals(1, data.active().size());
        data.remove(instance);
        assertTrue(data.active().isEmpty());
    }

    @Test
    void saveLoadRoundTrip() {
        AnomalySavedData data = AnomalySavedData.createEmpty();
        AnomalyInstance instance = anomalyAt(10, 64, 10);
        data.add(instance);
        data.addWard(new BlockPos(0, 64, 0));

        AnomalySavedData restored = AnomalySavedData.load(data.save(new CompoundTag()));
        assertEquals(1, restored.active().size());
        assertEquals(instance, restored.active().get(0));
        assertTrue(restored.isWarded(new BlockPos(4, 64, 0), WardBlockEntity.RADIUS));
    }
}
