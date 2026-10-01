package com.nexus.echoes.research.codex;

import com.nexus.echoes.research.PlayerResearchState;

import java.util.Objects;

/**
 * Pure codex visibility rule: an entry is readable when it has no research
 * requirement or the player completed the required research.
 */
public final class CodexVisibility {

    private CodexVisibility() {
    }

    public static boolean isVisible(CodexEntry entry, PlayerResearchState state) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(state, "state");
        return entry.requiredResearch() == null || state.isCompleted(entry.requiredResearch());
    }
}
