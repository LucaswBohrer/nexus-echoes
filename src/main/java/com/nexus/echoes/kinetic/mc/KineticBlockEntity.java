package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.NodeState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Base block entity for kinetic network members.
 *
 * <ul>
 *   <li>registers/unregisters with the level's {@link KineticManager};</li>
 *   <li>receives the server-simulated {@link NodeState} and persists it;</li>
 *   <li>syncs that state to the client with vanilla update packets so
 *       renderers and GUIs always show server-authoritative values.</li>
 * </ul>
 */
public abstract class KineticBlockEntity extends BlockEntity implements KineticNodeProvider {

    protected NodeState kineticState = NodeState.of(
            Rpm.ZERO, Torque.ZERO, RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);

    protected KineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ------------------------------------------------------------ registration

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).register(worldPosition, this);
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
            KineticManager.get(serverLevel).unregister(worldPosition);
        }
    }

    /** Call when a node parameter changed (ratio, clutch, redstone…). */
    protected void markKineticDirty() {
        if (level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).markDirty();
        }
    }

    // ------------------------------------------------------------------ state

    @Override
    public void onKineticSnapshot(NodeState state) {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!state.equals(kineticState)) {
            kineticState = state;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Last server-simulated state. On the client this mirrors the update packet. */
    public NodeState getKineticState() {
        return kineticState;
    }

    // -------------------------------------------------------------------- nbt

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        KineticStateNbt.write(tag, kineticState);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kineticState = KineticStateNbt.read(tag, worldPosition, kineticState);
    }

    // -------------------------------------------------------------------- sync

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
