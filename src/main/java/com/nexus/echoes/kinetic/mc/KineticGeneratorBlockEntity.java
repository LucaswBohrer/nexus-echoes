package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kinetic source: a steady prime mover (placeholder for waterwheel/windmill).
 *
 * <p>Rated 120 RPM / 50 N·m clockwise. Disabled while receiving a redstone
 * signal. All physics are simulated by the {@link KineticManager}; this block
 * entity only declares its ratings and displays the simulated result.
 */
public class KineticGeneratorBlockEntity extends KineticBlockEntity implements MenuProvider {

    public static final Rpm RATED_RPM = Rpm.of(120);
    public static final Torque RATED_TORQUE = Torque.ofNewtonMeters(50);
    public static final RotationDirection RATED_DIRECTION = RotationDirection.CLOCKWISE;

    /**
     * ContainerData indices: 0 rpm, 1 torque (mN·m), 2 power (W, derived),
     * 3 status ordinal, 4 direction ordinal (0=CW, 1=CCW).
     */
    private final ContainerData data = new SimpleContainerData(5) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(kineticState.rpm().value());
                case 1 -> (int) Math.round(kineticState.torque().newtonMeters() * 1000);
                case 2 -> (int) Math.round(kineticState.power().watts());
                case 3 -> kineticState.status().ordinal();
                case 4 -> kineticState.direction() == RotationDirection.CLOCKWISE ? 0 : 1;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // display-only
        }
    };

    public KineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.KINETIC_GENERATOR_BE.get(), pos, state);
    }

    @Override
    public NodeRole getKineticRole() {
        return NodeRole.SOURCE;
    }

    @Override
    public Rpm getRatedRpm() {
        return RATED_RPM;
    }

    @Override
    public Torque getRatedTorque() {
        return RATED_TORQUE;
    }

    @Override
    public RotationDirection getRatedDirection() {
        return RATED_DIRECTION;
    }

    @Override
    public boolean isKineticEnabled() {
        return level == null || !level.hasNeighborSignal(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // no extra persistent state yet: ratings are constants, enabled is redstone
    }

    // --------------------------------------------------------------------- gui

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.nexus_echoes.kinetic_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new GeneratorMenu(windowId, playerInventory, worldPosition, data);
    }
}
