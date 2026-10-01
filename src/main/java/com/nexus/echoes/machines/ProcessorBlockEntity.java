package com.nexus.echoes.machines;

import com.nexus.echoes.kinetic.Rpm;
import com.nexus.echoes.kinetic.Torque;
import com.nexus.echoes.machines.kinetic.AbstractKineticMachineBlockEntity;
import com.nexus.echoes.machines.recipe.ProcessingRecipe;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NEXUS Processor — second industrial machine (Phase 3).
 *
 * <p>Refines dust into advanced material: {@code dust → refined material}.
 * Requires <b>240 RPM / 15 N·m</b> — deliberately faster than a lone
 * generator, so the player must insert a 2:1 gearbox (120 → 240 RPM,
 * torque 50 → ~25 N·m ≥ 15 N·m). This is the chain's first transmission
 * requirement, not an arbitrary number.
 */
public class ProcessorBlockEntity extends AbstractKineticMachineBlockEntity {

    public static final Rpm REQUIRED_RPM = Rpm.of(240);
    public static final Torque REQUIRED_TORQUE = Torque.ofNewtonMeters(15);

    public ProcessorBlockEntity(BlockPos pos, BlockState state) {
        // Kinetic-only: no Phase 1 energy buffer (0/0) — power comes from the shaft.
        super(NexusRegistries.PROCESSOR_BE.get(), pos, state, 3, REQUIRED_RPM, REQUIRED_TORQUE, 0, 0);
    }

    @Override
    protected RecipeType<ProcessingRecipe> recipeType() {
        return NexusRegistries.PROCESSING.get();
    }

    @Override
    protected boolean doWork() {
        return processingTick();
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ProcessorMenu(windowId, playerInventory, this, getMenuData());
    }
}
