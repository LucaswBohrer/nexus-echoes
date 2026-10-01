package com.nexus.echoes.machines;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.machines.kinetic.MachineStatus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Shared menu for industrial {@link com.nexus.echoes.machines.recipe.ProcessingRecipe}
 * machines (Phase 3): input slot, output slot, byproduct slot, player inventory.
 *
 * <p>{@link ContainerData} is synced by vanilla, so screens always render
 * server-authoritative values. Indices: 0 rpm, 1 torque (mN·m), 2 node status
 * ordinal, 3 progress, 4 maxProgress, 5 machine status ordinal.
 */
public abstract class ProcessingMenu extends AbstractContainerMenu {

    protected final ContainerLevelAccess access;
    protected final ContainerData data;

    protected ProcessingMenu(MenuType<?> type, int windowId, Inventory playerInventory,
                             BlockEntity blockEntity, ContainerData data) {
        super(type, windowId);
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;
        addDataSlots(data);

        IItemHandler handler = ((AbstractNexusMachineBlockEntity) blockEntity).getItemHandler();
        addSlot(new SlotItemHandler(handler, 0, 56, 35));   // input
        addSlot(new OutputSlot(handler, 1, 116, 35));       // output
        addSlot(new OutputSlot(handler, 2, 116, 57));       // byproduct

        layoutPlayerInventory(playerInventory);
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
    public ItemStack quickMoveStack(Player player, int index) {        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            moved = inSlot.copy();
            if (index < 3) {
                if (!moveItemStackTo(inSlot, 3, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(inSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }
        return moved;
    }

    // ------------------------------------------------------- synced readouts

    /** ContainerData indices: 0 rpm, 1 torque (mN·m), 2 node status, 3 progress, 4 max, 5 machine status. */
    public int getRpm() {
        return data.get(0);
    }

    public int getTorqueMilliNm() {
        return data.get(1);
    }

    public NodeStatus getNodeStatus() {
        NodeStatus[] values = NodeStatus.values();
        int ordinal = data.get(2);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NodeStatus.NO_INPUT;
    }

    public int getProgress() {
        return data.get(3);
    }

    public int getMaxProgress() {
        return data.get(4);
    }

    public MachineStatus getMachineStatus() {
        MachineStatus[] values = MachineStatus.values();
        int ordinal = data.get(5);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MachineStatus.IDLE;
    }

    /** Output slots never accept manual insertion (automation extracts from them). */
    protected static class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
