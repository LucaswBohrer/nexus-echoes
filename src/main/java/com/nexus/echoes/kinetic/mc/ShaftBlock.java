package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.sim.SimNode;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A straight axle: passes rotation through unchanged. */
public class ShaftBlock extends KineticBlock {

    public ShaftBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity(pos, state);
    }

    public static class ShaftBlockEntity extends KineticBlockEntity {
        public ShaftBlockEntity(BlockPos pos, BlockState state) {
            super(NexusRegistries.SHAFT_BE.get(), pos, state);
        }

        @Override
        public NodeRole getKineticRole() {
            return NodeRole.TRANSMISSION;
        }

        @Override
        public SimNode.TransmissionKind getTransmissionKind() {
            return SimNode.TransmissionKind.SHAFT;
        }
    }
}
