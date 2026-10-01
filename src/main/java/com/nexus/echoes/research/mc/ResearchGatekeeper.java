package com.nexus.echoes.research.mc;

import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.ResearchGating;
import com.nexus.echoes.research.ResearchGraph;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Thin Minecraft glue over the pure {@link ResearchGating} rules: resolves the
 * player's state and the loaded graph, formats denial messages. All calls are
 * server-side; the pure rule stays unit-testable.
 */
public final class ResearchGatekeeper {

    private ResearchGatekeeper() {
    }

    /** May the player keep a crafted item? Sends a denial message when not. */
    public static boolean checkCraft(ServerPlayer player, ResourceLocation itemId) {
        ResourceLocation tech = ResearchGating.technologyForCraftedItem(itemId);
        if (tech == null) {
            return true;
        }
        ResearchGraph graph = ResearchManager.graph(player.getServer());
        PlayerResearchState state = ResearchManager.stateOf(player);
        if (ResearchGating.canCraft(graph, state, itemId)) {
            return true;
        }
        deny(player, tech);
        return false;
    }

    /** May the player place the block? */
    public static boolean checkPlace(ServerPlayer player, ResourceLocation blockId) {
        ResourceLocation tech = ResearchGating.technologyForPlacedBlock(blockId);
        if (tech == null) {
            return true;
        }
        ResearchGraph graph = ResearchManager.graph(player.getServer());
        PlayerResearchState state = ResearchManager.stateOf(player);
        if (ResearchGating.canPlace(graph, state, blockId)) {
            return true;
        }
        deny(player, tech);
        return false;
    }

    /** May the player operate (open the GUI of) the machine block? */
    public static boolean checkUseMachine(ServerPlayer player, ResourceLocation blockId) {
        return checkPlace(player, blockId);
    }

    private static void deny(ServerPlayer player, ResourceLocation technology) {
        ResearchDefinitionView view = ResearchDefinitionView.forTechnology(technology);
        player.displayClientMessage(Component.translatable(
                "message.nexus_echoes.research_locked",
                view.describe()), true);
    }

    /**
     * Human description of which research unlocks a technology, resolved from
     * the loaded definitions (falls back to the raw id when unknown).
     */
    private record ResearchDefinitionView(String describe) {
        static ResearchDefinitionView forTechnology(ResourceLocation technology) {
            ResearchGraph graph = ResearchDefinitionLoader.get();
            if (graph != null) {
                for (var def : graph.all()) {
                    if (def.unlocks().contains(technology)) {
                        return new ResearchDefinitionView(def.title());
                    }
                }
            }
            return new ResearchDefinitionView(technology.toString());
        }
    }
}
