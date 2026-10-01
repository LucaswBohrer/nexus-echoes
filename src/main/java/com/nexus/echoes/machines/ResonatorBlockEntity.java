package com.nexus.echoes.machines;

import com.nexus.echoes.core.NexusConfig;
import com.nexus.echoes.energy.api.EnergyType;
import com.nexus.echoes.kinetic.RotationDirection;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeRole;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.mc.KineticManager;
import com.nexus.echoes.kinetic.mc.KineticNodeProvider;
import com.nexus.echoes.kinetic.mc.KineticStateNbt;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.machines.recipe.ResonatorRecipe;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Resonator logic (Phase 2): consumes <b>kinetic</b> energy instead of the
 * Phase 1 resonant storage. Requires 120 RPM / 30 N·m at its shaft — the
 * {@link KineticManager} simulates the network and this machine only runs
 * while its {@link NodeState} is {@link NodeStatus#OK}.
 *
 * <p>The inherited resonant storage is kept (harmless, unused) so the Phase 1
 * energy API stays intact; the creative cell still has a valid producer
 * contract for future machines.
 */
public class ResonatorBlockEntity extends AbstractNexusMachineBlockEntity
        implements KineticNodeProvider {

    public static final Rpm REQUIRED_RPM = Rpm.of(120);
    public static final Torque REQUIRED_TORQUE = Torque.ofNewtonMeters(30);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private Optional<ResonatorRecipe> cachedRecipe = Optional.empty();
    private ItemStack lastInput = ItemStack.EMPTY;

    private NodeState kineticState = NodeState.of(
            Rpm.ZERO, Torque.ZERO, RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);

    /**
     * Kinetic ContainerData indices: 0 rpm, 1 torque (mN·m), 2 status ordinal,
     * 3 progress, 4 maxProgress.
     */
    private final ContainerData kineticData = new SimpleContainerData(5) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(kineticState.rpm().value());
                case 1 -> (int) Math.round(kineticState.torque().newtonMeters() * 1000);
                case 2 -> kineticState.status().ordinal();
                case 3 -> progress;
                case 4 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 3 -> progress = value;
                case 4 -> maxProgress = value;
                default -> {
                }
            }
        }
    };

    public ResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(NexusRegistries.RESONATOR_BE.get(), pos, state,
                EnergyType.RESONANT,
                NexusConfig.RESONATOR_CAPACITY.get(),
                NexusConfig.RESONATOR_MAX_RECEIVE.get(),
                2);
    }

    // ------------------------------------------------------- kinetic consumer

    @Override
    public NodeRole getKineticRole() {
        return NodeRole.CONSUMER;
    }

    @Override
    public Rpm getRequiredRpm() {
        return REQUIRED_RPM;
    }

    @Override
    public Torque getRequiredTorque() {
        return REQUIRED_TORQUE;
    }

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

    public NodeState getKineticState() {
        return kineticState;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).register(worldPosition, this);
        }
    }

    @Override
    public void setRemoved() {
        unregisterKinetic();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterKinetic();
        super.onChunkUnloaded();
    }

    private void unregisterKinetic() {
        if (level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).unregister(worldPosition);
        }
    }

    @Override
    protected ContainerData getMenuData() {
        return kineticData;
    }

    // ------------------------------------------------------------------ work

    @Override
    protected boolean doWork() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        Optional<ResonatorRecipe> recipe = findRecipe(input);

        if (recipe.isEmpty()) {
            return resetProgress();
        }

        ResonatorRecipe r = recipe.get();
        ItemStack result = r.getResultItem(level.registryAccess());

        if (!canAcceptOutput(result)) {
            return resetProgress();
        }

        // Kinetic gate: the network simulation decides if we have usable power.
        if (kineticState.status() != NodeStatus.OK) {
            return resetProgress(); // stalled: no shaft power / underpowered
        }

        maxProgress = r.getProcessingTime();
        progress++;

        if (progress >= maxProgress) {
            craft(input, result);
        }
        return true;
    }

    private Optional<ResonatorRecipe> findRecipe(ItemStack input) {
        if (input.isEmpty()) {
            cachedRecipe = Optional.empty();
            lastInput = ItemStack.EMPTY;
            return cachedRecipe;
        }
        // Cache: only query the recipe manager when the input actually changed.
        if (ItemStack.isSameItemSameTags(input, lastInput) && cachedRecipe.isPresent()) {
            return cachedRecipe;
        }
        lastInput = input.copy();
        SimpleContainer container = new SimpleContainer(input.copy());
        cachedRecipe = level.getRecipeManager().getRecipeFor(NexusRegistries.RESONATING.get(), container, level);
        return cachedRecipe;
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(ItemStack input, ItemStack result) {
        input.shrink(1);
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        progress = 0;
        cachedRecipe = Optional.empty(); // re-evaluate next tick
    }

    private boolean resetProgress() {
        if (progress != 0) {
            progress = 0;
            return true;
        }
        return false;
    }

    @Override
    protected void onInventoryChanged(int slot) {
        if (slot == INPUT_SLOT) {
            cachedRecipe = Optional.empty();
        }
    }

    // ------------------------------------------------------------ persistence

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

    // -------------------------------------------------------------------- gui

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(windowId, playerInventory, this, getMenuData());
    }
}
