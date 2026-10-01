package com.nexus.echoes.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** NBT persistence: round-trips, defaults, malformed data, clone simulation. */
class ResearchPersistenceTest {

    private static PlayerResearchState populated() {
        PlayerResearchState state = new PlayerResearchState();
        state.addPoints(137);
        state.markCompleted(new ResourceLocation("nexus_echoes", "foundations"));
        state.markCompleted(new ResourceLocation("nexus_echoes", "transmission"));
        state.claimSource("discover_nexus_ore");
        return state;
    }

    @Test
    void roundTripPreservesEverything() {
        PlayerResearchState original = populated();
        PlayerResearchState loaded = PlayerResearchState.fromNbt(original.toNbt());
        assertEquals(137, loaded.points());
        assertEquals(original.completed(), loaded.completed());
        assertEquals(original.claimedSources(), loaded.claimedSources());
    }

    @Test
    void emptyStateRoundTrips() {
        PlayerResearchState loaded = PlayerResearchState.fromNbt(new PlayerResearchState().toNbt());
        assertEquals(0, loaded.points());
        assertTrue(loaded.completed().isEmpty());
        assertTrue(loaded.claimedSources().isEmpty());
    }

    @Test
    void missingFieldsDefaultToEmpty() {
        PlayerResearchState loaded = PlayerResearchState.fromNbt(new CompoundTag());
        assertEquals(0, loaded.points());
        assertTrue(loaded.completed().isEmpty());
        assertTrue(loaded.claimedSources().isEmpty());
    }

    @Test
    void nullTagYieldsEmptyState() {
        PlayerResearchState loaded = PlayerResearchState.fromNbt(null);
        assertEquals(0, loaded.points());
    }

    @Test
    void negativePointsClampToZero() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Points", -50);
        assertEquals(0, PlayerResearchState.fromNbt(tag).points());
    }

    @Test
    void malformedCompletedIdIsSkipped() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("nexus_echoes:foundations"));
        list.add(StringTag.valueOf("not a valid id!!!"));
        list.add(StringTag.valueOf("nexus_echoes:transmission"));
        tag.put("Completed", list);
        PlayerResearchState loaded = PlayerResearchState.fromNbt(tag);
        assertEquals(2, loaded.completed().size());
        assertTrue(loaded.isCompleted(new ResourceLocation("nexus_echoes", "foundations")));
        assertTrue(loaded.isCompleted(new ResourceLocation("nexus_echoes", "transmission")));
    }

    @Test
    void wrongListTypeIsIgnored() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Completed", 42); // not a list at all
        PlayerResearchState loaded = PlayerResearchState.fromNbt(tag);
        assertTrue(loaded.completed().isEmpty());
    }

    @Test
    void saveIsIdempotent() {
        PlayerResearchState state = populated();
        CompoundTag first = state.toNbt();
        PlayerResearchState reloaded = PlayerResearchState.fromNbt(first);
        assertEquals(first, reloaded.toNbt());
    }

    /**
     * Player clone/respawn simulation: the UUID-keyed row is serialized to
     * disk and deserialized for the "new" player object — progression must be
     * identical because the key never changes.
     */
    @Test
    void cloneRespawnKeepsProgression() {
        PlayerResearchState beforeDeath = populated();
        CompoundTag disk = beforeDeath.toNbt(); // what SavedData writes

        PlayerResearchState afterRespawn = PlayerResearchState.fromNbt(disk); // same UUID row
        assertEquals(beforeDeath.points(), afterRespawn.points());
        assertEquals(beforeDeath.completed(), afterRespawn.completed());
        assertEquals(beforeDeath.claimedSources(), afterRespawn.claimedSources());

        // once-only sources stay claimed after respawn: no double-dip
        assertFalse(afterRespawn.claimSource("discover_nexus_ore"));
    }

    @Test
    void logoutLoginEquivalent() {
        PlayerResearchState session1 = populated();
        CompoundTag saved = session1.toNbt();
        PlayerResearchState session2 = PlayerResearchState.fromNbt(saved);
        assertEquals(session1.toNbt(), session2.toNbt());
    }
}
