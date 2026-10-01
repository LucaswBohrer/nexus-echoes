package com.nexus.echoes.dimension.block;

import com.nexus.echoes.dimension.mc.AnomalySavedData;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Anomaly Ward block entity: a passive counterplay device.
 *
 * <p>The ward does not tick. On load it registers its position with the
 * level's {@link AnomalySavedData}; on removal it unregisters. The
 * {@code AnomalyManager} consults the registered ward positions when
 * applying anomaly effects — event-driven, no scans. The ward works in any
 * dimension but only matters where anomalies exist (The Hollow).
 */
public class WardBlockEntity extends BlockEntity {

    /** Effect suppression radius in blocks. */
    public static final int RADIUS = 24;

    public WardBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.WARD_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            AnomalySavedData.get(serverLevel).addWard(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        unregister();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregister();
        super.onChunkUnloaded();
    }

    private void unregister() {
        if (level instanceof ServerLevel serverLevel) {
            AnomalySavedData.get(serverLevel).removeWard(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }
}
