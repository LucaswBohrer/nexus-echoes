package com.nexus.echoes.machines;

import com.nexus.echoes.energy.EnergyNbtHelper;
import com.nexus.echoes.energy.NexusEnergyStorage;
import com.nexus.echoes.energy.api.EnergyType;
import com.nexus.echoes.energy.api.INexusEnergy;
import com.nexus.echoes.energy.api.INexusEnergyProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for every NEXUS machine block entity.
 *
 * <p>Provides, once and for all:
 * <ul>
 *   <li>typed energy storage ({@link INexusEnergy}) with NBT persistence;</li>
 *   <li>progress / max-progress processing state;</li>
 *   <li>server-only ticking — the client never simulates;</li>
 *   <li>sync via vanilla block-entity update packets, throttled by a dirty flag;</li>
 *   <li>{@link ContainerData} so GUIs always render server-synced values;</li>
 *   <li>item inventory exposed through the Forge item-handler capability.</li>
 * </ul>
 *
 * <p>Concrete machines implement {@link #doWork()} and {@link #createMenu}.
 */
public abstract class AbstractNexusMachineBlockEntity extends BlockEntity
        implements INexusEnergyProvider, net.minecraft.world.MenuProvider {

    /** Ticks between energy-I/O pulses and forced syncs. Keeps TPS cost flat. */
    private static final int SYNC_INTERVAL_TICKS = 10;

    protected final NexusEnergyStorage energy;
    protected final ItemStackHandler inventory;

    protected int progress;
    protected int maxProgress;

    /** ContainerData indices: 0 energy, 1 maxEnergy, 2 progress, 3 maxProgress. */
    protected final ContainerData data = new SimpleContainerData(4) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.getEnergyStored();
                case 1 -> energy.getMaxEnergyStored();
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy.setEnergyStored(value);
                case 2 -> progress = value;
                case 3 -> maxProgress = value;
                default -> {
                }
            }
        }
    };

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.empty();
    private int tickCounter;
    private int lastBroadcastEnergy = -1;
    private int lastBroadcastProgress = -1;

    protected AbstractNexusMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                             EnergyType energyType, int capacity, int maxReceive, int slots) {
        super(type, pos, state);
        this.energy = new NexusEnergyStorage(energyType, capacity, maxReceive) {
            @Override
            protected void onEnergyChanged() {
                setChanged();
            }
        };
        this.inventory = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                onInventoryChanged(slot);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                // Automation (hoppers/pipes) insertion rule. Manual insertion
                // is governed by the menu's slots; internal machine output
                // uses setStackInSlot and bypasses this check.
                return allowsExternalInsert(slot, stack);
            }
        };
        this.maxProgress = 0;
    }

    /**
     * Whether automation (hoppers, pipes) may <b>insert</b> into a slot.
     * Extraction is always allowed from every slot (including input, so
     * automation can pull out wrong items or unload the machine).
     */
    protected boolean allowsExternalInsert(int slot, ItemStack stack) {
        return true;
    }

    /** Entry point for the block's ticker. Server only — client BEs are display-only. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractNexusMachineBlockEntity be) {
        be.tickServer();
    }

    protected void tickServer() {
        boolean changed = doWork();
        if (changed) {
            setChanged();
        }
        if (--tickCounter <= 0) {
            tickCounter = SYNC_INTERVAL_TICKS;
            broadcastIfDirty(true);
        } else {
            broadcastIfDirty(false);
        }
    }

    /**
     * One simulation step. Runs ONLY on the server.
     *
     * @return true if persistent state changed (marks the chunk dirty)
     */
    protected abstract boolean doWork();

    /** Hook for subclasses to invalidate cached recipes etc. */
    protected void onInventoryChanged(int slot) {
    }

    /** Concrete machines expose their GUI menu here. */
    @Override
    public abstract AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player);

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ------------------------------------------------------------- networking

    /**
     * Sends a vanilla block-entity update packet when energy/progress moved
     * materially (or when the interval forces it). Throttled by design.
     */
    protected void broadcastIfDirty(boolean force) {
        if (level == null || level.isClientSide()) {
            return;
        }
        int e = energy.getEnergyStored();
        boolean energyMoved = Math.abs(e - lastBroadcastEnergy) > Math.max(1, energy.getMaxEnergyStored() / 50);
        boolean progressMoved = progress != lastBroadcastProgress;
        if (force || energyMoved || progressMoved) {
            lastBroadcastEnergy = e;
            lastBroadcastProgress = progress;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------ persistence

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        EnergyNbtHelper.writeTo(tag, energy);
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        EnergyNbtHelper.readFrom(tag, energy);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
    }

    // ------------------------------------------------------------ capabilities

    @Override
    public void onLoad() {
        super.onLoad();
        itemHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    // ----------------------------------------------------------------- access

    @Override
    public INexusEnergy getNexusEnergy(@Nullable Direction direction) {
        return energy;
    }

    public SimpleContainer getInventory() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        return container;
    }

    protected ItemStackHandler getItemHandler() {
        return inventory;
    }

    public ContainerData getContainerData() {
        return data;
    }

    /**
     * The {@link ContainerData} handed to this machine's menu. Defaults to the
     * energy/progress array; machines with their own sync layout override it
     * (e.g. the kinetic Resonator).
     */
    protected ContainerData getMenuData() {
        return data;
    }
}
