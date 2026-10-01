package com.nexus.echoes.dimension.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player travel origins for Hollow trips (ADR-011).
 *
 * <p>When a player travels Overworld → Hollow, the departure point (spire
 * position) is recorded here keyed by UUID. The obelisk uses it as the
 * return target. Stored server-wide like research state: keyed by player,
 * independent of dimension load state.
 */
public class PlayerTravelData extends SavedData {

    private static final String KEY = "nexus_echoes_travel_origins";

    /** Immutable departure record. */
    public record Origin(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private final Map<UUID, Origin> origins = new HashMap<>();

    public static PlayerTravelData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(PlayerTravelData::load, PlayerTravelData::new, KEY);
    }

    public void record(UUID playerId, ResourceKey<Level> dimension, BlockPos pos) {
        origins.put(playerId, new Origin(dimension, pos.immutable()));
        setDirty();
    }

    public Origin originOf(UUID playerId) {
        return origins.get(playerId);
    }

    public void clear(UUID playerId) {
        if (origins.remove(playerId) != null) {
            setDirty();
        }
    }

    // ------------------------------------------------------------- persistence

    private static PlayerTravelData load(CompoundTag tag) {
        PlayerTravelData data = new PlayerTravelData();
        ListTag list = tag.getList("Origins", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            try {
                UUID id = t.getUUID("Player");
                ResourceKey<Level> dim = ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        new ResourceLocation(t.getString("Dimension")));
                BlockPos pos = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
                data.origins.put(id, new Origin(dim, pos));
            } catch (RuntimeException ignored) {
                // Drop malformed entries.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Origin> entry : origins.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Player", entry.getKey());
            t.putString("Dimension", entry.getValue().dimension().location().toString());
            BlockPos pos = entry.getValue().pos();
            t.putInt("X", pos.getX());
            t.putInt("Y", pos.getY());
            t.putInt("Z", pos.getZ());
            list.add(t);
        }
        tag.put("Origins", list);
        return tag;
    }

    /** For tests: rebuild without a server. */
    public static PlayerTravelData createEmpty() {
        return new PlayerTravelData();
    }
}
