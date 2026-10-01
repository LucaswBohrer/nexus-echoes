package com.nexus.echoes.research.codex;

import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import com.nexus.echoes.research.PlayerResearchState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Codex entries gated on Hollow discoveries stay hidden until granted
 * (Phase 5 progressive codex).
 */
class CodexDiscoveryGateTest {

    private CodexEntry gatedEntry() {
        return new CodexEntry(
                new ResourceLocation("nexus_echoes", "obelisks"),
                "Obelisks", "exploration",
                List.of("The obelisks are the way back."),
                null, DiscoveryIds.OBELISK);
    }

    @Test
    void hiddenWithoutDiscovery() {
        assertFalse(CodexVisibility.isVisible(gatedEntry(),
                new PlayerResearchState(), Set.of()));
    }

    @Test
    void visibleAfterDiscoveryGranted() {
        assertTrue(CodexVisibility.isVisible(gatedEntry(),
                new PlayerResearchState(), Set.of(DiscoveryIds.OBELISK)));
    }

    @Test
    void otherDiscoveryDoesNotUnlock() {
        assertFalse(CodexVisibility.isVisible(gatedEntry(),
                new PlayerResearchState(), Set.of(DiscoveryIds.RUIN_FOUND)));
    }

    @Test
    void researchGateStillApplies() {
        CodexEntry entry = new CodexEntry(
                new ResourceLocation("nexus_echoes", "the_hollow"),
                "The Hollow", "exploration",
                List.of("Past the charged spire..."),
                new ResourceLocation("nexus_echoes", "dimensional_resonance"),
                DiscoveryIds.ENTER_HOLLOW);
        PlayerResearchState state = new PlayerResearchState();
        assertFalse(CodexVisibility.isVisible(entry, state,
                Set.of(DiscoveryIds.ENTER_HOLLOW)));
    }

    @Test
    void ungatedEntriesUnaffected() {
        CodexEntry entry = new CodexEntry(
                new ResourceLocation("nexus_echoes", "welcome"),
                "Welcome", "introduction",
                List.of("Hello."),
                (ResourceLocation) null);
        assertTrue(CodexVisibility.isVisible(entry, new PlayerResearchState()));
    }
}
