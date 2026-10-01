package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gating rules: locked technology denies craft/place/use, unlocked allows.
 * These are the pure decisions the server-side event handlers enforce.
 */
class ResearchGatingTest {

    private ResearchGraph graph;
    private PlayerResearchState locked;
    private PlayerResearchState unlocked;

    private static ResourceLocation item(String path) {
        return new ResourceLocation("nexus_echoes", path);
    }

    @BeforeEach
    void setup() {
        graph = ResearchGraph.validate(List.of(
                new ResearchDefinition(new ResourceLocation("nexus_echoes", "industrial_foundations"),
                        "F", "d", "c", 50, List.of(), List.of(ResearchTechnologies.CRUSHER)),
                new ResearchDefinition(new ResourceLocation("nexus_echoes", "kinetic_transmission"),
                        "T", "d", "c", 100, List.of(new ResourceLocation("nexus_echoes", "industrial_foundations")),
                        List.of(ResearchTechnologies.PROCESSOR)),
                new ResearchDefinition(new ResourceLocation("nexus_echoes", "advanced_processing"),
                        "A", "d", "c", 150,
                        List.of(new ResourceLocation("nexus_echoes", "kinetic_transmission")),
                        List.of(ResearchTechnologies.NEXUS_COMPONENT))));
        locked = new PlayerResearchState();
        unlocked = new PlayerResearchState();
        unlocked.addPoints(1000);
        ResearchService.completeResearch(graph, unlocked,
                new ResourceLocation("nexus_echoes", "industrial_foundations"));
        ResearchService.completeResearch(graph, unlocked,
                new ResourceLocation("nexus_echoes", "kinetic_transmission"));
        ResearchService.completeResearch(graph, unlocked,
                new ResourceLocation("nexus_echoes", "advanced_processing"));
    }

    @Test
    void lockedPlayerCannotCraftCrusher() {
        assertFalse(ResearchGating.canCraft(graph, locked, item("crusher")));
    }

    @Test
    void unlockedPlayerCanCraftCrusher() {
        assertTrue(ResearchGating.canCraft(graph, unlocked, item("crusher")));
    }

    @Test
    void partiallyProgressedPlayerBlockedAtNextTier() {
        PlayerResearchState mid = new PlayerResearchState();
        mid.addPoints(500);
        ResearchService.completeResearch(graph, mid,
                new ResourceLocation("nexus_echoes", "industrial_foundations"));
        assertTrue(ResearchGating.canCraft(graph, mid, item("crusher")));
        assertFalse(ResearchGating.canCraft(graph, mid, item("processor")));
        assertFalse(ResearchGating.canCraft(graph, mid, item("nexus_component")));
    }

    @Test
    void ungatedItemsAlwaysAllowed() {
        assertTrue(ResearchGating.canCraft(graph, locked, item("gear")));
        assertTrue(ResearchGating.canCraft(graph, locked, new ResourceLocation("minecraft", "stick")));
    }

    @Test
    void lockedPlayerCannotPlaceMachine() {
        assertFalse(ResearchGating.canPlace(graph, locked, item("crusher")));
        assertFalse(ResearchGating.canPlace(graph, locked, item("processor")));
    }

    @Test
    void unlockedPlayerCanPlaceMachine() {
        assertTrue(ResearchGating.canPlace(graph, unlocked, item("crusher")));
        assertTrue(ResearchGating.canPlace(graph, unlocked, item("processor")));
    }

    @Test
    void lockedPlayerCannotUseMachine() {
        assertFalse(ResearchGating.canUseMachine(graph, locked, item("crusher")));
    }

    @Test
    void unlockedPlayerCanUseMachine() {
        assertTrue(ResearchGating.canUseMachine(graph, unlocked, item("processor")));
    }

    @Test
    void technologyMappingIsStable() {
        assertEquals(ResearchTechnologies.CRUSHER,
                ResearchGating.technologyForCraftedItem(item("crusher")));
        assertEquals(ResearchTechnologies.PROCESSOR,
                ResearchGating.technologyForPlacedBlock(item("processor")));
        assertEquals(ResearchTechnologies.NEXUS_COMPONENT,
                ResearchGating.technologyForCraftedItem(item("nexus_component")));
        assertNull(ResearchGating.technologyForCraftedItem(item("gear")));
    }

    @Test
    void nullGraphOrStateDenies() {
        assertFalse(ResearchGating.canCraft(null, locked, item("crusher")));
        assertFalse(ResearchGating.canCraft(graph, null, item("crusher")));
    }
}
