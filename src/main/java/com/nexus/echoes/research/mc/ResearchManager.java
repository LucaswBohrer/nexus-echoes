package com.nexus.echoes.research.mc;

import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchOutcome;
import com.nexus.echoes.research.ResearchService;
import com.nexus.echoes.research.ResearchSource;
import com.nexus.echoes.research.network.ResearchSyncPacket;
import com.nexus.echoes.network.NexusNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Server-side entry point for research operations (ADR-010).
 *
 * <p>Every mutation goes through here: load the graph, apply the pure
 * {@link ResearchService} rule to the player's state, mark dirty, sync the
 * snapshot to the client. Nothing here runs on the client — the client only
 * holds the last {@link ResearchSyncPacket}.
 */
public final class ResearchManager {

    private ResearchManager() {
    }

    public static ResearchGraph graph(MinecraftServer server) {
        ResearchGraph graph = ResearchDefinitionLoader.get();
        return graph != null ? graph : ResearchGraph.validate(java.util.List.of());
    }

    public static PlayerResearchState stateOf(ServerPlayer player) {
        return ResearchSavedData.get(player.server.overworld()).getOrCreate(player.getUUID());
    }

    /** Runs a mutation, persists it and syncs the client. Returns the outcome. */
    public static ResearchOutcome mutate(ServerPlayer player, Consumer<PlayerResearchState> op) {
        ResearchSavedData data = ResearchSavedData.get(player.server.overworld());
        op.accept(data.getOrCreate(player.getUUID()));
        data.setDirty();
        syncToClient(player);
        return ResearchOutcome.OK;
    }

    /** Grants points from a gameplay source (mining, crafting, …). */
    public static ResearchOutcome grantFromSource(ServerPlayer player, ResearchSource source) {
        ResearchSavedData data = ResearchSavedData.get(player.server.overworld());
        PlayerResearchState state = data.getOrCreate(player.getUUID());
        ResearchOutcome outcome = ResearchService.grantFromSource(state, source);
        if (outcome == ResearchOutcome.OK) {
            data.setDirty();
            syncToClient(player);
        }
        return outcome;
    }

    /** Attempts to complete a research for the player (GUI button / command). */
    public static ResearchOutcome tryComplete(ServerPlayer player, ResourceLocation researchId) {
        ResearchSavedData data = ResearchSavedData.get(player.server.overworld());
        PlayerResearchState state = data.getOrCreate(player.getUUID());
        ResearchOutcome outcome = ResearchService.completeResearch(graph(player.getServer()), state, researchId);
        if (outcome == ResearchOutcome.OK) {
            data.setDirty();
            syncToClient(player);
        }
        return outcome;
    }

    /** Pushes the player's current snapshot to their client. */
    public static void syncToClient(ServerPlayer player) {
        PlayerResearchState state = stateOf(player);
        NexusNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ResearchSyncPacket(state.points(), List.copyOf(state.completed())));
    }
}
