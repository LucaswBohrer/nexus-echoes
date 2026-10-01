package com.nexus.echoes.dimension.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

/**
 * Low, wide scavenger body: flat chassis box, sensor stalk, six leg stubs.
 * Placeholder geometry — reads as "machine-eating bug" at a glance.
 */
public class ScrapCrawlerModel<T extends Entity> extends EntityModel<T> {

    public static final String LAYER = "scrap_crawler";

    private final ModelPart root;
    private final ModelPart body;

    public ScrapCrawlerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -4.0F, -8.0F, 12.0F, 4.0F, 16.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 20).addBox(-2.0F, -7.0F, 6.0F, 4.0F, 3.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        for (int i = 0; i < 3; i++) {
            float z = -5.0F + i * 5.0F;
            body.addOrReplaceChild("leg_l" + i,
                    CubeListBuilder.create()
                            .texOffs(36, 0).addBox(0.0F, -2.0F, -1.0F, 5.0F, 4.0F, 2.0F,
                                    new CubeDeformation(0.0F)),
                    PartPose.offset(6.0F, 0.0F, z));
            body.addOrReplaceChild("leg_r" + i,
                    CubeListBuilder.create()
                            .texOffs(36, 0).addBox(-5.0F, -2.0F, -1.0F, 5.0F, 4.0F, 2.0F,
                                    new CubeDeformation(0.0F)),
                    PartPose.offset(-6.0F, 0.0F, z));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
