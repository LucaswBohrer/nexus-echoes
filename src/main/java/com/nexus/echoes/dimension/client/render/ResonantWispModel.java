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
 * Drifting energy knot: small bright core with four orbiting shard petals.
 * Placeholder geometry — reads as "living spark" at a glance.
 */
public class ResonantWispModel<T extends Entity> extends EntityModel<T> {

    public static final String LAYER = "resonant_wisp";

    private final ModelPart root;
    private final ModelPart core;

    public ResonantWispModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2;
            core.addOrReplaceChild("petal" + i,
                    CubeListBuilder.create()
                            .texOffs(24, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 6.0F,
                                    new CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(
                            (float) (Math.cos(a) * 5.0), 0.0F, (float) (Math.sin(a) * 5.0),
                            0.0F, (float) -a, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        core.yRot = ageInTicks * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
