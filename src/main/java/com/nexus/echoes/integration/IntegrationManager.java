package com.nexus.echoes.integration;

import net.minecraftforge.fml.ModList;

/**
 * Central gate for optional integrations with other mods.
 *
 * <p>Integrations are detected dynamically and must NEVER become hard
 * dependencies. All {@code ModList.get().isLoaded(...)} checks go through here
 * so the integration surface is auditable in one file.
 */
public final class IntegrationManager {

    private IntegrationManager() {
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean isCreateLoaded() {
        return isModLoaded("create");
    }

    public static boolean isJeiLoaded() {
        return isModLoaded("jei");
    }

    public static boolean isMekanismLoaded() {
        return isModLoaded("mekanism");
    }

    public static boolean isAe2Loaded() {
        return isModLoaded("ae2");
    }

    public static boolean isThermalLoaded() {
        return isModLoaded("thermal");
    }

    public static boolean isReiLoaded() {
        return isModLoaded("rei");
    }
}
