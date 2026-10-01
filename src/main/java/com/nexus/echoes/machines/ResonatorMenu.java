package com.nexus.echoes.machines;

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
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Server/client menu for the Resonator (Phase 2: kinetic).
 *
 * <p>The {@link ContainerData} is synced by vanilla (no custom packets needed),
 * so the screen always renders server-authoritative values.
 * Indices: 0 rpm, 1 torque (mN·m), 2 status ordinal, 3 progress, 4 maxProgress.
 */
public class ResonatorMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final ContainerData data;

    /** Server-side constructor. */
    public ResonatorMenu(int windowId, Inventory playerInventory,
                         ResonatorBlockEntity blockEntity, ContainerData data) {
        super(NexusRegistries.RESONATOR_MENU.get(), windowId);
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;

        addSlot(new net.minecraftforge.items.SlotItemHandler(blockEntity.getItemHandler(), 0, 56, 35));
        addSlot(new OutputSlot(blockEntity.getItemHandler(), 1, 116, 35));

        layoutPlayerInventory(playerInventory);
    }

    /** Client-side constructor (from network buffer). */
    public ResonatorMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, blockEntityAt(playerInventory, buf.readBlockPos()), new SimpleContainerData(5));
    }

    private static ResonatorBlockEntity blockEntityAt(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof ResonatorBlockEntity resonator) {
            return resonator;
        }
        throw new IllegalStateException("No Resonator at " + pos);
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
        return stillValid(access, player, NexusRegistries.RESONATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack inSlot = slot.getItem();
            moved = inSlot.copy();
            if (index < 2) {
                if (!moveItemStackTo(inSlot, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(inSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (inSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    public int getRpm() {
        return data.get(0);
    }

    /** Torque in milli-newton-meters (int precision over the wire). */
    public int getTorqueMilliNm() {
        return data.get(1);
    }

    public int getStatusOrdinal() {
        return data.get(2);
    }

    public int getProgress() {
        return data.get(3);
    }

    public int getMaxProgress() {
        return data.get(4);
    }

    /** Output slot: items can be taken but never placed by the player. */
    private static class OutputSlot extends net.minecraftforge.items.SlotItemHandler {
        OutputSlot(net.minecraftforge.items.IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
