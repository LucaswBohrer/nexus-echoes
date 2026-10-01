package com.nexus.echoes.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Per-player research state: the only thing the research domain mutates.
 *
 * <p>Integer points only — no floating-point accumulation. The state itself
 * enforces its invariants (no negative balances, no duplicate completions);
 * rule evaluation (prerequisites, costs) lives in {@link ResearchService}.
 * Serialization is explicit NBT so the same code runs in-game and in tests.
 */
public final class PlayerResearchState {

    private int points;
    private final Set<ResourceLocation> completed = new LinkedHashSet<>();
    private final Set<String> claimedSources = new LinkedHashSet<>();

    public PlayerResearchState() {
    }

    public int points() {
        return points;
    }

    /** Completed research ids, in completion order. */
    public Set<ResourceLocation> completed() {
        return Collections.unmodifiableSet(completed);
    }

    /** Once-only source ids already claimed by this player. */
    public Set<String> claimedSources() {
        return Collections.unmodifiableSet(claimedSources);
    }

    public boolean isCompleted(ResourceLocation id) {
        return completed.contains(id);
    }

    /**
     * Adds research points. Non-positive amounts are rejected.
     *
     * @return {@code false} if {@code amount <= 0} (state unchanged)
     */
    public boolean addPoints(int amount) {
        if (amount <= 0) {
            return false;
        }
        points = Math.addExact(points, amount);
        return true;
    }

    /**
     * Spends points if the balance covers the cost. Never goes negative.
     *
     * @return {@code false} if {@code cost <= 0} or balance insufficient
     */
    public boolean spendPoints(int cost) {
        if (cost <= 0 || points < cost) {
            return false;
        }
        points -= cost;
        return true;
    }

    /** Records a completion. Returns {@code false} if already completed. */
    public boolean markCompleted(ResourceLocation id) {
        return completed.add(id);
    }

    /** Claims a once-only source. Returns {@code false} if already claimed. */
    public boolean claimSource(String sourceId) {
        return claimedSources.add(sourceId);
    }

    // ------------------------------------------------------------- NBT

    private static final String TAG_POINTS = "Points";
    private static final String TAG_COMPLETED = "Completed";
    private static final String TAG_CLAIMED = "ClaimedSources";

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_POINTS, points);
        ListTag done = new ListTag();
        for (ResourceLocation id : completed) {
            done.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_COMPLETED, done);
        ListTag claimed = new ListTag();
        for (String s : claimedSources) {
            claimed.add(StringTag.valueOf(s));
        }
        tag.put(TAG_CLAIMED, claimed);
        return tag;
    }

    /**
     * Reads state defensively: missing fields default to empty, negative
     * points clamp to zero, malformed ids are skipped (never crash a save).
     */
    public static PlayerResearchState fromNbt(CompoundTag tag) {
        PlayerResearchState state = new PlayerResearchState();
        if (tag == null) {
            return state;
        }
        state.points = Math.max(0, tag.getInt(TAG_POINTS));
        ListTag done = tag.getList(TAG_COMPLETED, Tag.TAG_STRING);
        for (int i = 0; i < done.size(); i++) {
            try {
                state.completed.add(new ResourceLocation(done.getString(i)));
            } catch (Exception ignored) {
                // malformed id in save data: skip, keep the rest
            }
        }
        ListTag claimed = tag.getList(TAG_CLAIMED, Tag.TAG_STRING);
        for (int i = 0; i < claimed.size(); i++) {
            state.claimedSources.add(claimed.getString(i));
        }
        return state;
    }
}
