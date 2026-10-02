package com.nexus.echoes.ether.phenomenon;

/**
 * The three v1 Ether phenomenon effects (Phase 6B plan v2, S4).
 *
 * <p>Fixed for v1. New effects are a content decision for a later phase,
 * not an extension point to be built speculatively.
 */
public enum PhenomenonEffect {

    /**
     * Opportunity: grants resonance to players inside the radius while
     * active. No stability modifier.
     */
    RESONANCE_SURGE,

    /**
     * Hazard: applies a local negative stability modifier and a kinetic
     * derate inside the radius while active. The derate scales with the
     * effective stability at the storm center.
     */
    ETHER_STORM,

    /**
     * Unstable: relocates entities inside the radius to nearby safe
     * positions. No stability modifier.
     */
    DIMENSIONAL_RIFT
}
