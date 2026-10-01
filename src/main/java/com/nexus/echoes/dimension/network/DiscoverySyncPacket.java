package com.nexus.echoes.dimension.network;

import com.nexus.echoes.dimension.client.DiscoveryClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: authoritative Hollow discovery snapshot for one player.
 *
 * <p>Mirrors {@code ResearchSyncPacket}: sent on login and after every
 * server-side discovery grant. The client treats it as read-only display
 * data for Codex visibility — discoveries are never granted client-side.
 */
public record DiscoverySyncPacket(List<ResourceLocation> discoveries) {

    public static void encode(DiscoverySyncPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.discoveries().size());
        for (ResourceLocation id : packet.discoveries()) {
            buf.writeResourceLocation(id);
        }
    }

    public static DiscoverySyncPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<ResourceLocation> discoveries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            discoveries.add(buf.readResourceLocation());
        }
        return new DiscoverySyncPacket(List.copyOf(discoveries));
    }

    public static void handle(DiscoverySyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DiscoveryClientState.update(packet.discoveries()));
        ctx.get().setPacketHandled(true);
    }
}
