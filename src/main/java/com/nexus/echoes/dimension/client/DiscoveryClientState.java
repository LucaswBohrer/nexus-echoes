package com.nexus.echoes.dimension.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side read-only snapshot of the player's Hollow discoveries.
 *
 * <p>Updated exclusively by {@code DiscoverySyncPacket}. Mirrors
 * {@code ResearchClientState}: the client never grants discoveries — it
 * only renders Codex entries that the server already authorized.
 */
@OnlyIn(Dist.CLIENT)
public final class DiscoveryClientState {

    private static final Set<ResourceLocation> discoveries = new LinkedHashSet<>();
    private static long version;

    private DiscoveryClientState() {
    }

    public static synchronized void update(List<ResourceLocation> newDiscoveries) {
        discoveries.clear();
        discoveries.addAll(newDiscoveries);
        version++;
    }

    public static synchronized Set<ResourceLocation> all() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(discoveries));
    }

    public static synchronized boolean has(ResourceLocation id) {
        return discoveries.contains(id);
    }

    /** Bumps on every snapshot; screens poll it to refresh. */
    public static synchronized long version() {
        return version;
    }
}
