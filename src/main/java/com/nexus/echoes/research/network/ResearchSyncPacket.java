package com.nexus.echoes.research.network;

import com.nexus.echoes.research.client.ResearchClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: authoritative research snapshot for one player.
 *
 * <p>Sent on login and after every server-side mutation. The client treats
 * this as read-only display data — it never grants, spends or completes.
 */
public record ResearchSyncPacket(int points, List<ResourceLocation> completed) {

    public static void encode(ResearchSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.points());
        buf.writeInt(packet.completed().size());
        for (ResourceLocation id : packet.completed()) {
            buf.writeResourceLocation(id);
        }
    }

    public static ResearchSyncPacket decode(FriendlyByteBuf buf) {
        int points = buf.readInt();
        int count = buf.readInt();
        List<ResourceLocation> completed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            completed.add(buf.readResourceLocation());
        }
        return new ResearchSyncPacket(points, List.copyOf(completed));
    }

    public static void handle(ResearchSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ResearchClientState.update(packet.points(), packet.completed()));
        ctx.get().setPacketHandled(true);
    }
}
