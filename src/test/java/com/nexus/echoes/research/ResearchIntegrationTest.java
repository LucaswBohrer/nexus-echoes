package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full progression scenario (ADR-010):
 *
 * <pre>
 * new player → ore/sources → industrial_foundations → kinetic_transmission
 *   → advanced_processing → dimensional_resonance → hollow_access unlock
 * </pre>
 *
 * plus: player B isolation throughout, and a save/load round-trip mid-run.
 */
class ResearchIntegrationTest {

    private ResearchGraph graph;

    private static ResourceLocation id(String path) {
        return new ResourceLocation("nexus_echoes", path);
    }

    @BeforeEach
    void setup() {
        graph = ResearchGraph.validate(List.of(
                new ResearchDefinition(id("industrial_foundations"), "Industrial Foundations", "d", "industrial",
                        50, List.of(), List.of(ResearchTechnologies.CRUSHER)),
                new ResearchDefinition(id("kinetic_transmission"), "Kinetic Transmission", "d", "kinetic",
                        100, List.of(id("industrial_foundations")), List.of(ResearchTechnologies.PROCESSOR)),
                new ResearchDefinition(id("advanced_processing"), "Advanced Processing", "d", "industrial",
                        150, List.of(id("kinetic_transmission")), List.of(ResearchTechnologies.NEXUS_COMPONENT)),
                new ResearchDefinition(id("dimensional_resonance"), "Dimensional Resonance", "d", "theoretical",
                        250, List.of(id("advanced_processing")), List.of(ResearchTechnologies.HOLLOW_ACCESS))));
    }

    private void mineOre(PlayerResearchState state, int times) {
        for (int i = 0; i < times; i++) {
            ResearchService.grantFromSource(state, ResearchSources.DISCOVER_NEXUS_ORE);
            ResearchService.grantFromSource(state, ResearchSources.MINE_NEXUS_ORE);
        }
    }

    @Test
    void fullProgressionFromZeroToDimensionalResonance() {
        PlayerResearchState player = new PlayerResearchState();

        // locked at the start: no technology usable
        assertFalse(ResearchGating.canCraft(graph, player, id("crusher")));
        assertFalse(ResearchService.isUnlocked(graph, player, ResearchTechnologies.HOLLOW_ACCESS));

        // earn: first ore is a discovery, then the steady trickle + first gear
        mineOre(player, 1);
        assertEquals(12, player.points()); // 10 discovery + 2 mining
        ResearchService.grantFromSource(player, ResearchSources.CRAFT_GEAR);
        assertEquals(22, player.points());
        mineOre(player, 14); // +28, discovery already claimed
        assertEquals(50, player.points());

        // tier 1: industrial foundations → crusher usable
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, player, id("industrial_foundations")));
        assertTrue(ResearchGating.canCraft(graph, player, id("crusher")));
        assertTrue(ResearchGating.canPlace(graph, player, id("crusher")));
        assertTrue(ResearchGating.canUseMachine(graph, player, id("crusher")));
        assertFalse(ResearchGating.canCraft(graph, player, id("processor")));

        // tier 2: kinetic transmission → processor usable
        mineOre(player, 50); // +100
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, player, id("kinetic_transmission")));
        assertTrue(ResearchGating.canCraft(graph, player, id("processor")));

        // tier 3: advanced processing → component craftable
        mineOre(player, 75); // +150
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, player, id("advanced_processing")));
        assertTrue(ResearchGating.canCraft(graph, player, id("nexus_component")));

        // tier 4: dimensional resonance → hollow_access unlock exists, no dimension built
        mineOre(player, 125); // +250
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, player, id("dimensional_resonance")));
        assertTrue(ResearchService.isUnlocked(graph, player, ResearchTechnologies.HOLLOW_ACCESS));

        assertEquals(4, player.completed().size());
    }

    @Test
    void progressionSurvivesSaveLoadMidRun() {
        PlayerResearchState player = new PlayerResearchState();
        mineOre(player, 20); // 10 discovery + 40 mining = 50
        ResearchService.completeResearch(graph, player, id("industrial_foundations"));

        // simulated restart: serialize everything, load fresh
        PlayerResearchState reloaded = PlayerResearchState.fromNbt(player.toNbt());

        assertTrue(reloaded.isCompleted(id("industrial_foundations")));
        assertTrue(ResearchGating.canCraft(graph, reloaded, id("crusher")));
        // discovery sources stay claimed: no re-farming the bonus
        assertEquals(ResearchOutcome.ALREADY_COMPLETED,
                ResearchService.grantFromSource(reloaded, ResearchSources.DISCOVER_NEXUS_ORE));
        // progression continues from the loaded state
        mineOre(reloaded, 50);
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, reloaded, id("kinetic_transmission")));
    }

    @Test
    void playerBIsolationThroughWholeRun() {
        PlayerResearchState a = new PlayerResearchState();
        PlayerResearchState b = new PlayerResearchState();

        mineOre(a, 20); // 10 discovery + 40 mining = 50
        ResearchService.completeResearch(graph, a, id("industrial_foundations"));

        assertTrue(ResearchGating.canCraft(graph, a, id("crusher")));
        assertFalse(ResearchGating.canCraft(graph, b, id("crusher")));
        assertEquals(0, b.points());
        assertTrue(b.completed().isEmpty());

        // B earns independently and is exactly one tier behind
        mineOre(b, 20);
        assertEquals(ResearchOutcome.OK,
                ResearchService.completeResearch(graph, b, id("industrial_foundations")));
        assertTrue(ResearchGating.canCraft(graph, b, id("crusher")));
        assertFalse(ResearchGating.canCraft(graph, b, id("processor")));
    }

    @Test
    void cannotSkipTiers() {
        PlayerResearchState player = new PlayerResearchState();
        player.addPoints(10000);
        assertEquals(ResearchOutcome.MISSING_PREREQUISITE,
                ResearchService.completeResearch(graph, player, id("dimensional_resonance")));
        assertEquals(ResearchOutcome.MISSING_PREREQUISITE,
                ResearchService.completeResearch(graph, player, id("advanced_processing")));
        assertFalse(ResearchService.isUnlocked(graph, player, ResearchTechnologies.HOLLOW_ACCESS));
    }

    @Test
    void economyTotalsAreDeterministic() {
        // 1 discovery + N mining ticks + 1 gear
        PlayerResearchState player = new PlayerResearchState();
        ResearchService.grantFromSource(player, ResearchSources.DISCOVER_NEXUS_ORE);
        ResearchService.grantFromSource(player, ResearchSources.CRAFT_GEAR);
        for (int i = 0; i < 7; i++) {
            ResearchService.grantFromSource(player, ResearchSources.MINE_NEXUS_ORE);
        }
        assertEquals(10 + 10 + 7 * 2, player.points());
    }
}
