package com.nexus.echoes.client.screen;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.machines.ProcessingMenu;
import com.nexus.echoes.machines.kinetic.MachineStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Shared screen for industrial processing machines (Phase 3).
 *
 * <p>Common NEXUS machine identity: input/output/byproduct slots, progress
 * bar, and the kinetic readout (RPM / torque / power / status). All values
 * come from the menu's server-synced {@code ContainerData}; the client never
 * computes authoritative state.
 */
public abstract class MachineScreen<T extends ProcessingMenu> extends AbstractContainerScreen<T> {

    protected static final int PANEL = 0xFF0B1220;
    protected static final int PANEL_EDGE = 0xFF1E2A3A;
    protected static final int CYAN = 0xFF35E0E6;
    protected static final int TEXT = 0xFFD7E3F4;
    protected static final int DIM = 0xFF5A6A7A;
    protected static final int WARN = 0xFFE0A335;
    protected static final int BAD = 0xFFE05252;

    protected MachineScreen(T menu, Inventory playerInventory, Component title) {
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

        // slot captions
        gui.drawString(font, "IN", x + 56, y + 24, DIM, false);
        gui.drawString(font, "OUT", x + 112, y + 24, DIM, false);
        gui.drawString(font, "BY", x + 134, y + 50, DIM, false);

        // progress bar between input and output
        int barX = x + 79, barY = y + 38, barW = 34, barH = 6;
        gui.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, PANEL_EDGE);
        gui.fill(barX, barY, barX + barW, barY + barH, 0xFF060B14);
        int max = menu.getMaxProgress();
        if (max > 0) {
            int filled = (int) (barW * Math.min(1.0, menu.getProgress() / (double) max));
            gui.fill(barX, barY, barX + filled, barY + barH, CYAN);
        }

        // kinetic readout
        double torqueNm = menu.getTorqueMilliNm() / 1000.0;
        double powerW = torqueNm * menu.getRpm() * 2 * Math.PI / 60.0;

        gui.drawString(font, "RPM", x + 12, y + 66, DIM, false);
        gui.drawString(font, String.valueOf(menu.getRpm()), x + 12, y + 76, TEXT, false);
        gui.drawString(font, "TORQUE", x + 62, y + 66, DIM, false);
        gui.drawString(font, String.format("%.1f Nm", torqueNm), x + 62, y + 76, TEXT, false);
        gui.drawString(font, "POWER", x + 118, y + 66, DIM, false);
        gui.drawString(font, String.format("%.0f W", powerW), x + 118, y + 76, TEXT, false);

        MachineStatus ms = menu.getMachineStatus();
        int dot = switch (ms) {
            case RUNNING -> CYAN;
            case BROWNOUT, BLOCKED -> WARN;
            case NO_POWER, IDLE -> DIM;
        };
        gui.fill(x + 12, y + 94, x + 18, y + 100, dot);
        gui.drawString(font, ms.display(), x + 22, y + 93, dot, false);

        NodeStatus ns = menu.getNodeStatus();
        int ndot = switch (ns) {
            case OK -> CYAN;
            case UNDERPOWERED -> WARN;
            case OVERLOADED, CONFLICT -> BAD;
            case NO_INPUT, DISABLED -> DIM;
        };
        gui.drawString(font, "SHAFT " + ns.name(), x + 118, y + 93, ndot, false);
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
