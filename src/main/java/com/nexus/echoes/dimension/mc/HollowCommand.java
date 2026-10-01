package com.nexus.echoes.dimension.mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.nexus.echoes.dimension.anomaly.AnomalyInstance;
import com.nexus.echoes.dimension.api.HollowDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * {@code /nexus hollow} — development/admin tools (ADR-011).
 *
 * <p>Read commands need no permission. Anything that teleports or mutates
 * world state requires permission level 2 and never runs on the client.
 */
public final class HollowCommand {

    private HollowCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nexus")
                .then(Commands.literal("hollow")
                        .executes(HollowCommand::info)
                        .then(Commands.literal("info").executes(HollowCommand::info))
                        .then(Commands.literal("anomalies").executes(HollowCommand::anomalies))
                        .then(Commands.literal("tp")
                                .requires(s -> s.hasPermission(2))
                                .executes(HollowCommand::tp))
                        .then(Commands.literal("back")
                                .requires(s -> s.hasPermission(2))
                                .executes(HollowCommand::back))));
    }

    private static ServerPlayer playerOf(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getEntity() instanceof ServerPlayer player ? player : null;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus hollow in-game."));
            return 0;
        }
        boolean access = HollowTravel.canAccess(player);
        int discoveries = HollowDiscoveryData.get(player.getServer()).count(player.getUUID());
        boolean inHollow = HollowDimensions.HOLLOW_LEVEL.equals(player.level().dimension());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Hollow access: " + (access ? "UNLOCKED" : "LOCKED")
                        + " | discoveries: " + discoveries
                        + " | dimension: " + (inHollow ? "the_hollow" : "overworld")),
                false);
        return 1;
    }

    private static int anomalies(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus hollow in-game."));
            return 0;
        }
        ServerLevel hollow = player.getServer().getLevel(HollowDimensions.HOLLOW_LEVEL);
        if (hollow == null) {
            ctx.getSource().sendFailure(Component.literal("The Hollow is not loaded."));
            return 0;
        }
        List<AnomalyInstance> active = AnomalySavedData.get(hollow).active();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Active anomalies: " + active.size()), false);
        for (AnomalyInstance anomaly : active) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    " - " + anomaly.definitionId() + " @ "
                            + anomaly.x() + "," + anomaly.y() + "," + anomaly.z()),
                    false);
        }
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus hollow in-game."));
            return 0;
        }
        BlockPos from = player.blockPosition();
        boolean ok = HollowTravel.travelTo(player, from);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal("Travel failed (see chat)."));
            return 0;
        }
        return 1;
    }

    private static int back(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = playerOf(ctx);
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run /nexus hollow in-game."));
            return 0;
        }
        if (!HollowTravel.travelBack(player)) {
            ctx.getSource().sendFailure(Component.literal("No recorded return link."));
            return 0;
        }
        return 1;
    }
}
