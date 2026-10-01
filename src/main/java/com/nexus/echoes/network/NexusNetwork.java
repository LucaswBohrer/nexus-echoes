package com.nexus.echoes.network;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.dimension.network.DiscoverySyncPacket;
import com.nexus.echoes.research.network.BuyResearchPacket;
import com.nexus.echoes.research.network.OpenResearchPacket;
import com.nexus.echoes.research.network.ResearchSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Versioned network channel for NEXUS.
 *
 * <p>Phase 1 sync rides on vanilla mechanisms ({@code ContainerData} for GUIs,
 * block-entity update packets for world display). Phase 4 adds the first
 * custom packets: research open/buy (C2S) and the research snapshot (S2C).
 * Packet ids are append-only: never reuse an id.
 */
public final class NexusNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NexusEchoes.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextId;

    private NexusNetwork() {
    }

    public static void register() {
        nextId = 0;
        CHANNEL.registerMessage(id(), ResearchSyncPacket.class,
                ResearchSyncPacket::encode, ResearchSyncPacket::decode, ResearchSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id(), OpenResearchPacket.class,
                OpenResearchPacket::encode, OpenResearchPacket::decode, OpenResearchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id(), BuyResearchPacket.class,
                BuyResearchPacket::encode, BuyResearchPacket::decode, BuyResearchPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id(), DiscoverySyncPacket.class,
                DiscoverySyncPacket::encode, DiscoverySyncPacket::decode, DiscoverySyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    private static int id() {
        return nextId++;
    }
}
