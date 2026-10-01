package com.nexus.echoes.dimension.anomaly;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Static definition of one anomaly kind: identity, duration, radius,
 * intensity. Data lives in code (a tiny fixed set) rather than JSON — the
 * set is behavior, not content, and each entry maps to adapter handling.
 */
public record AnomalyDefinition(
        ResourceLocation id,
        AnomalyEffect effect,
        /** Lifetime in ticks once spawned. */
        int baseDurationTicks,
        /** Effect radius in blocks. */
        int radius,
        /** 0..1 scale applied to the effect strength. */
        double baseIntensity) {

    public AnomalyDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (baseDurationTicks <= 0) {
            throw new IllegalArgumentException("duration must be positive: " + id);
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be positive: " + id);
        }
        if (Double.isNaN(baseIntensity) || baseIntensity < 0 || baseIntensity > 1) {
            throw new IllegalArgumentException("intensity must be in [0,1]: " + id);
        }
    }

    /** The built-in Phase 5 set. Order is stable; ids are the contract. */
    public static List<AnomalyDefinition> builtins(String namespace) {
        return List.of(
                new AnomalyDefinition(
                        new ResourceLocation(namespace, "static_field"),
                        AnomalyEffect.STATIC_FIELD, 20 * 90, 12, 0.5),
                new AnomalyDefinition(
                        new ResourceLocation(namespace, "echo_burst"),
                        AnomalyEffect.ECHO_BURST, 20 * 30, 8, 0.6),
                new AnomalyDefinition(
                        new ResourceLocation(namespace, "spatial_rift"),
                        AnomalyEffect.SPATIAL_RIFT, 20 * 45, 10, 0.7),
                new AnomalyDefinition(
                        new ResourceLocation(namespace, "research_echo"),
                        AnomalyEffect.RESEARCH_ECHO, 20 * 120, 6, 1.0));
    }
}
