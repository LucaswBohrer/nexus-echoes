package com.nexus.echoes.dimension.feature;

import com.mojang.serialization.Codec;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Dangerous anomaly structure (ADR-011): a fracture spire.
 *
 * <p>A jagged hollow-stone spike ringed by unstable-fracture blocks — the
 * high-risk/high-reward exploration target. Fractures cluster resonant ore,
 * so the danger marks the reward. The anomaly manager additionally weights
 * spawns near these (documented in ADR-011); the feature itself only builds
 * the terrain.
 */
public class FractureSpireFeature extends Feature<NoneFeatureConfiguration> {

    public FractureSpireFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockState stone = NexusRegistries.HOLLOW_STONE.get().defaultBlockState();
        BlockState fracture = NexusRegistries.UNSTABLE_FRACTURE.get().defaultBlockState();
        BlockState ore = NexusRegistries.HOLLOW_ORE.get().defaultBlockState();

        int height = 8 + random.nextInt(8);
        // Jagged spike.
        for (int y = 0; y < height; y++) {
            int r = Math.max(0, 2 - y / 4) + (random.nextFloat() < 0.3 ? 1 : 0);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r + 1) {
                        BlockPos p = origin.offset(dx, y, dz);
                        if (level.getBlockState(p).isAir() || level.getBlockState(p).liquid()) {
                            setBlock(level, p, random.nextFloat() < 0.12 ? ore : stone);
                        }
                    }
                }
            }
        }
        // Fracture ring at the base.
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int dist = 3 + random.nextInt(4);
            BlockPos p = origin.offset(
                    (int) Math.round(Math.cos(angle) * dist), 0,
                    (int) Math.round(Math.sin(angle) * dist));
            if (level.getBlockState(p).isAir()) {
                setBlock(level, p, fracture);
            }
        }
        return true;
    }
}
