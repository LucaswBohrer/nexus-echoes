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
import com.nexus.echoes.machines.CreativeCellBlock;
import com.nexus.echoes.machines.CreativeCellBlockEntity;
import com.nexus.echoes.machines.ResonatorBlock;
import com.nexus.echoes.machines.ResonatorBlockEntity;
import com.nexus.echoes.machines.ResonatorMenu;
import com.nexus.echoes.machines.recipe.ResonatorRecipe;
import com.nexus.echoes.machines.recipe.ResonatorRecipeSerializer;
import net.minecraft.util.valueproviders.UniformInt;
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

    // ------------------------------------------------------------------- items

    public static final RegistryObject<Item> NEXUS_SHARD = ITEMS.register("nexus_shard",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RESONANT_CRYSTAL = ITEMS.register("resonant_crystal",
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

    // ------------------------------------------------------------------- menus

    public static final RegistryObject<MenuType<ResonatorMenu>> RESONATOR_MENU =
            menuType("resonator", () -> IForgeMenuType.create(ResonatorMenu::new));

    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU =
            menuType("kinetic_generator", () -> IForgeMenuType.create(GeneratorMenu::new));

    // ------------------------------------------------------------------ recipes

    public static final RegistryObject<RecipeType<ResonatorRecipe>> RESONATING =
            recipeType("resonating");

    public static final RegistryObject<RecipeSerializer<ResonatorRecipe>> RESONATING_SERIALIZER =
            recipeSerializer("resonating", ResonatorRecipeSerializer::new);

    // ------------------------------------------------------------------ wiring

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENU_TYPES.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        NexusCreativeTabs.TABS.register(bus);
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
