package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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

    // 第一人称脱离模式（sbm:fp_emitter_local_space）
    private boolean fpMode = false;
    private boolean fpLocalPosition = false;
    private boolean fpLocalRotation = false;
    private boolean fpLocalVelocity = false;
    private boolean fpToWorld = false;

    // 缓存的组件引用（构造时一次性查找，避免热路径中重复查找）
    @Nullable private final EmitterLifetime lifetimeComponent;
    @Nullable private final EmitterRate rateComponent;
    @Nullable private final EmitterShape shapeComponent;
    @Nullable private final ParticleInitialSpeed initialSpeedComponent;
    @Nullable private final ParticleLifetimeExpression lifetimeExprComponent;
    @Nullable private final ParticleAppearanceBillboard billboardComponent;
    @Nullable private final ParticleAppearanceTinting tintingComponent;
    @Nullable private final ParticleMotion motionComponent;

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
        this.compiled = molang.getOrCompile(definition);

        // 一次性缓存所有组件引用
        this.lifetimeComponent = definition.findComponent(EmitterLifetime.class);
        this.rateComponent = definition.findComponent(EmitterRate.class);
        this.shapeComponent = definition.findComponent(EmitterShape.class);
        this.initialSpeedComponent = definition.findComponent(ParticleInitialSpeed.class);
        this.lifetimeExprComponent = definition.findComponent(ParticleLifetimeExpression.class);
        this.billboardComponent = definition.findComponent(ParticleAppearanceBillboard.class);
        this.tintingComponent = definition.findComponent(ParticleAppearanceTinting.class);
        this.motionComponent = definition.findComponent(ParticleMotion.class);

        // 读取 emitter_local_space 配置
        EmitterLocalSpace localSpace = definition.findComponent(EmitterLocalSpace.class);
        if (localSpace != null) {
            this.localPosition = localSpace.position();
            this.localRotation = localSpace.position() && localSpace.rotation();
            this.localVelocity = localSpace.velocity();
        } else {
            this.localPosition = false;
            this.localRotation = false;
            this.localVelocity = false;
        }

        // 计算发射器生命周期
        bindEmitterContext();
        if (lifetimeComponent != null) {
            this.emitterLifetime = (float) compiled.emitterActiveTime().evaluate(ctx());
        } else {
            this.emitterLifetime = Float.MAX_VALUE;
        }
    }

    /**
     * 获取当前粒子环境的 MolangContext。
     */
    private MolangContext<?> ctx() {
        return molang.getContext();
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

    public boolean isLocalVelocity() {
        return localVelocity;
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
                emitterLifetime = (float) compiled.emitterActiveTime().evaluate(ctx());
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
        if (lifetimeComponent instanceof EmitterLifetime.Looping looping) {
            // 进入休眠
            sleeping = true;
            bindEmitterContext();
            sleepTimer = (float) compiled.emitterSleepTime().evaluate(ctx());
            if (sleepTimer <= 0) {
                // 无休眠，直接重启
                sleeping = false;
                emitterAge = 0;
                hasEmittedInstant = false;
                spawnAccumulator = 0;
                bindEmitterContext();
                emitterLifetime = (float) compiled.emitterActiveTime().evaluate(ctx());
            }
        } else {
            // Once 模式，停止发射（但已有粒子继续存活）
            active = false;
        }
    }

    private void emitParticles(float dt) {
        if (compiled.rate() instanceof CompiledExpressions.RateCompiled.Instant instant) {
            if (!hasEmittedInstant) {
                hasEmittedInstant = true;
                int count = (int) instant.amount().evaluate(ctx());
                for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
                    spawnParticle();
                }
            }
        } else if (compiled.rate() instanceof CompiledExpressions.RateCompiled.Steady steady) {
            int maxP = (int) steady.maxParticles().evaluate(ctx());
            float spawnRate = (float) steady.spawnRate().evaluate(ctx());
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
        if (lifetimeExprComponent != null) {
            p.maxLifetime = (float) compiled.particleMaxLifetime().evaluate(ctx());
        }

        // 初始尺寸
        applyAppearance(p);

        // 初始颜色
        applyTinting(p);

        // 初始自旋
        if (compiled.spin() != null) {
            p.rotation = (float) compiled.spin().rotation().evaluate(ctx());
            p.rotationRate = (float) compiled.spin().rotationRate().evaluate(ctx());
        }

        // 根据 emitter_local_space 配置转换坐标空间
        if (hasTransform) {
            applyLocalSpaceOnSpawn(p);
        }

        if (spawnCallback != null && (externalParticleManagement || p.worldSpace)) {
            spawnCallback.onParticleSpawned(p);
        } else {
            particles.add(p);
        }
    }

    private void applyShape(ParticleInstance p) {
        if (shapeComponent instanceof EmitterShape.Point point) {
            p.x = (float) compiled.shape().offset()[0].evaluate(ctx());
            p.y = (float) compiled.shape().offset()[1].evaluate(ctx());
            p.z = (float) compiled.shape().offset()[2].evaluate(ctx());
        } else if (shapeComponent instanceof EmitterShape.Sphere sphere) {
            float r = (float) compiled.shape().radius().evaluate(ctx());
            float ox = (float) compiled.shape().offset()[0].evaluate(ctx());
            float oy = (float) compiled.shape().offset()[1].evaluate(ctx());
            float oz = (float) compiled.shape().offset()[2].evaluate(ctx());

            // 随机方向
            float theta = (float) (RANDOM.nextFloat() * Math.PI * 2);
            float phi = (float) (Math.acos(2 * RANDOM.nextFloat() - 1));
            float dist = sphere.surfaceOnly() ? r : r * (float) Math.cbrt(RANDOM.nextFloat());

            p.x = ox + dist * (float) (Math.sin(phi) * Math.cos(theta));
            p.y = oy + dist * (float) Math.cos(phi);
            p.z = oz + dist * (float) (Math.sin(phi) * Math.sin(theta));
        } else if (shapeComponent instanceof EmitterShape.Box box) {
            float ox = (float) compiled.shape().offset()[0].evaluate(ctx());
            float oy = (float) compiled.shape().offset()[1].evaluate(ctx());
            float oz = (float) compiled.shape().offset()[2].evaluate(ctx());
            float hx = (float) compiled.shape().halfDimensions()[0].evaluate(ctx());
            float hy = (float) compiled.shape().halfDimensions()[1].evaluate(ctx());
            float hz = (float) compiled.shape().halfDimensions()[2].evaluate(ctx());

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
        if (initialSpeedComponent == null || shapeComponent == null) return;

        float speed = (float) compiled.initialSpeed().evaluate(ctx());
        if (speed == 0) return;

        float dx, dy, dz;

        // 确定方向
        EmitterShape.DirectionMode dirMode = shapeComponent.directionMode();
        if (dirMode == EmitterShape.DirectionMode.CUSTOM) {
            dx = (float) compiled.shape().direction()[0].evaluate(ctx());
            dy = (float) compiled.shape().direction()[1].evaluate(ctx());
            dz = (float) compiled.shape().direction()[2].evaluate(ctx());
        } else {
            // outwards/inwards — 从形状中心向外/向内
            float ox = (float) compiled.shape().offset()[0].evaluate(ctx());
            float oy = (float) compiled.shape().offset()[1].evaluate(ctx());
            float oz = (float) compiled.shape().offset()[2].evaluate(ctx());
            dx = p.x - ox;
            dy = p.y - oy;
            dz = p.z - oz;
        }

        // inwards 时取反方向
        if (dirMode == EmitterShape.DirectionMode.INWARDS) {
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
     * <p>
     * 当 FP 模式启用且 fpLocalPosition=true 时，粒子在发射时使用定位器位置
     * （通过 worldTransform 转换到摄像机空间），但标记为 fpDetached 而非 worldSpace，
     * 使其留在内部列表由第一人称管线管理。
     */
    private void applyLocalSpaceOnSpawn(ParticleInstance p) {
        // 确定实际使用的 local space 配置
        boolean effectiveLocalPos = fpMode ? fpLocalPosition : localPosition;
        boolean effectiveLocalRot = fpMode ? fpLocalRotation : localRotation;

        if (!effectiveLocalPos) {
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

            if (!effectiveLocalRot) {
                transformVelocityToWorldSpace(p, scale);
            } else {
                // 速度留在局部空间方向，但大小仍需缩放
                p.vx *= scale;
                p.vy *= scale;
                p.vz *= scale;
            }
        } else if (fpMode) {
            // FP 模式下 position=true：发射时使用定位器位置，但发射后脱离定位器。
            // toWorld 控制脱离后的粒子去向。
            if (fpToWorld) {
                // 投放到世界：用 worldTransform 转换到摄像机空间，标记 worldSpace
                float scale = (float) Math.sqrt(
                        worldTransform.m00() * worldTransform.m00() +
                        worldTransform.m01() * worldTransform.m01() +
                        worldTransform.m02() * worldTransform.m02());
                p.spawnScale = scale;

                Vector4f worldPos = worldTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
                p.x = worldPos.x;
                p.y = worldPos.y;
                p.z = worldPos.z;
                p.worldSpace = true;

                if (!effectiveLocalRot) {
                    transformVelocityToWorldSpace(p, scale);
                } else {
                    p.vx *= scale;
                    p.vy *= scale;
                    p.vz *= scale;
                }
            } else {
                // 留在第一人称管线内部：用 emitterTransform 转换到模型空间，
                // 粒子跟随摄像机但不再跟随定位器
                float scale = (float) Math.sqrt(
                        emitterTransform.m00() * emitterTransform.m00() +
                        emitterTransform.m01() * emitterTransform.m01() +
                        emitterTransform.m02() * emitterTransform.m02());
                p.spawnScale = scale;

                Vector4f modelPos = emitterTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
                p.x = modelPos.x;
                p.y = modelPos.y;
                p.z = modelPos.z;
                p.fpDetached = true;

                if (!effectiveLocalRot) {
                    // 用 emitterTransform 转换速度方向到模型空间
                    transformVelocityByMatrix(p, emitterTransform, scale);
                } else {
                    p.vx *= scale;
                    p.vy *= scale;
                    p.vz *= scale;
                }
            }
        } else {
            // localPosition=true：粒子留在局部空间，从 emitterTransform（locator）提取缩放
            p.spawnScale = (float) Math.sqrt(
                    emitterTransform.m00() * emitterTransform.m00() +
                    emitterTransform.m01() * emitterTransform.m01() +
                    emitterTransform.m02() * emitterTransform.m02());
        }
    }

    /**
     * 将粒子的局部速度转换到世界空间：旋转方向 + 缩放速度大小。
     */
    private void transformVelocityToWorldSpace(ParticleInstance p, float scale) {
        transformVelocityByMatrix(p, worldTransform, scale);
    }

    /**
     * 用指定矩阵的旋转部分变换粒子速度方向，并乘以缩放。
     */
    private static void transformVelocityByMatrix(ParticleInstance p, Matrix4f mat, float scale) {
        float c0x = mat.m00(), c0y = mat.m01(), c0z = mat.m02();
        float c1x = mat.m10(), c1y = mat.m11(), c1z = mat.m12();
        float c2x = mat.m20(), c2y = mat.m21(), c2z = mat.m22();
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
    }

    private void applyAppearance(ParticleInstance p) {
        var app = compiled.appearance();
        if (app != null) {
            p.width = (float) app.sizeW().evaluate(ctx());
            p.height = (float) app.sizeH().evaluate(ctx());

            if (app.uv() instanceof CompiledExpressions.UVCompiled.Flipbook fb) {
                int maxFrame = (int) fb.maxFrame().evaluate(ctx());
                float frame;
                if (fb.stretch()) {
                    // 拉伸到生命周期
                    frame = (p.maxLifetime > 0) ? (p.age / p.maxLifetime) * maxFrame : 0;
                } else {
                    frame = p.age * fb.fps();
                }
                int frameIdx = (int) frame;
                if (fb.loop()) {
                    frameIdx = maxFrame > 0 ? frameIdx % maxFrame : 0;
                } else {
                    frameIdx = Math.min(frameIdx, maxFrame - 1);
                }
                frameIdx = Math.max(0, frameIdx);

                float baseU = (float) fb.baseUV()[0].evaluate(ctx());
                float baseV = (float) fb.baseUV()[1].evaluate(ctx());
                float sizeU = (float) fb.sizeUV()[0].evaluate(ctx());
                float sizeV = (float) fb.sizeUV()[1].evaluate(ctx());
                float stepU = (float) fb.stepUV()[0].evaluate(ctx());
                float stepV = (float) fb.stepUV()[1].evaluate(ctx());

                float u = baseU + stepU * frameIdx;
                float v = baseV + stepV * frameIdx;
                p.u0 = u / fb.texW();
                p.v0 = v / fb.texH();
                p.u1 = (u + sizeU) / fb.texW();
                p.v1 = (v + sizeV) / fb.texH();
            } else if (app.uv() instanceof CompiledExpressions.UVCompiled.Static suv) {
                float u = (float) suv.u().evaluate(ctx());
                float v = (float) suv.v().evaluate(ctx());
                float w = (float) suv.w().evaluate(ctx());
                float h = (float) suv.h().evaluate(ctx());
                p.u0 = u / suv.texW();
                p.v0 = v / suv.texH();
                p.u1 = (u + w) / suv.texW();
                p.v1 = (v + h) / suv.texH();
            }
        }
    }

    private void applyTinting(ParticleInstance p) {
        if (compiled.color() instanceof CompiledExpressions.ColorCompiled.Static sc) {
            p.r = (float) sc.r().evaluate(ctx());
            p.g = (float) sc.g().evaluate(ctx());
            p.b = (float) sc.b().evaluate(ctx());
            p.a = sc.a() != null ? (float) sc.a().evaluate(ctx()) : 1f;
        } else if (compiled.color() instanceof CompiledExpressions.ColorCompiled.Gradient gc) {
            float t = (float) gc.interpolant().evaluate(ctx());
            if (tintingComponent instanceof ParticleAppearanceTinting.GradientColor gradient) {
                applyGradientColor(p, gradient.stops(), gradient.colors(), t);
            }
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
        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);

        // 执行 per_render_expression（在其他组件求值之前，用于设置 variable.xxx）
        if (compiled.perRenderExpression() != null) {
            compiled.perRenderExpression().evaluate(ctx());
        }

        var mot = compiled.motion();
        if (mot != null) {
            // 应用加速度
            if (mot.accelX() != null) {
                p.vx += (float) mot.accelX().evaluate(ctx()) * dt;
                p.vy += (float) mot.accelY().evaluate(ctx()) * dt;
                p.vz += (float) mot.accelZ().evaluate(ctx()) * dt;
            }
            // 应用阻力
            if (mot.dragCoefficient() != null) {
                float drag = (float) mot.dragCoefficient().evaluate(ctx());
                float factor = Math.max(0, 1f - drag * dt);
                p.vx *= factor;
                p.vy *= factor;
                p.vz *= factor;
            }
            // 应用旋转加速度
            if (mot.rotationAcceleration() != null) {
                p.rotationRate += (float) mot.rotationAcceleration().evaluate(ctx()) * dt;
            }
            // 应用旋转阻力
            if (mot.rotationDragCoefficient() != null) {
                float rotDrag = (float) mot.rotationDragCoefficient().evaluate(ctx());
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
        if (compiled.expirationExpr() != null) {
            if (compiled.expirationExpr().evaluate(ctx()) != 0) {
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
        emitterLifetime = (float) compiled.emitterActiveTime().evaluate(ctx());
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

    /**
     * 启用第一人称脱离模式。检查定义中是否有 {@link FPEmitterLocalSpace} 组件，
     * 有则读取其配置，在粒子生成时覆盖原版 {@link EmitterLocalSpace} 的行为。
     * <p>
     * 当 FP 模式下 {@code position=true} 时，粒子在发射时使用定位器位置
     * （通过 worldTransform 转换到摄像机空间），但发射后脱离定位器。
     * {@code toWorld} 控制脱离后的粒子是投放到世界还是留在第一人称管线内部。
     */
    public void enableFPMode() {
        FPEmitterLocalSpace fpSpace = definition.findComponent(FPEmitterLocalSpace.class);
        if (fpSpace != null) {
            this.fpMode = true;
            this.fpLocalPosition = fpSpace.position();
            // position=false, rotation=true 是无效组合，按 rotation=false 处理
            this.fpLocalRotation = fpSpace.position() && fpSpace.rotation();
            this.fpLocalVelocity = fpSpace.velocity();
            this.fpToWorld = fpSpace.toWorld();
        }
    }

    public boolean isFPMode() {
        return fpMode;
    }

    public ParticleMolangEnvironment getMolang() {
        return molang;
    }

}
