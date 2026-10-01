package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.dimension.anomaly.AnomalyDefinition;
import com.nexus.echoes.dimension.anomaly.AnomalyEffect;
import com.nexus.echoes.dimension.anomaly.AnomalyInstance;
import com.nexus.echoes.dimension.anomaly.AnomalyService;
import com.nexus.echoes.dimension.block.WardBlockEntity;
import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import com.nexus.echoes.dimension.entity.RiftPhantomEntity;
import com.nexus.echoes.kinetic.mc.KineticManager;
import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Server-side anomaly driver for The Hollow (ADR-011).
 *
 * <p>Runs on a 20-tick cadence — never per tick, never a global scan:
 * <ul>
 *   <li>expires finished anomalies (clearing their kinetic derates),</li>
 *   <li>rarely spawns a new anomaly near a random player in the Hollow,</li>
 *   <li>applies each active anomaly's effect to players in its radius.</li>
 * </ul>
 * The client only renders (particles/sound). All activation, timing, rewards
 * and persistence are server-owned. Ward blocks suppress effects in their
 * radius; the check is a bounded iteration over registered ward positions.
 */
public final class AnomalyManager {

    private static final Logger LOG = LogManager.getLogger();

    /** Sweep cadence in ticks. */
    public static final int SWEEP_INTERVAL_TICKS = 20;
    /** Per-sweep spawn roll: keeps anomalies sparse. */
    private static final double SPAWN_CHANCE_PER_SWEEP = 0.06;
    /** Anomaly spawn ring around the chosen player. */
    private static final int SPAWN_MIN_DIST = 24;
    private static final int SPAWN_MAX_DIST = 64;

    private final List<AnomalyDefinition> definitions =
            AnomalyDefinition.builtins(NexusEchoes.MOD_ID);
    /** Rift-phantom spawn cooldown per player (transient). */
    private final Map<UUID, Long> lastRiftTeleport = new HashMap<>();
    /** Players already rewarded for witnessing each anomaly instance (transient). */
    private final Map<UUID, Long> lastResearchEchoReward = new HashMap<>();

    private final Random random = new Random();

    public void tick(ServerLevel hollow) {
        if (hollow.getGameTime() % SWEEP_INTERVAL_TICKS != 0) {
            return;
        }
        AnomalySavedData data = AnomalySavedData.get(hollow);
        long now = hollow.getGameTime();

        // Expire.
        AnomalyService.Partition partition =
                AnomalyService.partition(now, new ArrayList<>(data.active()));
        for (AnomalyInstance expired : partition.expired()) {
            data.remove(expired);
            clearAnomalyDerates(hollow, expired);
            onAnomalyExpired(hollow, expired);
        }

        // Maybe spawn near a random player actually in the Hollow.
        List<ServerPlayer> players = playersIn(hollow);
        if (!players.isEmpty() && random.nextDouble() < SPAWN_CHANCE_PER_SWEEP) {
            ServerPlayer anchor = players.get(random.nextInt(players.size()));
            trySpawnNear(hollow, data, anchor, now);
        }

        // Apply effects.
        for (AnomalyInstance anomaly : data.active()) {
            applyEffect(hollow, data, anomaly, now);
        }
    }

    // ------------------------------------------------------------------ spawn

    private void trySpawnNear(ServerLevel hollow, AnomalySavedData data,
                              ServerPlayer anchor, long now) {
        Optional<AnomalyDefinition> selected =
                AnomalyService.selectSpawn(random, data.active().size(), definitions);
        if (selected.isEmpty()) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = SPAWN_MIN_DIST + random.nextDouble() * (SPAWN_MAX_DIST - SPAWN_MIN_DIST);
        int x = anchor.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int z = anchor.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        BlockPos column = new BlockPos(x, 0, z);
        if (!hollow.hasChunkAt(column)) {
            return;
        }
        int y = hollow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        AnomalyDefinition def = selected.get();
        AnomalyInstance instance = AnomalyService.spawn(random, def, x, y, z, now);
        data.add(instance);
        LOG.debug("[nexus_echoes] anomaly spawned: {} at {},{},{}",
                def.id(), x, y, z);
        hollow.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, y + 1.5, z + 0.5,
                24, 1.0, 1.5, 1.0, 0.05);
    }

    // ----------------------------------------------------------------- effects

    private void applyEffect(ServerLevel hollow, AnomalySavedData data,
                             AnomalyInstance anomaly, long now) {
        AnomalyDefinition def = definitionOf(anomaly.definitionId());
        if (def == null) {
            return;
        }
        BlockPos center = new BlockPos(anomaly.x(), anomaly.y(), anomaly.z());
        AABB box = new AABB(center).inflate(def.radius());
        for (ServerPlayer player : hollow.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (data.isWarded(player.blockPosition(), WardBlockEntity.RADIUS)) {
                continue;
            }
            grantWitnessReward(hollow, data, player, anomaly);
            switch (def.effect()) {
                case STATIC_FIELD -> applyStaticField(hollow, player, center, def, anomaly);
                case ECHO_BURST -> applyEchoBurst(hollow, player, center, def);
                case SPATIAL_RIFT -> applySpatialRift(hollow, player, center, def, now);
                case RESEARCH_ECHO -> applyResearchEcho(hollow, player, center, def, now);
            }
        }
        // Static fields also sap nearby machines — adapter-layer derate.
        if (def.effect() == AnomalyEffect.STATIC_FIELD) {
            applyStaticFieldDerate(hollow, center, def, anomaly);
        }
        renderAnomaly(hollow, center, def, now);
    }

    private void applyStaticField(ServerLevel hollow, ServerPlayer player,
                                  BlockPos center, AnomalyDefinition def, AnomalyInstance anomaly) {
        int strength = anomaly.intensity() > 0.7 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                SWEEP_INTERVAL_TICKS * 2, strength, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                SWEEP_INTERVAL_TICKS * 2, 0, false, true));
    }

    private void applyStaticFieldDerate(ServerLevel hollow, BlockPos center,
                                        AnomalyDefinition def, AnomalyInstance anomaly) {
        KineticManager kinetic = KineticManager.get(hollow);
        double factor = 1.0 - 0.6 * anomaly.intensity();
        AABB box = new AABB(center).inflate(def.radius());
        // Bounded: iterate the box, but only positions that actually host
        // kinetic nodes get derates — providerAt is a map lookup.
        BlockPos.betweenClosed(
                new BlockPos((int) box.minX, (int) box.minY, (int) box.minZ),
                new BlockPos((int) box.maxX, (int) box.maxY, (int) box.maxZ))
                .forEach(p -> {
                    if (kinetic.providerAt(p) != null) {
                        kinetic.setDerate(p, factor);
                    }
                });
    }

    private void clearAnomalyDerates(ServerLevel hollow, AnomalyInstance anomaly) {
        AnomalyDefinition def = definitionOf(anomaly.definitionId());
        if (def == null || def.effect() != AnomalyEffect.STATIC_FIELD) {
            return;
        }
        // Only clear derates that no other active static field still covers.
        KineticManager kinetic = KineticManager.get(hollow);
        BlockPos center = new BlockPos(anomaly.x(), anomaly.y(), anomaly.z());
        AABB box = new AABB(center).inflate(def.radius());
        AnomalySavedData data = AnomalySavedData.get(hollow);
        BlockPos.betweenClosed(
                new BlockPos((int) box.minX, (int) box.minY, (int) box.minZ),
                new BlockPos((int) box.maxX, (int) box.maxY, (int) box.maxZ))
                .forEach(p -> {
                    if (kinetic.providerAt(p) == null) {
                        return;
                    }
                    boolean stillCovered = false;
                    for (AnomalyInstance other : data.active()) {
                        AnomalyDefinition otherDef = definitionOf(other.definitionId());
                        if (otherDef != null && otherDef.effect() == AnomalyEffect.STATIC_FIELD
                                && !other.instanceId().equals(anomaly.instanceId())) {
                            BlockPos oc = new BlockPos(other.x(), other.y(), other.z());
                            if (oc.distSqr(p) <= (long) otherDef.radius() * otherDef.radius()) {
                                stillCovered = true;
                                break;
                            }
                        }
                    }
                    if (!stillCovered) {
                        kinetic.clearDerate(p);
                    }
                });
    }

    private void applyEchoBurst(ServerLevel hollow, ServerPlayer player,
                                BlockPos center, AnomalyDefinition def) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                20 * 10, 0, false, true));
        hollow.playSound(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 0.6F);
    }

    private void applySpatialRift(ServerLevel hollow, ServerPlayer player,
                                  BlockPos center, AnomalyDefinition def, long now) {
        Long last = lastRiftTeleport.get(player.getUUID());
        if (last != null && now - last < 20 * 15) {
            return;
        }
        lastRiftTeleport.put(player.getUUID(), now);
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 4.0 + random.nextDouble() * 6.0;
        int nx = player.getBlockX() + (int) Math.round(Math.cos(angle) * dist);
        int nz = player.getBlockZ() + (int) Math.round(Math.sin(angle) * dist);
        BlockPos column = new BlockPos(nx, 0, nz);
        if (!hollow.hasChunkAt(column)) {
            return;
        }
        int ny = hollow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, nx, nz);
        hollow.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 16, 0.4, 0.8, 0.4, 0.1);
        player.teleportTo(nx + 0.5, ny, nz + 0.5);
        maybeSpawnPhantom(hollow, new BlockPos(nx, ny, nz));
    }

    private void maybeSpawnPhantom(ServerLevel hollow, BlockPos pos) {
        if (random.nextDouble() > 0.35) {
            return;
        }
        EntityType<RiftPhantomEntity> type = NexusRegistries.RIFT_PHANTOM.get();
        RiftPhantomEntity phantom = type.create(hollow);
        if (phantom != null) {
            phantom.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            hollow.addFreshEntity(phantom);
        }
    }

    private void applyResearchEcho(ServerLevel hollow, ServerPlayer player,
                                   BlockPos center, AnomalyDefinition def, long now) {
        Long last = lastResearchEchoReward.get(player.getUUID());
        if (last != null && now - last < 20 * 120) {
            return;
        }
        lastResearchEchoReward.put(player.getUUID(), now);
        ResearchManager.grantFromSource(player, ResearchSources.DISCOVER_ANOMALY);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.nexus_echoes.research_echo"), false);
    }

    private void grantWitnessReward(ServerLevel hollow, AnomalySavedData data,
                                    ServerPlayer player, AnomalyInstance anomaly) {
        if (HollowDiscoveryData.get(hollow.getServer()).grant(player.getUUID(), DiscoveryIds.ANOMALY_SEEN)) {
            ResearchManager.grantFromSource(player, ResearchSources.DISCOVER_ANOMALY);
            DiscoverySync.syncTo(player);
        }
    }

    private void onAnomalyExpired(ServerLevel hollow, AnomalyInstance anomaly) {
        // Rift phantoms belong to their rift: discard them when it closes.
        AnomalyDefinition def = definitionOf(anomaly.definitionId());
        if (def != null && def.effect() == AnomalyEffect.SPATIAL_RIFT) {
            BlockPos center = new BlockPos(anomaly.x(), anomaly.y(), anomaly.z());
            AABB box = new AABB(center).inflate(def.radius() * 2.0);
            for (RiftPhantomEntity phantom :
                    hollow.getEntitiesOfClass(RiftPhantomEntity.class, box)) {
                phantom.discard();
            }
        }
    }

    private void renderAnomaly(ServerLevel hollow, BlockPos center,
                               AnomalyDefinition def, long now) {
        // Server-driven vanilla particles only — no custom packets.
        var particle = switch (def.effect()) {
            case STATIC_FIELD -> ParticleTypes.ELECTRIC_SPARK;
            case ECHO_BURST -> ParticleTypes.SONIC_BOOM;
            case SPATIAL_RIFT -> ParticleTypes.REVERSE_PORTAL;
            case RESEARCH_ECHO -> ParticleTypes.ENCHANT;
        };
        hollow.sendParticles(particle,
                center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                4, def.radius() / 3.0, 1.5, def.radius() / 3.0, 0.02);
    }

    // ------------------------------------------------------------------ helpers

    private AnomalyDefinition definitionOf(ResourceLocation id) {
        for (AnomalyDefinition def : definitions) {
            if (def.id().equals(id)) {
                return def;
            }
        }
        return null;
    }

    private List<ServerPlayer> playersIn(ServerLevel hollow) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : hollow.players()) {
            out.add(player);
        }
        return out;
    }

    /** Anchor point for the scanner's anomaly sweep (test seam). */
    Vec3 anomalyCenter(AnomalyInstance anomaly) {
        return new Vec3(anomaly.x() + 0.5, anomaly.y(), anomaly.z() + 0.5);
    }
}
