package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleDescription;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.ParticleAppearanceBillboard;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 粒子渲染器。将发射器中的所有粒子渲染到 PoseStack 局部坐标系中。
 */
@OnlyIn(Dist.CLIENT)
public final class ParticleRenderer {

    private ParticleRenderer() {}

    /**
     * 渲染一个发射器的所有粒子。
     * <p>
     * 发射器变换矩阵从 emitter 实例上读取。对于局部空间粒子（worldSpace=false），
     * 渲染时会叠加发射器变换；对于世界空间粒子（worldSpace=true），直接使用粒子坐标。
     */
    public static void render(ParticleEmitterInstance emitter, PoseStack poseStack,
                               MultiBufferSource bufferSource, int light, float partialTick,
                               Matrix4f worldToView) {
        List<ParticleInstance> particles = emitter.getParticles();
        if (particles.isEmpty()) return;

        var definition = emitter.getDefinition();
        ParticleDescription desc = definition.getDescription();
        ResourceLocation texture = desc.getTexture();

        // 选择 RenderType
        RenderType renderType = getRenderType(desc.getMaterial(), texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        // 确定朝向模式
        ParticleAppearanceBillboard billboard = definition.findComponent(ParticleAppearanceBillboard.class);
        ParticleAppearanceBillboard.FaceCameraMode mode = billboard != null
                ? billboard.faceCameraMode()
                : ParticleAppearanceBillboard.FaceCameraMode.ROTATE_XYZ;

        Matrix4f emitterTransform = emitter.getEmitterTransform();
        boolean localPos = emitter.isLocalPosition();
        boolean localRot = emitter.isLocalRotation();

        for (ParticleInstance particle : particles) {
            BillboardHelper.renderBillboard(particle, poseStack, consumer, light, mode,
                    emitterTransform, localPos, localRot, worldToView);
        }
    }

    /**
     * 根据材质类型选择 RenderType。
     */
    private static RenderType getRenderType(ParticleDescription.Material material, ResourceLocation texture) {
        return switch (material) {
            case PARTICLES_OPAQUE -> RenderType.entityCutout(texture);
            case PARTICLES_ALPHA -> RenderType.entityTranslucent(texture);
            case PARTICLES_BLEND -> RenderType.entityTranslucent(texture);
            case PARTICLES_ADD -> ParticleRenderType.additiveParticle(texture);
        };
    }
}
