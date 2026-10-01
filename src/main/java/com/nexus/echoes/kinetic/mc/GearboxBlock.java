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
 * Selectable-ratio transmission. Right-click cycles through the presets;
 * the ratio applies to the flow passing through (speed × ratio, torque ÷ ratio).
 */
public class GearboxBlock extends KineticBlock {

    public GearboxBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GearboxBlockEntity gearbox) {
            if (!level.isClientSide()) {
                gearbox.cycleRatio();
                player.displayClientMessage(Component.translatable(
                        "message.nexus_echoes.gearbox_ratio",
                        gearbox.ratioLabel(gearbox.getGearboxRatio())), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    public static class GearboxBlockEntity extends KineticBlockEntity {

        /** Speed ratios (output:input): 1:2, 1:1, 2:1, 4:1. */
        private static final double[] RATIOS = {0.5, 1.0, 2.0, 4.0};

        private int ratioIndex = 1; // default 1:1

        public GearboxBlockEntity(BlockPos pos, BlockState state) {
            super(NexusRegistries.GEARBOX_BE.get(), pos, state);
        }

        @Override
        public NodeRole getKineticRole() {
            return NodeRole.TRANSMISSION;
        }

        @Override
        public SimNode.TransmissionKind getTransmissionKind() {
            return SimNode.TransmissionKind.GEARBOX;
        }

        @Override
        public double getGearboxRatio() {
            return RATIOS[ratioIndex];
        }

        public void cycleRatio() {
            ratioIndex = (ratioIndex + 1) % RATIOS.length;
            setChanged();
            markKineticDirty();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        static String ratioLabel(double ratio) {
            if (ratio >= 1.0) {
                return (int) ratio + ":1";
            }
            return "1:" + (int) Math.round(1.0 / ratio);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("ratioIndex", ratioIndex);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            ratioIndex = Math.floorMod(tag.getInt("ratioIndex"), RATIOS.length);
        }
    }
}
