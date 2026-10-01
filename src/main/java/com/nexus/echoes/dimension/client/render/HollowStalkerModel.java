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
 * Gaunt biped: tall thin torso, long arms, shard-crested head.
 * Placeholder geometry — reads as "starved predator" at a glance.
 */
public class HollowStalkerModel<T extends Entity> extends EntityModel<T> {

    public static final String LAYER = "hollow_stalker";

    private final ModelPart root;

    public HollowStalkerModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("torso",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -14.0F, -2.0F, 8.0F, 14.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-3.0F, -5.0F, -3.0F, 6.0F, 5.0F, 6.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(24, 18).addBox(-1.0F, -9.0F, -1.0F, 2.0F, 4.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_l",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("arm_r",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("leg_l",
                CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("leg_r",
                CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, 14.0F, 0.0F));
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
