package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dependency-graph validation: duplicates, unknown prerequisites,
 * self-dependencies, cycles and deterministic ordering.
 */
class ResearchGraphTest {

    private static final ResourceLocation A = id("a");
    private static final ResourceLocation B = id("b");
    private static final ResourceLocation C = id("c");

    private static ResourceLocation id(String path) {
        return new ResourceLocation("nexus_echoes", path);
    }

    private static ResearchDefinition def(String path, int cost, String... prereqs) {
        List<ResourceLocation> pre = java.util.Arrays.stream(prereqs).map(ResearchGraphTest::id).toList();
        return new ResearchDefinition(id(path), path, "desc", "cat", cost, pre, List.of());
    }

    @Test
    void validLinearChainPassesAndOrdersTopologically() {
        ResearchGraph graph = ResearchGraph.validate(List.of(def("a", 10), def("b", 10, "a"), def("c", 10, "b")));
        assertEquals(3, graph.size());
        List<ResourceLocation> order = graph.topologicalOrder();
        assertTrue(order.indexOf(A) < order.indexOf(B));
        assertTrue(order.indexOf(B) < order.indexOf(C));
    }

    @Test
    void diamondDependenciesPass() {
        ResearchGraph graph = ResearchGraph.validate(List.of(
                def("a", 10), def("b", 10, "a"), def("c", 10, "a"), def("d", 10, "b", "c")));
        assertEquals(4, graph.size());
        List<ResourceLocation> order = graph.topologicalOrder();
        assertTrue(order.indexOf(A) < order.indexOf(id("d")));
    }

    @Test
    void duplicateIdRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(def("a", 10), def("a", 20))));
        assertTrue(e.getMessage().contains("duplicate"));
    }

    @Test
    void unknownPrerequisiteRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(def("a", 10, "ghost"))));
        assertTrue(e.getMessage().contains("unknown research"));
    }

    @Test
    void selfDependencyRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(def("a", 10, "a"))));
        assertTrue(e.getMessage().contains("itself"));
    }

    @Test
    void twoNodeCycleRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(def("a", 10, "b"), def("b", 10, "a"))));
        assertTrue(e.getMessage().contains("cycle"));
    }

    @Test
    void threeNodeCycleRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ResearchGraph.validate(List.of(
                        def("a", 10, "c"), def("b", 10, "a"), def("c", 10, "b"))));
        assertTrue(e.getMessage().contains("cycle"));
    }

    @Test
    void zeroCostRejected() {
        assertThrows(IllegalArgumentException.class, () -> def("a", 0));
    }

    @Test
    void negativeCostRejected() {
        assertThrows(IllegalArgumentException.class, () -> def("a", -5));
    }

    @Test
    void emptyGraphValid() {
        ResearchGraph graph = ResearchGraph.validate(List.of());
        assertEquals(0, graph.size());
        assertTrue(graph.topologicalOrder().isEmpty());
    }

    @Test
    void definitionOrderIsStable() {
        ResearchGraph g1 = ResearchGraph.validate(List.of(def("a", 10), def("b", 10)));
        ResearchGraph g2 = ResearchGraph.validate(List.of(def("b", 10), def("a", 10)));
        assertEquals(List.of(A, B), g1.topologicalOrder());
        assertEquals(List.of(B, A), g2.topologicalOrder());
    }
}
