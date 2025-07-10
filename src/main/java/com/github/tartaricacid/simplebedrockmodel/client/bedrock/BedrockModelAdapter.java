package com.github.tartaricacid.simplebedrockmodel.client.bedrock;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class BedrockModelAdapter extends Model {
    private final BedrockModel model;

    public BedrockModelAdapter(BedrockModel model, Function<ResourceLocation, RenderType> pRenderType) {
        super(pRenderType);
        this.model = model;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
    }

    public BedrockModel getModel() {
        return model;
    }
}
