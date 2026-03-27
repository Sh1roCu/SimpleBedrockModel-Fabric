package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import team.unnamed.mocha.runtime.MochaFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 发射器实例。管理粒子的发射逻辑和粒子池。
 * <p>
 * 所有坐标在局部坐标系中工作。
 */
public class ParticleEmitterInstance {
    private static final int MAX_PARTICLES = 1000;
    private static final Random RANDOM = new Random();

    private final ParticleEffectDefinition definition;
    private final ParticleMolangEnvironment molang;

    // 编译后的 Molang 函数缓存
    private final CompiledExpressions compiled;

    // 发射器状态
    private float emitterAge;
    private float emitterLifetime;
    private boolean active = true;
    private boolean sleeping;
    private float sleepTimer;
    private boolean hasEmittedInstant;
    private float spawnAccumulator; // 用于 Steady 模式的累积

    // 随机种子
    private final int emitterRandom1 = RANDOM.nextInt();
    private final int emitterRandom2 = RANDOM.nextInt();
    private final int emitterRandom3 = RANDOM.nextInt();
    private final int emitterRandom4 = RANDOM.nextInt();

    // 粒子池
    private final List<ParticleInstance> particles = new ArrayList<>();
    private final List<ParticleInstance> pool = new ArrayList<>();

    // 发射器变换（由外部每帧设置）
    // emitterTransform: locator 局部变换（发射器局部空间 → 模型空间，用于局部空间粒子的渲染）
    private final Matrix4f emitterTransform = new Matrix4f();
    // worldTransform: 完整变换（发射器局部空间 → 世界空间），用于世界空间粒子的 spawn 和速度转换
    private final Matrix4f worldTransform = new Matrix4f();
    private boolean hasTransform = false;

    // emitter_local_space 配置
    private final boolean localPosition;
    private final boolean localRotation;
    private final boolean localVelocity;

    // 粒子生成回调
    @Nullable
    private ParticleSpawnCallback spawnCallback;
    private boolean externalParticleManagement;

    /**
     * 粒子生成回调接口。
     * <p>
     * 两种使用模式：
     * <ul>
     *   <li>全部外部管理（{@code externalParticleManagement=true}）：所有粒子都通过回调交给外部，不加入内部列表。
     *       用于 {@link com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager}。</li>
     *   <li>世界空间粒子分流（{@code externalParticleManagement=false}）：仅 {@code worldSpace=true} 的粒子走回调，
     *       局部空间粒子仍留在内部列表。用于第一人称管线将世界空间粒子投递到原版 ParticleEngine。</li>
     * </ul>
     */
    @FunctionalInterface
    public interface ParticleSpawnCallback {
        void onParticleSpawned(ParticleInstance particle);
    }

    public ParticleEmitterInstance(ParticleEffectDefinition definition, ParticleMolangEnvironment molang) {
        this.definition = definition;
        this.molang = molang;
        this.compiled = new CompiledExpressions(definition, molang);

        // 读取 emitter_local_space 配置
        EmitterLocalSpace localSpace = definition.findComponent(EmitterLocalSpace.class);
        if (localSpace != null) {
            this.localPosition = localSpace.position();
            // position=false, rotation=true 是无效组合，按 rotation=false 处理
            this.localRotation = localSpace.position() && localSpace.rotation();
        } else {
            this.localPosition = false;
            this.localRotation = false;
        }

        // 计算发射器生命周期
        bindEmitterContext();
        EmitterLifetime lifetime = definition.findComponent(EmitterLifetime.class);
        if (lifetime instanceof EmitterLifetime.Looping looping) {
            this.emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
        } else if (lifetime instanceof EmitterLifetime.Once once) {
            this.emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
        } else {
            this.emitterLifetime = Float.MAX_VALUE;
        }
    }

    /**
     * 设置发射器的当前变换矩阵。每帧由外部调用，在 tick 之前。
     *
     * @param locatorTransform locator 局部变换矩阵（发射器局部空间 → 模型空间）
     * @param worldTransformIn 完整变换矩阵（发射器局部空间 → 世界空间）
     */
    public void setEmitterTransform(Matrix4f locatorTransform, Matrix4f worldTransformIn) {
        emitterTransform.set(locatorTransform);
        worldTransform.set(worldTransformIn);
        hasTransform = true;
    }

    /**
     * 获取当前发射器变换矩阵。
     */
    public Matrix4f getEmitterTransform() {
        return emitterTransform;
    }

    public boolean isLocalPosition() {
        return localPosition;
    }

    public boolean isLocalRotation() {
        return localRotation;
    }

    /**
     * 更新发射器和所有粒子。
     * @param dt 时间步长（秒）
     */
    public void tick(float dt) {
        if (!active) {
            // 发射器已停止，但仍需更新存活的粒子
            updateParticles(dt);
            return;
        }

        // 处理休眠
        if (sleeping) {
            sleepTimer -= dt;
            if (sleepTimer <= 0) {
                sleeping = false;
                emitterAge = 0;
                hasEmittedInstant = false;
                spawnAccumulator = 0;
                bindEmitterContext();
                emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
            }
            // 休眠期间仍然更新已有粒子
            updateParticles(dt);
            return;
        }

        emitterAge += dt;
        bindEmitterContext();

        // 检查发射器生命周期
        if (emitterAge >= emitterLifetime) {
            handleLifetimeEnd();
        } else {
            // 发射粒子
            emitParticles(dt);
        }

        // 更新所有粒子
        updateParticles(dt);
    }

    private void handleLifetimeEnd() {
        EmitterLifetime lifetime = definition.findComponent(EmitterLifetime.class);
        if (lifetime instanceof EmitterLifetime.Looping looping) {
            // 进入休眠
            sleeping = true;
            bindEmitterContext();
            sleepTimer = (float) compiled.emitterSleepTime.evaluate();
            if (sleepTimer <= 0) {
                // 无休眠，直接重启
                sleeping = false;
                emitterAge = 0;
                hasEmittedInstant = false;
                spawnAccumulator = 0;
                bindEmitterContext();
                emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
            }
        } else {
            // Once 模式，停止发射（但已有粒子继续存活）
            active = false;
        }
    }

    private void emitParticles(float dt) {
        EmitterRate rate = definition.findComponent(EmitterRate.class);
        if (rate instanceof EmitterRate.Instant instant) {
            if (!hasEmittedInstant) {
                hasEmittedInstant = true;
                int count = (int) compiled.emitterAmount.evaluate();
                for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
                    spawnParticle();
                }
            }
        } else if (rate instanceof EmitterRate.Steady steady) {
            int maxP = (int) compiled.emitterMaxParticles.evaluate();
            float spawnRate = (float) compiled.emitterSpawnRate.evaluate();
            spawnAccumulator += spawnRate * dt;
            while (spawnAccumulator >= 1f && particles.size() < maxP && particles.size() < MAX_PARTICLES) {
                spawnAccumulator -= 1f;
                spawnParticle();
            }
        }
    }

    private void spawnParticle() {
        ParticleInstance p = obtainParticle();
        p.reset();

        // 随机种子
        p.random1 = RANDOM.nextFloat();
        p.random2 = RANDOM.nextFloat();
        p.random3 = RANDOM.nextFloat();
        p.random4 = RANDOM.nextFloat();

        // 初始位置（由形状决定，在发射器局部空间中）
        applyShape(p);

        // 初始速度（在发射器局部空间中）
        applyInitialSpeed(p);

        // 生命周期
        molang.bindParticle(0, 1, p.random1, p.random2, p.random3, p.random4);
        ParticleLifetimeExpression lifetimeComp = definition.findComponent(ParticleLifetimeExpression.class);
        if (lifetimeComp != null) {
            p.maxLifetime = (float) compiled.particleMaxLifetime.evaluate();
        }

        // 初始尺寸
        applyAppearance(p);

        // 初始颜色
        applyTinting(p);

        // 初始自旋
        if (compiled.initialRotation != null) {
            p.rotation = (float) compiled.initialRotation.evaluate();
        }
        if (compiled.initialRotationRate != null) {
            p.rotationRate = (float) compiled.initialRotationRate.evaluate();
        }

        // 根据 emitter_local_space 配置转换坐标空间
        if (hasTransform) {
            applyLocalSpaceOnSpawn(p);
        }

        // 粒子分流：
        // 1. 全部外部管理模式：所有粒子走回调
        // 2. 有回调但非全部外部管理：仅 worldSpace 粒子走回调，局部空间粒子留在内部
        // 3. 无回调：全部留在内部列表
        if (spawnCallback != null && (externalParticleManagement || p.worldSpace)) {
            spawnCallback.onParticleSpawned(p);
        } else {
            particles.add(p);
        }
    }

    private void applyShape(ParticleInstance p) {
        EmitterShape shape = definition.findComponent(EmitterShape.class);
        if (shape instanceof EmitterShape.Point point) {
            p.x = (float) compiled.shapeOffset[0].evaluate();
            p.y = (float) compiled.shapeOffset[1].evaluate();
            p.z = (float) compiled.shapeOffset[2].evaluate();
        } else if (shape instanceof EmitterShape.Sphere sphere) {
            float r = (float) compiled.shapeRadius.evaluate();
            float ox = (float) compiled.shapeOffset[0].evaluate();
            float oy = (float) compiled.shapeOffset[1].evaluate();
            float oz = (float) compiled.shapeOffset[2].evaluate();

            // 随机方向
            float theta = (float) (RANDOM.nextFloat() * Math.PI * 2);
            float phi = (float) (Math.acos(2 * RANDOM.nextFloat() - 1));
            float dist = sphere.surfaceOnly() ? r : r * (float) Math.cbrt(RANDOM.nextFloat());

            p.x = ox + dist * (float) (Math.sin(phi) * Math.cos(theta));
            p.y = oy + dist * (float) Math.cos(phi);
            p.z = oz + dist * (float) (Math.sin(phi) * Math.sin(theta));
        } else if (shape instanceof EmitterShape.Box box) {
            float ox = (float) compiled.shapeOffset[0].evaluate();
            float oy = (float) compiled.shapeOffset[1].evaluate();
            float oz = (float) compiled.shapeOffset[2].evaluate();
            float hx = (float) compiled.shapeHalfDimensions[0].evaluate();
            float hy = (float) compiled.shapeHalfDimensions[1].evaluate();
            float hz = (float) compiled.shapeHalfDimensions[2].evaluate();

            if (box.surfaceOnly()) {
                // 在盒体表面随机
                int face = RANDOM.nextInt(6);
                float u = RANDOM.nextFloat() * 2 - 1;
                float v = RANDOM.nextFloat() * 2 - 1;
                switch (face) {
                    case 0 -> { p.x = ox + hx; p.y = oy + u * hy; p.z = oz + v * hz; }
                    case 1 -> { p.x = ox - hx; p.y = oy + u * hy; p.z = oz + v * hz; }
                    case 2 -> { p.y = oy + hy; p.x = ox + u * hx; p.z = oz + v * hz; }
                    case 3 -> { p.y = oy - hy; p.x = ox + u * hx; p.z = oz + v * hz; }
                    case 4 -> { p.z = oz + hz; p.x = ox + u * hx; p.y = oy + v * hy; }
                    case 5 -> { p.z = oz - hz; p.x = ox + u * hx; p.y = oy + v * hy; }
                }
            } else {
                p.x = ox + (RANDOM.nextFloat() * 2 - 1) * hx;
                p.y = oy + (RANDOM.nextFloat() * 2 - 1) * hy;
                p.z = oz + (RANDOM.nextFloat() * 2 - 1) * hz;
            }
        } else {
            // 默认：原点
            p.x = p.y = p.z = 0;
        }
    }

    private void applyInitialSpeed(ParticleInstance p) {
        ParticleInitialSpeed speedComp = definition.findComponent(ParticleInitialSpeed.class);
        if (speedComp == null) return;

        float speed = (float) compiled.initialSpeed.evaluate();
        if (speed == 0) return;

        EmitterShape shape = definition.findComponent(EmitterShape.class);
        float dx, dy, dz;

        // 确定方向
        if (shape instanceof EmitterShape.Point point && point.directionMode() == EmitterShape.DirectionMode.CUSTOM) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else if (shape instanceof EmitterShape.Sphere sphere && sphere.directionMode() == EmitterShape.DirectionMode.CUSTOM) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else if (shape instanceof EmitterShape.Box box && box.directionMode() == EmitterShape.DirectionMode.CUSTOM) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else {
            // outwards/inwards — 从形状中心向外/向内
            float ox = (float) compiled.shapeOffset[0].evaluate();
            float oy = (float) compiled.shapeOffset[1].evaluate();
            float oz = (float) compiled.shapeOffset[2].evaluate();
            dx = p.x - ox;
            dy = p.y - oy;
            dz = p.z - oz;
        }

        // inwards 时取反方向
        boolean inwards = false;
        if (shape instanceof EmitterShape.Point point) inwards = point.directionMode() == EmitterShape.DirectionMode.INWARDS;
        else if (shape instanceof EmitterShape.Sphere sphere) inwards = sphere.directionMode() == EmitterShape.DirectionMode.INWARDS;
        else if (shape instanceof EmitterShape.Box box) inwards = box.directionMode() == EmitterShape.DirectionMode.INWARDS;
        if (inwards) {
            dx = -dx;
            dy = -dy;
            dz = -dz;
        }

        // 归一化
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0001f) {
            dx /= len;
            dy /= len;
            dz /= len;
        } else {
            // 随机方向
            float theta = (float) (RANDOM.nextFloat() * Math.PI * 2);
            float phi = (float) (Math.acos(2 * RANDOM.nextFloat() - 1));
            dx = (float) (Math.sin(phi) * Math.cos(theta));
            dy = (float) Math.cos(phi);
            dz = (float) (Math.sin(phi) * Math.sin(theta));
        }

        p.vx = dx * speed;
        p.vy = dy * speed;
        p.vz = dz * speed;
    }

    /**
     * 根据 emitter_local_space 配置，在粒子生成时转换坐标空间。
     * <p>
     * 粒子的位置和速度最初在发射器局部空间中计算。
     * 根据 localPosition / localRotation 决定是否转换到世界空间。
     * 同时从变换矩阵中提取缩放快照存入粒子。
     */
    private void applyLocalSpaceOnSpawn(ParticleInstance p) {
        if (!localPosition) {
            // 从 worldTransform 提取缩放（取第一列长度作为统一缩放）
            float scale = (float) Math.sqrt(
                    worldTransform.m00() * worldTransform.m00() +
                    worldTransform.m01() * worldTransform.m01() +
                    worldTransform.m02() * worldTransform.m02());
            p.spawnScale = scale;

            // 用 worldTransform 将局部位置转换到世界空间（包含缩放）
            Vector4f worldPos = worldTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
            p.x = worldPos.x;
            p.y = worldPos.y;
            p.z = worldPos.z;
            p.worldSpace = true;

            if (!localRotation) {
                // 将局部速度转换到世界空间：旋转方向 + 缩放速度大小
                float c0x = worldTransform.m00(), c0y = worldTransform.m01(), c0z = worldTransform.m02();
                float c1x = worldTransform.m10(), c1y = worldTransform.m11(), c1z = worldTransform.m12();
                float c2x = worldTransform.m20(), c2y = worldTransform.m21(), c2z = worldTransform.m22();
                float s0 = (float) Math.sqrt(c0x * c0x + c0y * c0y + c0z * c0z);
                float s1 = (float) Math.sqrt(c1x * c1x + c1y * c1y + c1z * c1z);
                float s2 = (float) Math.sqrt(c2x * c2x + c2y * c2y + c2z * c2z);
                if (s0 > 1e-6f && s1 > 1e-6f && s2 > 1e-6f) {
                    // 先旋转方向（归一化列向量）
                    float rx = (c0x / s0) * p.vx + (c1x / s1) * p.vy + (c2x / s2) * p.vz;
                    float ry = (c0y / s0) * p.vx + (c1y / s1) * p.vy + (c2y / s2) * p.vz;
                    float rz = (c0z / s0) * p.vx + (c1z / s1) * p.vy + (c2z / s2) * p.vz;
                    // 再乘以缩放
                    p.vx = rx * scale;
                    p.vy = ry * scale;
                    p.vz = rz * scale;
                }
            } else {
                // 速度留在局部空间方向，但大小仍需缩放
                p.vx *= scale;
                p.vy *= scale;
                p.vz *= scale;
            }
        } else {
            // localPosition=true：粒子留在局部空间，从 emitterTransform（locator）提取缩放
            p.spawnScale = (float) Math.sqrt(
                    emitterTransform.m00() * emitterTransform.m00() +
                    emitterTransform.m01() * emitterTransform.m01() +
                    emitterTransform.m02() * emitterTransform.m02());
        }
    }

    private void applyAppearance(ParticleInstance p) {
        if (compiled.particleSizeW != null) {
            p.width = (float) compiled.particleSizeW.evaluate();
            p.height = (float) compiled.particleSizeH.evaluate();
        }

        // Flipbook UV
        if (compiled.flipbookBaseUV != null) {
            int maxFrame = (int) compiled.flipbookMaxFrame.evaluate();
            float frame;
            if (compiled.flipbookStretch) {
                // 拉伸到生命周期
                frame = (p.maxLifetime > 0) ? (p.age / p.maxLifetime) * maxFrame : 0;
            } else {
                frame = p.age * compiled.flipbookFPS;
            }
            int frameIdx = (int) frame;
            if (compiled.flipbookLoop) {
                frameIdx = maxFrame > 0 ? frameIdx % maxFrame : 0;
            } else {
                frameIdx = Math.min(frameIdx, maxFrame - 1);
            }
            frameIdx = Math.max(0, frameIdx);

            float baseU = (float) compiled.flipbookBaseUV[0].evaluate();
            float baseV = (float) compiled.flipbookBaseUV[1].evaluate();
            float sizeU = (float) compiled.flipbookSizeUV[0].evaluate();
            float sizeV = (float) compiled.flipbookSizeUV[1].evaluate();
            float stepU = (float) compiled.flipbookStepUV[0].evaluate();
            float stepV = (float) compiled.flipbookStepUV[1].evaluate();

            float u = baseU + stepU * frameIdx;
            float v = baseV + stepV * frameIdx;
            p.u0 = u / compiled.flipbookTexW;
            p.v0 = v / compiled.flipbookTexH;
            p.u1 = (u + sizeU) / compiled.flipbookTexW;
            p.v1 = (v + sizeV) / compiled.flipbookTexH;
            return;
        }

        // 静态 UV
        ParticleAppearanceBillboard billboard = definition.findComponent(ParticleAppearanceBillboard.class);
        if (billboard != null && billboard.uv() != null) {
            var uv = billboard.uv();
            float u = (float) compiled.uvU.evaluate();
            float v = (float) compiled.uvV.evaluate();
            float w = (float) compiled.uvW.evaluate();
            float h = (float) compiled.uvH.evaluate();
            p.u0 = u / uv.textureWidth();
            p.v0 = v / uv.textureHeight();
            p.u1 = (u + w) / uv.textureWidth();
            p.v1 = (v + h) / uv.textureHeight();
        }
    }

    private void applyTinting(ParticleInstance p) {
        ParticleAppearanceTinting tinting = definition.findComponent(ParticleAppearanceTinting.class);
        if (tinting instanceof ParticleAppearanceTinting.StaticColor color) {
            p.r = (float) compiled.colorR.evaluate();
            p.g = (float) compiled.colorG.evaluate();
            p.b = (float) compiled.colorB.evaluate();
            p.a = compiled.colorA != null ? (float) compiled.colorA.evaluate() : 1f;
        } else if (tinting instanceof ParticleAppearanceTinting.GradientColor gradient) {
            float t = (float) compiled.colorInterpolant.evaluate();
            applyGradientColor(p, gradient.stops(), gradient.colors(), t);
        }
    }

    private void applyGradientColor(ParticleInstance p, float[] stops, float[][] colors, float t) {
        if (colors.length == 0) return;
        if (colors.length == 1) {
            p.r = colors[0][0]; p.g = colors[0][1]; p.b = colors[0][2]; p.a = colors[0][3];
            return;
        }
        // 根据 stops 做非均匀插值
        t = Math.max(stops[0], Math.min(stops[stops.length - 1], t));
        // 找到 t 所在的区间
        int idx = 0;
        for (int i = 0; i < stops.length - 1; i++) {
            if (t >= stops[i]) idx = i;
        }
        if (idx >= colors.length - 1) {
            idx = colors.length - 2;
        }
        float segStart = stops[idx];
        float segEnd = stops[idx + 1];
        float frac = (segEnd > segStart) ? (t - segStart) / (segEnd - segStart) : 0;
        frac = Math.max(0, Math.min(1, frac));
        float[] c0 = colors[idx];
        float[] c1 = colors[idx + 1];
        p.r = c0[0] + (c1[0] - c0[0]) * frac;
        p.g = c0[1] + (c1[1] - c0[1]) * frac;
        p.b = c0[2] + (c1[2] - c0[2]) * frac;
        p.a = c0[3] + (c1[3] - c0[3]) * frac;
    }

    private void updateParticles(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            ParticleInstance p = particles.get(i);

            updateSingleParticle(p, dt);

            if (!p.alive) {
                particles.remove(i);
                recycleParticle(p);
            }
        }
    }

    /**
     * 对单个粒子执行一帧的组件驱动更新。
     * <p>
     * 包括：Molang 绑定、per_render_expression、动态运动（加速度/阻力/旋转）、
     * 外观（尺寸/flipbook UV）、颜色、位置/旋转/age 更新、过期检查。
     * <p>
     * 此方法同时被内部 {@link #updateParticles(float)} 和外部
     * {@link SnowStormParticle} 调用。
     *
     * @param p  要更新的粒子实例
     * @param dt 时间步长（秒）
     */
    public void updateSingleParticle(ParticleInstance p, float dt) {
        ParticleMotion motion = definition.findComponent(ParticleMotion.class);

        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);

        // 执行 per_render_expression（在其他组件求值之前，用于设置 variable.xxx）
        if (compiled.perRenderExpression != null) {
            compiled.perRenderExpression.evaluate();
        }

        if (motion instanceof ParticleMotion.Dynamic dynamic) {
            // 应用加速度
            if (compiled.accelX != null) {
                p.vx += (float) compiled.accelX.evaluate() * dt;
                p.vy += (float) compiled.accelY.evaluate() * dt;
                p.vz += (float) compiled.accelZ.evaluate() * dt;
            }
            // 应用阻力
            if (compiled.dragCoefficient != null) {
                float drag = (float) compiled.dragCoefficient.evaluate();
                float factor = Math.max(0, 1f - drag * dt);
                p.vx *= factor;
                p.vy *= factor;
                p.vz *= factor;
            }
            // 应用旋转加速度
            if (compiled.rotationAcceleration != null) {
                p.rotationRate += (float) compiled.rotationAcceleration.evaluate() * dt;
            }
            // 应用旋转阻力
            if (compiled.rotationDragCoefficient != null) {
                float rotDrag = (float) compiled.rotationDragCoefficient.evaluate();
                float rotFactor = Math.max(0, 1f - rotDrag * dt);
                p.rotationRate *= rotFactor;
            }
        }

        // 更新尺寸（可能随时间变化）
        applyAppearance(p);

        // 更新颜色
        applyTinting(p);

        p.tick(dt);

        // 检查过期条件
        if (compiled.expirationExpr != null) {
            if (compiled.expirationExpr.evaluate() != 0) {
                p.alive = false;
            }
        }
    }

    private void bindEmitterContext() {
        molang.bindEmitter(emitterAge, emitterLifetime, emitterRandom1, emitterRandom2, emitterRandom3, emitterRandom4);
    }

    private ParticleInstance obtainParticle() {
        if (!pool.isEmpty()) {
            return pool.remove(pool.size() - 1);
        }
        return new ParticleInstance();
    }

    private void recycleParticle(ParticleInstance p) {
        if (pool.size() < 200) {
            pool.add(p);
        }
    }

    /**
     * 补偿观察者（摄像机）的位移，使世界空间粒子在世界中保持固定。
     * 每帧 tick 之前由外部调用，传入摄像机本帧的位移量。
     */
    public void applyViewerOffset(float dx, float dy, float dz) {
        for (ParticleInstance p : particles) {
            if (p.worldSpace) {
                p.x -= dx;
                p.y -= dy;
                p.z -= dz;
            }
        }
    }

    public List<ParticleInstance> getParticles() {
        return particles;
    }

    public boolean isFinished() {
        return !active && particles.isEmpty();
    }

    public boolean isActive() {
        return active;
    }

    /**
     * 重启发射器。
     */
    public void restart() {
        emitterAge = 0;
        active = true;
        sleeping = false;
        hasEmittedInstant = false;
        spawnAccumulator = 0;
        particles.clear();
        hasTransform = false;
        emitterTransform.identity();
        worldTransform.identity();
        bindEmitterContext();
        emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
    }

    public ParticleEffectDefinition getDefinition() {
        return definition;
    }

    /**
     * 设置外部粒子管理模式。启用后，<b>所有</b>新生成的粒子不会加入内部列表，
     * 而是通过 {@link ParticleSpawnCallback} 交给外部处理。
     * <p>
     * 用于 {@link com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager}。
     *
     * @param callback 粒子生成回调，传 null 则恢复内部管理
     */
    public void setExternalParticleManagement(@Nullable ParticleSpawnCallback callback) {
        this.spawnCallback = callback;
        this.externalParticleManagement = callback != null;
    }

    /**
     * 设置世界空间粒子分流回调。仅 {@code worldSpace=true} 的粒子走回调，
     * 局部空间粒子仍留在内部列表由第一人称管线管理。
     * <p>
     * 用于第一人称管线将世界空间粒子投递到原版 ParticleEngine。
     *
     * @param callback 世界空间粒子回调，传 null 则取消分流
     */
    public void setWorldSpaceParticleCallback(@Nullable ParticleSpawnCallback callback) {
        this.spawnCallback = callback;
        this.externalParticleManagement = false;
    }

    public ParticleMolangEnvironment getMolang() {
        return molang;
    }

    /**
     * 预编译的 Molang 表达式缓存。
     */
    private static class CompiledExpressions {
        // Emitter
        final MochaFunction emitterActiveTime;
        @Nullable final MochaFunction emitterSleepTime;
        @Nullable final MochaFunction emitterAmount;
        @Nullable final MochaFunction emitterSpawnRate;
        @Nullable final MochaFunction emitterMaxParticles;

        // Shape
        final MochaFunction[] shapeOffset;
        @Nullable final MochaFunction[] shapeDirection;
        @Nullable final MochaFunction shapeRadius;
        @Nullable final MochaFunction[] shapeHalfDimensions;

        // Particle
        @Nullable final MochaFunction initialSpeed;
        @Nullable final MochaFunction particleMaxLifetime;
        @Nullable final MochaFunction expirationExpr;
        @Nullable final MochaFunction particleSizeW;
        @Nullable final MochaFunction particleSizeH;

        // UV
        @Nullable final MochaFunction uvU;
        @Nullable final MochaFunction uvV;
        @Nullable final MochaFunction uvW;
        @Nullable final MochaFunction uvH;

        // Flipbook
        @Nullable final MochaFunction[] flipbookBaseUV;
        @Nullable final MochaFunction[] flipbookSizeUV;
        @Nullable final MochaFunction[] flipbookStepUV;
        @Nullable final MochaFunction flipbookMaxFrame;
        float flipbookFPS;
        boolean flipbookStretch;
        boolean flipbookLoop;
        int flipbookTexW;
        int flipbookTexH;

        // Color
        @Nullable final MochaFunction colorR;
        @Nullable final MochaFunction colorG;
        @Nullable final MochaFunction colorB;
        @Nullable final MochaFunction colorA;
        @Nullable final MochaFunction colorInterpolant;

        // Motion
        @Nullable final MochaFunction accelX;
        @Nullable final MochaFunction accelY;
        @Nullable final MochaFunction accelZ;
        @Nullable final MochaFunction dragCoefficient;
        @Nullable final MochaFunction rotationAcceleration;
        @Nullable final MochaFunction rotationDragCoefficient;

        // Initial Spin
        @Nullable final MochaFunction initialRotation;
        @Nullable final MochaFunction initialRotationRate;

        // Initialization
        @Nullable final MochaFunction perRenderExpression;

        CompiledExpressions(ParticleEffectDefinition def, ParticleMolangEnvironment molang) {
            // Emitter Lifetime
            EmitterLifetime lifetime = def.findComponent(EmitterLifetime.class);
            if (lifetime instanceof EmitterLifetime.Looping looping) {
                emitterActiveTime = molang.compile(looping.activeTime());
                emitterSleepTime = molang.compile(looping.sleepTime());
            } else if (lifetime instanceof EmitterLifetime.Once once) {
                emitterActiveTime = molang.compile(once.activeTime());
                emitterSleepTime = null;
            } else {
                emitterActiveTime = () -> Float.MAX_VALUE;
                emitterSleepTime = null;
            }

            // Emitter Rate
            EmitterRate rate = def.findComponent(EmitterRate.class);
            if (rate instanceof EmitterRate.Instant instant) {
                emitterAmount = molang.compile(instant.amount());
                emitterSpawnRate = null;
                emitterMaxParticles = null;
            } else if (rate instanceof EmitterRate.Steady steady) {
                emitterAmount = null;
                emitterSpawnRate = molang.compile(steady.spawnRate());
                emitterMaxParticles = molang.compile(steady.maxParticles());
            } else {
                emitterAmount = null;
                emitterSpawnRate = null;
                emitterMaxParticles = null;
            }

            // Shape
            EmitterShape shape = def.findComponent(EmitterShape.class);
            if (shape instanceof EmitterShape.Point point) {
                shapeOffset = compileArray3(molang, point.offset());
                shapeDirection = point.direction() != null ? compileArray3(molang, point.direction()) : null;
                shapeRadius = null;
                shapeHalfDimensions = null;
            } else if (shape instanceof EmitterShape.Sphere sphere) {
                shapeOffset = compileArray3(molang, sphere.offset());
                shapeDirection = sphere.direction() != null ? compileArray3(molang, sphere.direction()) : null;
                shapeRadius = molang.compile(sphere.radius());
                shapeHalfDimensions = null;
            } else if (shape instanceof EmitterShape.Box box) {
                shapeOffset = compileArray3(molang, box.offset());
                shapeDirection = box.direction() != null ? compileArray3(molang, box.direction()) : null;
                shapeRadius = null;
                shapeHalfDimensions = compileArray3(molang, box.halfDimensions());
            } else {
                shapeOffset = new MochaFunction[]{() -> 0, () -> 0, () -> 0};
                shapeDirection = null;
                shapeRadius = null;
                shapeHalfDimensions = null;
            }

            // Initial Speed
            ParticleInitialSpeed speedComp = def.findComponent(ParticleInitialSpeed.class);
            initialSpeed = speedComp != null ? molang.compile(speedComp.speed()) : null;

            // Particle Lifetime
            ParticleLifetimeExpression lifetimeExpr = def.findComponent(ParticleLifetimeExpression.class);
            if (lifetimeExpr != null) {
                particleMaxLifetime = molang.compile(lifetimeExpr.maxLifetime());
                expirationExpr = lifetimeExpr.expirationExpression() != null ? molang.compile(lifetimeExpr.expirationExpression()) : null;
            } else {
                particleMaxLifetime = () -> 1;
                expirationExpr = null;
            }

            // Appearance Billboard
            ParticleAppearanceBillboard billboard = def.findComponent(ParticleAppearanceBillboard.class);
            if (billboard != null) {
                particleSizeW = molang.compile(billboard.size()[0]);
                particleSizeH = molang.compile(billboard.size()[1]);
                if (billboard.uv() != null) {
                    var uv = billboard.uv();
                    uvU = molang.compile(uv.u());
                    uvV = molang.compile(uv.v());
                    uvW = molang.compile(uv.width());
                    uvH = molang.compile(uv.height());
                } else {
                    uvU = uvV = uvW = uvH = null;
                }
                if (billboard.flipbook() != null) {
                    var fb = billboard.flipbook();
                    flipbookBaseUV = new MochaFunction[]{molang.compile(fb.baseUV()[0]), molang.compile(fb.baseUV()[1])};
                    flipbookSizeUV = new MochaFunction[]{molang.compile(fb.sizeUV()[0]), molang.compile(fb.sizeUV()[1])};
                    flipbookStepUV = new MochaFunction[]{molang.compile(fb.stepUV()[0]), molang.compile(fb.stepUV()[1])};
                    flipbookMaxFrame = molang.compile(fb.maxFrame());
                    flipbookFPS = fb.framesPerSecond();
                    flipbookStretch = fb.stretchToLifetime();
                    flipbookLoop = fb.loop();
                    flipbookTexW = fb.textureWidth();
                    flipbookTexH = fb.textureHeight();
                } else {
                    flipbookBaseUV = flipbookSizeUV = flipbookStepUV = null;
                    flipbookMaxFrame = null;
                }
            } else {
                particleSizeW = particleSizeH = null;
                uvU = uvV = uvW = uvH = null;
                flipbookBaseUV = flipbookSizeUV = flipbookStepUV = null;
                flipbookMaxFrame = null;
            }

            // Tinting
            ParticleAppearanceTinting tinting = def.findComponent(ParticleAppearanceTinting.class);
            if (tinting instanceof ParticleAppearanceTinting.StaticColor color) {
                colorR = molang.compile(color.r());
                colorG = molang.compile(color.g());
                colorB = molang.compile(color.b());
                colorA = color.a() != null ? molang.compile(color.a()) : null;
                colorInterpolant = null;
            } else if (tinting instanceof ParticleAppearanceTinting.GradientColor gradient) {
                colorR = colorG = colorB = colorA = null;
                colorInterpolant = molang.compile(gradient.interpolant());
            } else {
                colorR = colorG = colorB = colorA = colorInterpolant = null;
            }

            // Motion
            ParticleMotion motion = def.findComponent(ParticleMotion.class);
            if (motion instanceof ParticleMotion.Dynamic dynamic) {
                if (dynamic.linearAcceleration() != null) {
                    MochaFunction[] accel = compileArray3(molang, dynamic.linearAcceleration());
                    accelX = accel[0];
                    accelY = accel[1];
                    accelZ = accel[2];
                } else {
                    accelX = accelY = accelZ = null;
                }
                dragCoefficient = dynamic.linearDragCoefficient() != null ? molang.compile(dynamic.linearDragCoefficient()) : null;
                rotationAcceleration = dynamic.rotationAcceleration() != null ? molang.compile(dynamic.rotationAcceleration()) : null;
                rotationDragCoefficient = dynamic.rotationDragCoefficient() != null ? molang.compile(dynamic.rotationDragCoefficient()) : null;
            } else {
                accelX = accelY = accelZ = null;
                dragCoefficient = null;
                rotationAcceleration = null;
                rotationDragCoefficient = null;
            }

            // Initial Spin
            ParticleInitialSpin spin = def.findComponent(ParticleInitialSpin.class);
            if (spin != null) {
                initialRotation = molang.compile(spin.rotation());
                initialRotationRate = molang.compile(spin.rotationRate());
            } else {
                initialRotation = null;
                initialRotationRate = null;
            }

            // Initialization
            ParticleInitialization init = def.findComponent(ParticleInitialization.class);
            if (init != null && init.perRenderExpression() != null && !init.perRenderExpression().isEmpty()) {
                perRenderExpression = molang.compile(init.perRenderExpression());
            } else {
                perRenderExpression = null;
            }
        }

        private static MochaFunction[] compileArray3(ParticleMolangEnvironment molang, String[] exprs) {
            return new MochaFunction[]{
                    molang.compile(exprs[0]),
                    molang.compile(exprs[1]),
                    molang.compile(exprs[2])
            };
        }
    }
}
