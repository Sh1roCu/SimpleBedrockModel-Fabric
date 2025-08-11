package example.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import example.animation.GunAnimationGraph;
import example.capability.ModCapability;
import example.init.ExampleModRegister;
import example.resource.BedrockModelRegister;
import example.resource.KnownResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DeagleWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Material material = new Material(TextureAtlas.LOCATION_BLOCKS, KnownResources.DEAGLE.withPrefix("item/"));

    private final BedrockModel model;

    public DeagleWithoutLevelRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        model = BedrockModelRegister.INSTANCE.getModel(KnownResources.DEAGLE);
        // 隐藏手臂分组里面的模型，因为要替换成玩家自己的手臂模型
        BedrockBone leftHandBone = model.getBone("lefthand_pos");
        BedrockBone rightHandBone = model.getBone("righthand_pos");
        if (leftHandBone != null) {
            leftHandBone.visible = false;
        }
        if (rightHandBone != null) {
            rightHandBone.visible = false;
        }
    }

    @SubscribeEvent
    public static void onFirstPersonRender(RenderHandEvent event) {
        if (event.getItemStack().getItem() == ExampleModRegister.DEAGLE_ITEM && event.getHand() == InteractionHand.MAIN_HAND) {
            Minecraft mc = Minecraft.getInstance();
            BedrockModel bedrockModel = BedrockModelRegister.INSTANCE.getModel(KnownResources.DEAGLE);
            // 从 AnimationInstance 中获取 AnimationGraph，然后计算当前帧的 Pose，然后混合并 apply
            if (mc.getCameraEntity() instanceof Player player) {
                player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).ifPresent(capability -> {
                    GunAnimationGraph animationGraph = capability.getAnimationInstance().getAnimationGraph();
                    if (animationGraph != null) {
                        bedrockModel.applyPose(animationGraph.getPose());
                    }
                });
            }
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            {
                // 反转 Bobbing
                if (mc.options.bobView().get() && (mc.getCameraEntity() instanceof Player player)) {
                    float f = player.walkDist - player.walkDistO;
                    float f1 = -(player.walkDist + f * event.getPartialTick());
                    float f2 = Mth.lerp(event.getPartialTick(), player.oBob, player.bob);
                    poseStack.mulPose(Axis.XN.rotationDegrees(Math.abs(Mth.cos(f1 * (float)Math.PI - 0.2F) * f2) * 5.0F));
                    poseStack.mulPose(Axis.ZN.rotationDegrees(Mth.sin(f1 * (float)Math.PI) * f2 * 3.0F));
                    poseStack.translate(-Mth.sin(f1 * (float)Math.PI) * f2 * 0.5F, Math.abs(Mth.cos(f1 * (float)Math.PI) * f2), 0.0F);
                }
                // 这里的 translate 是为了把枪放到合适位置，为了方便直接硬编码了
                poseStack.translate(0.125, -0.5, -1.03125);
                // 执行渲染
                VertexConsumer buffer = material.buffer(event.getMultiBufferSource(), RenderType::entityCutout);
                bedrockModel.renderToBuffer(poseStack, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                // 渲染手臂
                if (mc.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
                    BedrockBone leftHandBone = bedrockModel.getBone("lefthand_pos");
                    BedrockBone rightHandBone = bedrockModel.getBone("righthand_pos");
                    RenderSystem.setShaderTexture(0, abstractClientPlayer.getSkinTextureLocation());
                    PlayerRenderer playerrenderer = (PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(abstractClientPlayer);
                    if (leftHandBone != null) {
                        Matrix4f globalTransform = leftHandBone.getGlobalTransform();
                        poseStack.pushPose();
                        poseStack.mulPoseMatrix(globalTransform);
                        playerrenderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), abstractClientPlayer);
                        poseStack.popPose();
                    }
                    if (rightHandBone != null) {
                        Matrix4f globalTransform = rightHandBone.getGlobalTransform();
                        poseStack.pushPose();
                        poseStack.mulPoseMatrix(globalTransform);
                        playerrenderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), abstractClientPlayer);
                        poseStack.popPose();
                    }
                }
            }
            poseStack.popPose();
            event.setCanceled(true);
            // 恢复被动画影响的模型
            bedrockModel.applyPose(bedrockModel.getBindPose());
        }
    }

    @ParametersAreNonnullByDefault
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                             int light, int overlay) {
        // 第一人称不用这个渲染，而是改为监听 RenderHandEvent 渲染。这里测试实现的比较粗糙，实际生产环境需要斟酌
        if (ctx.firstPerson()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        VertexConsumer buffer = material.buffer(bufferSource, RenderType::entityCutout);
        model.renderToBuffer(poseStack, buffer, light, overlay);
        poseStack.popPose();
    }

    private static void resetRotation(ModelPart part) {
        part.xRot = 0;
        part.yRot = 0;
        part.zRot = 0;
    }
}
