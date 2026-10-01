package com.nexus.echoes.machines.recipe;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Headless tests for {@link ProcessingRecipe} parameter validation.
 *
 * <p>Calls the pure static {@code validate} directly: full recipe
 * construction needs {@link net.minecraft.world.item.ItemStack} /
 * registries, which require a bootstrapped Minecraft runtime.
 */
class ProcessingRecipeValidationTest {

    private static final ResourceLocation ID = new ResourceLocation("nexus_echoes:test");

    @Test
    void validParamsPass() {
        assertDoesNotThrow(() -> ProcessingRecipe.validate(ID, 200, false, 0.0f, false));
        assertDoesNotThrow(() -> ProcessingRecipe.validate(ID, 1, true, 0.3f, false));
    }

    @Test
    void chanceBoundariesAreInclusive() {
        assertDoesNotThrow(() -> ProcessingRecipe.validate(ID, 200, true, 0.0f, false));
        assertDoesNotThrow(() -> ProcessingRecipe.validate(ID, 200, true, 1.0f, false));
    }

    @Test
    void zeroProcessingTimeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, 0, false, 0.0f, false));
    }

    @Test
    void negativeProcessingTimeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, -50, false, 0.0f, false));
    }

    @Test
    void negativeChanceRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, 200, true, -0.1f, false));
    }

    @Test
    void chanceAboveOneRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, 200, true, 1.5f, false));
    }

    @Test
    void nanChanceRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, 200, true, Float.NaN, false));
    }

    @Test
    void emptyByproductStackRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProcessingRecipe.validate(ID, 200, true, 0.5f, true));
    }

    @Test
    void chanceIgnoredWithoutByproduct() {
        // No byproduct section: chance value is irrelevant, must not throw.
        assertDoesNotThrow(() -> ProcessingRecipe.validate(ID, 200, false, 7.5f, false));
    }
}
