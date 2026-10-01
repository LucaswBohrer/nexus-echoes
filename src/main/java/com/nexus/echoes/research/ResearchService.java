package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Central authority for research rules. All methods are pure functions of
 * {@code (graph, state)} — no players, no levels, no packets — so the entire
 * progression ruleset is unit-testable without Minecraft.
 *
 * <p>The Minecraft adapter ({@code research.mc}) owns persistence and event
 * wiring and must call these methods server-side only.
 */
public final class ResearchService {

    private ResearchService() {
    }

    /**
     * Grants points from a source. Once-per-player sources are claimed
     * atomically: a repeated trigger grants nothing.
     */
    public static ResearchOutcome grantFromSource(PlayerResearchState state, ResearchSource source) {
        if (state == null || source == null) {
            return ResearchOutcome.INVALID_AMOUNT;
        }
        if (source.oncePerPlayer() && !state.claimSource(source.id())) {
            return ResearchOutcome.ALREADY_COMPLETED;
        }
        return state.addPoints(source.points()) ? ResearchOutcome.OK : ResearchOutcome.INVALID_AMOUNT;
    }

    /** Whether the player may currently complete the given research. */
    public static ResearchOutcome canComplete(ResearchGraph graph, PlayerResearchState state,
                                              ResourceLocation researchId) {
        if (graph == null || state == null || researchId == null) {
            return ResearchOutcome.UNKNOWN_RESEARCH;
        }
        ResearchDefinition def = graph.get(researchId);
        if (def == null) {
            return ResearchOutcome.UNKNOWN_RESEARCH;
        }
        if (state.isCompleted(researchId)) {
            return ResearchOutcome.ALREADY_COMPLETED;
        }
        for (ResourceLocation prereq : def.prerequisites()) {
            if (!state.isCompleted(prereq)) {
                return ResearchOutcome.MISSING_PREREQUISITE;
            }
        }
        if (state.points() < def.cost()) {
            return ResearchOutcome.INSUFFICIENT_POINTS;
        }
        return ResearchOutcome.OK;
    }

    /**
     * Completes a research: validates prerequisites and cost, spends the
     * points, records completion — atomically. No partial state on failure.
     */
    public static ResearchOutcome completeResearch(ResearchGraph graph, PlayerResearchState state,
                                                   ResourceLocation researchId) {
        ResearchOutcome check = canComplete(graph, state, researchId);
        if (check != ResearchOutcome.OK) {
            return check;
        }
        ResearchDefinition def = graph.get(researchId);
        // spendPoints cannot fail here (canComplete verified balance), but
        // guard anyway so a failure can never record an unpaid completion.
        if (!state.spendPoints(def.cost())) {
            return ResearchOutcome.INSUFFICIENT_POINTS;
        }
        state.markCompleted(researchId);
        return ResearchOutcome.OK;
    }

    /**
     * The single choke point for technology gating (ADR-010): a technology is
     * unlocked when any completed research lists it in {@code unlocks}.
     * Unknown technologies are locked by default.
     */
    public static boolean isUnlocked(ResearchGraph graph, PlayerResearchState state,
                                     ResourceLocation technologyId) {
        if (graph == null || state == null || technologyId == null) {
            return false;
        }
        for (ResourceLocation completedId : state.completed()) {
            ResearchDefinition def = graph.get(completedId);
            if (def != null && def.unlocks().contains(technologyId)) {
                return true;
            }
        }
        return false;
    }

    /** All researches the player could complete right now (for GUI/tests). */
    public static List<ResourceLocation> available(ResearchGraph graph, PlayerResearchState state) {
        if (graph == null || state == null) {
            return List.of();
        }
        return graph.topologicalOrder().stream()
                .filter(id -> canComplete(graph, state, id) == ResearchOutcome.OK)
                .toList();
    }
}
