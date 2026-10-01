package com.nexus.echoes.machines;

import com.nexus.echoes.core.NexusConfig;
import com.nexus.echoes.energy.api.EnergyType;
import com.nexus.echoes.energy.api.INexusEnergy;
import com.nexus.echoes.energy.api.INexusEnergyProvider;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pushes resonant energy into adjacent {@link INexusEnergyProvider}s.
 *
 * <p>Phase 1 transmission model (ADR-005): neighbor-push every 10 ticks, exact
 * type match. Infinite source — it never depletes.
 */
public class CreativeCellBlockEntity extends BlockEntity {

    private static final int PULSE_INTERVAL_TICKS = 10;

    private int tickCounter;

    public CreativeCellBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.CREATIVE_CELL_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeCellBlockEntity be) {
        if (--be.tickCounter <= 0) {
            be.tickCounter = PULSE_INTERVAL_TICKS;
            be.pushEnergy();
        }
    }

    private void pushEnergy() {
        if (level == null || level.isClientSide()) {
            return;
        }
        int perTick = NexusConfig.CELL_TRANSFER_PER_TICK.get();
        int amount = perTick * PULSE_INTERVAL_TICKS;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof INexusEnergyProvider provider) {
                INexusEnergy storage = provider.getNexusEnergy(direction.getOpposite());
                if (storage != null
                        && storage.getEnergyType() == EnergyType.RESONANT
                        && storage.canReceive()) {
                    storage.receiveEnergy(amount, false);
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("tickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tickCounter = tag.getInt("tickCounter");
    }
}
