package example.client.render.blockentity;

import com.maydaymemory.mae.basic.Animation;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.AdditiveBlender;
import com.maydaymemory.mae.blend.SimpleAdditiveBlender;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.PlayingState;
import com.maydaymemory.mae.control.runner.StopState;
import com.maydaymemory.mae.util.MathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import example.block.blockentity.TestBlockEntity;
import example.client.resource.BedrockAnimationLoader;
import example.client.resource.BedrockModelLoader;
import example.init.ExampleModRegister;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class TestBlockEntityRenderer extends BedrockModelBlockEntityRenderer<TestBlockEntity> {
    private static final Material MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, ExampleModRegister.modLoc("block/test"));
    private static final AdditiveBlender BLENDER = new SimpleAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    public TestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(BedrockModelLoader.TEST_MODEL, MATERIAL, RenderType::entityCutout);
    }

    @Override
    public void render(@NotNull TestBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.animationRunner == null) {
            String currentAnimation = blockEntity.currentAnimation();
            Animation animation = BedrockAnimationLoader.getAnimations(BedrockAnimationLoader.TEST_ANIMATION).get(currentAnimation);
            blockEntity.animationRunner = new AnimationRunner(animation, new AnimationContext(MathUtil.toNanos(animation.getEndTimeS())));
            blockEntity.animationRunner.getAnimationContext().setState(new PlayingState(System::nanoTime, StopState::new));
        }
        if (blockEntity.animationRunner.getAnimationContext().isEnd()) {
            String nextAnimation = blockEntity.nextAnimation();
            Animation animation = BedrockAnimationLoader.getAnimations(BedrockAnimationLoader.TEST_ANIMATION).get(nextAnimation);
            blockEntity.animationRunner = new AnimationRunner(animation, new AnimationContext(MathUtil.toNanos(animation.getEndTimeS())));
            blockEntity.animationRunner.getAnimationContext().setState(new PlayingState(System::nanoTime, StopState::new));
        }
        blockEntity.animationRunner.tick();
        Pose bindPose = model.getBindPose();
        Pose animationPose = blockEntity.animationRunner.evaluate();
        Pose blended = BLENDER.blend(bindPose, animationPose);
        model.applyPose(blended);
        super.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
}
