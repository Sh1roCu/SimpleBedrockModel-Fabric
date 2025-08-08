package example.client.render.blockentity;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import example.resource.BedrockModelRegister;
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
import java.util.function.Supplier;

public abstract class BedrockModelBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private final Function<ResourceLocation, RenderType> renderTypeFunction;
    private final Material material;
    protected Supplier<BedrockModel> model;

    public BedrockModelBlockEntityRenderer(ResourceLocation modelLocation, Material material,
                                           Function<ResourceLocation, RenderType> renderTypeFunction) {
        this.material = material;
        this.renderTypeFunction = renderTypeFunction;
        this.model = Suppliers.memoize(() -> Objects.requireNonNull(BedrockModelRegister.INSTANCE.getModel(modelLocation)));
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        VertexConsumer buffer = material.buffer(bufferSource, renderTypeFunction);
        BlockState blockState = blockEntity.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        }
        model.get().renderToBuffer(poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
