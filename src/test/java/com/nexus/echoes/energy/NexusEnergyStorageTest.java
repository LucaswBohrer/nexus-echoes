package com.nexus.echoes.energy;

import com.nexus.echoes.energy.api.EnergyType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NexusEnergyStorage} — pure Java, no Minecraft.
 * Covers the contract semantics from {@code INexusEnergy}.
 */
class NexusEnergyStorageTest {

    @Test
    void receiveClampsToFreeCapacity() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 10_000);
        assertEquals(1000, s.receiveEnergy(5000, false));
        assertEquals(1000, s.getEnergyStored());
        assertEquals(0, s.receiveEnergy(100, false));
    }

    @Test
    void receiveClampsToMaxReceive() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 10_000, 250);
        assertEquals(250, s.receiveEnergy(1000, false));
        assertEquals(250, s.getEnergyStored());
    }

    @Test
    void extractClampsToStored() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 10_000, 10_000);
        s.receiveEnergy(300, false);
        assertEquals(300, s.extractEnergy(9999, false));
        assertEquals(0, s.getEnergyStored());
        assertEquals(0, s.extractEnergy(10, false));
    }

    @Test
    void extractClampsToMaxExtract() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 10_000, 10_000, 40);
        s.receiveEnergy(1000, false);
        assertEquals(40, s.extractEnergy(5000, false));
        assertEquals(960, s.getEnergyStored());
    }

    @Test
    void simulateNeverChangesState() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 500);
        assertEquals(500, s.receiveEnergy(500, true));
        assertEquals(0, s.getEnergyStored());
        s.receiveEnergy(500, false);
        assertEquals(500, s.extractEnergy(500, true));
        assertEquals(500, s.getEnergyStored());
    }

    @Test
    void rejectsNonPositiveAmounts() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 500);
        assertEquals(0, s.receiveEnergy(0, false));
        assertEquals(0, s.receiveEnergy(-10, false));
        assertEquals(0, s.extractEnergy(-5, false));
    }

    @Test
    void canReceiveCanExtractFollowLimits() {
        NexusEnergyStorage noReceive = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 0, 100);
        assertFalse(noReceive.canReceive());
        assertEquals(0, noReceive.receiveEnergy(100, false));

        NexusEnergyStorage noExtract = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 100, 0);
        assertFalse(noExtract.canExtract());
        noExtract.receiveEnergy(500, false);
        assertEquals(0, noExtract.extractEnergy(100, false));
    }

    @Test
    void transferRequiresMatchingType() {
        NexusEnergyStorage a = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 500, 500);
        NexusEnergyStorage b = new NexusEnergyStorage(EnergyType.KINETIC, 1000, 500, 500);
        assertFalse(a.canTransferTo(b));
        NexusEnergyStorage c = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 0, 500);
        assertFalse(a.canTransferTo(c)); // c cannot receive
    }

    @Test
    void setEnergyStoredClamps() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 500);
        s.setEnergyStored(5000);
        assertEquals(1000, s.getEnergyStored());
        s.setEnergyStored(-50);
        assertEquals(0, s.getEnergyStored());
    }

    @Test
    void serializeDeserializeRoundTrip() {
        NexusEnergyStorage s = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 1000);
        s.receiveEnergy(777, false);
        Map<String, Integer> data = s.serialize();

        NexusEnergyStorage restored = new NexusEnergyStorage(EnergyType.RESONANT, 1000, 500);
        restored.deserialize(data);
        assertEquals(777, restored.getEnergyStored());
    }

    @Test
    void nullTypeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new NexusEnergyStorage(null, 1000, 500));
    }
}
