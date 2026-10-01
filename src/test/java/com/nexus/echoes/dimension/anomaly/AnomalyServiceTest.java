package com.nexus.echoes.dimension.anomaly;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anomaly lifecycle rules are pure and bounded (Phase 5):
 * spawn cap, expiry partitioning, intensity bounds, persistence round-trip.
 */
class AnomalyServiceTest {

    private static final List<AnomalyDefinition> DEFS =
            AnomalyDefinition.builtins("nexus_echoes");

    @Test
    void spawnDeclinedAtCap() {
        Optional<AnomalyDefinition> selected = AnomalyService.selectSpawn(
                new Random(1), AnomalyService.MAX_ACTIVE_ANOMALIES, DEFS);
        assertTrue(selected.isEmpty());
    }

    @Test
    void spawnAcceptedBelowCap() {
        Optional<AnomalyDefinition> selected = AnomalyService.selectSpawn(
                new Random(1), 0, DEFS);
        assertTrue(selected.isPresent());
    }

    @Test
    void spawnedIntensityWithinBounds() {
        for (AnomalyDefinition def : DEFS) {
            AnomalyInstance instance = AnomalyService.spawn(
                    new Random(7), def, 10, 64, -5, 100L);
            assertTrue(instance.intensity() >= 0.0 && instance.intensity() <= 1.0);
            assertEquals(100L + def.baseDurationTicks(), instance.expiresTick());
            assertFalse(instance.isExpired(100L));
            assertTrue(instance.isExpired(instance.expiresTick()));
        }
    }

    @Test
    void partitionSplitsActiveAndExpired() {
        // "dead" starts at tick 0 and expires; "live" starts exactly when "dead"
        // expires, so it is genuinely active at the partition tick.
        AnomalyInstance dead = AnomalyService.spawn(new Random(3), DEFS.get(0), 8, 64, 8, 0L);
        AnomalyInstance live = AnomalyService.spawn(new Random(2), DEFS.get(0), 0, 64, 0, dead.expiresTick());
        AnomalyService.Partition partition = AnomalyService.partition(
                dead.expiresTick(), List.of(live, dead));
        assertTrue(partition.active().stream().anyMatch(i -> i.instanceId().equals(live.instanceId())));
        assertTrue(partition.expired().stream().anyMatch(i -> i.instanceId().equals(dead.instanceId())));
        assertEquals(1, partition.active().size());
        assertEquals(1, partition.expired().size());
    }

    @Test
    void builtinDefinitionsAreDistinct() {
        assertTrue(DEFS.size() >= 4);
        long distinct = DEFS.stream().map(AnomalyDefinition::id).distinct().count();
        assertEquals(DEFS.size(), distinct);
    }

    @Test
    void instancePersistenceRoundTrip() {
        AnomalyInstance original = AnomalyService.spawn(new Random(5), DEFS.get(0), 3, 70, -9, 42L);
        AnomalyInstance restored = AnomalyInstance.read(original.write());
        assertEquals(original, restored);
    }
}
