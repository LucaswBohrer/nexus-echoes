package com.nexus.echoes.network;

import com.nexus.echoes.NexusEchoes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Versioned network channel for NEXUS.
 *
 * <p>Phase 1 sync rides on vanilla mechanisms ({@code ContainerData} for GUIs,
 * block-entity update packets for world display) — this channel exists so
 * Phase 2+ packets (C2S GUI buttons, kinetic graph deltas, …) have a home.
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
        // Phase 2+: CHANNEL.registerMessage(id(), XxxPacket.class, ...);
        nextId = 0; // keeps the id allocator honest even with zero packets
    }

    @SuppressWarnings("unused")
    private static int id() {
        return nextId++;
    }
}
