package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.dimension.api.HollowDimensions;
import com.nexus.echoes.dimension.travel.HollowTravelService;
import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.ResearchGraph;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.ResearchTechnologies;
import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;

/**
 * Server-authoritative Hollow travel (ADR-011).
 *
 * <p>Overworld → Hollow: gated on {@code hollow_access} (per player), spire
 * charge, and a bounded safe-spawn search. Hollow → Overworld: through the
 * obelisk, returning to the recorded departure spire. All movement uses
 * {@code ServerPlayer.changeDimension} with a custom {@code ITeleporter};
 * the client never decides destinations.
 */
public final class HollowTravel {

    private static final Logger LOG = LogManager.getLogger();

    private HollowTravel() {
    }

    // ------------------------------------------------------------ overworld →

    /**
     * Attempts Overworld → Hollow travel from a spire at {@code spirePos}.
     * Returns false (with a player message) when any precondition fails.
     */
    public static boolean travelTo(ServerPlayer player, BlockPos spirePos) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerLevel hollow = server.getLevel(HollowDimensions.HOLLOW_LEVEL);
        if (hollow == null) {
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.hollow_unavailable"), true);
            LOG.warn("[nexus_echoes] Hollow dimension not loaded for {}", player.getGameProfile().getName());
            return false;
        }
        if (!canAccess(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.hollow_locked"), true);
            return false;
        }

        Optional<HollowTravelService.SafeSpawn> spawn =
                HollowTravelService.findSafeSpawn(0, 0, heightQuery(hollow));
        if (spawn.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.hollow_no_spawn"), true);
            return false;
        }

        // Record the departure BEFORE teleporting so the return link exists
        // even if the player logs out mid-transit.
        PlayerTravelData.get(server).record(player.getUUID(), player.level().dimension(), spirePos);
        HollowDiscoveryData discoveries = HollowDiscoveryData.get(server);
        for (ResourceLocation discovery : HollowDiscoveryData.firstEntryDiscoveries()) {
            if (discoveries.grant(player.getUUID(), discovery)) {
                ResearchManager.grantFromSource(player, ResearchSources.ENTER_HOLLOW);
                DiscoverySync.syncTo(player);
                player.displayClientMessage(Component.translatable(
                        "message.nexus_echoes.hollow_first_entry"), false);
            }
        }

        HollowTravelService.SafeSpawn s = spawn.get();
        Vec3 dest = new Vec3(s.x() + 0.5, s.y(), s.z() + 0.5);
        player.changeDimension(hollow, new FixedTeleporter(dest));
        return true;
    }

    /** Per-player gate: research unlock, not a global flag. */
    public static boolean canAccess(ServerPlayer player) {
        ResearchGraph graph = ResearchManager.graph(player.getServer());
        return com.nexus.echoes.research.ResearchService.isUnlocked(
                graph, ResearchManager.stateOf(player), ResearchTechnologies.HOLLOW_ACCESS);
    }

    // ------------------------------------------------------------ hollow → ow

    /**
     * Obelisk return: Hollow → the recorded departure spire. Safe-spawns near
     * the spire; falls back to the Overworld spawn when the origin is gone.
     */
    public static boolean travelBack(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        PlayerTravelData.Origin origin = PlayerTravelData.get(server).originOf(player.getUUID());
        if (origin == null) {
            return false;
        }
        ServerLevel target = server.getLevel(origin.dimension());
        if (target == null) {
            return false;
        }
        BlockPos anchor = origin.pos();
        Vec3 dest = safeReturnSpot(target, anchor)
                .orElseGet(() -> new Vec3(target.getSharedSpawnPos().getX() + 0.5,
                        target.getSharedSpawnPos().getY(), target.getSharedSpawnPos().getZ() + 0.5));
        player.changeDimension(target, new FixedTeleporter(dest));
        return true;
    }

    // ----------------------------------------------------------------- search

    private static HollowTravelService.HeightQuery heightQuery(ServerLevel level) {
        return new HollowTravelService.HeightQuery() {
            @Override
            public int surfaceY(int x, int z) {
                if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                    return level.getMinBuildHeight();
                }
                return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            }

            @Override
            public boolean isObstructed(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.hasChunkAt(pos)) {
                    return true;
                }
                BlockState state = level.getBlockState(pos);
                return !state.getCollisionShape(level, pos).isEmpty() || state.liquid();
            }
        };
    }

    /** Bounded safe spot near the return anchor (16-block radius). */
    static Optional<Vec3> safeReturnSpot(ServerLevel level, BlockPos anchor) {
        for (int r = 0; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos p = anchor.offset(dx, 0, dz);
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            p.getX(), p.getZ());
                    if (isSafe(level, p.getX(), y, p.getZ())) {
                        return Optional.of(new Vec3(p.getX() + 0.5, y, p.getZ() + 0.5));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafe(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockState below = level.getBlockState(feet.below());
        return below.isSolidRender(level, feet.below())
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getFluidState(feet).isSource();
    }

    // ------------------------------------------------------- obelisk scanning

    /**
     * Bounded spiral scan for the nearest obelisk core block, coarse-grained
     * (4-block steps, heightmap columns). Called only on scanner use (5 s
     * cooldown) — never per tick.
     */
    public static BlockPos findNearestObelisk(ServerLevel hollow, BlockPos origin, int radius) {
        ResourceLocation coreId = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                .getKey(NexusRegistries.OBELISK_CORE.get());
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int r = 0; r <= radius; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    BlockPos column = new BlockPos(x, 0, z);
                    if (!hollow.hasChunkAt(column)) {
                        continue;
                    }
                    int top = hollow.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    for (int y = Math.max(1, top - 40); y <= Math.min(hollow.getMaxBuildHeight() - 1, top + 40); y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (coreId != null && coreId.equals(
                                net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                        .getKey(hollow.getBlockState(p).getBlock()))) {
                            double d = origin.distSqr(p);
                            if (d < bestD) {
                                bestD = d;
                                best = p.immutable();
                            }
                            break;
                        }
                    }
                }
            }
        }
        return best;
    }

    // -------------------------------------------------------------- teleporter

    /** Places the entity exactly at the precomputed destination. */
    private record FixedTeleporter(Vec3 dest) implements ITeleporter {

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld,
                                  ServerLevel destWorld, float yaw,
                                  Function<Boolean, Entity> repositionEntity) {
            Entity moved = repositionEntity.apply(false);
            moved.moveTo(dest.x(), dest.y(), dest.z(), yaw, moved.getXRot());
            return moved;
        }
    }
}
