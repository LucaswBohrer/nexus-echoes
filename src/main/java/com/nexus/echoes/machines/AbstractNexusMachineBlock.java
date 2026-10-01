package com.nexus.echoes.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for every NEXUS machine block.
 *
 * <p>Concrete machines only define: their block entity type, ticker wiring and
 * how the GUI is opened. Simulation, energy, sync and persistence live in
 * {@link AbstractNexusMachineBlockEntity}.
 */
public abstract class AbstractNexusMachineBlock extends BaseEntityBlock {

    protected AbstractNexusMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Drops the machine's inventory when broken (machines own their contents).
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractNexusMachineBlockEntity machine) {
                Containers.dropContents(level, pos, machine.getInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
