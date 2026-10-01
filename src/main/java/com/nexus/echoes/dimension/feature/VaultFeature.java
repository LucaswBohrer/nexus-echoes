package com.nexus.echoes.dimension.feature;

import com.mojang.serialization.Codec;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Research/remnant structure (ADR-011): a buried research chamber.
 *
 * <p>A small plating-lined room sunk into the ground with a loot chest at its
 * heart (data-driven loot table {@code nexus_echoes:chests/vault}). The
 * chamber tells the "research chamber" beat of the environmental story:
 * someone worked here. Chest loot is set via the loot-table id — the feature
 * only builds the room.
 */
public class VaultFeature extends Feature<NoneFeatureConfiguration> {

    public static final ResourceLocation LOOT =
            new ResourceLocation("nexus_echoes", "chests/vault");

    public VaultFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        // Sink the chamber a few blocks so it reads as "buried".
        BlockPos origin = context.origin().below(3 + random.nextInt(3));

        BlockState plating = NexusRegistries.RUSTED_PLATING.get().defaultBlockState();
        BlockState stone = NexusRegistries.HOLLOW_STONE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState chest = Blocks.CHEST.defaultBlockState();

        int w = 5, h = 4, d = 5;
        // Hollow box: plating shell, air interior.
        for (int dx = -1; dx <= w; dx++) {
            for (int dy = -1; dy <= h; dy++) {
                for (int dz = -1; dz <= d; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    boolean shell = dx == -1 || dx == w || dy == -1 || dy == h || dz == -1 || dz == d;
                    setBlock(level, p, shell
                            ? (random.nextFloat() < 0.8 ? plating : stone)
                            : air);
                }
            }
        }
        // Stairwell up: leave an air shaft on one side.
        for (int dy = 0; dy <= h + 2; dy++) {
            setBlock(level, origin.offset(w / 2, dy, -1), air);
            setBlock(level, origin.offset(w / 2, dy, d + 1), air);
        }
        // The chest at the heart, wired to the data-driven loot table.
        BlockPos chestPos = origin.offset(w / 2, 0, d / 2);
        setBlock(level, chestPos, chest);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chestBe) {
            chestBe.setLootTable(LOOT, random.nextLong());
        }
        return true;
    }
}
