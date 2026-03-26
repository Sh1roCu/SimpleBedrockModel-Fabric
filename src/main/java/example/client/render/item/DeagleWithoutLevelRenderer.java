package example.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelReloadListenerEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import example.animation.DeagleAnimationGraph;
import example.animation.GunAnimationGraph;
import example.capability.ModCapability;
import example.init.ExampleModRegister;
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
import net.minecraft.resources.ResourceLocation;
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

import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DeagleWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Material material = new Material(TextureAtlas.LOCATION_BLOCKS, KnownResources.DEAGLE.withPrefix("item/"));

    private static BedrockModel model;

    // 粒子系统
    private static final ParticleSystem particleSystem = new ParticleSystem();
    private static long lastRenderTimeNano;

    // 暂时只能想到这么丑的办法
    @Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModelReloadListenerRegister {
        @SubscribeEvent
        public static void onModelReloadListenerRegister(RegisterBedrockModelReloadListenerEvent event) {
            event.register(map -> {
                model = map.get(KnownResources.DEAGLE);
                BedrockBone leftHandBone = model.getBone("lefthand_pos");
                BedrockBone rightHandBone = model.getBone("righthand_pos");
                if (leftHandBone != null) {
                    leftHandBone.visible = false;
                }
                if (rightHandBone != null) {
                    rightHandBone.visible = false;
                }
                // 资源重载时清空粒子缓存
                particleSystem.clear();
            });
        }
    }

    public DeagleWithoutLevelRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent
    public static void onFirstPersonRender(RenderHandEvent event) {
        if (event.getItemStack().getItem() == ExampleModRegister.DEAGLE_ITEM && event.getHand() == InteractionHand.MAIN_HAND) {
            Minecraft mc = Minecraft.getInstance();
            DeagleAnimationGraph deagleGraph = null;
            // 从 AnimationInstance 中获取 AnimationGraph，然后计算当前帧的 Pose，然后混合并 apply
            if (mc.getCameraEntity() instanceof Player player) {
                player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).ifPresent(capability -> {
                    GunAnimationGraph animationGraph = capability.getAnimationInstance().getAnimationGraph();
                    if (animationGraph != null) {
                        model.applyPose(animationGraph.getPose());
                    }
                });
                // 获取 DeagleAnimationGraph 引用用于检查粒子触发
                var cap = player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).orElse(null);
                if (cap != null && cap.getAnimationInstance().getAnimationGraph() instanceof DeagleAnimationGraph dag) {
                    deagleGraph = dag;
                }
            }

            // 计算帧间 dt
            long now = System.nanoTime();
            float dt = lastRenderTimeNano == 0 ? 0 : (now - lastRenderTimeNano) / 1_000_000_000f;
            dt = Math.min(dt, 0.1f); // 限制最大 dt 防止卡顿时粒子爆炸
            lastRenderTimeNano = now;

            // 检查是否需要发射粒子（从动画通道获取）
            if (deagleGraph != null) {
                List<ParticleEffectData> pendingParticles = deagleGraph.consumePendingParticles();
                for (ParticleEffectData data : pendingParticles) {
                    ParticleEffectDefinition def = ParticleDefinitionLoader.getInstance().getDefinition(data.effect());
                    if (def != null) {
                        particleSystem.addEmitter(def);
                    }
                }
            }

            // tick 粒子
            particleSystem.tick(dt);

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
                model.renderToBuffer(poseStack, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                // 渲染手臂
                if (mc.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
                    BedrockBone leftHandBone = model.getBone("lefthand_pos");
                    BedrockBone rightHandBone = model.getBone("righthand_pos");
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
                // 渲染粒子（挂到 muzzle_pos 骨骼位置）
                if (particleSystem.getParticleCount() > 0) {
                    BedrockBone muzzleBone = model.getBone("muzzle_pos");
                    if (muzzleBone != null) {
                        poseStack.pushPose();
                        poseStack.mulPoseMatrix(muzzleBone.getGlobalTransform());
                        particleSystem.render(poseStack, event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
                        poseStack.popPose();
                    } else {
                        particleSystem.render(poseStack, event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
                    }
                }

            }
            poseStack.popPose();
            event.setCanceled(true);
            // 恢复被动画影响的模型
            model.applyPose(model.getBindPose());
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
