package com.nexus.echoes.dimension.discovery;

import com.nexus.echoes.NexusEchoes;
import net.minecraft.resources.ResourceLocation;

/**
 * Stable discovery identifiers for The Hollow (ADR-011).
 *
 * <p>Discoveries are per-player, once-only unlocks stored in
 * {@code HollowDiscoveryData} and synced to the client. They gate Codex
 * entries ({@code requiredDiscovery}) and grant research points through the
 * existing {@link com.nexus.echoes.research.ResearchSource} abstraction —
 * there is no second progression system.
 */
public final class DiscoveryIds {

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NexusEchoes.MOD_ID, path);
    }

    /** First arrival in The Hollow. */
    public static final ResourceLocation ENTER_HOLLOW = id("enter_hollow");
    /** Used a hollow obelisk (the return landmark). */
    public static final ResourceLocation OBELISK = id("obelisk");
    /** Found a ruin, vault or fracture spire. */
    public static final ResourceLocation RUIN_FOUND = id("ruin_found");
    /** Stood inside an active anomaly. */
    public static final ResourceLocation ANOMALY_SEEN = id("anomaly_seen");
    /** Reached the Deep Hollow biome. */
    public static final ResourceLocation DEEP_HOLLOW = id("deep_hollow");
    /** First analyzed a memory fragment. */
    public static final ResourceLocation MEMORY_FRAGMENT = id("memory_fragment");

    private DiscoveryIds() {
    }
}
