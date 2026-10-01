package com.nexus.echoes.research;

/**
 * The built-in Phase 4 research sources (ADR-010).
 *
 * <p>Small, coherent first economy: discovering and mining the mod's own ore
 * plus engaging with the Phase 2 kinetic line. Repeatable income stays low
 * so points are earned by playing, not idled. Future sources (scanning,
 * quests, …) register here without touching the service.
 */
public final class ResearchSources {

    /** First nexus ore block mined: a discovery, once per player. */
    public static final ResearchSource DISCOVER_NEXUS_ORE =
            new ResearchSource("discover_nexus_ore", 10, true);

    /** Every nexus ore block mined: the steady trickle. */
    public static final ResearchSource MINE_NEXUS_ORE =
            new ResearchSource("mine_nexus_ore", 2, false);

    /** First gear crafted: engaging with kinetic technology, once per player. */
    public static final ResearchSource CRAFT_GEAR =
            new ResearchSource("craft_gear", 10, true);

    // ------------------------------------------------------------ Phase 5: Hollow

    /** First step into The Hollow: a discovery, once per player. */
    public static final ResearchSource ENTER_HOLLOW =
            new ResearchSource("enter_hollow", 25, true);

    /** First use of an obelisk return core: a discovery, once per player. */
    public static final ResearchSource DISCOVER_OBELISK =
            new ResearchSource("discover_obelisk", 15, true);

    /** First ruin/vault structure found: a discovery, once per player. */
    public static final ResearchSource DISCOVER_RUIN =
            new ResearchSource("discover_ruin", 15, true);

    /** First anomaly witnessed: a discovery, once per player. */
    public static final ResearchSource DISCOVER_ANOMALY =
            new ResearchSource("discover_anomaly", 10, true);

    /** Analyzing a memory fragment: repeatable deep-exploration income. */
    public static final ResearchSource ANALYZE_MEMORY_FRAGMENT =
            new ResearchSource("analyze_memory_fragment", 20, false);

    private ResearchSources() {
    }
}
