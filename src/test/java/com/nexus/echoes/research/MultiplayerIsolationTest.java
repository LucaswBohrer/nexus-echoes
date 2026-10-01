package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multiplayer isolation: two players' states are fully independent objects;
 * service operations on one never leak into the other.
 */
class MultiplayerIsolationTest {

    private static ResourceLocation id(String path) {
        return new ResourceLocation("nexus_echoes", path);
    }

    private static ResearchGraph graph() {
        return ResearchGraph.validate(List.of(
                new ResearchDefinition(id("foundations"), "F", "d", "c", 50,
                        List.of(), List.of(id("tech_a"))),
                new ResearchDefinition(id("transmission"), "T", "d", "c", 100,
                        List.of(id("foundations")), List.of(id("tech_b")))));
    }

    @Test
    void pointsAreIndependent() {
        PlayerResearchState a = new PlayerResearchState();
        PlayerResearchState b = new PlayerResearchState();
        a.addPoints(100);
        assertEquals(100, a.points());
        assertEquals(0, b.points());
        b.spendPoints(10); // no-op, still independent
        assertEquals(100, a.points());
    }

    @Test
    void completionsAreIndependent() {
        ResearchGraph graph = graph();
        PlayerResearchState a = new PlayerResearchState();
        PlayerResearchState b = new PlayerResearchState();
        a.addPoints(200);
        b.addPoints(200);
        ResearchService.completeResearch(graph, a, id("foundations"));
        assertTrue(a.isCompleted(id("foundations")));
        assertFalse(b.isCompleted(id("foundations")));
    }

    @Test
    void unlocksAreIndependent() {
        ResearchGraph graph = graph();
        PlayerResearchState a = new PlayerResearchState();
        PlayerResearchState b = new PlayerResearchState();
        a.addPoints(200);
        b.addPoints(200);
        ResearchService.completeResearch(graph, a, id("foundations"));
        assertTrue(ResearchService.isUnlocked(graph, a, id("tech_a")));
        assertFalse(ResearchService.isUnlocked(graph, b, id("tech_a")));
    }

    @Test
    void onceSourcesAreIndependent() {
        PlayerResearchState a = new PlayerResearchState();
        PlayerResearchState b = new PlayerResearchState();
        ResearchSource src = new ResearchSource("discover", 10, true);
        assertEquals(ResearchOutcome.OK, ResearchService.grantFromSource(a, src));
        assertEquals(ResearchOutcome.OK, ResearchService.grantFromSource(b, src));
        assertEquals(10, a.points());
        assertEquals(10, b.points());
    }

    @Test
    void nbtRowsDoNotShareMutableState() {
        PlayerResearchState a = new PlayerResearchState();
        a.addPoints(77);
        a.markCompleted(id("foundations"));
        PlayerResearchState b = PlayerResearchState.fromNbt(a.toNbt());
        b.addPoints(1000);
        b.markCompleted(id("transmission"));
        assertEquals(77, a.points());
        assertFalse(a.isCompleted(id("transmission")));
        assertEquals(1077, b.points());
    }
}
