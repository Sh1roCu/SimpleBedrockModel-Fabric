package com.github.tartaricacid.simplebedrockmodel.client.bedrock;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockModel;
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
    public void setupAnim(T t, float v, float v1, float v2, float v3, float v4) {}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
    }

    public BedrockModel getModel() {
        return model;
    }
}
