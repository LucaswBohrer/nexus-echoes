package com.nexus.echoes.machines.recipe;

import com.google.gson.JsonObject;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Industrial processing recipe (Phase 3, ADR-009): one input → one output,
 * with an optional probabilistic byproduct and a processing time in ticks.
 *
 * <p>Used by the {@code crushing} and {@code processing} recipe types; the
 * schema is shared so new machine types only need a new {@link RecipeType}.
 * Kinetic requirements stay machine properties (see ADR-009) — the schema is
 * ready to grow without breaking existing JSONs.
 *
 * <pre>{@code
 * {
 *   "type": "nexus_echoes:crushing",
 *   "ingredient": { "item": "nexus_echoes:nexus_ore" },
 *   "result": { "item": "nexus_echoes:nexus_dust", "count": 2 },
 *   "byproduct": { "item": "minecraft:cobblestone", "count": 1, "chance": 0.3 },
 *   "processingTime": 200
 * }
 * }</pre>
 */
public class ProcessingRecipe implements Recipe<SimpleContainer> {

    /** Optional secondary output rolled with {@link Byproduct#chance()} on completion. */
    public record Byproduct(ItemStack stack, float chance) {
    }

    private final ResourceLocation id;
    private final RecipeType<ProcessingRecipe> type;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final Optional<Byproduct> byproduct;
    private final int processingTime;

    public ProcessingRecipe(ResourceLocation id, RecipeType<ProcessingRecipe> type,
                            Ingredient ingredient, ItemStack result,
                            Optional<Byproduct> byproduct, int processingTime) {
        validate(id, processingTime,
                byproduct.isPresent(),
                byproduct.map(Byproduct::chance).orElse(0.0f),
                byproduct.map(bp -> bp.stack().isEmpty()).orElse(false));
        this.id = id;
        this.type = type;
        this.ingredient = ingredient;
        this.result = result;
        this.byproduct = byproduct;
        this.processingTime = processingTime;
    }

    /**
     * Pure validation of recipe parameters. Static and free of item/registry
     * access so datapack errors fail fast and unit tests can run headless
     * (no Minecraft bootstrap).
     *
     * @throws IllegalArgumentException on non-positive processing time, a
     *         byproduct chance outside [0,1], or an empty byproduct stack
     */
    static void validate(ResourceLocation id, int processingTime,
                         boolean hasByproduct, float byproductChance, boolean byproductEmpty) {
        if (processingTime <= 0) {
            throw new IllegalArgumentException(
                    "processingTime must be > 0 (recipe " + id + "): " + processingTime);
        }
        if (hasByproduct) {
            if (Float.isNaN(byproductChance) || byproductChance < 0.0f || byproductChance > 1.0f) {
                throw new IllegalArgumentException(
                        "byproduct chance must be in [0,1] (recipe " + id + "): " + byproductChance);
            }
            if (byproductEmpty) {
                throw new IllegalArgumentException("byproduct item must not be empty (recipe " + id + ")");
            }
        }
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return ingredient.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        // The JSON "type" field names the serializer; dispatch back to it.
        // Both serializers share this class, parameterized by recipe type.
        return NexusRegistries.CRUSHING.get() == type
                ? NexusRegistries.CRUSHING_SERIALIZER.get()
                : NexusRegistries.PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public Optional<Byproduct> getByproduct() {
        return byproduct;
    }

    public static class Serializer implements RecipeSerializer<ProcessingRecipe> {

        private final RecipeType<ProcessingRecipe> type;

        public Serializer(RecipeType<ProcessingRecipe> type) {
            this.type = type;
        }

        @Override
        public ProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            Optional<Byproduct> byproduct = Optional.empty();
            if (GsonHelper.isValidNode(json, "byproduct")) {
                JsonObject bp = GsonHelper.getAsJsonObject(json, "byproduct");
                ItemStack stack = ShapedRecipe.itemStackFromJson(bp);
                float chance = GsonHelper.getAsFloat(bp, "chance", 1.0f);
                byproduct = Optional.of(new Byproduct(stack, chance));
            }
            int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
            return new ProcessingRecipe(id, type, ingredient, result, byproduct, processingTime);
        }

        @Override
        public @Nullable ProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            Optional<Byproduct> byproduct = buf.readBoolean()
                    ? Optional.of(new Byproduct(buf.readItem(), buf.readFloat()))
                    : Optional.empty();
            int processingTime = buf.readVarInt();
            return new ProcessingRecipe(id, type, ingredient, result, byproduct, processingTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ProcessingRecipe recipe) {
            recipe.ingredient.toNetwork(buf);
            buf.writeItem(recipe.result);
            buf.writeBoolean(recipe.byproduct.isPresent());
            recipe.byproduct.ifPresent(bp -> {
                buf.writeItem(bp.stack());
                buf.writeFloat(bp.chance());
            });
            buf.writeVarInt(recipe.processingTime);
        }
    }
}
