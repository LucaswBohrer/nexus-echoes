package com.nexus.echoes.dimension.anomaly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Pure anomaly lifecycle rules (ADR-011): spawn selection under a global
 * cap, expiry, and instance creation. The MC adapter owns persistence,
 * ticking cadence and effect application; the client never decides anything.
 */
public final class AnomalyService {

    /** Maximum simultaneously active anomalies per Hollow level. Sparse by design. */
    public static final int MAX_ACTIVE_ANOMALIES = 8;

    private AnomalyService() {
    }

    /**
     * Selects an anomaly kind to spawn, or empty when the cap is reached or
     * the random roll declines. Research echos are rare by construction.
     */
    public static Optional<AnomalyDefinition> selectSpawn(
            Random random, int activeCount, List<AnomalyDefinition> definitions) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(definitions, "definitions");
        if (definitions.isEmpty() || activeCount >= MAX_ACTIVE_ANOMALIES) {
            return Optional.empty();
        }
        // weighted: research echos are the rare ones
        List<AnomalyDefinition> pool = new ArrayList<>();
        for (AnomalyDefinition def : definitions) {
            int weight = def.effect() == AnomalyEffect.RESEARCH_ECHO ? 1 : 4;
            for (int i = 0; i < weight; i++) {
                pool.add(def);
            }
        }
        return Optional.of(pool.get(random.nextInt(pool.size())));
    }

    /** Creates an instance; intensity jitters around the definition base. */
    public static AnomalyInstance spawn(
            Random random, AnomalyDefinition definition, int x, int y, int z, long nowTick) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(definition, "definition");
        double jitter = 0.85 + random.nextDouble() * 0.3;
        double intensity = Math.min(1.0, definition.baseIntensity() * jitter);
        return new AnomalyInstance(
                UUID.randomUUID(), definition.id(), x, y, z,
                nowTick, nowTick + definition.baseDurationTicks(), intensity);
    }

    /** Splits active instances into (still active, expired). Pure. */
    public static Partition partition(long nowTick, List<AnomalyInstance> instances) {
        List<AnomalyInstance> active = new ArrayList<>();
        List<AnomalyInstance> expired = new ArrayList<>();
        for (AnomalyInstance instance : instances) {
            if (instance.isExpired(nowTick)) {
                expired.add(instance);
            } else {
                active.add(instance);
            }
        }
        return new Partition(List.copyOf(active), List.copyOf(expired));
    }

    public record Partition(List<AnomalyInstance> active, List<AnomalyInstance> expired) {
    }
}
