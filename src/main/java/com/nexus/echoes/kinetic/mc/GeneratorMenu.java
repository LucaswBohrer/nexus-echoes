package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Display-only menu for the kinetic generator. All values are server-synced
 * {@code ContainerData}; the client never computes physics.
 */
public class GeneratorMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public GeneratorMenu(int windowId, Inventory playerInventory, BlockPos pos, ContainerData data) {
        super(NexusRegistries.GENERATOR_MENU.get(), windowId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.data = data;
        layoutPlayerInventory(playerInventory);
    }

    /** Client-side constructor (from network buffer). */
    public GeneratorMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, buf.readBlockPos(), new SimpleContainerData(5));
    }

    private void layoutPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, NexusRegistries.KINETIC_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            ItemStack moved = inSlot.copy();
            if (index < 27) {
                if (!moveItemStackTo(inSlot, 27, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(inSlot, 0, 27, false)) {
                return ItemStack.EMPTY;
            }
            if (inSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return moved;
        }
        return ItemStack.EMPTY;
    }

    public int getRpm() {
        return data.get(0);
    }

    /** Torque in milli-newton-meters (int precision over the wire). */
    public int getTorqueMilliNm() {
        return data.get(1);
    }

    /** Derived power in watts. */
    public int getPowerWatts() {
        return data.get(2);
    }

    public int getStatusOrdinal() {
        return data.get(3);
    }

    public boolean isClockwise() {
        return data.get(4) == 0;
    }
}
