package com.nexus.echoes.client;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.client.screen.GeneratorScreen;
import com.nexus.echoes.client.screen.ResonatorScreen;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup. Runs exclusively on {@link Dist#CLIENT}.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(NexusRegistries.RESONATOR_MENU.get(), ResonatorScreen::new);
            MenuScreens.register(NexusRegistries.GENERATOR_MENU.get(), GeneratorScreen::new);
        });
    }
}
