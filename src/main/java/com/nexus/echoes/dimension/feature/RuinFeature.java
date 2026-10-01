package com.nexus.echoes.dimension.feature;

import com.mojang.serialization.Codec;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Abandoned technological structure (ADR-011): a broken machine hall.
 *
 * <p>A cracked plating platform, corner pillars, a collapsed roof line and a
 * dead machine block at the center — the environmental storytelling unit of
 * the Machine Graveyard. Collapses vary per placement (deterministic from
 * the placement random). Grants the {@code ruin_found} discovery when a
 * player gets close (handled by the forge event layer, not here).
 */
public class RuinFeature extends Feature<NoneFeatureConfiguration> {

    public RuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockState plating = NexusRegistries.RUSTED_PLATING.get().defaultBlockState();
        BlockState stone = NexusRegistries.HOLLOW_STONE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int w = 7 + random.nextInt(4);
        int d = 7 + random.nextInt(4);

        // Platform.
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                BlockPos p = origin.offset(dx, 0, dz);
                BlockState existing = level.getBlockState(p);
                if (existing.isAir() || existing.liquid() || random.nextFloat() < 0.15) {
                    setBlock(level, p, random.nextFloat() < 0.7 ? plating : stone);
                }
            }
        }
        // Corner pillars, random heights (collapsed).
        int[][] corners = {{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}};
        for (int[] c : corners) {
            int h = 2 + random.nextInt(4);
            for (int y = 1; y <= h; y++) {
                BlockPos p = origin.offset(c[0], y, c[1]);
                if (level.getBlockState(p).isAir()) {
                    setBlock(level, p, plating);
                }
            }
        }
        // Dead machine at the center: a plating monolith with a missing core.
        BlockPos center = origin.offset(w / 2, 1, d / 2);
        for (int y = 0; y < 3; y++) {
            setBlock(level, center.above(y), plating);
        }
        // Hollow out the "missing core".
        setBlock(level, center.above(1), air);
        // Scattered debris.
        for (int i = 0; i < 8; i++) {
            BlockPos p = origin.offset(random.nextInt(w), 1, random.nextInt(d));
            if (level.getBlockState(p).isAir() && random.nextBoolean()) {
                setBlock(level, p, plating);
            }
        }
        return true;
    }
}
