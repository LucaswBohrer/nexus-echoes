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

    private ResearchSources() {
    }
}
