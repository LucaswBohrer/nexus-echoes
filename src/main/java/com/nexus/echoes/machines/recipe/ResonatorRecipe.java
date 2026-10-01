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

/**
 * A Resonator processing recipe, loaded from JSON (ADR-003).
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "type": "nexus_echoes:resonating",
 *   "ingredient": { "item": "nexus_echoes:nexus_shard" },
 *   "result": { "item": "nexus_echoes:resonant_crystal", "count": 1 },
 *   "processingTime": 200
 * }
 * }</pre>
 */
public class ResonatorRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int processingTime;

    public ResonatorRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result, int processingTime) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.processingTime = processingTime;
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
        return NexusRegistries.RESONATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return NexusRegistries.RESONATING.get();
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public static class Serializer implements RecipeSerializer<ResonatorRecipe> {

        @Override
        public ResonatorRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            int processingTime = GsonHelper.getAsInt(json, "processingTime", 200);
            return new ResonatorRecipe(id, ingredient, result, processingTime);
        }

        @Override
        public @Nullable ResonatorRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            ItemStack result = buf.readItem();
            int processingTime = buf.readVarInt();
            return new ResonatorRecipe(id, ingredient, result, processingTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ResonatorRecipe recipe) {
            recipe.ingredient.toNetwork(buf);
            buf.writeItem(recipe.result);
            buf.writeVarInt(recipe.processingTime);
        }
    }
}
