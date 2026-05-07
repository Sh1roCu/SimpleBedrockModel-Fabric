package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.debug;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class ParticleDebugRenderer {
    private static final double BOX_RADIUS = 0.25D;

    public static void onRenderLevelStage(WorldRenderContext context) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
//            return;
//        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.getEntityRenderDispatcher().shouldRenderHitBoxes()) {
            return;
        }

        if (WorldEmitterManager.getInstance().getEmitters().isEmpty()) {
            return;
        }

        PoseStack poseStack = context.matrixStack();
        Vec3 cameraPos = context.camera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        for (WorldEmitterManager.ActiveWorldEmitter emitter : WorldEmitterManager.getInstance().getEmitters()) {
            if (emitter.level != mc.level) {
                continue;
            }

            AABB box = new AABB(
                    emitter.worldX - BOX_RADIUS - cameraPos.x,
                    emitter.worldY - BOX_RADIUS - cameraPos.y,
                    emitter.worldZ - BOX_RADIUS - cameraPos.z,
                    emitter.worldX + BOX_RADIUS - cameraPos.x,
                    emitter.worldY + BOX_RADIUS - cameraPos.y,
                    emitter.worldZ + BOX_RADIUS - cameraPos.z
            );
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, box, 0.2F, 1.0F, 1.0F, 1.0F);
        }
        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
    }
}
