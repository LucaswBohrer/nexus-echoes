package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.api.HollowDimensions;
import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player discovery flags for The Hollow (ADR-011).
 *
 * <p>Stored globally (server-level SavedData, like research) rather than in
 * the dimension, so discovery survives dimension unloads and is keyed by
 * player UUID — exactly like {@code ResearchSavedData}. One set of granted
 * discovery ids per player; {@link #grant} returns true only on first grant
 * so once-only rewards are trivially enforceable.
 */
public class HollowDiscoveryData extends SavedData {

    private static final String KEY = "nexus_echoes_hollow_discoveries";

    private final Map<UUID, Set<String>> discovered = new HashMap<>();

    public static HollowDiscoveryData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(HollowDiscoveryData::load, HollowDiscoveryData::new, KEY);
    }

    /** Grants a discovery; true only the first time. */
    public boolean grant(UUID playerId, ResourceLocation discoveryId) {
        Set<String> set = discovered.computeIfAbsent(playerId, k -> new HashSet<>());
        if (set.add(discoveryId.toString())) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean has(UUID playerId, ResourceLocation discoveryId) {
        Set<String> set = discovered.get(playerId);
        return set != null && set.contains(discoveryId.toString());
    }

    public Set<String> all(UUID playerId) {
        return Collections.unmodifiableSet(
                discovered.getOrDefault(playerId, Collections.emptySet()));
    }

    public int count(UUID playerId) {
        return discovered.getOrDefault(playerId, Collections.emptySet()).size();
    }

    // ------------------------------------------------------------- persistence

    public static HollowDiscoveryData load(CompoundTag tag) {
        HollowDiscoveryData data = new HollowDiscoveryData();
        CompoundTag players = tag.getCompound("Players");
        for (String uuidStr : players.getAllKeys()) {
            try {
                UUID id = UUID.fromString(uuidStr);
                Set<String> set = new HashSet<>();
                ListTag list = players.getList(uuidStr, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    set.add(list.getString(i));
                }
                data.discovered.put(id, set);
            } catch (IllegalArgumentException ignored) {
                // Drop malformed player entries.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> entry : discovered.entrySet()) {
            ListTag list = new ListTag();
            for (String id : entry.getValue()) {
                list.add(StringTag.valueOf(id));
            }
            players.put(entry.getKey().toString(), list);
        }
        tag.put("Players", players);
        return tag;
    }

    /** For tests: rebuild without a server. */
    public static HollowDiscoveryData createEmpty() {
        return new HollowDiscoveryData();
    }

    /** Discovery ids granted for first entry (ADR-011 §research integration). */
    public static Set<ResourceLocation> firstEntryDiscoveries() {
        return Set.of(DiscoveryIds.ENTER_HOLLOW);
    }

    /** The dimension this data is conceptually attached to. */
    public static ResourceLocation dimensionKey() {
        return HollowDimensions.HOLLOW_ID;
    }
}
