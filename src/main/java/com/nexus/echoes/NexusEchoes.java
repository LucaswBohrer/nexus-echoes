package com.nexus.echoes;

import com.nexus.echoes.core.NexusConfig;
import com.nexus.echoes.network.NexusNetwork;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * NEXUS: Echoes of Reality — mod entrypoint.
 *
 * This class wires modules together and does nothing else. All content lives
 * behind the registry / energy / machine abstractions (see docs/ARCHITECTURE.md).
 */
@Mod(NexusEchoes.MOD_ID)
public class NexusEchoes {

    public static final String MOD_ID = "nexus_echoes";

    public NexusEchoes() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        NexusRegistries.register(bus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NexusConfig.SPEC);

        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(NexusNetwork::register);
    }
}
