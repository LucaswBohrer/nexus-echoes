package com.nexus.echoes.research.client.screen;

import com.nexus.echoes.network.NexusNetwork;
import com.nexus.echoes.research.ResearchDefinition;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchOutcome;
import com.nexus.echoes.research.client.ResearchClientState;
import com.nexus.echoes.research.mc.ResearchDefinitionLoader;
import com.nexus.echoes.research.network.BuyResearchPacket;
import com.nexus.echoes.research.network.OpenResearchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * The NEXUS research screen (Phase 4).
 *
 * <p>Functional, not flashy: points balance, every research with status,
 * cost, prerequisites and unlocks (hover for details). Buying sends a C2S
 * packet — the server validates and answers with a snapshot, which refreshes
 * this screen. All displayed state is server-authoritative; the client never
 * decides.
 */
@OnlyIn(Dist.CLIENT)
public class ResearchScreen extends Screen {

    private static final int PANEL = 0xFF0B1220;
    private static final int PANEL_EDGE = 0xFF1E2A3A;
    private static final int CYAN = 0xFF35E0E6;
    private static final int TEXT = 0xFFD7E3F4;
    private static final int DIM = 0xFF5A6A7A;
    private static final int GOOD = 0xFF4ADE80;

    private long seenVersion = -1;

    public ResearchScreen() {
        super(Component.translatable("gui.nexus_echoes.research"));
    }

    @Override
    protected void init() {
        // ask the server for a fresh snapshot every time the screen opens
        NexusNetwork.CHANNEL.sendToServer(new OpenResearchPacket());
        rebuild();
        seenVersion = ResearchClientState.version();
    }

    private void rebuild() {
        clearWidgets();
        int cx = width / 2;

        addRenderableWidget(Button.builder(Component.translatable("gui.nexus_echoes.codex"),
                        b -> Minecraft.getInstance().setScreen(new CodexScreen(this)))
                .bounds(cx + 60, 28, 100, 20).build());

        ResearchGraph graph = ResearchDefinitionLoader.get();
        if (graph == null) {
            return;
        }
        int y = 56;
        for (ResourceLocation id : graph.topologicalOrder()) {
            ResearchDefinition def = graph.get(id);
            ResearchOutcome outcome = displayOutcome(def);
            Button button = Button.builder(Component.literal(buttonLabel(def, outcome)),
                            b -> NexusNetwork.CHANNEL.sendToServer(new BuyResearchPacket(id)))
                    .bounds(cx - 160, y, 320, 20)
                    .tooltip(Tooltip.create(detailText(graph, def, outcome)))
                    .build();
            button.active = outcome == ResearchOutcome.OK;
            addRenderableWidget(button);
            y += 24;
        }
    }

    /** Display-only status; the server re-validates on buy. */
    private ResearchOutcome displayOutcome(ResearchDefinition def) {
        if (ResearchClientState.isCompleted(def.id())) {
            return ResearchOutcome.ALREADY_COMPLETED;
        }
        for (ResourceLocation prereq : def.prerequisites()) {
            if (!ResearchClientState.isCompleted(prereq)) {
                return ResearchOutcome.MISSING_PREREQUISITE;
            }
        }
        return ResearchClientState.points() >= def.cost()
                ? ResearchOutcome.OK : ResearchOutcome.INSUFFICIENT_POINTS;
    }

    private String buttonLabel(ResearchDefinition def, ResearchOutcome outcome) {
        String tag = switch (outcome) {
            case ALREADY_COMPLETED -> "[COMPLETE] ";
            case OK -> "[RESEARCH] ";
            case INSUFFICIENT_POINTS -> "[NEED PTS] ";
            default -> "[LOCKED] ";
        };
        return tag + def.title() + " — " + def.cost() + " pts";
    }

    private Component detailText(ResearchGraph graph, ResearchDefinition def, ResearchOutcome outcome) {
        List<String> lines = new ArrayList<>();
        lines.add(def.description());
        if (!def.prerequisites().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation p : def.prerequisites()) {
                ResearchDefinition pd = graph.get(p);
                names.add((pd != null ? pd.title() : p.toString())
                        + (ResearchClientState.isCompleted(p) ? " ✓" : " ✗"));
            }
            lines.add("Requires: " + String.join(", ", names));
        }
        if (!def.unlocks().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (ResourceLocation u : def.unlocks()) {
                names.add(prettyTech(u));
            }
            lines.add("Unlocks: " + String.join(", ", names));
        }
        lines.add(switch (outcome) {
            case OK -> "Click to research.";
            case INSUFFICIENT_POINTS -> "Not enough research points.";
            case MISSING_PREREQUISITE -> "Complete the prerequisites first.";
            default -> "";
        });
        return Component.literal(String.join("\n", lines.stream().filter(s -> !s.isEmpty()).toList()));
    }

    private String prettyTech(ResourceLocation tech) {
        return switch (tech.getPath()) {
            case "crusher" -> "Crusher technology";
            case "processor" -> "Processor technology";
            case "nexus_component" -> "Nexus Component crafting";
            case "hollow_access" -> "??? (future)";
            default -> tech.toString();
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (ResearchClientState.version() != seenVersion) {
            rebuild();
            seenVersion = ResearchClientState.version();
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
        gui.drawCenteredString(font,
                Component.translatable("gui.nexus_echoes.research_points", ResearchClientState.points()),
                cx, 30, TEXT);
        ResearchGraph graph = ResearchDefinitionLoader.get();
        if (graph != null) {
            int done = ResearchClientState.completed().size();
            gui.drawString(font, done + "/" + graph.size(), cx + 150, 14, GOOD, false);
            gui.drawString(font, Component.translatable("gui.nexus_echoes.research_hint").getString(),
                    cx - 170, height - 20, DIM, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
