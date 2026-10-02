package com.nexus.echoes.ether.stability;

import java.util.List;

/**
 * Ether Stability composition (Phase 6A gate, C1 — Phase 6, S3).
 *
 * <p>Pure domain: no Minecraft imports, no position knowledge, no spatial
 * state. Stability is always computed on demand as:
 *
 * <pre>
 * effectiveStability = clamp(baseline + sum(unexpired modifier magnitudes))
 * </pre>
 *
 * <p>There is no {@code EtherStabilityField}, no per-block stability map,
 * and no continuous spatial simulation in v1. The adapter layer
 * ({@code PhenomenonManager}) selects which phenomena affect a queried
 * position and passes their modifiers here.
 */
public final class StabilityService {

    /** Global Ether baseline for v1. Phenomena modify locally around this. */
    public static final int BASELINE = 100;

    /** Composition bounds. */
    public static final int MIN = 0;
    public static final int MAX = 200;

    private StabilityService() {
    }

    /**
     * Composes the effective stability from the baseline and the supplied
     * modifiers. Expired modifiers are ignored. Summation is
     * order-independent, so overlapping phenomena compose deterministically.
     * An empty modifier list yields the baseline (position outside every
     * phenomenon).
     *
     * @param modifiers modifiers affecting the queried position, supplied
     *                  by the adapter layer
     * @param nowTick   current tick for expiry filtering
     */
    public static int effectiveStability(List<StabilityModifier> modifiers, long nowTick) {
        int total = BASELINE;
        for (StabilityModifier modifier : modifiers) {
            if (!modifier.isExpired(nowTick)) {
                total += modifier.magnitude();
            }
        }
        return Math.max(MIN, Math.min(MAX, total));
    }

    /**
     * Kinetic derate factor for an ether storm, derived from the effective
     * stability at the storm center. Lower stability means a stronger
     * derate. Bounded to {@code [0.25, 0.75]}; initial tuning.
     */
    public static double stormDerate(int effectiveStability) {
        double derate = 0.25 + (BASELINE - effectiveStability) / 200.0;
        return Math.max(0.25, Math.min(0.75, derate));
    }
}
