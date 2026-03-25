package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.ParticleRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 粒子系统管理器。持有一个或多个发射器实例，统一管理 tick 和渲染。
 * <p>
 * 使用方式：
 * <pre>
 * ParticleSystem system = new ParticleSystem();
 * system.addEmitter(definition);  // 添加粒子效果
 * // 每帧调用
 * system.tick(deltaTime);
 * system.render(poseStack, bufferSource, light, partialTick);
 * </pre>
 */
public class ParticleSystem {
    private final List<ParticleEmitterInstance> emitters = new ArrayList<>();
    private final ParticleMolangEnvironment molang = new ParticleMolangEnvironment();

    /**
     * 添加一个粒子效果发射器。
     * @return 创建的发射器实例，可用于后续控制
     */
    public ParticleEmitterInstance addEmitter(ParticleEffectDefinition definition) {
        ParticleEmitterInstance emitter = new ParticleEmitterInstance(definition, molang);
        emitters.add(emitter);
        return emitter;
    }

    /**
     * 更新所有发射器和粒子。
     * @param dt 时间步长（秒）
     */
    public void tick(float dt) {
        for (int i = emitters.size() - 1; i >= 0; i--) {
            ParticleEmitterInstance emitter = emitters.get(i);
            emitter.tick(dt);
            // 自动移除已完成的非循环发射器
            if (emitter.isFinished()) {
                emitters.remove(i);
            }
        }
    }

    /**
     * 渲染所有粒子。在目标 PoseStack 坐标系中绘制。
     */
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, float partialTick) {
        for (ParticleEmitterInstance emitter : emitters) {
            ParticleRenderer.render(emitter, poseStack, bufferSource, light, partialTick);
        }
    }

    /**
     * 移除所有发射器和粒子。
     */
    public void clear() {
        emitters.clear();
    }

    /**
     * 获取当前活跃的粒子总数。
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
