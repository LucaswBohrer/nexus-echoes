package com.nexus.echoes.dimension.anomaly;

/**
 * The small initial anomaly effect set (ADR-011).
 *
 * <p>Deliberately tiny: each effect has a clear server-side meaning and a
 * client presentation that needs no custom packets (vanilla particles and
 * sounds). The enum is the extension point — new effects add a value plus
 * handling in the MC adapter, never a framework rewrite.
 */
public enum AnomalyEffect {

    /**
     * Dampens kinetic machines in radius (temporary adapter-layer factor;
     * the pure kinetic simulator is never touched).
     */
    STATIC_FIELD,

    /** Sensory burst: particles, sound and a brief slowing of entities. */
    ECHO_BURST,

    /** Rare spatial instability: short random displacement of entities. */
    SPATIAL_RIFT,

    /** A coherent echo: grants research points once per player. */
    RESEARCH_ECHO
}
