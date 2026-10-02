package com.nexus.echoes.ether.phenomenon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phenomenon lifecycle rules (Phase 6, S4).
 *
 * <p>Covers: definitions fixed for v1, expiry partition, spawn cap,
 * affect-radius boundary, explicit world ownership.
 */
class PhenomenonServiceTest {

    @Test
    void definitionsAreFixed() {
        assertEquals(3, PhenomenonDefinition.ALL.size());
        assertEquals(PhenomenonEffect.ETHER_STORM, PhenomenonDefinition.ETHER_STORM.effect());
        assertEquals(-40, PhenomenonDefinition.ETHER_STORM.stabilityDelta());
        assertEquals(0, PhenomenonDefinition.RESONANCE_SURGE.stabilityDelta(),
                "surge contributes no stability modifier — explicit, not an omission");
        assertEquals(0, PhenomenonDefinition.DIMENSIONAL_RIFT.stabilityDelta());
        assertEquals(PhenomenonDefinition.ETHER_STORM,
                PhenomenonDefinition.byId("nexus_echoes:ether_storm"));
        assertThrows(IllegalArgumentException.class,
                () -> PhenomenonDefinition.byId("nexus_echoes:nope"));
    }

    @Test
    void partitionSeparatesExpired() {
        PhenomenonInstance fresh = new PhenomenonInstance(UUID.randomUUID(),
                PhenomenonDefinition.RESONANCE_SURGE, PhenomenonOwner.WORLD, 0, 64, 0, 1000L);
        PhenomenonInstance old = new PhenomenonInstance(UUID.randomUUID(),
                PhenomenonDefinition.RESONANCE_SURGE, PhenomenonOwner.WORLD, 10, 64, 0, -5000L);

        PhenomenonService.Partition p = PhenomenonService.partition(List.of(fresh, old), 1000L);
        assertEquals(List.of(fresh), p.active());
        assertEquals(List.of(old), p.expired());
    }

    @Test
    void spawnRespectsCap() {
        List<PhenomenonInstance> active = new ArrayList<>();
        Random random = new Random(7);
        for (int i = 0; i < PhenomenonService.MAX_ACTIVE; i++) {
            Optional<PhenomenonInstance> spawned =
                    PhenomenonService.trySpawn(random, active, i, 64, 0, 0L);
            assertTrue(spawned.isPresent());
            active.add(spawned.get());
        }
        assertTrue(PhenomenonService.trySpawn(random, active, 99, 64, 0, 0L).isEmpty(),
                "no spawn beyond MAX_ACTIVE");
        assertTrue(PhenomenonService.trySpawnOf(PhenomenonDefinition.ETHER_STORM,
                active, 99, 64, 0, 0L).isEmpty());
    }

    @Test
    void affectsRadiusBoundary() {
        PhenomenonInstance storm = new PhenomenonInstance(UUID.randomUUID(),
                PhenomenonDefinition.ETHER_STORM, PhenomenonOwner.WORLD, 0, 64, 0, 0L);
        int r = PhenomenonDefinition.ETHER_STORM.radius();
        assertTrue(storm.affects(r, 64, 0), "on-radius counts as affected");
        assertFalse(storm.affects(r + 1, 64, 0));
        assertTrue(storm.affects(0, 64, 0));
        assertEquals(PhenomenonOwner.WORLD, storm.owner());
    }

    @Test
    void expiryBoundary() {
        PhenomenonInstance inst = new PhenomenonInstance(UUID.randomUUID(),
                PhenomenonDefinition.DIMENSIONAL_RIFT, PhenomenonOwner.WORLD, 0, 64, 0, 100L);
        long expires = 100L + PhenomenonDefinition.DIMENSIONAL_RIFT.durationTicks();
        assertFalse(inst.isExpired(expires - 1));
        assertTrue(inst.isExpired(expires));
        assertEquals(expires, inst.expiresTick());
    }
}
