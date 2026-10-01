package com.nexus.echoes.research.mc;

import com.nexus.echoes.research.PlayerResearchState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative per-player research storage (ADR-010).
 *
 * <p>One row per player UUID. UUID-keying means research survives death,
 * respawn/clone, dimension changes and logout/login with no special handling;
 * {@link SavedData} disk persistence covers server restarts. There is no
 * client-side copy of this data — clients only receive a sync snapshot.
 */
public class ResearchSavedData extends SavedData {

    public static final String DATA_ID = "nexus_echoes_research";

    private final Map<UUID, PlayerResearchState> states = new HashMap<>();

    public ResearchSavedData() {
    }

    public static ResearchSavedData get(ServerLevel overworld) {
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(ResearchSavedData::load, ResearchSavedData::new, DATA_ID);
    }

    /** Returns the player's state, creating an empty one on first access. */
    public PlayerResearchState getOrCreate(UUID playerId) {
        PlayerResearchState state = states.get(playerId);
        if (state == null) {
            state = new PlayerResearchState();
            states.put(playerId, state);
            setDirty();
        }
        return state;
    }

    /** Replaces a player's state wholesale (admin reset). */
    public void reset(UUID playerId) {
        states.put(playerId, new PlayerResearchState());
        setDirty();
    }

    // ------------------------------------------------------------- disk

    private static final String TAG_STATES = "States";

    public static ResearchSavedData load(CompoundTag tag) {
        ResearchSavedData data = new ResearchSavedData();
        ListTag list = tag.getList(TAG_STATES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                UUID id = entry.getUUID("Player");
                data.states.put(id, PlayerResearchState.fromNbt(entry.getCompound("State")));
            } catch (Exception ignored) {
                // corrupt row: skip it, keep the rest of the save
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerResearchState> e : states.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", e.getKey());
            entry.put("State", e.getValue().toNbt());
            list.add(entry);
        }
        tag.put(TAG_STATES, list);
        return tag;
    }
}
