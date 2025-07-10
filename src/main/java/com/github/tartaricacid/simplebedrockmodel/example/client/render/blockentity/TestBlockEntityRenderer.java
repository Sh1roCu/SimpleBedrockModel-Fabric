package com.github.tartaricacid.simplebedrockmodel.example.client.render.blockentity;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import com.github.tartaricacid.simplebedrockmodel.example.block.blockentity.TestBlockEntity;
import com.github.tartaricacid.simplebedrockmodel.example.client.resource.BedrockAnimationLoader;
import com.github.tartaricacid.simplebedrockmodel.example.client.resource.BedrockModelLoader;
import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.AdditiveBlender;
import com.maydaymemory.mae.blend.SimpleAdditiveBlender;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.LoopingState;
import com.maydaymemory.mae.util.MathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TestBlockEntityRenderer extends BedrockModelBlockEntityRenderer<TestBlockEntity> {
    public static final Material MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "block/test"));
    private static AnimationRunner animationRunner;
    private static final AdditiveBlender blender = new SimpleAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public TestBlockEntityRenderer(BlockEntityRendererProvider.Context pContext) {
        super(BedrockModelLoader.TEST, MATERIAL, RenderType::entityCutout);
    }

    @Override
    public void render(@NotNull TestBlockEntity pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        if (animationRunner == null) {
            Animation animation = BedrockAnimationLoader.getAnimations(BedrockAnimationLoader.TEST).get("治疗魔法蓄力ing");
            animationRunner = new AnimationRunner(animation, new AnimationContext(MathUtil.toNanos(animation.getEndTimeS())));
            animationRunner.getAnimationContext().setState(new LoopingState(System::nanoTime));
        }
        animationRunner.tick();
        Pose bindPose = model.getBindPose();
        Pose animationPose = animationRunner.evaluate();
        Pose blended = blender.blend(bindPose, animationPose);
        model.applyPose(blended);
        super.render(pBlockEntity, pPartialTick, pPoseStack, pBuffer, pPackedLight, pPackedOverlay);
    }
}
