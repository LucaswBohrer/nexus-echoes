package com.nexus.echoes.ether.phenomenon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Pure phenomenon lifecycle rules (Phase 6B plan v2, S4).
 *
 * <p>Spawn-under-cap selection and active/expired partitioning. No
 * Minecraft imports; the manager ({@code ether/mc}) owns the world
 * interaction and calls into these rules.
 */
public final class PhenomenonService {

    /** Maximum simultaneously active phenomena (v1 bound). */
    public static final int MAX_ACTIVE = 6;

    private PhenomenonService() {
    }

    /** Result of partitioning instances by expiry. */
    public record Partition(List<PhenomenonInstance> active,
                            List<PhenomenonInstance> expired) {
    }

    /**
     * Splits instances into still-active and expired at {@code nowTick}.
     * Deterministic; the expired list drives cleanup in encounter order.
     */
    public static Partition partition(Collection<PhenomenonInstance> instances, long nowTick) {
        List<PhenomenonInstance> active = new ArrayList<>();
        List<PhenomenonInstance> expired = new ArrayList<>();
        for (PhenomenonInstance instance : instances) {
            if (instance.isExpired(nowTick)) {
                expired.add(instance);
            } else {
                active.add(instance);
            }
        }
        return new Partition(List.copyOf(active), List.copyOf(expired));
    }

    /**
     * Attempts to spawn one phenomenon of a random definition at the given
     * position. Returns empty when the active cap is reached — the caller
     * must not spawn beyond {@link #MAX_ACTIVE}.
     */
    public static Optional<PhenomenonInstance> trySpawn(Random random,
                                                       Collection<PhenomenonInstance> active,
                                                       int x, int y, int z,
                                                       long nowTick) {
        if (active.size() >= MAX_ACTIVE) {
            return Optional.empty();
        }
        List<PhenomenonDefinition> defs = PhenomenonDefinition.ALL;
        PhenomenonDefinition def = defs.get(random.nextInt(defs.size()));
        return Optional.of(new PhenomenonInstance(
                UUID.randomUUID(), def, PhenomenonOwner.WORLD, x, y, z, nowTick));
    }

    /**
     * Attempts to spawn one phenomenon of the given definition. Used when
     * spawn rules (biome weighting etc.) select the type in the adapter.
     */
    public static Optional<PhenomenonInstance> trySpawnOf(PhenomenonDefinition definition,
                                                         Collection<PhenomenonInstance> active,
                                                         int x, int y, int z,
                                                         long nowTick) {
        if (active.size() >= MAX_ACTIVE) {
            return Optional.empty();
        }
        return Optional.of(new PhenomenonInstance(
                UUID.randomUUID(), definition, PhenomenonOwner.WORLD, x, y, z, nowTick));
    }
}
