package com.nexus.echoes.energy;

import net.minecraft.nbt.CompoundTag;

/**
 * Bridges {@link NexusEnergyStorage} (pure Java) and Minecraft NBT.
 * Kept separate so energy semantics remain unit-testable without Minecraft.
 */
public final class EnergyNbtHelper {

    private static final String KEY = "nexusEnergy";

    private EnergyNbtHelper() {
    }

    public static void writeTo(CompoundTag tag, NexusEnergyStorage storage) {
        tag.putInt(KEY, storage.getEnergyStored());
    }

    public static void readFrom(CompoundTag tag, NexusEnergyStorage storage) {
        storage.setEnergyStored(tag.contains(KEY) ? tag.getInt(KEY) : 0);
    }
}
