package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.NodeState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * NBT persistence for the last simulated {@link NodeState}. Shared by
 * {@link KineticBlockEntity} and the migrated Resonator so both persist the
 * same snapshot fields.
 */
public final class KineticStateNbt {

    private KineticStateNbt() {
    }

    public static void write(CompoundTag parent, NodeState state) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("rpm", state.rpm().value());
        tag.putDouble("torque", state.torque().newtonMeters());
        tag.putString("direction", state.direction().name());
        tag.putString("status", state.status().name());
        parent.put("kineticState", tag);
    }

    public static NodeState read(CompoundTag parent, BlockPos pos, NodeState fallback) {
        if (!parent.contains("kineticState")) {
            return fallback;
        }
        try {
            CompoundTag tag = parent.getCompound("kineticState");
            return NodeState.of(
                    Rpm.of(tag.getDouble("rpm")),
                    Torque.ofNewtonMeters(tag.getDouble("torque")),
                    RotationDirection.valueOf(tag.getString("direction")),
                    NodeStatus.valueOf(tag.getString("status")));
        } catch (IllegalArgumentException e) {
            return fallback; // corrupt entry: keep previous state
        }
    }
}
