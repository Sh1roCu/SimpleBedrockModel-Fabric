package com.github.tartaricacid.simplebedrockmodel.example.client.render.blockentity;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockModel;
import com.github.tartaricacid.simplebedrockmodel.example.client.resource.BedrockModelLoader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public abstract class BedrockModelBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T>{
    private final Material material;
    private final Function<ResourceLocation, RenderType> renderTypeFunction;
    protected BedrockModel model;

    public BedrockModelBlockEntityRenderer(ResourceLocation modelLocation,
                                           Material material,
                                           Function<ResourceLocation, RenderType> renderTypeFunction) {
        this.material = material;
        this.renderTypeFunction = renderTypeFunction;
        this.model = Objects.requireNonNull(BedrockModelLoader.getModel(modelLocation));;
    }

    @Override
    public void render(@NotNull T pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        VertexConsumer buffer = material.buffer(pBuffer, renderTypeFunction);
        BlockState blockState = pBlockEntity.getBlockState();
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            float rotation = facing.toYRot();
            pPoseStack.pushPose();
            pPoseStack.translate(0.5, 0, 0.5);
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
            model.renderToBuffer(pPoseStack, buffer, pPackedLight, pPackedOverlay);
            pPoseStack.popPose();
        } else {
            pPoseStack.pushPose();
            pPoseStack.translate(0.5, 0, 0.5);
            model.renderToBuffer(pPoseStack, buffer, pPackedLight, pPackedOverlay);
            pPoseStack.popPose();
        }
    }
}
