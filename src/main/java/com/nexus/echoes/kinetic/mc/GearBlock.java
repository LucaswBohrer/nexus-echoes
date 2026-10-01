package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.sim.SimNode;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A toothed wheel. Two gears adjacent to each other <b>mesh</b>: the driven
 * gear spins at {@code teethDriver / teethDriven} × the speed with flipped
 * direction. A gear fed by a shaft (or anything that is not a gear) just rides
 * along unchanged — it only meshes gear-to-gear.
 */
public class GearBlock extends KineticBlock {

    public GearBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearBlockEntity(pos, state);
    }

    public static class GearBlockEntity extends KineticBlockEntity {

        public static final int TEETH = 12;

        public GearBlockEntity(BlockPos pos, BlockState state) {
            super(NexusRegistries.GEAR_BE.get(), pos, state);
        }

        @Override
        public NodeRole getKineticRole() {
            return NodeRole.TRANSMISSION;
        }

        @Override
        public SimNode.TransmissionKind getTransmissionKind() {
            return SimNode.TransmissionKind.GEAR;
        }

        @Override
        public int getGearTeeth() {
            return TEETH;
        }
    }
}
