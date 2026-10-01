package com.nexus.echoes.research;

import net.minecraft.resources.ResourceLocation;

/**
 * Pure gating decisions (ADR-010). Given the player's state and the loaded
 * graph, answers "may this player craft/place/use this?".
 *
 * <p>Forge has no clean per-player recipe hook ({@code ItemCraftedEvent}
 * fires post-hoc from {@code ResultSlot}), so the <em>rule</em> lives here —
 * fully unit-testable — while the <em>enforcement</em> (void-on-craft,
 * cancel-on-place, deny-on-use) lives in the MC adapter, all server-side.
 */
public final class ResearchGating {

    private ResearchGating() {
    }

    /** Technology required to keep a crafted item, or {@code null} if ungated. */
    public static ResourceLocation technologyForCraftedItem(ResourceLocation itemId) {
        return ResearchTechnologies.CRAFT_GATE.get(itemId);
    }

    /** Technology required to place a block, or {@code null} if ungated. */
    public static ResourceLocation technologyForPlacedBlock(ResourceLocation blockId) {
        return ResearchTechnologies.PLACE_GATE.get(blockId);
    }

    /** May the player keep the crafted item? */
    public static boolean canCraft(ResearchGraph graph, PlayerResearchState state,
                                   ResourceLocation itemId) {
        ResourceLocation tech = technologyForCraftedItem(itemId);
        return tech == null || ResearchService.isUnlocked(graph, state, tech);
    }

    /** May the player place the block? */
    public static boolean canPlace(ResearchGraph graph, PlayerResearchState state,
                                   ResourceLocation blockId) {
        ResourceLocation tech = technologyForPlacedBlock(blockId);
        return tech == null || ResearchService.isUnlocked(graph, state, tech);
    }

    /** May the player operate the machine block (open its GUI)? */
    public static boolean canUseMachine(ResearchGraph graph, PlayerResearchState state,
                                        ResourceLocation blockId) {
        return canPlace(graph, state, blockId);
    }
}
