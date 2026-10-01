package com.nexus.echoes.client.screen;

import com.nexus.echoes.machines.ProcessorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Processor GUI: shared machine identity, no extra widgets yet. */
public class ProcessorScreen extends MachineScreen<ProcessorMenu> {
    public ProcessorScreen(ProcessorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
