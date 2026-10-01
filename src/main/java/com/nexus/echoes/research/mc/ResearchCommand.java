package com.nexus.echoes.research.mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nexus.echoes.research.PlayerResearchState;
import com.nexus.echoes.research.ResearchDefinition;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchOutcome;
import com.nexus.echoes.research.ResearchService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /nexus research} — status plus explicit dev/admin tools (ADR-010).
 *
 * <p>Read commands need no permission (a player may always inspect their own
 * progression). Mutating commands ({@code add}, {@code complete},
 * {@code reset}) require permission level 2 and are documented as
 * development/admin tools — they never run on the client.
 */
public final class ResearchCommand {

    private ResearchCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nexus")
                .then(Commands.literal("research")
                        .executes(ResearchCommand::status)
                        .then(Commands.literal("list").executes(ResearchCommand::list))
                        .then(Commands.literal("add")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ResearchCommand::add)))
                        .then(Commands.literal("complete")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.string())
                                        .executes(ResearchCommand::complete)))
                        .then(Commands.literal("reset")
                                .requires(s -> s.hasPermission(2))
                                .executes(ResearchCommand::reset))));
    }

    private static ServerPlayer playerOf(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus research in-game."));
            return 0;
        }
        ResearchGraph graph = ResearchManager.graph(player.getServer());
        PlayerResearchState state = ResearchManager.stateOf(player);
        ctx.getSource().sendSystemMessage(Component.translatable(
                "message.nexus_echoes.research_status", state.points(), state.completed().size()));
        List<ResourceLocation> available = ResearchService.available(graph, state);
        if (available.isEmpty()) {
            ctx.getSource().sendSystemMessage(
                    Component.translatable("message.nexus_echoes.research_none_available"));
        } else {
            for (ResourceLocation id : available) {
                ResearchDefinition def = graph.get(id);
                ctx.getSource().sendSystemMessage(Component.literal(
                        "  " + def.title() + " — " + def.cost() + " pts"));
            }
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus research in-game."));
            return 0;
        }
        ResearchGraph graph = ResearchManager.graph(player.getServer());
        PlayerResearchState state = ResearchManager.stateOf(player);
        List<String> lines = new ArrayList<>();
        for (ResourceLocation id : graph.topologicalOrder()) {
            ResearchDefinition def = graph.get(id);
            String tag = state.isCompleted(id) ? "[COMPLETE]"
                    : ResearchService.canComplete(graph, state, id) == ResearchOutcome.OK ? "[AVAILABLE]"
                    : "[LOCKED]";
            lines.add(tag + " " + id + " (" + def.cost() + " pts)");
        }
        if (lines.isEmpty()) {
            ctx.getSource().sendSystemMessage(
                    Component.translatable("message.nexus_echoes.research_none_defined"));
            return 1;
        }
        lines.forEach(l -> ctx.getSource().sendSystemMessage(Component.literal(l)));
        return 1;
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus research in-game."));
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        if (amount <= 0) {
            ctx.getSource().sendFailure(
                    Component.translatable("message.nexus_echoes.research_invalid_amount"));
            return 0;
        }
        ResearchManager.mutate(player, s -> s.addPoints(amount));
        ctx.getSource().sendSystemMessage(Component.translatable(
                "message.nexus_echoes.research_added",
                amount, ResearchManager.stateOf(player).points()));
        return 1;
    }

    private static int complete(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus research in-game."));
            return 0;
        }
        String raw = StringArgumentType.getString(ctx, "id");
        ResourceLocation id;
        try {
            id = new ResourceLocation(raw);
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                    Component.translatable("message.nexus_echoes.research_unknown", raw));
            return 0;
        }
        ResearchOutcome outcome = ResearchManager.tryComplete(player, id);
        return switch (outcome) {
            case OK -> {
                ctx.getSource().sendSystemMessage(Component.translatable(
                        "message.nexus_echoes.research_completed", raw));
                yield 1;
            }
            case ALREADY_COMPLETED -> {
                ctx.getSource().sendFailure(
                        Component.translatable("message.nexus_echoes.research_already"));
                yield 0;
            }
            case MISSING_PREREQUISITE -> {
                ctx.getSource().sendFailure(
                        Component.translatable("message.nexus_echoes.research_missing_prereq"));
                yield 0;
            }
            case INSUFFICIENT_POINTS -> {
                ctx.getSource().sendFailure(
                        Component.translatable("message.nexus_echoes.research_insufficient"));
                yield 0;
            }
            default -> {
                ctx.getSource().sendFailure(
                        Component.translatable("message.nexus_echoes.research_unknown", raw));
                yield 0;
            }
        };
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus research in-game."));
            return 0;
        }
        ResearchSavedData.get(player.server.overworld()).reset(player.getUUID());
        ResearchManager.syncToClient(player);
        ctx.getSource().sendSystemMessage(
                Component.translatable("message.nexus_echoes.research_reset"));
        return 1;
    }
}
