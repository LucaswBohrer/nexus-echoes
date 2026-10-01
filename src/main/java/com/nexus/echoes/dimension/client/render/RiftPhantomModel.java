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
 * Torn silhouette: elongated body with trailing tatter planes.
 * Placeholder geometry — reads as "something that should not be" at a glance.
 */
public class RiftPhantomModel<T extends Entity> extends EntityModel<T> {

    public static final String LAYER = "rift_phantom";

    private final ModelPart root;

    public RiftPhantomModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -16.0F, -2.0F, 6.0F, 16.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-2.5F, -4.0F, -2.5F, 5.0F, 4.0F, 5.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, 0.0F));
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("tatter" + i,
                    CubeListBuilder.create()
                            .texOffs(24, 20).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F - i * 2, 1.0F,
                                    new CubeDeformation(0.0F)),
                    PartPose.offset(-2.0F + i * 2.0F, 18.0F, 0.0F));
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
