package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.network.DiscoverySyncPacket;
import com.nexus.echoes.network.NexusNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side fan-out of the player's discovery snapshot.
 *
 * <p>Sent on login (a fresh client must see previously granted discoveries)
 * and after every grant. The packet is tiny (a handful of ids) and
 * infrequent — no per-tick traffic.
 */
public final class DiscoverySync {

    private DiscoverySync() {
    }

    public static void syncTo(ServerPlayer player) {
        HollowDiscoveryData data = HollowDiscoveryData.get(player.getServer());
        List<ResourceLocation> ids = new ArrayList<>();
        for (String raw : data.all(player.getUUID())) {
            ids.add(new ResourceLocation(raw));
        }
        NexusNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DiscoverySyncPacket(ids));
    }
}
