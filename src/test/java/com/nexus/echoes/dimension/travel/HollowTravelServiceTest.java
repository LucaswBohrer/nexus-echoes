package com.nexus.echoes.dimension.travel;

import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.ResearchDefinition;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchService;
import com.nexus.echoes.research.ResearchTechnologies;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hollow travel is gated on research and lands on safe ground (Phase 5).
 *
 * <p>Covers: access denial without {@code hollow_access}, access with it,
 * safe-spawn selection over a fake height field, and rejection of fully
 * obstructed columns.
 */
class HollowTravelServiceTest {

    private static final ResourceLocation TECH =
            new ResourceLocation("nexus_echoes", "hollow_access");

    private ResearchGraph graphWithHollowAccess() {
        return ResearchGraph.validate(List.of(
                new ResearchDefinition(
                        new ResourceLocation("nexus_echoes", "dimensional_resonance"),
                        "Dimensional Resonance", "d", "theoretical", 250,
                        List.of(), List.of(TECH))));
    }

    @Test
    void travelDeniedWithoutResearch() {
        ResearchGraph graph = graphWithHollowAccess();
        PlayerResearchState state = new PlayerResearchState();
        assertFalse(HollowTravelService.canTravel(graph, state));
    }

    @Test
    void travelAllowedAfterCompletingResearch() {
        ResearchGraph graph = graphWithHollowAccess();
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(1000);
        ResearchService.completeResearch(graph, state,
                new ResourceLocation("nexus_echoes", "dimensional_resonance"));
        assertTrue(HollowTravelService.canTravel(graph, state));
    }

    @Test
    void technologyIdMatchesGate() {
        assertEquals(ResearchTechnologies.HOLLOW_ACCESS, TECH);
    }

    @Test
    void safeSpawnPicksClearColumn() {
        HollowTravelService.HeightQuery flat = new HollowTravelService.HeightQuery() {
            @Override
            public int surfaceY(int x, int z) {
                return 64;
            }

            @Override
            public boolean isObstructed(int x, int y, int z) {
                return false;
            }
        };
        Optional<HollowTravelService.SafeSpawn> spawn =
                HollowTravelService.findSafeSpawn(0, 0, flat);
        assertTrue(spawn.isPresent());
        assertEquals(65, spawn.get().y());
    }

    @Test
    void safeSpawnRejectsObstructedColumns() {
        HollowTravelService.HeightQuery buried = new HollowTravelService.HeightQuery() {
            @Override
            public int surfaceY(int x, int z) {
                return 200;
            }

            @Override
            public boolean isObstructed(int x, int y, int z) {
                return true;
            }
        };
        assertTrue(HollowTravelService.findSafeSpawn(0, 0, buried).isEmpty());
    }
}
