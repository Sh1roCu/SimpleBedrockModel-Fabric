package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 世界粒子发射器管理器。
 * <p>
 * 管理世界空间中的粒子发射器生命周期。发射器产出的粒子被转换为
 * {@link SnowStormParticle} 并投递到 Minecraft 原版 {@code ParticleEngine}，
 * 由原版管线管理粒子的渲染和碰撞。
 * <p>
 * 使用方式：
 * <pre>
 * WorldEmitterManager manager = WorldEmitterManager.getInstance();
 * manager.addEmitter(clientLevel, position, definition);
 * // 每 tick 调用
 * manager.tick();
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class WorldEmitterManager {

    private static WorldEmitterManager INSTANCE;

    private final List<ActiveWorldEmitter> emitters = new ArrayList<>();

    public static WorldEmitterManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WorldEmitterManager();
        }
        return INSTANCE;
    }

    /**
     * 添加一个世界粒子发射器。
     *
     * @param level      客户端世界
     * @param pos        发射器世界坐标
     * @param definition 粒子效果定义
     * @return 创建的发射器实例，可用于后续控制；如果 definition 为 null 则返回 null
     */
    @Nullable
    public ParticleEmitterInstance addEmitter(ClientLevel level, Vec3 pos, @Nullable ParticleEffectDefinition definition) {
        if (definition == null) {
            SimpleBedrockModel.LOGGER.warn("Attempted to add world emitter with null definition");
            return null;
        }

        ParticleMolangEnvironment molang = new ParticleMolangEnvironment();
        ParticleEmitterInstance emitter = new ParticleEmitterInstance(definition, molang);

        ActiveWorldEmitter active = new ActiveWorldEmitter();
        active.emitter = emitter;
        active.molang = molang;
        active.level = level;
        active.worldX = pos.x;
        active.worldY = pos.y;
        active.worldZ = pos.z;
        active.definition = definition;

        // 设置发射器变换：世界粒子不需要 locator 变换，
        // emitterTransform 为单位矩阵，worldTransform 为平移到世界坐标的矩阵
        Matrix4f identity = new Matrix4f();
        Matrix4f worldTransform = new Matrix4f().translation((float) pos.x, (float) pos.y, (float) pos.z);
        emitter.setEmitterTransform(identity, worldTransform);

        // 设置外部粒子管理：新生成的粒子通过回调投递到 ParticleEngine
        emitter.setExternalParticleManagement(particle -> {
            deliverParticle(active, particle);
        });

        emitters.add(active);
        return emitter;
    }

    /**
     * 每 tick 驱动所有发射器。
     * <p>
     * 应在 {@code TickEvent.ClientTickEvent} 中调用。
     */
    public void tick() {
        float dt = 1f / 20f; // 固定 tick 步长

        for (int i = emitters.size() - 1; i >= 0; i--) {
            ActiveWorldEmitter active = emitters.get(i);

            // 检查世界是否仍然有效
            if (active.level != Minecraft.getInstance().level) {
                emitters.remove(i);
                continue;
            }

            active.emitter.tick(dt);

            if (active.emitter.isFinished()) {
                emitters.remove(i);
            }
        }
    }

    /**
     * 将发射器产出的粒子转换为 {@link SnowStormParticle} 并投递到 ParticleEngine。
     */
    private void deliverParticle(ActiveWorldEmitter active, ParticleInstance particle) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.particleEngine == null || active.level == null) return;

        // 粒子坐标已经在世界空间中（ParticleEmitterInstance 的 applyLocalSpaceOnSpawn 已处理）
        // 如果粒子是世界空间的，坐标已经是绝对世界坐标
        // 如果粒子是局部空间的（localPosition=true），需要加上发射器位置
        double px, py, pz;
        if (particle.worldSpace) {
            px = particle.x;
            py = particle.y;
            pz = particle.z;
        } else {
            px = active.worldX + particle.x;
            py = active.worldY + particle.y;
            pz = active.worldZ + particle.z;
        }

        // 更新粒子位置为世界坐标
        particle.x = (float) px;
        particle.y = (float) py;
        particle.z = (float) pz;

        SnowStormParticle worldParticle = new SnowStormParticle(
                active.level, particle, active.definition, active.molang, active.emitter);

        mc.particleEngine.add(worldParticle);
    }

    /**
     * 清空所有发射器。
     */
    public void clear() {
        emitters.clear();
    }

    /**
     * 获取当前活跃的发射器数量。
     */
    public int getEmitterCount() {
        return emitters.size();
    }

    /**
     * 活跃的世界发射器上下文。
     */
    private static class ActiveWorldEmitter {
        ParticleEmitterInstance emitter;
        ParticleMolangEnvironment molang;
        ClientLevel level;
        double worldX, worldY, worldZ;
        ParticleEffectDefinition definition;
    }
}
