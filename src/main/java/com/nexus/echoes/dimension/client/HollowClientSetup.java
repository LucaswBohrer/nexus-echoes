package com.nexus.echoes.dimension.client;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.dimension.api.HollowDimensions;
import com.nexus.echoes.dimension.client.render.HollowRenderers;
import com.nexus.echoes.dimension.client.render.HollowStalkerModel;
import com.nexus.echoes.dimension.client.render.ResonantWispModel;
import com.nexus.echoes.dimension.client.render.RiftPhantomModel;
import com.nexus.echoes.dimension.client.render.ScrapCrawlerModel;
import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Hollow setup (ADR-011).
 *
 * <p>Registers entity renderers + model layers and the dimension's visual
 * identity (sky/fog). Everything here is presentation — the server owns all
 * gameplay state.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HollowClientSetup {

    private HollowClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HollowRenderers.SCRAP_CRAWLER, ScrapCrawlerModel::createLayer);
        event.registerLayerDefinition(HollowRenderers.HOLLOW_STALKER, HollowStalkerModel::createLayer);
        event.registerLayerDefinition(HollowRenderers.RESONANT_WISP, ResonantWispModel::createLayer);
        event.registerLayerDefinition(HollowRenderers.RIFT_PHANTOM, RiftPhantomModel::createLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NexusRegistries.SCRAP_CRAWLER.get(),
                HollowRenderers.ScrapCrawlerRenderer::new);
        event.registerEntityRenderer(NexusRegistries.HOLLOW_STALKER.get(),
                HollowRenderers.HollowStalkerRenderer::new);
        event.registerEntityRenderer(NexusRegistries.RESONANT_WISP.get(),
                HollowRenderers.ResonantWispRenderer::new);
        event.registerEntityRenderer(NexusRegistries.RIFT_PHANTOM.get(),
                HollowRenderers.RiftPhantomRenderer::new);
    }

    /**
     * Registers the Hollow's visual identity (sky/fog) through Forge's
     * dimension-effects event. The id must match the dimension type's
     * {@code effects} field ({@code nexus_echoes:the_hollow}).
     */
    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(effectsId(), new HollowDimensionEffects());
    }

    /**
     * Visual identity: low amber-teal sky, thick warm-grey fog, constant
     * twilight brightness. Distinct from Overworld day cycles, Nether red
     * and End void — industrial dusk, abandoned but readable.
     */
    public static final class HollowDimensionEffects extends DimensionSpecialEffects {

        public HollowDimensionEffects() {
            super(256, false, SkyType.NORMAL, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
            // Warm grey-teal haze that barely responds to light level.
            return new Vec3(0.16, 0.15, 0.13).lerp(color, 0.15);
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return y < 40;
        }
    }

    /** Stable reference for tests. */
    public static ResourceLocation effectsId() {
        return new ResourceLocation(HollowDimensions.HOLLOW_ID.getNamespace(), "the_hollow");
    }
}
