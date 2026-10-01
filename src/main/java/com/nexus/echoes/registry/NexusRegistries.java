package com.nexus.echoes.registry;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.core.NexusCreativeTabs;
import com.nexus.echoes.kinetic.mc.ClutchBlock;
import com.nexus.echoes.kinetic.mc.GearBlock;
import com.nexus.echoes.kinetic.mc.GearboxBlock;
import com.nexus.echoes.kinetic.mc.GeneratorMenu;
import com.nexus.echoes.kinetic.mc.KineticGeneratorBlock;
import com.nexus.echoes.kinetic.mc.KineticGeneratorBlockEntity;
import com.nexus.echoes.kinetic.mc.ShaftBlock;
import com.nexus.echoes.dimension.block.AnomalyWardBlock;
import com.nexus.echoes.dimension.block.DimensionalSpireBlock;
import com.nexus.echoes.dimension.block.ObeliskCoreBlock;
import com.nexus.echoes.dimension.block.ResonantGrowthBlock;
import com.nexus.echoes.dimension.block.SpireBlockEntity;
import com.nexus.echoes.dimension.block.UnstableFractureBlock;
import com.nexus.echoes.dimension.block.WardBlockEntity;
import com.nexus.echoes.dimension.entity.HollowStalkerEntity;
import com.nexus.echoes.dimension.entity.ResonantWispEntity;
import com.nexus.echoes.dimension.entity.RiftPhantomEntity;
import com.nexus.echoes.dimension.entity.ScrapCrawlerEntity;
import com.nexus.echoes.dimension.item.MemoryFragmentItem;
import com.nexus.echoes.dimension.item.ResonanceScannerItem;
import com.nexus.echoes.dimension.feature.FractureSpireFeature;
import com.nexus.echoes.dimension.feature.ObeliskFeature;
import com.nexus.echoes.dimension.feature.RuinFeature;
import com.nexus.echoes.dimension.feature.VaultFeature;
import com.nexus.echoes.machines.CreativeCellBlock;
import com.nexus.echoes.machines.CreativeCellBlockEntity;
import com.nexus.echoes.machines.CrusherBlock;
import com.nexus.echoes.machines.CrusherBlockEntity;
import com.nexus.echoes.machines.CrusherMenu;
import com.nexus.echoes.machines.ProcessorBlock;
import com.nexus.echoes.machines.ProcessorBlockEntity;
import com.nexus.echoes.machines.ProcessorMenu;
import com.nexus.echoes.machines.ResonatorBlock;
import com.nexus.echoes.machines.ResonatorBlockEntity;
import com.nexus.echoes.machines.ResonatorMenu;
import com.nexus.echoes.machines.recipe.ProcessingRecipe;
import com.nexus.echoes.machines.recipe.ResonatorRecipe;
import com.nexus.echoes.machines.recipe.ResonatorRecipeSerializer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * SINGLE choke point for every registration in the mod (ADR-002).
 *
 * <p>Content classes only declare {@link RegistryObject}s here. Nothing registers
 * anywhere else — one file to audit, one place where mod-id mistakes surface.
 */
public final class NexusRegistries {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NexusEchoes.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NexusEchoes.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, NexusEchoes.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, NexusEchoes.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, NexusEchoes.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, NexusEchoes.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, NexusEchoes.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, NexusEchoes.MOD_ID);

    // ------------------------------------------------------------------ blocks

    public static final RegistryObject<Block> NEXUS_ORE = BLOCKS.register("nexus_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    UniformInt.of(3, 7)));

    public static final RegistryObject<Block> RESONATOR = BLOCKS.register("resonator",
            () -> new ResonatorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> CREATIVE_ENERGY_CELL = BLOCKS.register("creative_energy_cell",
            () -> new CreativeCellBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_CYAN)
                            .strength(3.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 7)));

    // ------------------------------------------------------- kinetic (phase 2)

    public static final RegistryObject<Block> KINETIC_GENERATOR = BLOCKS.register("kinetic_generator",
            () -> new KineticGeneratorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> SHAFT = BLOCKS.register("shaft",
            () -> new ShaftBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)));

    public static final RegistryObject<Block> GEAR = BLOCKS.register("gear",
            () -> new GearBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F, 4.0F)
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> GEARBOX = BLOCKS.register("gearbox",
            () -> new GearboxBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 5.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> CLUTCH = BLOCKS.register("clutch",
            () -> new ClutchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 5.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ----------------------------------------------- industrial (phase 3)

    public static final RegistryObject<Block> CRUSHER = BLOCKS.register("crusher",
            () -> new CrusherBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> PROCESSOR = BLOCKS.register("processor",
            () -> new ProcessorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    // ---------------------------------------------------- hollow (phase 5)

    public static final RegistryObject<Block> HOLLOW_STONE = BLOCKS.register("hollow_stone",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(2.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE)));

    public static final RegistryObject<Block> RUSTED_PLATING = BLOCKS.register("rusted_plating",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(3.0F, 8.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> ASHEN_SOIL = BLOCKS.register("ashen_soil",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_GRAY)
                            .strength(1.2F, 1.2F)
                            .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> HOLLOW_ORE = BLOCKS.register("hollow_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.5F, 3.5F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    UniformInt.of(2, 5)));

    public static final RegistryObject<Block> DIMENSIONAL_SPIRE = BLOCKS.register("dimensional_spire",
            () -> new DimensionalSpireBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(4.0F, 12.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 9)));

    public static final RegistryObject<Block> OBELISK_CORE = BLOCKS.register("obelisk_core",
            () -> new ObeliskCoreBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(-1.0F, 3600000.0F)
                            .sound(SoundType.STONE)
                            .lightLevel(state -> 12)));

    public static final RegistryObject<Block> ANOMALY_WARD = BLOCKS.register("anomaly_ward",
            () -> new AnomalyWardBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .lightLevel(state -> 6)));

    public static final RegistryObject<Block> UNSTABLE_FRACTURE = BLOCKS.register("unstable_fracture",
            () -> new UnstableFractureBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_MAGENTA)
                            .strength(1.5F, 3.0F)
                            .sound(SoundType.GLASS)
                            .lightLevel(state -> 10)));

    public static final RegistryObject<Block> RESONANT_GROWTH = BLOCKS.register("resonant_growth",
            () -> new ResonantGrowthBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.PLANT)
                            .noCollission()
                            .instabreak()
                            .sound(SoundType.GRASS)
                            .lightLevel(state -> 4)));

    // ------------------------------------------------------------------- items

    public static final RegistryObject<Item> NEXUS_SHARD = ITEMS.register("nexus_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESONANT_CRYSTAL = ITEMS.register("resonant_crystal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NEXUS_DUST = ITEMS.register("nexus_dust",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> REFINED_NEXUS = ITEMS.register("refined_nexus",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NEXUS_PLATE = ITEMS.register("nexus_plate",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NEXUS_COMPONENT = ITEMS.register("nexus_component",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> NEXUS_ORE_ITEM = blockItem("nexus_ore", NEXUS_ORE);
    public static final RegistryObject<Item> RESONATOR_ITEM = blockItem("resonator", RESONATOR);
    public static final RegistryObject<Item> CREATIVE_ENERGY_CELL_ITEM =
            blockItem("creative_energy_cell", CREATIVE_ENERGY_CELL);
    public static final RegistryObject<Item> KINETIC_GENERATOR_ITEM = blockItem("kinetic_generator", KINETIC_GENERATOR);
    public static final RegistryObject<Item> SHAFT_ITEM = blockItem("shaft", SHAFT);
    public static final RegistryObject<Item> GEAR_ITEM = blockItem("gear", GEAR);
    public static final RegistryObject<Item> GEARBOX_ITEM = blockItem("gearbox", GEARBOX);
    public static final RegistryObject<Item> CLUTCH_ITEM = blockItem("clutch", CLUTCH);
    public static final RegistryObject<Item> CRUSHER_ITEM = blockItem("crusher", CRUSHER);
    public static final RegistryObject<Item> PROCESSOR_ITEM = blockItem("processor", PROCESSOR);

    // ---------------------------------------------------- hollow (phase 5)

    public static final RegistryObject<Item> HOLLOW_STONE_ITEM = blockItem("hollow_stone", HOLLOW_STONE);
    public static final RegistryObject<Item> RUSTED_PLATING_ITEM = blockItem("rusted_plating", RUSTED_PLATING);
    public static final RegistryObject<Item> ASHEN_SOIL_ITEM = blockItem("ashen_soil", ASHEN_SOIL);
    public static final RegistryObject<Item> HOLLOW_ORE_ITEM = blockItem("hollow_ore", HOLLOW_ORE);
    public static final RegistryObject<Item> DIMENSIONAL_SPIRE_ITEM = blockItem("dimensional_spire", DIMENSIONAL_SPIRE);
    public static final RegistryObject<Item> OBELISK_CORE_ITEM = blockItem("obelisk_core", OBELISK_CORE);
    public static final RegistryObject<Item> ANOMALY_WARD_ITEM = blockItem("anomaly_ward", ANOMALY_WARD);
    public static final RegistryObject<Item> UNSTABLE_FRACTURE_ITEM = blockItem("unstable_fracture", UNSTABLE_FRACTURE);
    public static final RegistryObject<Item> RESONANT_GROWTH_ITEM = blockItem("resonant_growth", RESONANT_GROWTH);

    public static final RegistryObject<Item> MEMORY_FRAGMENT = ITEMS.register("memory_fragment",
            () -> new MemoryFragmentItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> RESONANCE_SCANNER = ITEMS.register("resonance_scanner",
            () -> new ResonanceScannerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RESONANT_SHARD = ITEMS.register("resonant_shard",
            () -> new Item(new Item.Properties()));

    // ---------------------------------------------------------- block entities

    public static final RegistryObject<BlockEntityType<ResonatorBlockEntity>> RESONATOR_BE =
            blockEntity("resonator", ResonatorBlockEntity::new, RESONATOR);

    public static final RegistryObject<BlockEntityType<CreativeCellBlockEntity>> CREATIVE_CELL_BE =
            blockEntity("creative_energy_cell", CreativeCellBlockEntity::new, CREATIVE_ENERGY_CELL);

    public static final RegistryObject<BlockEntityType<KineticGeneratorBlockEntity>> KINETIC_GENERATOR_BE =
            blockEntity("kinetic_generator", KineticGeneratorBlockEntity::new, KINETIC_GENERATOR);
    public static final RegistryObject<BlockEntityType<ShaftBlock.ShaftBlockEntity>> SHAFT_BE =
            blockEntity("shaft", ShaftBlock.ShaftBlockEntity::new, SHAFT);
    public static final RegistryObject<BlockEntityType<GearBlock.GearBlockEntity>> GEAR_BE =
            blockEntity("gear", GearBlock.GearBlockEntity::new, GEAR);
    public static final RegistryObject<BlockEntityType<GearboxBlock.GearboxBlockEntity>> GEARBOX_BE =
            blockEntity("gearbox", GearboxBlock.GearboxBlockEntity::new, GEARBOX);
    public static final RegistryObject<BlockEntityType<ClutchBlock.ClutchBlockEntity>> CLUTCH_BE =
            blockEntity("clutch", ClutchBlock.ClutchBlockEntity::new, CLUTCH);

    public static final RegistryObject<BlockEntityType<CrusherBlockEntity>> CRUSHER_BE =
            blockEntity("crusher", CrusherBlockEntity::new, CRUSHER);
    public static final RegistryObject<BlockEntityType<ProcessorBlockEntity>> PROCESSOR_BE =
            blockEntity("processor", ProcessorBlockEntity::new, PROCESSOR);

    // ---------------------------------------------------- hollow (phase 5)

    public static final RegistryObject<BlockEntityType<SpireBlockEntity>> SPIRE_BE =
            blockEntity("dimensional_spire", SpireBlockEntity::new, DIMENSIONAL_SPIRE);
    public static final RegistryObject<BlockEntityType<WardBlockEntity>> WARD_BE =
            blockEntity("anomaly_ward", WardBlockEntity::new, ANOMALY_WARD);

    // ------------------------------------------------------------------- menus

    public static final RegistryObject<MenuType<ResonatorMenu>> RESONATOR_MENU =
            menuType("resonator", () -> IForgeMenuType.create(ResonatorMenu::new));

    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU =
            menuType("kinetic_generator", () -> IForgeMenuType.create(GeneratorMenu::new));

    public static final RegistryObject<MenuType<CrusherMenu>> CRUSHER_MENU =
            menuType("crusher", () -> IForgeMenuType.create(CrusherMenu::new));

    public static final RegistryObject<MenuType<ProcessorMenu>> PROCESSOR_MENU =
            menuType("processor", () -> IForgeMenuType.create(ProcessorMenu::new));

    // ------------------------------------------------------------------ recipes

    public static final RegistryObject<RecipeType<ResonatorRecipe>> RESONATING =
            recipeType("resonating");

    public static final RegistryObject<RecipeSerializer<ResonatorRecipe>> RESONATING_SERIALIZER =
            recipeSerializer("resonating", ResonatorRecipeSerializer::new);

    public static final RegistryObject<RecipeType<ProcessingRecipe>> CRUSHING =
            recipeType("crushing");

    public static final RegistryObject<RecipeType<ProcessingRecipe>> PROCESSING =
            recipeType("processing");

    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> CRUSHING_SERIALIZER =
            recipeSerializer("crushing", () -> new ProcessingRecipe.Serializer(CRUSHING.get()));

    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> PROCESSING_SERIALIZER =
            recipeSerializer("processing", () -> new ProcessingRecipe.Serializer(PROCESSING.get()));

    // ---------------------------------------------------------------- entities

    public static final RegistryObject<EntityType<ScrapCrawlerEntity>> SCRAP_CRAWLER =
            entityType("scrap_crawler", () -> EntityType.Builder
                    .of(ScrapCrawlerEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.7F)
                    .clientTrackingRange(10)
                    .build("scrap_crawler"));

    public static final RegistryObject<EntityType<HollowStalkerEntity>> HOLLOW_STALKER =
            entityType("hollow_stalker", () -> EntityType.Builder
                    .of(HollowStalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.2F)
                    .clientTrackingRange(10)
                    .build("hollow_stalker"));

    public static final RegistryObject<EntityType<ResonantWispEntity>> RESONANT_WISP =
            entityType("resonant_wisp", () -> EntityType.Builder
                    .of(ResonantWispEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .build("resonant_wisp"));

    public static final RegistryObject<EntityType<RiftPhantomEntity>> RIFT_PHANTOM =
            entityType("rift_phantom", () -> EntityType.Builder
                    .of(RiftPhantomEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.9F)
                    .clientTrackingRange(10)
                    .build("rift_phantom"));

    // ---------------------------------------------------------------- features

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> OBELISK_FEATURE =
            feature("obelisk", () -> new ObeliskFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> RUIN_FEATURE =
            feature("ruin", () -> new RuinFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> VAULT_FEATURE =
            feature("vault", () -> new VaultFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FRACTURE_SPIRE_FEATURE =
            feature("fracture_spire", () -> new FractureSpireFeature(NoneFeatureConfiguration.CODEC));

    // ------------------------------------------------------------------ wiring

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENU_TYPES.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        ENTITY_TYPES.register(bus);
        FEATURES.register(bus);
        NexusCreativeTabs.TABS.register(bus);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.Entity> RegistryObject<EntityType<T>> entityType(
            String name, Supplier<EntityType<T>> factory) {
        return (RegistryObject<EntityType<T>>) (RegistryObject<?>) ENTITY_TYPES.register(name, factory);
    }

    @SuppressWarnings("unchecked")
    private static <C extends net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration>
    RegistryObject<Feature<C>> feature(String name, Supplier<Feature<C>> factory) {
        return (RegistryObject<Feature<C>>) (RegistryObject<?>) FEATURES.register(name, factory);
    }

    private NexusRegistries() {
    }

    // ----------------------------------------------------------------- helpers

    private static RegistryObject<Item> blockItem(String name, Supplier<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> RegistryObject<BlockEntityType<T>> blockEntity(
            String name, BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block> block) {
        return (RegistryObject<BlockEntityType<T>>) (RegistryObject<?>) BLOCK_ENTITIES.register(name,
                () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> RegistryObject<MenuType<T>> menuType(
            String name, Supplier<MenuType<T>> factory) {
        return (RegistryObject<MenuType<T>>) (RegistryObject<?>) MENU_TYPES.register(name, factory);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RegistryObject<RecipeType<T>> recipeType(String name) {
        return (RegistryObject<RecipeType<T>>) (RegistryObject<?>) RECIPE_TYPES.register(name,
                () -> new RecipeType<T>() {
                    @Override
                    public String toString() {
                        return NexusEchoes.MOD_ID + ":" + name;
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static <T extends RecipeSerializer<?>> RegistryObject<T> recipeSerializer(String name, Supplier<T> factory) {
        return (RegistryObject<T>) (RegistryObject<?>) RECIPE_SERIALIZERS.register(name, factory);
    }
}
