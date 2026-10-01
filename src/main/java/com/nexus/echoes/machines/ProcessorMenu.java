package com.nexus.echoes.machines;

import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Menu for the Processor. Server ctor used by the BE, buffer ctor by the client. */
public class ProcessorMenu extends ProcessingMenu {

    public ProcessorMenu(int windowId, Inventory playerInventory,
                         ProcessorBlockEntity blockEntity, ContainerData data) {
        super(NexusRegistries.PROCESSOR_MENU.get(), windowId, playerInventory, blockEntity, data);
    }

    public ProcessorMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, blockEntityAt(playerInventory, buf.readBlockPos()),
                new SimpleContainerData(6));
    }

    private static ProcessorBlockEntity blockEntityAt(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ProcessorBlockEntity processor) {
            return processor;
        }
        throw new IllegalStateException("No Processor at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, NexusRegistries.PROCESSOR.get());
    }
}
