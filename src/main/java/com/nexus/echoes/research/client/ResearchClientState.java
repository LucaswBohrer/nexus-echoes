package com.nexus.echoes.research.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side read-only snapshot of the player's research state.
 *
 * <p>Updated exclusively by {@code ResearchSyncPacket}. The client never
 * mutates progression — buy/complete requests go through C2S packets and the
 * server answers with a fresh snapshot.
 */
@OnlyIn(Dist.CLIENT)
public final class ResearchClientState {

    private static int points;
    private static final Set<ResourceLocation> completed = new LinkedHashSet<>();
    private static long version;

    private ResearchClientState() {
    }

    public static synchronized void update(int newPoints, List<ResourceLocation> newCompleted) {
        points = Math.max(0, newPoints);
        completed.clear();
        completed.addAll(newCompleted);
        version++;
    }

    public static synchronized int points() {
        return points;
    }

    public static synchronized Set<ResourceLocation> completed() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(completed));
    }

    public static synchronized boolean isCompleted(ResourceLocation id) {
        return completed.contains(id);
    }

    /** Bumps on every snapshot; screens poll it to refresh. */
    public static synchronized long version() {
        return version;
    }
}
