package com.nexus.echoes.research.network;

import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: the client requests completion of a research (GUI button).
 *
 * <p>Never trusted: the server re-validates prerequisites, cost and
 * duplicates through {@link ResearchManager#tryComplete}, then syncs the
 * authoritative snapshot back.
 */
public record BuyResearchPacket(ResourceLocation researchId) {

    public static void encode(BuyResearchPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.researchId());
    }

    public static BuyResearchPacket decode(FriendlyByteBuf buf) {
        return new BuyResearchPacket(buf.readResourceLocation());
    }

    public static void handle(BuyResearchPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ResearchManager.tryComplete(player, packet.researchId());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
