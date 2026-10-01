package com.nexus.echoes.machines.kinetic;

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
import com.nexus.echoes.machines.AbstractNexusMachineBlockEntity;
import com.nexus.echoes.machines.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Base for machines that consume <b>kinetic</b> energy (Phase 3, ADR-009).
 *
 * <p>Unifies what the Phase 2 Resonator implemented ad-hoc: kinetic network
 * registration, snapshot handling, NBT persistence, a 6-index
 * {@link ContainerData} (rpm, torque mN·m, node status, progress, max
 * progress, machine status), and the standard recipe-processing loop with
 * proportional brownout via {@link ProcessingGovernor}.
 *
 * <p>Concrete machines only declare:
 * <ul>
 *   <li>required RPM / torque (constructor);</li>
 *   <li>their {@link ProcessingRecipe} type (or override {@link #doWork()}
 *       entirely, like the Resonator with its legacy recipe type).</li>
 * </ul>
 */
public abstract class AbstractKineticMachineBlockEntity extends AbstractNexusMachineBlockEntity
        implements KineticNodeProvider {

    protected static final int INPUT_SLOT = 0;
    protected static final int OUTPUT_SLOT = 1;
    protected static final int BYPRODUCT_SLOT = 2;

    private final Rpm requiredRpm;
    private final Torque requiredTorque;

    protected NodeState kineticState = NodeState.of(
            Rpm.ZERO, Torque.ZERO, RotationDirection.CLOCKWISE, NodeStatus.NO_INPUT);
    protected MachineStatus machineStatus = MachineStatus.IDLE;
    /** Fractional progress accumulator: brownout advances work by fractions of a tick. */
    protected double progressFrac;

    private Optional<ProcessingRecipe> cachedRecipe = Optional.empty();
    private ItemStack lastInput = ItemStack.EMPTY;

    /**
     * Kinetic ContainerData indices: 0 rpm, 1 torque (mN·m), 2 node status
     * ordinal, 3 progress, 4 maxProgress, 5 machine status ordinal.
     */
    private final ContainerData kineticData = new SimpleContainerData(6) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(kineticState.rpm().value());
                case 1 -> (int) Math.round(kineticState.torque().newtonMeters() * 1000);
                case 2 -> kineticState.status().ordinal();
                case 3 -> progress;
                case 4 -> maxProgress;
                case 5 -> machineStatus.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 3 -> progress = value;
                case 4 -> maxProgress = value;
                case 5 -> machineStatus = MachineStatus.values()[value];
                default -> {
                }
            }
        }
    };

    protected AbstractKineticMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                               int slots, Rpm requiredRpm, Torque requiredTorque,
                                               int energyCapacity, int energyMaxReceive) {
        // The Phase 1 energy field is kept for API compatibility. Kinetic-only
        // machines pass 0/0 (no parallel buffer); the Resonator restores its
        // Phase 2 configured storage. Mechanical power always comes from the
        // kinetic network — never from this field.
        super(type, pos, state, EnergyType.RESONANT, energyCapacity, energyMaxReceive, slots);
        this.requiredRpm = requiredRpm;
        this.requiredTorque = requiredTorque;
    }

    // ------------------------------------------------------- kinetic consumer

    @Override
    public NodeRole getKineticRole() {
        return NodeRole.CONSUMER;
    }

    @Override
    public Rpm getRequiredRpm() {
        return requiredRpm;
    }

    @Override
    public Torque getRequiredTorque() {
        return requiredTorque;
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

    public MachineStatus getMachineStatus() {
        return machineStatus;
    }

    /** Refresh the machine status display without touching progress. */
    protected void updateMachineStatus(boolean hasWork, boolean outputBlocked) {
        MachineStatus next = ProcessingGovernor.deriveStatus(hasWork, outputBlocked, kineticState.status());
        if (next != machineStatus) {
            machineStatus = next;
        }
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

    /** Call when a kinetic node parameter changed (requirements are machine-fixed). */
    protected void markKineticDirty() {
        if (level instanceof ServerLevel serverLevel) {
            KineticManager.get(serverLevel).markDirty();
        }
    }

    @Override
    protected ContainerData getMenuData() {
        return kineticData;
    }

    /**
     * Automation rule for kinetic machines: only the input slot accepts
     * external insertion; output and byproduct are extract-only (mirrors the
     * menu's {@code OutputSlot}). Internal crafting bypasses this via
     * {@code setStackInSlot}.
     */
    @Override
    protected boolean allowsExternalInsert(int slot, ItemStack stack) {
        return slot == INPUT_SLOT;
    }

    // ------------------------------------------------------- processing loop

    /**
     * The {@link ProcessingRecipe} type this machine consumes, or {@code null}
     * when the machine uses its own recipe type and overrides {@link #doWork()}
     * directly (Resonator).
     */
    protected RecipeType<ProcessingRecipe> recipeType() {
        return null;
    }

    /**
     * Standard one-tick processing step for {@link ProcessingRecipe} machines.
     * Server only. Returns true when persistent state changed.
     */
    protected boolean processingTick() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        Optional<ProcessingRecipe> recipe = findRecipe(input);

        if (recipe.isEmpty()) {
            updateMachineStatus(false, false);
            return resetProgress();
        }

        ProcessingRecipe r = recipe.get();
        ItemStack result = r.getResultItem(level.registryAccess());
        boolean blocked = !canAcceptOutput(result, r);

        updateMachineStatus(true, blocked);
        if (blocked) {
            return resetProgress();
        }

        double factor = ProcessingGovernor.speedFactor(
                kineticState.status(), kineticState.rpm(), requiredRpm,
                kineticState.torque(), requiredTorque);
        if (factor <= 0.0) {
            return resetProgress(); // stalled: no shaft power / underpowered to zero
        }

        maxProgress = r.getProcessingTime();
        progressFrac += factor;
        boolean advanced = false;
        while (progressFrac >= 1.0 && progress < maxProgress) {
            progressFrac -= 1.0;
            progress++;
            advanced = true;
        }

        if (progress >= maxProgress) {
            craft(input, r);
            return true;
        }
        return advanced;
    }

    private Optional<ProcessingRecipe> findRecipe(ItemStack input) {
        if (input.isEmpty()) {
            cachedRecipe = Optional.empty();
            lastInput = ItemStack.EMPTY;
            return cachedRecipe;
        }
        if (ItemStack.isSameItemSameTags(input, lastInput) && cachedRecipe.isPresent()) {
            return cachedRecipe;
        }
        lastInput = input.copy();
        SimpleContainer container = new SimpleContainer(input.copy());
        cachedRecipe = level.getRecipeManager().getRecipeFor(recipeType(), container, level);
        return cachedRecipe;
    }

    private boolean canAcceptOutput(ItemStack result, ProcessingRecipe recipe) {
        if (!fits(OUTPUT_SLOT, result)) {
            return false;
        }
        return recipe.getByproduct()
                .map(bp -> fits(BYPRODUCT_SLOT, bp.stack()))
                .orElse(true);
    }

    private boolean fits(int slot, ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private void craft(ItemStack input, ProcessingRecipe recipe) {
        input.shrink(1);
        insert(OUTPUT_SLOT, recipe.getResultItem(level.registryAccess()));
        recipe.getByproduct().ifPresent(bp -> {
            if (level.random.nextFloat() < bp.chance()) {
                insert(BYPRODUCT_SLOT, bp.stack());
            }
        });
        progress = 0;
        progressFrac = 0;
        cachedRecipe = Optional.empty(); // re-evaluate next tick
    }

    private void insert(int slot, ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, stack.copy());
        } else {
            existing.grow(stack.getCount());
        }
    }

    private boolean resetProgress() {
        boolean changed = progress != 0 || progressFrac != 0;
        progress = 0;
        progressFrac = 0;
        return changed;
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
        tag.putDouble("progressFrac", progressFrac);
        tag.putString("machineStatus", machineStatus.name());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kineticState = KineticStateNbt.read(tag, worldPosition, kineticState);
        progressFrac = tag.getDouble("progressFrac");
        try {
            machineStatus = MachineStatus.valueOf(tag.getString("machineStatus"));
        } catch (IllegalArgumentException e) {
            machineStatus = MachineStatus.IDLE;
        }
    }
}
