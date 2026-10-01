package com.nexus.echoes.machines;

import com.nexus.echoes.core.NexusConfig;
import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.mc.KineticManager;
import com.nexus.echoes.kinetic.sim.NodeState;
import com.nexus.echoes.machines.kinetic.AbstractKineticMachineBlockEntity;
import com.nexus.echoes.machines.recipe.ResonatorRecipe;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
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
 * <p>Phase 3: now extends {@link AbstractKineticMachineBlockEntity}; the
 * kinetic registration, snapshot, NBT and sync plumbing moved to the base.
 * It keeps its legacy {@code resonating} recipe type and its 5-index
 * {@link ContainerData} (rpm, torque mN·m, status, progress, maxProgress) so
 * the existing GUI keeps working.
 *
 * <p>The inherited Phase 2 resonant storage is restored with its configured
 * capacity (see {@code NexusConfig}) so the Phase 1 energy API contract is
 * unchanged; the creative cell still has a valid producer contract for
 * future machines. Kinetic machines (Crusher/Processor) pass 0/0 — they hold
 * no energy buffer at all.
 */
public class ResonatorBlockEntity extends AbstractKineticMachineBlockEntity {

    public static final Rpm REQUIRED_RPM = Rpm.of(120);
    public static final Torque REQUIRED_TORQUE = Torque.ofNewtonMeters(30);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private Optional<ResonatorRecipe> cachedRecipe = Optional.empty();
    private ItemStack lastInput = ItemStack.EMPTY;

    /**
     * Legacy 5-index kinetic ContainerData: 0 rpm, 1 torque (mN·m),
     * 2 status ordinal, 3 progress, 4 maxProgress.
     */
    private final ContainerData legacyData = new SimpleContainerData(5) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(getKineticState().rpm().value());
                case 1 -> (int) Math.round(getKineticState().torque().newtonMeters() * 1000);
                case 2 -> getKineticState().status().ordinal();
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
        // Phase 2 energy storage restored verbatim (config-driven capacity);
        // kinetic power still comes from the shaft, not from this field.
        super(NexusRegistries.RESONATOR_BE.get(), pos, state, 2, REQUIRED_RPM, REQUIRED_TORQUE,
                NexusConfig.RESONATOR_CAPACITY.get(), NexusConfig.RESONATOR_MAX_RECEIVE.get());
    }

    // ------------------------------------------------------- kinetic consumer
    // (role, requirements, registration, snapshot, NBT: inherited from the base)

    @Override
    protected ContainerData getMenuData() {
        return legacyData;
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
            updateMachineStatus(false, false);
            return resetProgress();
        }

        ResonatorRecipe r = recipe.get();
        ItemStack result = r.getResultItem(level.registryAccess());
        boolean blocked = !canAcceptOutput(result);

        updateMachineStatus(true, blocked);
        if (blocked) {
            return resetProgress();
        }

        // Kinetic gate: the network simulation decides if we have usable power.
        if (getKineticState().status() != NodeStatus.OK) {
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

    // -------------------------------------------------------------------- gui

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ResonatorMenu(windowId, playerInventory, this, getMenuData());
    }
}
