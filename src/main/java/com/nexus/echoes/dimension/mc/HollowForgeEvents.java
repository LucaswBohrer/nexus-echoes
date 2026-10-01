package com.nexus.echoes.dimension.mc;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.dimension.api.HollowDimensions;
import com.nexus.echoes.dimension.block.WardBlockEntity;
import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import com.nexus.echoes.dimension.entity.HollowStalkerEntity;
import com.nexus.echoes.dimension.entity.ResonantWispEntity;
import com.nexus.echoes.dimension.entity.RiftPhantomEntity;
import com.nexus.echoes.dimension.entity.ScrapCrawlerEntity;
import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-bus wiring for The Hollow (ADR-011).
 *
 * <p>All logic is event-driven and server-side:
 * <ul>
 *   <li>login / dimension change → discovery snapshot sync,</li>
 *   <li>server tick → the anomaly manager (20-tick cadence inside),</li>
 *   <li>player tick (throttled) → proximity discoveries and the Deep Hollow
 *       exposure hazard,</li>
 *   <li>mod bus → entity attributes and spawn placements.</li>
 * </ul>
 * Nothing here scans the world per tick; proximity checks run every 200
 * ticks inside a bounded cube and exit early.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HollowForgeEvents {

    /** Throttle for proximity/exposure checks (ticks). */
    private static final int PROXIMITY_INTERVAL = 200;
    /** Proximity cube half-extent for structure discovery. */
    private static final int PROXIMITY_RADIUS = 16;

    private static final AnomalyManager ANOMALIES = new AnomalyManager();

    private HollowForgeEvents() {
    }

    // ------------------------------------------------------------ sync & travel

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DiscoverySync.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Drop transient per-player anomaly state (Phase 5.5 audit RISK-2):
        // cooldown entries must not accumulate for players who are gone.
        if (event.getEntity() instanceof ServerPlayer player) {
            ANOMALIES.onPlayerLogout(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Fresh snapshots after any transit; the client must never keep
            // stale discovery state across dimensions.
            DiscoverySync.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        HollowCommand.register(event.getDispatcher());
    }

    // ------------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerLevel hollow = event.getServer().getLevel(HollowDimensions.HOLLOW_LEVEL);
        if (hollow != null) {
            ANOMALIES.tick(hollow);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % PROXIMITY_INTERVAL != 0) {
            return;
        }
        if (!HollowDimensions.HOLLOW_LEVEL.equals(player.level().dimension())) {
            return;
        }
        checkProximityDiscoveries(player);
        checkDeepHollowExposure(player);
    }

    // ------------------------------------------------------ proximity discovery

    private static void checkProximityDiscoveries(ServerPlayer player) {
        ServerLevel hollow = player.serverLevel();
        HollowDiscoveryData data = HollowDiscoveryData.get(player.getServer());
        BlockPos origin = player.blockPosition();
        boolean foundObelisk = false;
        boolean foundRuin = false;

        outer:
        for (int dx = -PROXIMITY_RADIUS; dx <= PROXIMITY_RADIUS; dx++) {
            for (int dy = -PROXIMITY_RADIUS; dy <= PROXIMITY_RADIUS; dy++) {
                for (int dz = -PROXIMITY_RADIUS; dz <= PROXIMITY_RADIUS; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    // Never force chunk loads/generation from a discovery
                    // scan (Phase 5.5 audit RISK-3): only inspect chunks
                    // the server already has loaded.
                    if (!hollow.hasChunkAt(p)) {
                        continue;
                    }
                    ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS
                            .getKey(hollow.getBlockState(p).getBlock());
                    if (id == null) {
                        continue;
                    }
                    if ("minecraft".equals(id.getNamespace()) && "chest".equals(id.getPath())) {
                        // Vault chambers are the only chests in the Hollow.
                        foundRuin = true;
                    } else if (NexusEchoes.MOD_ID.equals(id.getNamespace())) {
                        switch (id.getPath()) {
                            case "obelisk_core" -> foundObelisk = true;
                            case "unstable_fracture", "rusted_plating" -> foundRuin = true;
                            default -> {
                            }
                        }
                    }
                    if (foundObelisk && foundRuin) {
                        break outer;
                    }
                }
            }
        }

        if (foundObelisk && data.grant(player.getUUID(), DiscoveryIds.OBELISK)) {
            ResearchManager.grantFromSource(player, ResearchSources.DISCOVER_OBELISK);
            DiscoverySync.syncTo(player);
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.discovery_obelisk"), false);
        }
        if (foundRuin && data.grant(player.getUUID(), DiscoveryIds.RUIN_FOUND)) {
            ResearchManager.grantFromSource(player, ResearchSources.DISCOVER_RUIN);
            DiscoverySync.syncTo(player);
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.discovery_ruin"), false);
        }
        // Deep Hollow biome arrival.
        var biome = hollow.getBiome(origin);
        if (biome.is(HollowDimensions.DEEP_HOLLOW_KEY)
                && data.grant(player.getUUID(), DiscoveryIds.DEEP_HOLLOW)) {
            DiscoverySync.syncTo(player);
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.discovery_deep"), false);
        }
    }

    // ------------------------------------------------------------------ hazard

    /**
     * Dimensional exposure (ADR-011): the Deep Hollow slowly weakens the
     * unprotected. Warning — the biome's darkness, particles and the first
     * Weakness application message. Risk — Weakness I while exposed.
     * Counterplay — an anomaly ward nearby, or leave the biome. Reward — the
     * biome holds the richest resonant ore and memory fragments.
     */
    private static void checkDeepHollowExposure(ServerPlayer player) {
        ServerLevel hollow = player.serverLevel();
        if (!hollow.getBiome(player.blockPosition()).is(HollowDimensions.DEEP_HOLLOW_KEY)) {
            return;
        }
        if (AnomalySavedData.get(hollow).isWarded(player.blockPosition(), WardBlockEntity.RADIUS)) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                PROXIMITY_INTERVAL + 40, 0, false, true));
        if (player.tickCount % (PROXIMITY_INTERVAL * 3) == 0) {
            player.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.exposure_warning"), true);
        }
    }

    // ------------------------------------------------------- entity registration

    /** Attribute maps for the four Hollow entities (mod bus). */
    @Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {

        private ModEvents() {
        }

        @SubscribeEvent
        public static void onAttributes(EntityAttributeCreationEvent event) {
            event.put(NexusRegistries.SCRAP_CRAWLER.get(), ScrapCrawlerEntity.attributes().build());
            event.put(NexusRegistries.HOLLOW_STALKER.get(), HollowStalkerEntity.attributes().build());
            event.put(NexusRegistries.RESONANT_WISP.get(), ResonantWispEntity.attributes().build());
            event.put(NexusRegistries.RIFT_PHANTOM.get(), RiftPhantomEntity.attributes().build());
        }

        @SubscribeEvent
        public static void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
            event.register(NexusRegistries.SCRAP_CRAWLER.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, spawnType, pos, random) ->
                            Mob.checkMobSpawnRules(type, level, spawnType, pos, random),
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(NexusRegistries.HOLLOW_STALKER.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(NexusRegistries.RESONANT_WISP.get(), SpawnPlacements.Type.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING, Mob::checkMobSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            // Rift phantoms never spawn from the world — only from anomalies.
        }
    }

    /** Spawn-cost / category metadata shared with tests. */
    public static MobSpawnSettings.SpawnerData spawner(EntityType<?> type, int weight, int min, int max) {
        return new MobSpawnSettings.SpawnerData(type, weight, min, max);
    }
}
