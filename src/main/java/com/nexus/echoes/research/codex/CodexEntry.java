package com.nexus.echoes.research.codex;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * One Codex entry: documentation, technology notes and lore fragments.
 *
 * <p>The Codex is data-driven ({@code data/nexus_echoes/codex/*.json}). It is
 * not a recipe book — entries teach systems, record discoveries and drip the
 * first layer of lore ("what is the Nexus?") without answering it. Phase 4
 * ships a small foundation set; the architecture supports arbitrary growth.
 *
 * @param id               stable namespaced id
 * @param title            display title
 * @param category         grouping, e.g. {@code introduction}, {@code lore}
 * @param content          body lines shown in order
 * @param requiredResearch research that must be completed to read this entry,
 *                         or {@code null} for always-visible entries
 */
public record CodexEntry(
        ResourceLocation id,
        String title,
        String category,
        List<String> content,
        ResourceLocation requiredResearch) {

    public CodexEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(content, "content");
        if (content.isEmpty()) {
            throw new IllegalArgumentException("codex entry must have content: " + id);
        }
        content = List.copyOf(content);
    }
}
