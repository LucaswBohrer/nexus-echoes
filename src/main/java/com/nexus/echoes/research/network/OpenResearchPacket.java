package com.nexus.echoes.research.network;

import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: the client asks for a fresh research snapshot (e.g. when the research
 * screen opens). The server answers with {@link ResearchSyncPacket}.
 */
public record OpenResearchPacket() {

    public static void encode(OpenResearchPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenResearchPacket decode(FriendlyByteBuf buf) {
        return new OpenResearchPacket();
    }

    public static void handle(OpenResearchPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ResearchManager.syncToClient(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
