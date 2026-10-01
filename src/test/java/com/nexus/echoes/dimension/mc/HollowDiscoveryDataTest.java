package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Discoveries are once-only and isolated per player UUID (Phase 5).
 *
 * <p>Covers: grant returns true only the first time, per-player isolation,
 * save/load round-trip, and the fixed first-entry discovery set.
 */
class HollowDiscoveryDataTest {

    @Test
    void grantIsOnceOnly() {
        HollowDiscoveryData data = HollowDiscoveryData.createEmpty();
        UUID player = UUID.randomUUID();
        assertTrue(data.grant(player, DiscoveryIds.ENTER_HOLLOW));
        assertFalse(data.grant(player, DiscoveryIds.ENTER_HOLLOW));
        assertTrue(data.has(player, DiscoveryIds.ENTER_HOLLOW));
        assertEquals(1, data.count(player));
    }

    @Test
    void discoveriesAreIsolatedPerPlayer() {
        HollowDiscoveryData data = HollowDiscoveryData.createEmpty();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        data.grant(alice, DiscoveryIds.OBELISK);
        assertTrue(data.has(alice, DiscoveryIds.OBELISK));
        assertFalse(data.has(bob, DiscoveryIds.OBELISK));
        assertEquals(0, data.count(bob));
    }

    @Test
    void distinctDiscoveriesAccumulate() {
        HollowDiscoveryData data = HollowDiscoveryData.createEmpty();
        UUID player = UUID.randomUUID();
        data.grant(player, DiscoveryIds.ENTER_HOLLOW);
        data.grant(player, DiscoveryIds.RUIN_FOUND);
        data.grant(player, DiscoveryIds.ANOMALY_SEEN);
        assertEquals(3, data.count(player));
    }

    @Test
    void saveLoadRoundTrip() {
        HollowDiscoveryData data = HollowDiscoveryData.createEmpty();
        UUID player = UUID.randomUUID();
        data.grant(player, DiscoveryIds.ENTER_HOLLOW);
        data.grant(player, DiscoveryIds.MEMORY_FRAGMENT);

        HollowDiscoveryData restored = HollowDiscoveryData.load(data.save(new CompoundTag()));
        assertTrue(restored.has(player, DiscoveryIds.ENTER_HOLLOW));
        assertTrue(restored.has(player, DiscoveryIds.MEMORY_FRAGMENT));
        assertEquals(2, restored.count(player));
        assertFalse(restored.grant(player, DiscoveryIds.ENTER_HOLLOW));
    }

    @Test
    void firstEntryDiscoveriesAreKnownIds() {
        for (ResourceLocation id : HollowDiscoveryData.firstEntryDiscoveries()) {
            assertTrue(id.getNamespace().equals("nexus_echoes"));
        }
        assertTrue(HollowDiscoveryData.firstEntryDiscoveries()
                .contains(DiscoveryIds.ENTER_HOLLOW));
    }
}
