package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class BedrockEntityModelAdapter<T extends Entity> extends EntityModel<T> {
    private final BedrockModel model;

    public BedrockEntityModelAdapter(BedrockModel model) {
        this.model = model;
    }

    public BedrockEntityModelAdapter(BedrockModel model, Function<ResourceLocation, RenderType> pRenderType) {
        super(pRenderType);
        this.model = model;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
    }

    public BedrockModel getModel() {
        return model;
    }
}
