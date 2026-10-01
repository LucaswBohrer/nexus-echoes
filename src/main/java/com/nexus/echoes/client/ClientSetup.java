package com.nexus.echoes.client;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.client.screen.CrusherScreen;
import com.nexus.echoes.client.screen.GeneratorScreen;
import com.nexus.echoes.client.screen.ProcessorScreen;
import com.nexus.echoes.client.screen.ResonatorScreen;
import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.client.ResearchKeys;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
            MenuScreens.register(NexusRegistries.CRUSHER_MENU.get(), CrusherScreen::new);
            MenuScreens.register(NexusRegistries.PROCESSOR_MENU.get(), ProcessorScreen::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ResearchKeys.OPEN_RESEARCH);
    }
}
