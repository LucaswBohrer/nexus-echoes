package com.nexus.echoes.dimension.item;

import com.nexus.echoes.dimension.api.HollowDimensions;
import com.nexus.echoes.dimension.mc.AnomalySavedData;
import com.nexus.echoes.dimension.mc.HollowTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Resonance Scanner: the second Hollow-derived technology (unlocked by the
 * {@code anomaly_studies} research). Right-click sweeps for the nearest
 * anomaly or obelisk landmark and reports bearing and distance — an
 * exploration tool, not a weapon. Server-side only; the client just swings.
 */
public class ResonanceScannerItem extends Item {

    private static final int RANGE = 128;
    private static final int COOLDOWN_TICKS = 100;

    public ResonanceScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!HollowDimensions.HOLLOW_LEVEL.equals(serverPlayer.serverLevel().dimension())) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.scanner_wrong_dimension"), true);
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel hollow = serverPlayer.serverLevel();
        BlockPos origin = serverPlayer.blockPosition();

        String found = scanAnomalies(hollow, origin);
        if (found == null) {
            found = scanObelisks(hollow, origin);
        }
        if (found != null) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.scanner_contact", found), false);
            hollow.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        } else {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.scanner_silence"), true);
        }
        serverPlayer.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static String scanAnomalies(ServerLevel hollow, BlockPos origin) {
        var anomalies = AnomalySavedData.get(hollow).active();
        double best = Double.MAX_VALUE;
        net.minecraft.resources.ResourceLocation bestId = null;
        net.minecraft.core.BlockPos bestPos = null;
        for (var anomaly : anomalies) {
            double d = origin.distSqr(new net.minecraft.core.BlockPos(anomaly.x(), anomaly.y(), anomaly.z()));
            if (d < best && d <= (double) RANGE * RANGE) {
                best = d;
                bestId = anomaly.definitionId();
                bestPos = new net.minecraft.core.BlockPos(anomaly.x(), anomaly.y(), anomaly.z());
            }
        }
        if (bestId == null || bestPos == null) {
            return null;
        }
        return Component.translatable("message.nexus_echoes.scanner_anomaly",
                bestId.getPath(), (int) Math.sqrt(best),
                bearing(origin, bestPos.getX(), bestPos.getZ())).getString();
    }

    private static String scanObelisks(ServerLevel hollow, BlockPos origin) {
        BlockPos found = HollowTravel.findNearestObelisk(hollow, origin, RANGE);
        if (found == null) {
            return null;
        }
        int dist = (int) Math.sqrt(origin.distSqr(found));
        return Component.translatable("message.nexus_echoes.scanner_obelisk",
                dist, bearing(origin, found.getX(), found.getZ())).getString();
    }

    private static String bearing(BlockPos origin, int x, int z) {
        double dx = x - origin.getX();
        double dz = z - origin.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        String[] points = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int idx = (int) Math.round(angle / 45.0);
        idx = ((idx % 8) + 8) % 8;
        return points[idx];
    }
}
