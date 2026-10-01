package com.nexus.echoes.energy;

import com.nexus.echoes.energy.api.EnergyType;
import com.nexus.echoes.energy.api.INexusEnergy;

import java.util.HashMap;
import java.util.Map;

/**
 * Reference {@link INexusEnergy} implementation: simple typed integer storage.
 *
 * <p>Deliberately free of Minecraft classes so energy semantics are unit-testable
 * as plain Java (see {@code src/test}). NBT persistence lives in
 * {@link EnergyNbtHelper}.
 */
public class NexusEnergyStorage implements INexusEnergy {

    protected final EnergyType type;
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public NexusEnergyStorage(EnergyType type, int capacity, int maxTransfer) {
        this(type, capacity, maxTransfer, maxTransfer);
    }

    public NexusEnergyStorage(EnergyType type, int capacity, int maxReceive, int maxExtract) {
        if (type == null) {
            throw new IllegalArgumentException("EnergyType must not be null");
        }
        this.type = type;
        this.capacity = Math.max(0, capacity);
        this.maxReceive = Math.max(0, maxReceive);
        this.maxExtract = Math.max(0, maxExtract);
    }

    @Override
    public EnergyType getEnergyType() {
        return type;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) {
            return 0;
        }
        int accepted = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
        if (accepted > 0 && !simulate) {
            energy += accepted;
            onEnergyChanged();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
        if (extracted > 0 && !simulate) {
            energy -= extracted;
            onEnergyChanged();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    /** Direct setter for loading / creative sources. Still fires the change hook. */
    public void setEnergyStored(int energy) {
        int clamped = Math.max(0, Math.min(capacity, energy));
        if (clamped != this.energy) {
            this.energy = clamped;
            onEnergyChanged();
        }
    }

    /** Serializes to a plain map (key → int) so tests stay Minecraft-free. */
    public Map<String, Integer> serialize() {
        Map<String, Integer> map = new HashMap<>();
        map.put("energy", energy);
        return map;
    }

    public void deserialize(Map<String, Integer> map) {
        setEnergyStored(map.getOrDefault("energy", 0));
    }

    protected void onEnergyChanged() {
    }
}
