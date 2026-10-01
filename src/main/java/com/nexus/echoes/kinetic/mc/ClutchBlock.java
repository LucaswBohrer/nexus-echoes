package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.sim.SimNode;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Engages/disengages the drivetrain. Right-click toggles; a disengaged clutch
 * blocks all flow downstream (consumers report NO_INPUT).
 */
public class ClutchBlock extends KineticBlock {

    public ClutchBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClutchBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof ClutchBlockEntity clutch) {
            if (!level.isClientSide()) {
                clutch.setEngaged(!clutch.isClutchEngaged());
                player.displayClientMessage(Component.translatable(
                        clutch.isClutchEngaged()
                                ? "message.nexus_echoes.clutch_engaged"
                                : "message.nexus_echoes.clutch_disengaged"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    public static class ClutchBlockEntity extends KineticBlockEntity {

        private boolean engaged = true;

        public ClutchBlockEntity(BlockPos pos, BlockState state) {
            super(NexusRegistries.CLUTCH_BE.get(), pos, state);
        }

        @Override
        public NodeRole getKineticRole() {
            return NodeRole.TRANSMISSION;
        }

        @Override
        public SimNode.TransmissionKind getTransmissionKind() {
            return SimNode.TransmissionKind.CLUTCH;
        }

        @Override
        public boolean isClutchEngaged() {
            return engaged;
        }

        public void setEngaged(boolean engaged) {
            if (this.engaged != engaged) {
                this.engaged = engaged;
                setChanged();
                markKineticDirty();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("engaged", engaged);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            engaged = !tag.contains("engaged") || tag.getBoolean("engaged");
        }
    }
}
