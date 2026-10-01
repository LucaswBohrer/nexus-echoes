package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The service is the single authority for progression rules: sources,
 * prerequisites, costs, duplicate completion and unlock queries.
 */
class ResearchServiceTest {

    private ResearchGraph graph;
    private PlayerResearchState state;

    private static ResourceLocation id(String path) {
        return new ResourceLocation("nexus_echoes", path);
    }

    @BeforeEach
    void setup() {
        graph = ResearchGraph.validate(List.of(
                new ResearchDefinition(id("foundations"), "Foundations", "d", "c", 50,
                        List.of(), List.of(id("tech_a"))),
                new ResearchDefinition(id("transmission"), "Transmission", "d", "c", 100,
                        List.of(id("foundations")), List.of(id("tech_b"), id("tech_c"))),
                new ResearchDefinition(id("advanced"), "Advanced", "d", "c", 150,
                        List.of(id("foundations"), id("transmission")), List.of(id("tech_d")))));
        state = new PlayerResearchState();
    }

    // ------------------------------------------------------------- sources

    @Test
    void repeatableSourceGrantsEveryTime() {
        ResearchSource src = new ResearchSource("mine", 2, false);
        assertEquals(ResearchOutcome.OK, ResearchService.grantFromSource(state, src));
        assertEquals(ResearchOutcome.OK, ResearchService.grantFromSource(state, src));
        assertEquals(4, state.points());
    }

    @Test
    void onceSourceGrantsOnlyOnce() {
        ResearchSource src = new ResearchSource("discover", 10, true);
        assertEquals(ResearchOutcome.OK, ResearchService.grantFromSource(state, src));
        assertEquals(ResearchOutcome.ALREADY_COMPLETED, ResearchService.grantFromSource(state, src));
        assertEquals(10, state.points());
    }

    @Test
    void nullSourceOrStateIsInvalid() {
        assertEquals(ResearchOutcome.INVALID_AMOUNT,
                ResearchService.grantFromSource(state, null));
        assertEquals(ResearchOutcome.INVALID_AMOUNT,
                ResearchService.grantFromSource(null, new ResearchSource("s", 1, false)));
    }

    // ------------------------------------------------------------- completion

    @Test
    void canCompleteWithNoPrerequisites() {
        state.addPoints(50);
        assertEquals(ResearchOutcome.OK,
                ResearchService.canComplete(graph, state, id("foundations")));
    }

    @Test
    void unknownResearchRejected() {
        state.addPoints(999);
        assertEquals(ResearchOutcome.UNKNOWN_RESEARCH,
                ResearchService.canComplete(graph, state, id("ghost")));
        assertEquals(ResearchOutcome.UNKNOWN_RESEARCH,
                ResearchService.completeResearch(graph, state, id("ghost")));
    }

    @Test
    void missingPrerequisiteBlocksCompletion() {
        state.addPoints(999);
        assertEquals(ResearchOutcome.MISSING_PREREQUISITE,
                ResearchService.canComplete(graph, state, id("transmission")));
    }

    @Test
    void multiplePrerequisitesAllRequired() {
        state.addPoints(999);
        state.markCompleted(id("foundations"));
        assertEquals(ResearchOutcome.MISSING_PREREQUISITE,
                ResearchService.canComplete(graph, state, id("advanced")));
        state.markCompleted(id("transmission"));
        assertEquals(ResearchOutcome.OK,
                ResearchService.canComplete(graph, state, id("advanced")));
    }

    @Test
    void insufficientPointsBlocksCompletion() {
        state.addPoints(49);
        assertEquals(ResearchOutcome.INSUFFICIENT_POINTS,
                ResearchService.canComplete(graph, state, id("foundations")));
    }

    @Test
    void exactCostSucceeds() {
        state.addPoints(50);
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, state, id("foundations")));
        assertEquals(0, state.points());
        assertTrue(state.isCompleted(id("foundations")));
    }

    @Test
    void completionSpendsPoints() {
        state.addPoints(80);
        ResearchService.completeResearch(graph, state, id("foundations"));
        assertEquals(30, state.points());
    }

    @Test
    void failedCompletionSpendsNothing() {
        state.addPoints(999);
        assertEquals(ResearchOutcome.MISSING_PREREQUISITE,
                ResearchService.completeResearch(graph, state, id("transmission")));
        assertEquals(999, state.points());
        assertFalse(state.isCompleted(id("transmission")));
    }

    @Test
    void duplicateCompletionPrevented() {
        state.addPoints(200);
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, state, id("foundations")));
        assertEquals(ResearchOutcome.ALREADY_COMPLETED,
                ResearchService.completeResearch(graph, state, id("foundations")));
        assertEquals(150, state.points()); // second attempt charged nothing
    }

    // ------------------------------------------------------------- unlocks

    @Test
    void lockedTechnologyByDefault() {
        state.addPoints(999);
        assertFalse(ResearchService.isUnlocked(graph, state, id("tech_a")));
    }

    @Test
    void completedResearchUnlocksItsTechnologies() {
        state.addPoints(200);
        ResearchService.completeResearch(graph, state, id("foundations"));
        assertTrue(ResearchService.isUnlocked(graph, state, id("tech_a")));
        assertFalse(ResearchService.isUnlocked(graph, state, id("tech_b")));
        ResearchService.completeResearch(graph, state, id("transmission"));
        assertTrue(ResearchService.isUnlocked(graph, state, id("tech_b")));
        assertTrue(ResearchService.isUnlocked(graph, state, id("tech_c")));
    }

    @Test
    void unknownTechnologyStaysLocked() {
        state.addPoints(999);
        ResearchService.completeResearch(graph, state, id("foundations"));
        assertFalse(ResearchService.isUnlocked(graph, state, id("nope")));
    }

    @Test
    void unlockSurvivesNullsSafely() {
        assertFalse(ResearchService.isUnlocked(null, state, id("tech_a")));
        assertFalse(ResearchService.isUnlocked(graph, null, id("tech_a")));
        assertFalse(ResearchService.isUnlocked(graph, state, null));
    }

    // ------------------------------------------------------------- available

    @Test
    void availableListsOnlyCompletableResearch() {
        state.addPoints(60);
        List<ResourceLocation> available = ResearchService.available(graph, state);
        assertEquals(List.of(id("foundations")), available);
        ResearchService.completeResearch(graph, state, id("foundations"));
        assertTrue(ResearchService.available(graph, state).isEmpty()); // 10 pts left, transmission costs 100
        state.addPoints(100);
        assertEquals(List.of(id("transmission")), ResearchService.available(graph, state));
    }
}
