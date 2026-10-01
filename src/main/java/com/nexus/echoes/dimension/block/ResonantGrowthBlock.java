package com.nexus.echoes.dimension.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Resonant Growth — the strange vegetation of the Resonant Forest.
 *
 * <p>Half plant, half circuit: it photosynthesizes and rectifies. Harvested
 * by hand (or shears) for echo fiber. A plain cross-shaped bush: no block
 * entity, no ticking.
 */
public class ResonantGrowthBlock extends BushBlock {

    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public ResonantGrowthBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(com.nexus.echoes.registry.NexusRegistries.ASHEN_SOIL.get())
                || super.mayPlaceOn(state, level, pos);
    }
}
