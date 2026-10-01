package com.nexus.echoes.client.screen;

import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.machines.ResonatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Resonator GUI (Phase 2: kinetic). Shows shaft state — RPM, torque, status —
 * instead of the Phase 1 energy bar, plus the recipe progress arrow.
 *
 * <p>Rendered with primitives in the NEXUS visual identity; all values come
 * from the menu's server-synced {@code ContainerData}. No game logic lives here.
 */
public class ResonatorScreen extends AbstractContainerScreen<ResonatorMenu> {

    private static final int PANEL = 0xFF0B1220;
    private static final int PANEL_EDGE = 0xFF1E2A3A;
    private static final int CYAN = 0xFF35E0E6;
    private static final int CYAN_DIM = 0xFF1A5A5E;
    private static final int TEXT = 0xFFD7E3F4;
    private static final int WARN = 0xFFE0A335;
    private static final int BAD = 0xFFE05252;

    public ResonatorScreen(ResonatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // main tech panel
        gui.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        gui.fill(x, y, x + imageWidth, y + 1, PANEL_EDGE);
        gui.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, PANEL_EDGE);
        gui.fill(x, y, x + 1, y + imageHeight, PANEL_EDGE);
        gui.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, PANEL_EDGE);
        // header strip
        gui.fill(x + 1, y + 1, x + imageWidth - 1, y + 20, 0xFF111B2C);

        // input / output slot frames
        drawSlotFrame(gui, x + 56 - 1, y + 35 - 1);
        drawSlotFrame(gui, x + 116 - 1, y + 35 - 1);

        // progress arrow (input -> output)
        int maxProgress = Math.max(1, menu.getMaxProgress());
        int arrowW = 24;
        int filled = (int) (arrowW * Math.min(1.0f, menu.getProgress() / (float) maxProgress));
        int ax = x + 79;
        int ay = y + 39;
        gui.fill(ax, ay, ax + arrowW, ay + 8, 0xFF0E1626);
        gui.fill(ax, ay, ax + filled, ay + 8, CYAN_DIM);
        if (filled > 0) {
            gui.fill(ax + filled - 1, ay, ax + filled, ay + 8, CYAN);
        }

        // kinetic readout (left column, replaces the Phase 1 energy bar)
        NodeStatus status = status();
        int color = switch (status) {
            case OK -> CYAN;
            case UNDERPOWERED -> WARN;
            case OVERLOADED, CONFLICT -> BAD;
            case NO_INPUT, DISABLED -> 0xFF5A6A7A;
        };
        // status dot
        gui.fill(x + 12, y + 18, x + 18, y + 24, color);
        gui.drawString(font, String.valueOf(menu.getRpm()), x + 22, y + 18, TEXT, false);
        gui.drawString(font, "RPM", x + 22, y + 28, 0xFF5A6A7A, false);
        String torque = String.format("%.1f Nm", menu.getTorqueMilliNm() / 1000.0);
        gui.drawString(font, torque, x + 12, y + 40, TEXT, false);
    }

    private NodeStatus status() {
        NodeStatus[] values = NodeStatus.values();
        int ordinal = menu.getStatusOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NodeStatus.NO_INPUT;
    }

    private void drawSlotFrame(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, 0xFF0E1626);
        gui.fill(x, y, x + 18, y + 1, PANEL_EDGE);
        gui.fill(x, y + 17, x + 18, y + 18, PANEL_EDGE);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        renderTooltip(gui, mouseX, mouseY);

        // kinetic tooltip over the readout area
        int bx = leftPos + 12;
        int by = topPos + 16;
        if (mouseX >= bx && mouseX <= bx + 60 && mouseY >= by && mouseY <= by + 36) {
            gui.renderTooltip(font,
                    Component.translatable("gui.nexus_echoes.kinetic_state",
                            menu.getRpm(), menu.getTorqueMilliNm() / 1000.0, status().name()),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(font, title, 24, 7, TEXT, false);
        gui.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }
}
