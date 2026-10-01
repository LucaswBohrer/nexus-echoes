package com.nexus.echoes.client.screen;

import com.nexus.echoes.machines.CrusherMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Crusher GUI: shared machine identity, no extra widgets yet. */
public class CrusherScreen extends MachineScreen<CrusherMenu> {
    public CrusherScreen(CrusherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
