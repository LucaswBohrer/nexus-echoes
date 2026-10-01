package com.nexus.echoes.research.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.nexus.echoes.NexusEchoes;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind that opens the research screen (client-only).
 */
@OnlyIn(Dist.CLIENT)
public final class ResearchKeys {

    public static final KeyMapping OPEN_RESEARCH = new KeyMapping(
            "key.nexus_echoes.open_research",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories." + NexusEchoes.MOD_ID);

    private ResearchKeys() {
    }
}
