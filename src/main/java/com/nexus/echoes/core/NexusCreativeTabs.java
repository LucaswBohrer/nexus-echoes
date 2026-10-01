package com.nexus.echoes.core;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Creative inventory tab for all NEXUS content.
 */
public final class NexusCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NexusEchoes.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NEXUS_TAB = TABS.register("nexus_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + NexusEchoes.MOD_ID))
                    .icon(() -> new ItemStack(NexusRegistries.NEXUS_SHARD.get()))
                    .displayItems((params, output) -> {
                        output.accept(NexusRegistries.NEXUS_SHARD.get());
                        output.accept(NexusRegistries.RESONANT_CRYSTAL.get());
                        output.accept(NexusRegistries.NEXUS_ORE.get());
                        output.accept(NexusRegistries.RESONATOR.get());
                        output.accept(NexusRegistries.CREATIVE_ENERGY_CELL.get());
                        output.accept(NexusRegistries.KINETIC_GENERATOR.get());
                        output.accept(NexusRegistries.SHAFT.get());
                        output.accept(NexusRegistries.GEAR.get());
                        output.accept(NexusRegistries.GEARBOX.get());
                        output.accept(NexusRegistries.CLUTCH.get());
                        output.accept(NexusRegistries.NEXUS_DUST.get());
                        output.accept(NexusRegistries.REFINED_NEXUS.get());
                        output.accept(NexusRegistries.NEXUS_PLATE.get());
                        output.accept(NexusRegistries.NEXUS_COMPONENT.get());
                        output.accept(NexusRegistries.CRUSHER.get());
                        output.accept(NexusRegistries.PROCESSOR.get());
                    })
                    .build());

    private NexusCreativeTabs() {
    }
}
