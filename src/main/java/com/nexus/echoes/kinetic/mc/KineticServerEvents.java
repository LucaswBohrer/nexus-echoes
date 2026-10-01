package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.NexusEchoes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-bus wiring for the kinetic system: per-level ticking and the
 * {@code /nexus kinetic} diagnostic command.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KineticServerEvents {

    private KineticServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            KineticManager.get(level).tick();
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        KineticCommand.register(event.getDispatcher());
    }
}
