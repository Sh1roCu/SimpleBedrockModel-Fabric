package example.client.render.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.animation.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.animation.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelReloadListenerEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import example.animation.DeagleAnimationGraph;
import example.animation.GunAnimationGraph;
import example.capability.ModCapability;
import example.init.ExampleModRegister;
import example.resource.KnownResources;
import net.minecraft.client.Camera;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DeagleWithoutLevelRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Material MATERIAL = new Material(TextureAtlas.LOCATION_BLOCKS, KnownResources.DEAGLE.withPrefix("item/"));
    private static final AnimationClock CLOCK = AnimationClocks.client();
    private static final FirstPersonParticleSystem PARTICLE_SYSTEM = new FirstPersonParticleSystem();
    private static final Map<ParticleEmitterInstance, String> EMITTER_LOCATOR_MAP = new HashMap<>();

    private static BedrockModel model;
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
                PARTICLE_SYSTEM.clear();
                EMITTER_LOCATOR_MAP.clear();
                lastRenderTimeNano = 0L;
            });
        }
    }

    public DeagleWithoutLevelRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /**
     * 用摄像机的 pitch/yaw/roll 构建视图旋转矩阵（与 Minecraft 内部一致）。
     * Minecraft 的视图矩阵构建顺序：先绕 X 旋转 pitch，再绕 Y 旋转 (yaw + 180)，最后绕 Z 旋转 roll。
     */
    private static Matrix4f buildCameraRotation(Camera camera, float rollRadians) {
        return new Matrix4f()
                .rotationX((float) Math.toRadians(camera.getXRot()))
                .rotateY((float) Math.toRadians(camera.getYRot() + 180f))
                .rotateZ(rollRadians);
    }

    @SubscribeEvent
    public static void onFirstPersonRender(RenderHandEvent event) {
        if (event.getItemStack().getItem() != ExampleModRegister.DEAGLE_ITEM || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

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
            var cap = player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).orElse(null);
            if (cap != null && cap.getAnimationInstance().getAnimationGraph() instanceof DeagleAnimationGraph dag) {
                deagleGraph = dag;
            }
        }

        long now = CLOCK.nowNanos();
        float dt = lastRenderTimeNano == 0L ? 0.0f : (now - lastRenderTimeNano) / 1_000_000_000f;
        dt = Math.min(dt, 0.1f);
        lastRenderTimeNano = now;

        if (deagleGraph != null) {
            List<ParticleEffectData> pendingParticles = deagleGraph.consumePendingParticles();
            for (ParticleEffectData data : pendingParticles) {
                ParticleEffectDefinition def = ParticleDefinitionLoader.getInstance().getDefinition(data.effect());
                if (def != null) {
                    ParticleEmitterInstance emitter = PARTICLE_SYSTEM.addEmitter(def);
                    String locator = data.locator();
                    if (locator != null && !locator.isEmpty()) {
                        EMITTER_LOCATOR_MAP.put(emitter, locator);
                    }
                }
            }
        }

        Matrix4f modelTransform = new Matrix4f();
        if (mc.options.bobView().get() && mc.getCameraEntity() instanceof Player player2) {
            float f = player2.walkDist - player2.walkDistO;
            float f1 = -(player2.walkDist + f * event.getPartialTick());
            float f2 = Mth.lerp(event.getPartialTick(), player2.oBob, player2.bob);
            modelTransform.rotate(Axis.XN.rotationDegrees(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F));
            modelTransform.rotate(Axis.ZN.rotationDegrees(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F));
            modelTransform.translate(-Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F, (float) Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0F);
        }
        modelTransform.translate(0.125f, -0.5f, -1.03125f);

        PoseStack poseStack = event.getPoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        float cameraRollRad = CameraStateCache.getCameraRollRadians();
        float cameraPitchRad = (float) Math.toRadians(camera.getXRot());
        Matrix4f cameraRotation = buildCameraRotation(camera, cameraRollRad);
        Matrix4f cameraRotationInv = new Matrix4f(cameraRotation).invert();
        Matrix4f poseInitial = new Matrix4f(poseStack.last().pose());
        Matrix4f toWorldAligned = new Matrix4f(cameraRotationInv).mul(poseInitial).mul(modelTransform);

        for (ParticleEmitterInstance emitter : PARTICLE_SYSTEM.getEmitters()) {
            String locatorName = EMITTER_LOCATOR_MAP.get(emitter);
            Matrix4f transform = null;
            if (locatorName != null) {
                transform = model.getLocatorTransform(locatorName);
            }
            if (transform == null) {
                BedrockBone bone = locatorName != null ? model.getBone(locatorName) : null;
                if (bone != null) {
                    transform = bone.getGlobalTransform();
                }
            }
            if (transform != null) {
                Matrix4f worldTransform = new Matrix4f(toWorldAligned).mul(transform);
                emitter.setEmitterTransform(transform, worldTransform);
            }
        }

        EMITTER_LOCATOR_MAP.keySet().removeIf(ParticleEmitterInstance::isFinished);
        if (!CLOCK.shouldTick()) {
            dt = 0.0f;
        }
        PARTICLE_SYSTEM.tick(dt);

        poseStack.pushPose();
        if (mc.options.bobView().get() && mc.getCameraEntity() instanceof Player player) {
            float f = player.walkDist - player.walkDistO;
            float f1 = -(player.walkDist + f * event.getPartialTick());
            float f2 = Mth.lerp(event.getPartialTick(), player.oBob, player.bob);
            poseStack.mulPose(Axis.XN.rotationDegrees(Math.abs(Mth.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F));
            poseStack.mulPose(Axis.ZN.rotationDegrees(Mth.sin(f1 * (float) Math.PI) * f2 * 3.0F));
            poseStack.translate(-Mth.sin(f1 * (float) Math.PI) * f2 * 0.5F, Math.abs(Mth.cos(f1 * (float) Math.PI) * f2), 0.0F);
        }
        poseStack.translate(0.125, -0.5, -1.03125);

        VertexConsumer buffer = MATERIAL.buffer(event.getMultiBufferSource(), RenderType::entityCutout);
        model.renderToBuffer(poseStack, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        if (mc.getCameraEntity() instanceof AbstractClientPlayer abstractClientPlayer) {
            BedrockBone leftHandBone = model.getBone("lefthand_pos");
            BedrockBone rightHandBone = model.getBone("righthand_pos");
            RenderSystem.setShaderTexture(0, abstractClientPlayer.getSkinTextureLocation());
            PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(abstractClientPlayer);
            if (leftHandBone != null) {
                Matrix4f globalTransform = leftHandBone.getGlobalTransform();
                poseStack.pushPose();
                poseStack.mulPoseMatrix(globalTransform);
                playerRenderer.renderLeftHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), abstractClientPlayer);
                poseStack.popPose();
            }
            if (rightHandBone != null) {
                Matrix4f globalTransform = rightHandBone.getGlobalTransform();
                poseStack.pushPose();
                poseStack.mulPoseMatrix(globalTransform);
                playerRenderer.renderRightHand(poseStack, event.getMultiBufferSource(), event.getPackedLight(), abstractClientPlayer);
                poseStack.popPose();
            }
        }
        if (PARTICLE_SYSTEM.getParticleCount() > 0) {
            PARTICLE_SYSTEM.render(poseStack, event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick(), cameraPitchRad, cameraRollRad, cameraRotation);
        }
        poseStack.popPose();

        event.setCanceled(true);
        model.applyPose(model.getBindPose());
    }

    @ParametersAreNonnullByDefault
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        if (ctx.firstPerson()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        VertexConsumer buffer = MATERIAL.buffer(bufferSource, RenderType::entityCutout);
        model.renderToBuffer(poseStack, buffer, light, overlay);
        poseStack.popPose();
    }

    private static void resetRotation(ModelPart part) {
        part.xRot = 0;
        part.yRot = 0;
        part.zRot = 0;
    }
}
