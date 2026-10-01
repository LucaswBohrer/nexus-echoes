package com.nexus.echoes.kinetic.mc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base block for kinetic network members. Notifies the {@link KineticManager}
 * when the topology changes (place / remove / neighbor update).
 */
public abstract class KineticBlock extends BaseEntityBlock {

    protected KineticBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        markDirty(level);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            markDirty(level);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.block.Block neighborBlock,
                                BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
        // redstone changes can enable/disable sources: re-simulate
        markDirty(level);
    }

    private static void markDirty(Level level) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).markDirty();
        }
    }
}
