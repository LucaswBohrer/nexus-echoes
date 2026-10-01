package com.nexus.echoes.research.client.screen;

import com.nexus.echoes.dimension.client.DiscoveryClientState;
import com.nexus.echoes.research.client.ResearchClientState;
import com.nexus.echoes.research.codex.CodexEntry;
import com.nexus.echoes.research.codex.CodexVisibility;
import com.nexus.echoes.research.mc.CodexLoader;
import com.nexus.echoes.research.PlayerResearchState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The NEXUS Codex (Phase 4 foundation).
 *
 * <p>Left: entries visible to the player (locked entries hidden entirely).
 * Right: the entry's content. Visibility is a pure function of the synced
 * completed-research set; entries come from the client's datapack copy.
 */
@OnlyIn(Dist.CLIENT)
public class CodexScreen extends Screen {

    private static final int PANEL = 0xFF0B1220;
    private static final int PANEL_EDGE = 0xFF1E2A3A;
    private static final int CYAN = 0xFF35E0E6;
    private static final int TEXT = 0xFFD7E3F4;
    private static final int DIM = 0xFF5A6A7A;

    private final Screen parent;
    private final List<CodexEntry> visible = new ArrayList<>();
    private CodexEntry selected;
    private long seenVersion = -1;
    private long seenDiscoveryVersion = -1;

    public CodexScreen(Screen parent) {
        super(Component.translatable("gui.nexus_echoes.codex"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
        seenVersion = ResearchClientState.version();
        seenDiscoveryVersion = DiscoveryClientState.version();
    }

    private void rebuild() {
        clearWidgets();
        visible.clear();

        // re-materialize a read-only state view from the synced snapshot
        PlayerResearchState view = new PlayerResearchState();
        for (ResourceLocation id : ResearchClientState.completed()) {
            view.markCompleted(id);
        }
        for (Map.Entry<ResourceLocation, CodexEntry> e : CodexLoader.get().entrySet()) {
            if (CodexVisibility.isVisible(e.getValue(), view, DiscoveryClientState.all())) {
                visible.add(e.getValue());
            }
        }
        if (selected != null && !visible.contains(selected)) {
            selected = null;
        }
        if (selected == null && !visible.isEmpty()) {
            selected = visible.get(0);
        }

        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.nexus_echoes.back"),
                        b -> Minecraft.getInstance().setScreen(parent))
                .bounds(cx - 170, height - 32, 80, 20).build());

        int y = 52;
        for (CodexEntry entry : visible) {
            CodexEntry ref = entry;
            addRenderableWidget(Button.builder(
                            Component.literal((entry.equals(selected) ? "> " : "") + entry.title()),
                            b -> {
                                selected = ref;
                                rebuild();
                            })
                    .bounds(cx - 170, y, 150, 20).build());
            y += 24;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ResearchClientState.version() != seenVersion
                || DiscoveryClientState.version() != seenDiscoveryVersion) {
            rebuild();
            seenVersion = ResearchClientState.version();
            seenDiscoveryVersion = DiscoveryClientState.version();
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        renderBackground(gui);
        int cx = width / 2;
        gui.fill(cx - 180, 8, cx + 180, height - 8, PANEL);
        gui.fill(cx - 180, 8, cx + 180, 9, PANEL_EDGE);
        super.render(gui, mouseX, mouseY, partialTick);
        gui.drawCenteredString(font, title, cx, 14, CYAN);

        if (selected != null) {
            int x = cx - 8;
            int y = 52;
            gui.drawString(font, selected.title(), x, y, CYAN, false);
            gui.drawString(font, selected.category(), x, y + 12, DIM, false);
            y += 30;
            for (String line : wrap(selected)) {
                if (y > height - 40) {
                    break;
                }
                gui.drawString(font, line, x, y, TEXT, false);
                y += 11;
            }
        } else {
            gui.drawString(font,
                    Component.translatable("gui.nexus_echoes.codex_empty").getString(),
                    cx - 8, 52, DIM, false);
        }
    }

    private List<String> wrap(CodexEntry entry) {
        List<String> out = new ArrayList<>();
        int maxWidth = 330;
        for (String paragraph : entry.content()) {
            // StringSplitter.splitLines returns FormattedText; use the plain strings.
            for (FormattedText line : font.getSplitter().splitLines(paragraph, maxWidth, Style.EMPTY)) {
                out.add(line.getString());
            }
            out.add("");
        }
        return out;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
