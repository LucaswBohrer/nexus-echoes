package com.nexus.echoes.machines;

import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Creative-only infinite resonant-energy source (ADR-006).
 *
 * <p>Exists so Phase 1 can prove producer → transmission → consumer without
 * preempting Phase 2's kinetic generators. No recipe, creative tab only.
 */
public class CreativeCellBlock extends AbstractNexusMachineBlock {

    public CreativeCellBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, NexusRegistries.CREATIVE_CELL_BE.get(),
                CreativeCellBlockEntity::serverTick);
    }
}
