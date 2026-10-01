package com.nexus.echoes.dimension.client.render;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.dimension.entity.HollowStalkerEntity;
import com.nexus.echoes.dimension.entity.ResonantWispEntity;
import com.nexus.echoes.dimension.entity.RiftPhantomEntity;
import com.nexus.echoes.dimension.entity.ScrapCrawlerEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client renderers for the four Hollow entities (ADR-011).
 *
 * <p>Simple geometric placeholder models with generated placeholder textures.
 * All rendering is client-only; behavior stays server-authoritative.
 */
public final class HollowRenderers {

    public static final ModelLayerLocation SCRAP_CRAWLER =
            new ModelLayerLocation(new ResourceLocation(NexusEchoes.MOD_ID, "scrap_crawler"), "main");
    public static final ModelLayerLocation HOLLOW_STALKER =
            new ModelLayerLocation(new ResourceLocation(NexusEchoes.MOD_ID, "hollow_stalker"), "main");
    public static final ModelLayerLocation RESONANT_WISP =
            new ModelLayerLocation(new ResourceLocation(NexusEchoes.MOD_ID, "resonant_wisp"), "main");
    public static final ModelLayerLocation RIFT_PHANTOM =
            new ModelLayerLocation(new ResourceLocation(NexusEchoes.MOD_ID, "rift_phantom"), "main");

    private HollowRenderers() {
    }

    public static class ScrapCrawlerRenderer
            extends MobRenderer<ScrapCrawlerEntity, ScrapCrawlerModel<ScrapCrawlerEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                NexusEchoes.MOD_ID, "textures/entity/scrap_crawler.png");

        public ScrapCrawlerRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ScrapCrawlerModel<>(ctx.bakeLayer(SCRAP_CRAWLER)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(ScrapCrawlerEntity entity) {
            return TEXTURE;
        }
    }

    public static class HollowStalkerRenderer
            extends MobRenderer<HollowStalkerEntity, HollowStalkerModel<HollowStalkerEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                NexusEchoes.MOD_ID, "textures/entity/hollow_stalker.png");

        public HollowStalkerRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HollowStalkerModel<>(ctx.bakeLayer(HOLLOW_STALKER)), 0.6F);
        }

        @Override
        public ResourceLocation getTextureLocation(HollowStalkerEntity entity) {
            return TEXTURE;
        }
    }

    public static class ResonantWispRenderer
            extends MobRenderer<ResonantWispEntity, ResonantWispModel<ResonantWispEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                NexusEchoes.MOD_ID, "textures/entity/resonant_wisp.png");

        public ResonantWispRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ResonantWispModel<>(ctx.bakeLayer(RESONANT_WISP)), 0.3F);
        }

        @Override
        public ResourceLocation getTextureLocation(ResonantWispEntity entity) {
            return TEXTURE;
        }
    }

    public static class RiftPhantomRenderer
            extends MobRenderer<RiftPhantomEntity, RiftPhantomModel<RiftPhantomEntity>> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                NexusEchoes.MOD_ID, "textures/entity/rift_phantom.png");

        public RiftPhantomRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new RiftPhantomModel<>(ctx.bakeLayer(RIFT_PHANTOM)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(RiftPhantomEntity entity) {
            return TEXTURE;
        }
    }
}
