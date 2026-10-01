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
 * NEXUS Crusher — first real industrial machine (Phase 3).
 *
 * <p>Crushes raw resources into dust: {@code ore → dust}. Requires
 * <b>120 RPM / 20 N·m</b> — a direct shaft drive from one Kinetic Generator.
 * Brownout slows crushing proportionally ({@code speed ∝ delivered/required
 * torque}); no shaft power stalls it.
 */
public class CrusherBlockEntity extends AbstractKineticMachineBlockEntity {

    public static final Rpm REQUIRED_RPM = Rpm.of(120);
    public static final Torque REQUIRED_TORQUE = Torque.ofNewtonMeters(20);

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        // Kinetic-only: no Phase 1 energy buffer (0/0) — power comes from the shaft.
        super(NexusRegistries.CRUSHER_BE.get(), pos, state, 3, REQUIRED_RPM, REQUIRED_TORQUE, 0, 0);
    }

    @Override
    protected RecipeType<ProcessingRecipe> recipeType() {
        return NexusRegistries.CRUSHING.get();
    }

    @Override
    protected boolean doWork() {
        return processingTick();
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new CrusherMenu(windowId, playerInventory, this, getMenuData());
    }
}
