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
 * The Obelisk — The Hollow's major landmark (ADR-011).
 *
 * <p>A tapered tower of rusted plating crowned with an obelisk core: the
 * return mechanism made visible from far away. Rare, unmissable, and the
 * only reliable way home. Deterministic shape from the placement random.
 */
public class ObeliskFeature extends Feature<NoneFeatureConfiguration> {

    public ObeliskFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockState plating = NexusRegistries.RUSTED_PLATING.get().defaultBlockState();
        BlockState core = NexusRegistries.OBELISK_CORE.get().defaultBlockState();

        int height = 14 + random.nextInt(6);
        // Foundation disc.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 10) {
                    BlockPos p = origin.offset(dx, 0, dz);
                    if (level.getBlockState(p).isAir()) {
                        setBlock(level, p, plating);
                    }
                }
            }
        }
        // Tapered shaft.
        for (int y = 1; y <= height; y++) {
            int r = y < height / 2 ? 1 : 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = origin.offset(dx, y, dz);
                    if (level.getBlockState(p).isAir() || level.getBlockState(p).liquid()) {
                        setBlock(level, p, plating);
                    }
                }
            }
        }
        // The core.
        BlockPos corePos = origin.offset(0, height + 1, 0);
        setBlock(level, corePos, core);
        setBlock(level, corePos.above(), Blocks.AIR.defaultBlockState());
        return true;
    }
}
