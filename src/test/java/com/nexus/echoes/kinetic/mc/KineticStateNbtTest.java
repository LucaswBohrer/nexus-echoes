package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.NodeState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kinetic state survives save/load (Phase 3, §28): write → read round-trips
 * the exact state, and a missing/corrupt tag falls back safely.
 */
class KineticStateNbtTest {

    private static final BlockPos POS = new BlockPos(1, 64, -3);
    private static final NodeState FALLBACK = NodeState.of(
            Rpm.ZERO, Torque.ZERO, RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);

    @Test
    void roundTripPreservesState() {
        NodeState original = NodeState.of(
                Rpm.of(240), Torque.ofNewtonMeters(12.5),
                RotationDirection.COUNTERCLOCKWISE, NodeStatus.UNDERPOWERED);
        CompoundTag tag = new CompoundTag();
        KineticStateNbt.write(tag, original);

        NodeState read = KineticStateNbt.read(tag, POS, FALLBACK);
        assertEquals(original, read);
    }

    @Test
    void missingTagReturnsFallback() {
        NodeState read = KineticStateNbt.read(new CompoundTag(), POS, FALLBACK);
        assertEquals(FALLBACK, read);
    }

    @Test
    void corruptTagReturnsFallback() {
        CompoundTag tag = new CompoundTag();
        CompoundTag bad = new CompoundTag();
        bad.putString("status", "NOT_A_STATUS");
        bad.putDouble("rpm", 120);
        bad.putDouble("torque", 5);
        bad.putString("direction", "CLOCKWISE");
        tag.put("kineticState", bad);

        NodeState read = KineticStateNbt.read(tag, POS, FALLBACK);
        assertEquals(FALLBACK, read);
    }
}
