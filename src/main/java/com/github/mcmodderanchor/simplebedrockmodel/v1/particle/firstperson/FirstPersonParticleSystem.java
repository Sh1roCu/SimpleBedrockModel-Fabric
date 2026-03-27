package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.ParticleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 第一人称粒子系统管理器。持有一个或多个发射器实例，统一管理 tick 和渲染。
 * <p>
 * 局部空间粒子（{@code emitter_local_space.position=true}）留在内部列表，
 * 跟随模型空间渲染。世界空间粒子（{@code worldSpace=true}）在生成时立即投递到
 * 原版 {@code ParticleEngine}，由原版管线管理生命周期、渲染和碰撞。
 */
@OnlyIn(Dist.CLIENT)
public class FirstPersonParticleSystem {
    private final List<ParticleEmitterInstance> emitters = new ArrayList<>();
    private final ParticleMolangEnvironment molang = new ParticleMolangEnvironment();

    /**
     * 添加一个粒子效果发射器。
     * <p>
     * 发射器产出的世界空间粒子会自动投递到原版 ParticleEngine，
     * 局部空间粒子留在内部列表由本系统管理。
     *
     * @return 创建的发射器实例，可用于后续控制
     */
    public ParticleEmitterInstance addEmitter(ParticleEffectDefinition definition) {
        ParticleEmitterInstance emitter = new ParticleEmitterInstance(definition, molang);

        // 设置世界空间粒子分流：worldSpace=true 的粒子投递到 ParticleEngine
        emitter.setWorldSpaceParticleCallback(particle ->
                deliverWorldSpaceParticle(particle, definition, molang, emitter));

        emitters.add(emitter);
        return emitter;
    }

    /**
     * 将世界空间粒子投递到原版 ParticleEngine。
     * <p>
     * 第一人称管线中，世界空间粒子的坐标在"以摄像机为原点的世界对齐空间"中，
     * 需要加上摄像机世界位置转换为真正的世界坐标。
     */
    private static void deliverWorldSpaceParticle(ParticleInstance particle,
                                                   ParticleEffectDefinition definition,
                                                   ParticleMolangEnvironment molang,
                                                   ParticleEmitterInstance emitter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.particleEngine == null) return;

        // 将坐标从"以摄像机为原点的世界对齐空间"转换为世界坐标
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        particle.x += (float) camPos.x;
        particle.y += (float) camPos.y;
        particle.z += (float) camPos.z;

        SnowStormParticle worldParticle = new SnowStormParticle(
                mc.level, particle, definition, molang, emitter);
        mc.particleEngine.add(worldParticle);
    }

    /**
     * 更新所有发射器和粒子。
     * <p>
     * 仅更新局部空间粒子。世界空间粒子由原版 ParticleEngine 管理。
     *
     * @param dt 时间步长（秒）
     */
    public void tick(float dt) {
        for (int i = emitters.size() - 1; i >= 0; i--) {
            ParticleEmitterInstance emitter = emitters.get(i);
            emitter.tick(dt);
            if (emitter.isFinished()) {
                emitters.remove(i);
            }
        }
    }

    /**
     * 渲染所有局部空间粒子。在目标 PoseStack 坐标系中绘制。
     * <p>
     * 世界空间粒子由原版 ParticleEngine 渲染，不经过此方法。
     *
     * @param cameraPitch  摄像机 pitch 角度（弧度）
     * @param cameraRoll   摄像机 roll 角度（弧度）
     */
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick,
                       float cameraPitch, float cameraRoll) {
        for (ParticleEmitterInstance emitter : emitters) {
            ParticleRenderer.render(emitter, poseStack, bufferSource, light, partialTick,
                    cameraPitch, cameraRoll);
        }
    }

    /**
     * 移除所有发射器和粒子。
     */
    public void clear() {
        emitters.clear();
    }

    /**
     * 获取当前活跃的局部空间粒子总数（不含已投递到 ParticleEngine 的世界空间粒子）。
     */
    public int getParticleCount() {
        int count = 0;
        for (ParticleEmitterInstance emitter : emitters) {
            count += emitter.getParticles().size();
        }
        return count;
    }

    public List<ParticleEmitterInstance> getEmitters() {
        return emitters;
    }

    public ParticleMolangEnvironment getMolangEnvironment() {
        return molang;
    }
}
