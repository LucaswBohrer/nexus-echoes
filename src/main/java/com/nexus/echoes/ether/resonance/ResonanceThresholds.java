package com.nexus.echoes.ether.resonance;

/**
 * Named Resonance tuning constants (Phase 6, S1).
 *
 * <p>These are initial gameplay values, adjustable in implementation —
 * they are not architectural decisions. The architecture only requires that
 * thresholds exist and are checked server-side.
 */
public final class ResonanceThresholds {

    private ResonanceThresholds() {
    }

    /** Minimum resonance for stabilized (repeat) Ether travel. */
    public static final int TRAVEL_MIN = 250;

    /** Resonance at or above which phase stalkers hunt the player. */
    public static final int STALKER_AGGRO = 600;

    /**
     * One-time resonance granted on first Ether arrival. This is an
     * Ether-exposure event (a gate-listed mutation source), not a research
     * handout: it is issued server-side by the arrival handler, exactly
     * once per player.
     */
    public static final int FIRST_EXPOSURE_GRANT = 50;
}
