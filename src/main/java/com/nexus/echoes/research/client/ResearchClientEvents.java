package com.nexus.echoes.research.client;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.research.client.screen.ResearchScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client tick handler: opens the research screen on keybind press.
 * The screen itself asks the server for a fresh snapshot on open.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ResearchClientEvents {

    private ResearchClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // consume stale presses while another screen is open
            ResearchKeys.OPEN_RESEARCH.consumeClick();
            return;
        }
        while (ResearchKeys.OPEN_RESEARCH.consumeClick()) {
            mc.setScreen(new ResearchScreen());
        }
    }
}
