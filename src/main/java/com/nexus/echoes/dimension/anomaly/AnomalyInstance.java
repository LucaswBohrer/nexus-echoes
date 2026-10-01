package com.nexus.echoes.dimension.anomaly;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

/**
 * One live anomaly: where, what, and when it ends.
 *
 * <p>Coordinates are plain ints — the domain does not depend on Minecraft
 * classes, so lifecycle rules stay unit-testable. The MC adapter converts to
 * and from {@code BlockPos}.
 */
public record AnomalyInstance(
        UUID instanceId,
        ResourceLocation definitionId,
        int x, int y, int z,
        long createdTick,
        long expiresTick,
        double intensity) {

    public AnomalyInstance {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(definitionId, "definitionId");
        if (expiresTick <= createdTick) {
            throw new IllegalArgumentException("anomaly must expire after creation");
        }
        if (Double.isNaN(intensity) || intensity < 0 || intensity > 1) {
            throw new IllegalArgumentException("intensity must be in [0,1]");
        }
    }

    public boolean isExpired(long nowTick) {
        return nowTick >= expiresTick;
    }

    /** Serializes to a fresh tag; the instance is the source of truth. */
    public net.minecraft.nbt.CompoundTag write() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putUUID("Id", instanceId);
        tag.putString("Definition", definitionId.toString());
        tag.putInt("X", x);
        tag.putInt("Y", y);
        tag.putInt("Z", z);
        tag.putLong("Created", createdTick);
        tag.putLong("Expires", expiresTick);
        tag.putDouble("Intensity", intensity);
        return tag;
    }

    /** Reads back what {@link #write()} wrote. */
    public static AnomalyInstance read(net.minecraft.nbt.CompoundTag tag) {
        return new AnomalyInstance(
                tag.getUUID("Id"),
                new ResourceLocation(tag.getString("Definition")),
                tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"),
                tag.getLong("Created"), tag.getLong("Expires"),
                tag.getDouble("Intensity"));
    }
}
