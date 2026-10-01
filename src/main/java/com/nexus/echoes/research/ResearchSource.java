package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;

/**
 * One gameplay source of research points.
 *
 * <p>Sources are how points enter the economy; the service decides whether a
 * grant applies. {@code oncePerPlayer} sources (first-time discoveries) are
 * tracked in {@link PlayerResearchState#claimedSources()} so they can never
 * be farmed.
 *
 * @param id            stable id, e.g. {@code discover_nexus_ore}
 * @param points        points granted per trigger, must be {@code > 0}
 * @param oncePerPlayer if {@code true}, granted at most once per player
 */
public record ResearchSource(String id, int points, boolean oncePerPlayer) {

    public ResearchSource {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("research source id must not be blank");
        }
        if (points <= 0) {
            throw new IllegalArgumentException("research source points must be > 0: " + id);
        }
    }
}
