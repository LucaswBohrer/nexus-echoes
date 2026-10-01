package com.nexus.echoes.dimension.api;

import com.nexus.echoes.NexusEchoes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/**
 * Stable identifiers for The Hollow (ADR-011).
 *
 * <p>The dimension itself is data-driven
 * ({@code data/nexus_echoes/dimension/hollow.json}); this class only
 * centralizes the keys so travel, persistence and tests cannot disagree on
 * the spelling. Biome ids mirror the {@code worldgen/biome/*.json} files.
 */
public final class HollowDimensions {

    public static final ResourceLocation HOLLOW_ID =
            new ResourceLocation(NexusEchoes.MOD_ID, "hollow");

    public static final ResourceKey<Level> HOLLOW_LEVEL =
            ResourceKey.create(Registries.DIMENSION, HOLLOW_ID);

    public static final ResourceLocation HOLLOW_TYPE_ID =
            new ResourceLocation(NexusEchoes.MOD_ID, "hollow");

    // ---------------------------------------------------------------- biomes

    public static final ResourceLocation HOLLOW_WASTES =
            new ResourceLocation(NexusEchoes.MOD_ID, "hollow_wastes");
    public static final ResourceLocation MACHINE_GRAVEYARD =
            new ResourceLocation(NexusEchoes.MOD_ID, "machine_graveyard");
    public static final ResourceLocation RESONANT_FOREST =
            new ResourceLocation(NexusEchoes.MOD_ID, "resonant_forest");
    public static final ResourceLocation DEEP_HOLLOW =
            new ResourceLocation(NexusEchoes.MOD_ID, "deep_hollow");

    /** Biome keys for server-side biome checks (events, hazards). */
    public static final ResourceKey<net.minecraft.world.level.biome.Biome> HOLLOW_WASTES_KEY =
            ResourceKey.create(Registries.BIOME, HOLLOW_WASTES);
    public static final ResourceKey<net.minecraft.world.level.biome.Biome> MACHINE_GRAVEYARD_KEY =
            ResourceKey.create(Registries.BIOME, MACHINE_GRAVEYARD);
    public static final ResourceKey<net.minecraft.world.level.biome.Biome> RESONANT_FOREST_KEY =
            ResourceKey.create(Registries.BIOME, RESONANT_FOREST);
    public static final ResourceKey<net.minecraft.world.level.biome.Biome> DEEP_HOLLOW_KEY =
            ResourceKey.create(Registries.BIOME, DEEP_HOLLOW);

    private HollowDimensions() {
    }
}
