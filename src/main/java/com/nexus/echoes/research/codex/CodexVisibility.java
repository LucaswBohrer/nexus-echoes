package com.nexus.echoes.research.codex;

import com.nexus.echoes.research.PlayerResearchState;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Set;

/**
 * Pure codex visibility rule: an entry is readable when every requirement it
 * declares is satisfied — research completion for {@code requiredResearch},
 * a granted Hollow discovery for {@code requiredDiscovery}.
 */
public final class CodexVisibility {

    private CodexVisibility() {
    }

    public static boolean isVisible(CodexEntry entry, PlayerResearchState state) {
        return isVisible(entry, state, Set.of());
    }

    public static boolean isVisible(CodexEntry entry, PlayerResearchState state,
                                    Set<ResourceLocation> discoveries) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(discoveries, "discoveries");
        if (entry.requiredResearch() != null && !state.isCompleted(entry.requiredResearch())) {
            return false;
        }
        return entry.requiredDiscovery() == null || discoveries.contains(entry.requiredDiscovery());
    }
}
