package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * One research entry: an instantaneous, purchasable technology unlock.
 *
 * <p>Phase 4 research has no active/in-progress timers: completing a research
 * validates prerequisites + cost atomically and records completion. Definitions
 * are data-driven ({@code data/nexus_echoes/research/*.json}); this record is
 * the validated in-memory form.
 *
 * @param id            stable namespaced id, e.g. {@code nexus_echoes:industrial_foundations}
 * @param title         display name (not an identifier)
 * @param description   short description shown in the GUI
 * @param category      free-form grouping tag, e.g. {@code industrial}
 * @param cost          research-point cost, must be {@code > 0}
 * @param prerequisites ids that must be completed first (may be empty)
 * @param unlocks       technology ids this research grants, e.g. {@code nexus_echoes:crusher}
 */
public record ResearchDefinition(
        ResourceLocation id,
        String title,
        String description,
        String category,
        int cost,
        List<ResourceLocation> prerequisites,
        List<ResourceLocation> unlocks) {

    public ResearchDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(prerequisites, "prerequisites");
        Objects.requireNonNull(unlocks, "unlocks");
        if (cost <= 0) {
            throw new IllegalArgumentException("research cost must be > 0: " + id);
        }
        prerequisites = List.copyOf(prerequisites);
        unlocks = List.copyOf(unlocks);
    }
}
