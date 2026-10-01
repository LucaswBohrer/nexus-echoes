package com.nexus.echoes.client.screen;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.mc.GeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Kinetic Generator GUI — display-only diagnostics screen following the NEXUS
 * visual identity. All values come from the menu's server-synced
 * {@code ContainerData}; the client never computes authoritative state.
 */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {

    private static final int PANEL = 0xFF0B1220;
    private static final int PANEL_EDGE = 0xFF1E2A3A;
    private static final int CYAN = 0xFF35E0E6;
    private static final int TEXT = 0xFFD7E3F4;
    private static final int DIM = 0xFF5A6A7A;
    private static final int WARN = 0xFFE0A335;
    private static final int BAD = 0xFFE05252;

    public GeneratorScreen(GeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        gui.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        gui.fill(x, y, x + imageWidth, y + 1, PANEL_EDGE);
        gui.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_EDGE);
        gui.fill(x, y, x + 1, y + imageHeight, PANEL_EDGE);
        gui.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_EDGE);
        gui.fill(x + 1, y + 1, x + imageWidth - 1, y + 20, 0xFF111B2C);

        NodeStatus status = status();
        int dot = switch (status) {
            case OK -> CYAN;
            case UNDERPOWERED -> WARN;
            case OVERLOADED, CONFLICT -> BAD;
            case NO_INPUT, DISABLED -> DIM;
        };
        gui.fill(x + 12, y + 30, x + 18, y + 36, dot);

        gui.drawString(font, "RPM", x + 12, y + 42, DIM, false);
        gui.drawString(font, String.valueOf(menu.getRpm()), x + 12, y + 52, TEXT, false);
        gui.drawString(font, "TORQUE", x + 12, y + 66, DIM, false);
        gui.drawString(font, String.format("%.1f Nm", menu.getTorqueMilliNm() / 1000.0), x + 12, y + 76, TEXT, false);
        gui.drawString(font, "POWER", x + 12, y + 90, DIM, false);
        gui.drawString(font, String.format("%.1f W", menu.getPowerWatts()), x + 12, y + 100, TEXT, false);
        gui.drawString(font, "STATUS", x + 92, y + 42, DIM, false);
        gui.drawString(font, status.name(), x + 92, y + 52, dot, false);
    }

    private NodeStatus status() {
        NodeStatus[] values = NodeStatus.values();
        int ordinal = menu.getStatusOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NodeStatus.NO_INPUT;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, 8, 7, TEXT, false);
        gui.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
