package com.nexus.echoes.ether.phenomenon;

import java.util.List;
import java.util.Objects;

/**
 * Static definition of an Ether phenomenon type (Phase 6B plan v2, S4).
 *
 * <p>Pure domain: ids are plain strings, durations are ticks, radii are
 * blocks. The three v1 definitions are fixed constants; the adapter layer
 * references them but never mutates them.
 */
public final class PhenomenonDefinition {

    /** Opportunity: +resonance inside radius while active. */
    public static final PhenomenonDefinition RESONANCE_SURGE = new PhenomenonDefinition(
            "nexus_echoes:resonance_surge", PhenomenonEffect.RESONANCE_SURGE,
            2400, 16, 0);

    /** Hazard: stability modifier + kinetic derate inside radius. */
    public static final PhenomenonDefinition ETHER_STORM = new PhenomenonDefinition(
            "nexus_echoes:ether_storm", PhenomenonEffect.ETHER_STORM,
            3600, 24, -40);

    /** Unstable: relocates entities inside radius to nearby safe spots. */
    public static final PhenomenonDefinition DIMENSIONAL_RIFT = new PhenomenonDefinition(
            "nexus_echoes:dimensional_rift", PhenomenonEffect.DIMENSIONAL_RIFT,
            1200, 8, 0);

    /** All v1 definitions, in canonical order. */
    public static final List<PhenomenonDefinition> ALL =
            List.of(RESONANCE_SURGE, ETHER_STORM, DIMENSIONAL_RIFT);

    private final String id;
    private final PhenomenonEffect effect;
    private final int durationTicks;
    private final int radius;
    private final int stabilityDelta;

    public PhenomenonDefinition(String id, PhenomenonEffect effect,
                                int durationTicks, int radius, int stabilityDelta) {
        this.id = Objects.requireNonNull(id, "id");
        this.effect = Objects.requireNonNull(effect, "effect");
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be positive");
        }
        this.durationTicks = durationTicks;
        this.radius = radius;
        this.stabilityDelta = stabilityDelta;
    }

    /** Definition id, e.g. {@code nexus_echoes:ether_storm}. */
    public String id() {
        return id;
    }

    public PhenomenonEffect effect() {
        return effect;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int radius() {
        return radius;
    }

    /**
     * Stability magnitude contributed while active. Zero for effects that
     * do not modify stability (explicitly none, not an omission).
     */
    public int stabilityDelta() {
        return stabilityDelta;
    }

    /** Lookup by id; throws on unknown ids (fail-fast, no silent default). */
    public static PhenomenonDefinition byId(String id) {
        for (PhenomenonDefinition def : ALL) {
            if (def.id().equals(id)) {
                return def;
            }
        }
        throw new IllegalArgumentException("Unknown phenomenon definition: " + id);
    }

    @Override
    public String toString() {
        return "PhenomenonDefinition{" + id + '}';
    }
}
