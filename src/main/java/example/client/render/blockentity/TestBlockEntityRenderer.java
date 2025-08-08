package example.client.render.blockentity;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.AdditiveBlender;
import com.maydaymemory.mae.blend.SimpleAdditiveBlender;
import com.mojang.blaze3d.vertex.PoseStack;
import example.animation.TestBlockAnimationInstance;
import example.block.blockentity.TestBlockEntity;
import example.init.ExampleModRegister;
import example.resource.KnownResources;
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
        super(KnownResources.TEST, MATERIAL, RenderType::entityCutout);
    }

    @Override
    public void render(@NotNull TestBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        TestBlockAnimationInstance animationInstance = blockEntity.getAnimationInstance();
        animationInstance.renderTick();
        Pose animationPose = animationInstance.getStateMachine().getPose();
        Pose bindPose = model.getBindPose();
        Pose blended = BLENDER.blend(bindPose, animationPose);
        model.applyPose(blended);
        super.render(blockEntity, partialTick, poseStack, buffer, packedLight, packedOverlay);
    }
}
