package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.anomaly.AnomalyInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Per-level persistent state for anomalies and anomaly wards.
 *
 * <p>World state — never static global state. Anomalies live only in The
 * Hollow; the data object exists per level but is only actively driven there.
 * Ward positions are recorded here (registered by {@code WardBlockEntity}
 * on load / unregistered on removal) so the anomaly manager can check them
 * without scanning the world.
 */
public class AnomalySavedData extends SavedData {

    private static final String KEY = "nexus_echoes_anomalies";

    private final List<AnomalyInstance> anomalies = new ArrayList<>();
    private final Set<BlockPos> wards = new HashSet<>();

    public static AnomalySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AnomalySavedData::load, AnomalySavedData::new, KEY);
    }

    // ----------------------------------------------------------------- access

    public List<AnomalyInstance> active() {
        return anomalies;
    }

    public void add(AnomalyInstance instance) {
        anomalies.add(instance);
        setDirty();
    }

    public void remove(AnomalyInstance instance) {
        anomalies.remove(instance);
        setDirty();
    }

    public void addWard(BlockPos pos) {
        if (wards.add(pos.immutable())) {
            setDirty();
        }
    }

    public void removeWard(BlockPos pos) {
        if (wards.remove(pos)) {
            setDirty();
        }
    }

    public Set<BlockPos> wards() {
        return wards;
    }

    /**
     * True when the given block position is protected by any registered ward.
     * Bounded by {@code wardRadius}; iterates registered wards only (sparse).
     */
    public boolean isWarded(BlockPos pos, int wardRadius) {
        long r2 = (long) wardRadius * wardRadius;
        for (BlockPos ward : wards) {
            if (ward.distSqr(pos) <= r2) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- persistence

    public static AnomalySavedData load(CompoundTag tag) {
        AnomalySavedData data = new AnomalySavedData();
        ListTag anomaliesTag = tag.getList("Anomalies", Tag.TAG_COMPOUND);
        for (int i = 0; i < anomaliesTag.size(); i++) {
            try {
                data.anomalies.add(AnomalyInstance.read(anomaliesTag.getCompound(i)));
            } catch (RuntimeException ignored) {
                // Corrupt/unknown definition entries are dropped on load.
            }
        }
        ListTag wardsTag = tag.getList("Wards", Tag.TAG_COMPOUND);
        for (int i = 0; i < wardsTag.size(); i++) {
            CompoundTag t = wardsTag.getCompound(i);
            data.wards.add(new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag anomaliesTag = new ListTag();
        for (AnomalyInstance anomaly : anomalies) {
            anomaliesTag.add(anomaly.write());
        }
        tag.put("Anomalies", anomaliesTag);
        ListTag wardsTag = new ListTag();
        for (BlockPos ward : wards) {
            CompoundTag t = new CompoundTag();
            t.putInt("X", ward.getX());
            t.putInt("Y", ward.getY());
            t.putInt("Z", ward.getZ());
            wardsTag.add(t);
        }
        tag.put("Wards", wardsTag);
        return tag;
    }

    /** For tests: rebuild without a level. */
    public static AnomalySavedData createEmpty() {
        return new AnomalySavedData();
    }

    @Override
    public String toString() {
        return "AnomalySavedData{anomalies=" + anomalies.size() + ", wards=" + wards.size() + '}';
    }
}
