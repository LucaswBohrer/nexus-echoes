package com.nexus.echoes.kinetic.mc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.nexus.echoes.kinetic.api.NodeStatus;
import com.nexus.echoes.kinetic.sim.KineticSnapshot;
import com.nexus.echoes.kinetic.sim.NodeState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /nexus kinetic} — read-only diagnostics for kinetic networks.
 * Server-authoritative: everything shown comes from the manager's snapshots.
 */
public final class KineticCommand {

    private KineticCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nexus")
                .then(Commands.literal("kinetic")
                        .executes(KineticCommand::report)));
    }

    private static int report(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Run /nexus kinetic in-game."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        KineticManager manager = KineticManager.get(level);

        BlockPos target = raycastTarget(player);
        int networkId = target != null ? manager.networkIdAt(target) : -1;

        if (networkId < 0) {
            return reportAll(source, manager);
        }
        KineticSnapshot snapshot = manager.snapshots().get(networkId);
        if (snapshot == null) {
            source.sendFailure(Component.literal("No kinetic network under the targeted block."));
            return 0;
        }
        describeNetwork(source, networkId, snapshot);
        return 1;
    }

    private static int reportAll(CommandSourceStack source, KineticManager manager) {
        Map<Integer, KineticSnapshot> snapshots = manager.snapshots();
        if (snapshots.isEmpty()) {
            source.sendSystemMessage(Component.literal(
                    "No kinetic networks simulated yet (" + manager.nodeCount() + " nodes registered)."));
            return 1;
        }
        source.sendSystemMessage(Component.literal(
                "Kinetic networks: " + snapshots.size() + " (" + manager.nodeCount() + " nodes)"));
        for (Map.Entry<Integer, KineticSnapshot> e : snapshots.entrySet()) {
            describeNetwork(source, e.getKey(), e.getValue());
        }
        return 1;
    }

    private static void describeNetwork(CommandSourceStack source, int networkId, KineticSnapshot snapshot) {
        String load = snapshot.loadFactor() == Double.POSITIVE_INFINITY
                ? "∞" : String.format("%.0f%%", snapshot.loadFactor() * 100);
        source.sendSystemMessage(Component.literal(
                "[net " + networkId + "] nodes=" + snapshot.states().size()
                        + " load=" + load
                        + " supply=" + (int) snapshot.totalSupplyPowerW() + "W"
                        + " demand=" + (int) snapshot.totalDemandPowerW() + "W"
                        + (snapshot.overloaded() ? " §cOVERLOADED" : " ok")
                        + (snapshot.conflict() ? " §cSOURCE CONFLICT" : "")));
        List<String> ids = new ArrayList<>(snapshot.states().keySet());
        ids.sort(String::compareTo);
        for (String id : ids) {
            NodeState s = snapshot.states().get(id);
            source.sendSystemMessage(Component.literal(
                    "  " + id + " " + (int) s.rpm().value() + "rpm "
                            + String.format("%.1f", s.torque().newtonMeters()) + "Nm "
                            + s.direction().name() + " " + statusTag(s.status())));
        }
    }

    private static String statusTag(NodeStatus status) {
        return switch (status) {
            case OK -> "ok";
            case OVERLOADED -> "§cOVERLOADED";
            case UNDERPOWERED -> "§eUNDERPOWERED";
            case NO_INPUT -> "§7no input";
            case DISABLED -> "§7disabled";
            case CONFLICT -> "§cSOURCE CONFLICT";
        };
    }

    /** Block the player is looking at (≤8m) if it hosts a kinetic node. */
    private static BlockPos raycastTarget(ServerPlayer player) {
        HitResult hit = player.pick(8.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        BlockEntity be = player.serverLevel().getBlockEntity(pos);
        return be instanceof KineticNodeProvider ? pos : null;
    }
}
