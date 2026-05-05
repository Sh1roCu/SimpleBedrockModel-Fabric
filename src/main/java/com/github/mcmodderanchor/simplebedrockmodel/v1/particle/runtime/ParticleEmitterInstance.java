package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.value.NumberValue;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.lifetime.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.motion.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.rate.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.tinting.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.curve.ParticleCurve;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event.IEventNode;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ParticleEmitterInstance {
    private static final int MAX_PARTICLES = 1000;
    private static final Random RANDOM = new Random();

    private final ParticleEffectDefinition definition;
    private final ParticleMolangEnvironment molang;

    // 发射器状态
    private float emitterAge;
    private float emitterLifetime;
    private boolean active = true;
    private boolean removed;
    private boolean sleeping;
    private float sleepTimer;
    private boolean hasEmittedInstant;
    private float spawnAccumulator;
    private float currentDt;

    private final int emitterRandom1 = RANDOM.nextInt();
    private final int emitterRandom2 = RANDOM.nextInt();
    private final int emitterRandom3 = RANDOM.nextInt();
    private final int emitterRandom4 = RANDOM.nextInt();

    private final List<ParticleInstance> particles = new ArrayList<>();
    private final List<ParticleInstance> pool = new ArrayList<>();

    private final Vector4f tempSpawnVec = new Vector4f();

    private final Matrix4f emitterTransform = new Matrix4f();
    private final Matrix4f worldTransform = new Matrix4f();
    private boolean hasTransform = false;

    private final boolean localPosition;
    private final boolean localRotation;
    private final boolean localVelocity;

    private boolean fpMode = false;
    private boolean fpLocalPosition = false;
    private boolean fpLocalRotation = false;
    private boolean fpLocalVelocity = false;
    private boolean fpToWorld = false;

    @Nullable
    private final IEmitterComponentDefinition lifetimeComponent;
    @Nullable
    private final IEmitterComponentDefinition rateComponent;
    @Nullable
    private final EmitterShape shapeComponent;
    @Nullable
    private final ParticleInitialSpeed initialSpeedComponent;
    @Nullable
    private final ParticleLifetimeExpression lifetimeExprComponent;
    @Nullable
    private final ParticleAppearanceBillboard billboardComponent;
    @Nullable
    private final IParticleComponentDefinition tintingComponent;
    @Nullable
    private final IParticleComponentDefinition motionComponent;
    @Nullable
    private final ParticleInitialSpin spinComponent;
    @Nullable
    private final ParticleInitialization initComponent;
    @Nullable
    private final EmitterInitialization emitterInitComponent;

    // 事件相关组件
    @Nullable
    private final EmitterLifetimeEvents lifetimeEventsComponent;
    @Nullable
    private final ParticleLifetimeEvents particleLifetimeEventsComponent;
    @Nullable
    private final ParticleLifetimeKillPlane killPlaneComponent;

    private final Map<String, ParticleCurve> curves;

    // 组件更新跳过标志：在构造时根据 definition 静态分析确定
    private final boolean needsPerFrameAppearance;
    private final boolean needsPerFrameTinting;

    // 事件系统状态
    private int lastTimelineIndex;
    private int lastTravelDistIndex;
    private float[] loopingTravelDistAccum;
    private float travelDistance;
    private float prevEmitterX, prevEmitterY, prevEmitterZ;
    private boolean hasPrevPosition;

    @Nullable
    private EventExecutor.EventContext eventContext;

    @Nullable
    private ParticleSpawnCallback spawnCallback;
    private boolean externalParticleManagement;

    @FunctionalInterface
    public interface ParticleSpawnCallback {
        void onParticleSpawned(ParticleInstance particle);
    }

    public ParticleEmitterInstance(ParticleEffectDefinition definition, ParticleMolangEnvironment molang) {
        this.definition = definition;
        this.molang = molang;

        this.lifetimeComponent = findFirst(definition,
                EmitterLifetimeLooping.class, EmitterLifetimeOnce.class, EmitterLifetimeExpression.class);
        this.rateComponent = findFirst(definition,
                EmitterRateInstant.class, EmitterRateSteady.class, EmitterRateManual.class);
        this.shapeComponent = definition.findComponent(EmitterShape.class);
        this.initialSpeedComponent = definition.findComponent(ParticleInitialSpeed.class);
        this.lifetimeExprComponent = definition.findComponent(ParticleLifetimeExpression.class);
        this.billboardComponent = definition.findComponent(ParticleAppearanceBillboard.class);
        this.tintingComponent = findFirst(definition,
                ParticleTintingStatic.class, ParticleTintingGradient.class);
        this.motionComponent = findFirst(definition,
                ParticleMotionDynamic.class, ParticleMotionParametric.class);
        this.spinComponent = definition.findComponent(ParticleInitialSpin.class);
        this.initComponent = definition.findComponent(ParticleInitialization.class);
        this.emitterInitComponent = definition.findComponent(EmitterInitialization.class);
        this.lifetimeEventsComponent = definition.findComponent(EmitterLifetimeEvents.class);
        this.particleLifetimeEventsComponent = definition.findComponent(ParticleLifetimeEvents.class);
        this.killPlaneComponent = definition.findComponent(ParticleLifetimeKillPlane.class);
        this.curves = definition.getCurves();

        // 初始化 looping travel distance 累计数组
        if (lifetimeEventsComponent != null && !lifetimeEventsComponent.loopingTravelDistanceEvents().isEmpty()) {
            this.loopingTravelDistAccum = new float[lifetimeEventsComponent.loopingTravelDistanceEvents().size()];
        } else {
            this.loopingTravelDistAccum = new float[0];
        }

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

        // 分析组件是否需要每帧更新
        this.needsPerFrameAppearance = computeNeedsPerFrameAppearance();
        this.needsPerFrameTinting = computeNeedsPerFrameTinting();

        startEmitterCycle();
    }

    private MolangContext<?> ctx() {
        return molang.getContext();
    }

    @SafeVarargs
    @Nullable
    private static <T extends IComponent> T findFirst(ParticleEffectDefinition def, Class<? extends T>... types) {
        for (Class<? extends T> type : types) {
            T c = def.findComponent(type);
            if (c != null) return c;
        }
        return null;
    }

    /**
     * 判断 applyAppearance 是否需要每帧调用。
     * <p>
     * 需要每帧更新的情况：有 flipbook（UV 随 age 变化）或 size 表达式是动态的。
     * 不需要的情况：size 是常量且无 flipbook，spawn 时设置一次即可。
     */
    private boolean computeNeedsPerFrameAppearance() {
        if (billboardComponent == null) return false;
        // flipbook 的帧索引依赖 particle age，必须每帧更新
        if (billboardComponent.flipbook() != null) return true;
        // size 表达式引用了变量（如 variable.curve_size），必须每帧更新
        return billboardComponent.dynamicSize();
    }

    /**
     * 判断 applyTinting 是否需要每帧调用。
     * <p>
     * GradientColor 的 interpolant 几乎总是依赖 particle_age，必须每帧更新。
     * StaticColor 的 RGBA 在 spawn 时设置一次即可。
     */
    private boolean computeNeedsPerFrameTinting() {
        return tintingComponent instanceof ParticleTintingGradient;
    }

    public void setEmitterTransform(Matrix4f locatorTransform, Matrix4f worldTransformIn) {
        emitterTransform.set(locatorTransform);
        worldTransform.set(worldTransformIn);
        hasTransform = true;
    }

    public Matrix4f getEmitterTransform() {
        return emitterTransform;
    }

    public Matrix4f getWorldTransform() {
        return worldTransform;
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

    public void tick(float dt) {
        if (removed) {
            updateParticles(dt);
            return;
        }

        if (sleeping) {
            sleepTimer -= dt;
            if (sleepTimer <= 0) {
                startEmitterCycle();
            }
            updateParticles(dt);
            return;
        }

        this.currentDt = dt;

        if (lifetimeComponent instanceof EmitterLifetimeExpression expression) {
            tickExpressionLifetime(expression, dt);
            return;
        }

        tickTimedLifetime(dt);
    }

    private void tickTimedLifetime(float dt) {
        emitterAge += dt;
        bindEmitterContext();
        runEmitterPerUpdateExpressions();
        updateEmitterLifetimeEvents();

        if (emitterAge >= emitterLifetime) {
            handleLifetimeEnd();
        } else if (active) {
            emitParticles(dt);
        }
        updateParticles(dt);
    }

    private void tickExpressionLifetime(EmitterLifetimeExpression expression, float dt) {
        emitterAge += dt;
        emitterLifetime = emitterAge;
        bindEmitterContext();
        runEmitterPerUpdateExpressions();
        updateEmitterLifetimeEvents();

        if (expression.expirationExpression().evaluate(ctx()) != 0) {
            fireEmitterExpirationEvents();
            removed = true;
            active = false;
            updateParticles(dt);
            return;
        }

        active = expression.activationExpression().evaluate(ctx()) != 0;
        emitterLifetime = emitterAge;
        bindEmitterContext();

        if (active) {
            emitParticles(dt);
        }
        updateParticles(dt);
    }

    private void handleLifetimeEnd() {
        if (lifetimeComponent instanceof EmitterLifetimeLooping looping) {
            fireEmitterExpirationEvents();
            sleeping = true;
            active = false;
            bindEmitterContext();
            sleepTimer = (float) looping.sleepTime().evaluate(ctx());
            if (sleepTimer <= 0) {
                startEmitterCycle();
            }
        } else {
            fireEmitterExpirationEvents();
            removed = true;
            active = false;
        }
    }

    private void emitParticles(float dt) {
        if (rateComponent instanceof EmitterRateInstant instant) {
            if (!hasEmittedInstant) {
                hasEmittedInstant = true;
                int count = (int) instant.amount().evaluate(ctx());
                for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
                    spawnParticleInternal();
                }
            }
        } else if (rateComponent instanceof EmitterRateSteady steady) {
            int maxP = (int) steady.maxParticles().evaluate(ctx());
            float spawnRate = (float) steady.spawnRate().evaluate(ctx());
            spawnAccumulator += spawnRate * dt;
            while (spawnAccumulator >= 1f && particles.size() < maxP && particles.size() < MAX_PARTICLES) {
                spawnAccumulator -= 1f;
                spawnParticleInternal();
            }
        } else if (rateComponent instanceof EmitterRateManual) {
            // 手动发射模式不自动生成粒子，由 emitManual(int) 触发。
        }
    }

    public int emitManual(int count) {
        if (!(rateComponent instanceof EmitterRateManual manual)) return 0;
        if (count <= 0 || removed || sleeping || !active) return 0;

        bindEmitterContext();
        int maxParticles = (int) manual.maxParticles().evaluate(ctx());
        int available = Math.min(Math.max(0, maxParticles - particles.size()), MAX_PARTICLES - particles.size());
        int spawnCount = Math.min(count, available);
        for (int i = 0; i < spawnCount; i++) {
            spawnParticleInternal();
        }
        return spawnCount;
    }

    private void spawnParticleInternal() {
        ParticleInstance p = obtainParticle();
        p.reset();
        p.random1 = RANDOM.nextFloat();
        p.random2 = RANDOM.nextFloat();
        p.random3 = RANDOM.nextFloat();
        p.random4 = RANDOM.nextFloat();

        // 粒子独立 Molang 环境
        p.emitter = this;
        p.molang = molang.createChild();

        applyShape(p);
        applyInitialSpeed(p);

        p.molang.bindParticle(0, 1, p.random1, p.random2, p.random3, p.random4);
        if (lifetimeExprComponent != null) {
            p.maxLifetime = (float) lifetimeExprComponent.maxLifetime().evaluate(ctx());
        }

        applyAppearance(p);
        applyTinting(p);

        if (spinComponent != null) {
            p.rotation = (float) spinComponent.rotation().evaluate(ctx());
            p.rotationRate = (float) spinComponent.rotationRate().evaluate(ctx());
        }

        if (hasTransform) applyLocalSpaceOnSpawn(p);

        // 初始化 KillPlane 状态
        if (killPlaneComponent != null) {
            p.insideKillPlane = evaluateKillPlane(killPlaneComponent, p) < 0;
        }

        if (spawnCallback != null && (externalParticleManagement || p.worldSpace)) {
            spawnCallback.onParticleSpawned(p);
        } else {
            particles.add(p);
        }

        // 触发粒子创建事件
        fireParticleCreationEvents(p);
    }

    private void applyShape(ParticleInstance p) {
        if (shapeComponent == null) {
            p.x = p.y = p.z = 0;
            return;
        }
        shapeComponent.applyPosition(p, ctx(), RANDOM);
    }

    private void applyInitialSpeed(ParticleInstance p) {
        if (initialSpeedComponent == null || shapeComponent == null) return;
        float speed = (float) initialSpeedComponent.speed().evaluate(ctx());
        if (speed == 0) return;
        shapeComponent.applyDirection(p, ctx(), RANDOM, speed);
    }

    private void applyLocalSpaceOnSpawn(ParticleInstance p) {
        boolean effectiveLocalPos = fpMode ? fpLocalPosition : localPosition;
        boolean effectiveLocalRot = fpMode ? fpLocalRotation : localRotation;
        if (!effectiveLocalPos) {
            float scale = extractScale(worldTransform);
            p.spawnScale = scale;
            worldTransform.transform(tempSpawnVec.set(p.x, p.y, p.z, 1));
            p.x = tempSpawnVec.x;
            p.y = tempSpawnVec.y;
            p.z = tempSpawnVec.z;
            p.worldSpace = true;
            if (!effectiveLocalRot) transformVelocityByMatrix(p, worldTransform, scale);
            else {
                p.vx *= scale;
                p.vy *= scale;
                p.vz *= scale;
            }
        } else if (fpMode) {
            if (fpToWorld) {
                float scale = extractScale(worldTransform);
                p.spawnScale = scale;
                worldTransform.transform(tempSpawnVec.set(p.x, p.y, p.z, 1));
                p.x = tempSpawnVec.x;
                p.y = tempSpawnVec.y;
                p.z = tempSpawnVec.z;
                p.worldSpace = true;
                if (!effectiveLocalRot) transformVelocityByMatrix(p, worldTransform, scale);
                else {
                    p.vx *= scale;
                    p.vy *= scale;
                    p.vz *= scale;
                }
            } else {
                float scale = extractScale(emitterTransform);
                p.spawnScale = scale;
                emitterTransform.transform(tempSpawnVec.set(p.x, p.y, p.z, 1));
                p.x = tempSpawnVec.x;
                p.y = tempSpawnVec.y;
                p.z = tempSpawnVec.z;
                p.fpDetached = true;
                if (!effectiveLocalRot) transformVelocityByMatrix(p, emitterTransform, scale);
                else {
                    p.vx *= scale;
                    p.vy *= scale;
                    p.vz *= scale;
                }
            }
        } else {
            p.spawnScale = extractScale(emitterTransform);
        }
    }

    private static float extractScale(Matrix4f mat) {
        return (float) Math.sqrt(mat.m00() * mat.m00() + mat.m01() * mat.m01() + mat.m02() * mat.m02());
    }

    private static void transformVelocityByMatrix(ParticleInstance p, Matrix4f mat, float scale) {
        float c0x = mat.m00(), c0y = mat.m01(), c0z = mat.m02();
        float c1x = mat.m10(), c1y = mat.m11(), c1z = mat.m12();
        float c2x = mat.m20(), c2y = mat.m21(), c2z = mat.m22();
        float s0 = (float) Math.sqrt(c0x * c0x + c0y * c0y + c0z * c0z);
        float s1 = (float) Math.sqrt(c1x * c1x + c1y * c1y + c1z * c1z);
        float s2 = (float) Math.sqrt(c2x * c2x + c2y * c2y + c2z * c2z);
        if (s0 > 1e-6f && s1 > 1e-6f && s2 > 1e-6f) {
            float rx = (c0x / s0) * p.vx + (c1x / s1) * p.vy + (c2x / s2) * p.vz;
            float ry = (c0y / s0) * p.vx + (c1y / s1) * p.vy + (c2y / s2) * p.vz;
            float rz = (c0z / s0) * p.vx + (c1z / s1) * p.vy + (c2z / s2) * p.vz;
            p.vx = rx * scale;
            p.vy = ry * scale;
            p.vz = rz * scale;
        }
    }

    private void applyAppearance(ParticleInstance p) {
        if (billboardComponent == null) return;
        p.width = (float) billboardComponent.size()[0].evaluate(ctx());
        p.height = (float) billboardComponent.size()[1].evaluate(ctx());

        if (billboardComponent.flipbook() != null) {
            var fb = billboardComponent.flipbook();
            int maxFrame = (int) fb.maxFrame().evaluate(ctx());
            float frame;
            if (fb.stretchToLifetime()) {
                frame = (p.maxLifetime > 0) ? (p.age / p.maxLifetime) * maxFrame : 0;
            } else {
                frame = p.age * fb.framesPerSecond();
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
            p.u0 = u / fb.textureWidth();
            p.v0 = v / fb.textureHeight();
            p.u1 = (u + sizeU) / fb.textureWidth();
            p.v1 = (v + sizeV) / fb.textureHeight();
        } else if (billboardComponent.uv() != null) {
            var uv = billboardComponent.uv();
            float u = (float) uv.u().evaluate(ctx());
            float v = (float) uv.v().evaluate(ctx());
            float w = (float) uv.width().evaluate(ctx());
            float h = (float) uv.height().evaluate(ctx());
            p.u0 = u / uv.textureWidth();
            p.v0 = v / uv.textureHeight();
            p.u1 = (u + w) / uv.textureWidth();
            p.v1 = (v + h) / uv.textureHeight();
        }
    }

    private void applyTinting(ParticleInstance p) {
        if (tintingComponent instanceof ParticleTintingStatic sc) {
            p.r = (float) sc.r().evaluate(ctx());
            p.g = (float) sc.g().evaluate(ctx());
            p.b = (float) sc.b().evaluate(ctx());
            p.a = sc.a() != null ? (float) sc.a().evaluate(ctx()) : 1f;
        } else if (tintingComponent instanceof ParticleTintingGradient gc) {
            float t = (float) gc.interpolant().evaluate(ctx());
            applyGradientColor(p, gc.stops(), gc.colors(), t);
        }
    }

    private void applyGradientColor(ParticleInstance p, float[] stops, MolangExpression[][] colors, float t) {
        if (colors.length == 0) return;
        if (colors.length == 1) {
            p.r = (float) colors[0][0].evaluate(ctx());
            p.g = (float) colors[0][1].evaluate(ctx());
            p.b = (float) colors[0][2].evaluate(ctx());
            p.a = (float) colors[0][3].evaluate(ctx());
            return;
        }
        t = Math.max(stops[0], Math.min(stops[stops.length - 1], t));
        int idx = 0;
        for (int i = 0; i < stops.length - 1; i++) {
            if (t >= stops[i]) idx = i;
        }
        if (idx >= colors.length - 1) idx = colors.length - 2;
        float segStart = stops[idx], segEnd = stops[idx + 1];
        float frac = (segEnd > segStart) ? (t - segStart) / (segEnd - segStart) : 0;
        frac = Math.max(0, Math.min(1, frac));
        MolangExpression[] c0 = colors[idx], c1 = colors[idx + 1];
        float r0 = (float) c0[0].evaluate(ctx()), r1 = (float) c1[0].evaluate(ctx());
        float g0 = (float) c0[1].evaluate(ctx()), g1 = (float) c1[1].evaluate(ctx());
        float b0 = (float) c0[2].evaluate(ctx()), b1 = (float) c1[2].evaluate(ctx());
        float a0 = (float) c0[3].evaluate(ctx()), a1 = (float) c1[3].evaluate(ctx());
        p.r = r0 + (r1 - r0) * frac;
        p.g = g0 + (g1 - g0) * frac;
        p.b = b0 + (b1 - b0) * frac;
        p.a = a0 + (a1 - a0) * frac;
    }

    private void updateParticles(float dt) {
        int size = particles.size();
        for (int i = size - 1; i >= 0; i--) {
            ParticleInstance p = particles.get(i);
            updateSingleParticle(p, dt);
            if (!p.alive) {
                fireParticleExpirationEvents(p);
                int last = particles.size() - 1;
                if (i != last) {
                    particles.set(i, particles.get(last));
                }
                particles.remove(last);
                recycleParticle(p);
            }
        }
    }

    /**
     * 对单个粒子执行一帧的组件驱动更新。
     * 此方法同时被内部 {@link #updateParticles(float)} 和外部 {@link SnowStormParticle} 调用。
     */
    public void updateSingleParticle(ParticleInstance p, float dt) {
        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);

        // per_render_expression
        if (initComponent != null && initComponent.perRenderExpression() != null) {
            initComponent.perRenderExpression().evaluate(ctx());
        }
        // per_update_expression
        if (initComponent != null && initComponent.perUpdateExpression() != null) {
            initComponent.perUpdateExpression().evaluate(ctx());
        }

        // Motion
        if (motionComponent instanceof ParticleMotionDynamic dyn) {
            dyn.applyLegacy(p, ctx(), dt);
        } else if (motionComponent instanceof ParticleMotionParametric par) {
            par.applyLegacy(p, ctx(), dt);
        }

        if (needsPerFrameAppearance) {
            applyAppearance(p);
        }
        if (needsPerFrameTinting) {
            applyTinting(p);
        }
        p.tick(dt);

        // 粒子 timeline 事件
        updateParticleTimelineEvents(p);

        // 过期条件
        if (lifetimeExprComponent != null && lifetimeExprComponent.expirationExpression() != null) {
            if (lifetimeExprComponent.expirationExpression().evaluate(ctx()) != 0) {
                p.alive = false;
            }
        }

        // KillPlane：检测粒子是否穿过平面（符号变化）
        if (p.alive && killPlaneComponent != null) {
            boolean nowInside = evaluateKillPlane(killPlaneComponent, p) < 0;
            if (nowInside != p.insideKillPlane) {
                p.alive = false;
            }
        }
    }

    private void startEmitterCycle() {
        removed = false;
        sleeping = false;
        active = true;
        resetLoopState();
        bindEmitterContext();
        runEmitterCreationExpressions();
        refreshEmitterLifetime();
        bindEmitterContext();
        fireEmitterCreationEvents();
    }

    private void resetLoopState() {
        emitterAge = 0;
        hasEmittedInstant = false;
        spawnAccumulator = 0;
        // 重置事件追踪状态
        lastTimelineIndex = 0;
        lastTravelDistIndex = 0;
        travelDistance = 0;
        hasPrevPosition = false;
        Arrays.fill(loopingTravelDistAccum, 0f);
    }

    private void refreshEmitterLifetime() {
        if (lifetimeComponent instanceof EmitterLifetimeExpression) {
            emitterLifetime = emitterAge;
        } else if (lifetimeComponent instanceof LifetimeComponent lc) {
            emitterLifetime = (float) lc.activeTime().evaluate(ctx());
        } else {
            emitterLifetime = Float.MAX_VALUE;
        }
    }

    private void runEmitterCreationExpressions() {
        if (emitterInitComponent != null && emitterInitComponent.creationExpression() != null) {
            emitterInitComponent.creationExpression().evaluate(ctx());
        }
    }

    private void runEmitterPerUpdateExpressions() {
        if (emitterInitComponent != null && emitterInitComponent.perUpdateExpression() != null) {
            emitterInitComponent.perUpdateExpression().evaluate(ctx());
        }
    }

    private void bindEmitterContext() {
        molang.bindEmitter(emitterAge, emitterLifetime, emitterRandom1, emitterRandom2, emitterRandom3, emitterRandom4);
        if (curves.isEmpty()) return;

        var variableStorage = molang.getVariableStorage();
        for (Map.Entry<String, ParticleCurve> entry : curves.entrySet()) {
            float value = CurveEvaluator.evaluate(entry.getValue(), ctx(), entry.getKey());
            variableStorage.set(entry.getKey(), NumberValue.of(value));
        }
    }

    private ParticleInstance obtainParticle() {
        if (!pool.isEmpty()) return pool.remove(pool.size() - 1);
        return new ParticleInstance();
    }

    private void recycleParticle(ParticleInstance p) {
        if (pool.size() < 200) pool.add(p);
    }

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
        return removed && particles.isEmpty();
    }

    public boolean isActive() {
        return active;
    }

    public ParticleEffectDefinition getDefinition() {
        return definition;
    }

    public ParticleMolangEnvironment getMolang() {
        return molang;
    }

    // ===== 供 Runtime 组件调用的 API =====

    /** 当前 tick 的 delta time（秒）。由 tick() 设置。 */
    public float getDt() { return currentDt; }

    /** 当前活跃粒子数 */
    public int getParticleCount() { return particles.size(); }

    /** 由 Runtime 组件调用的粒子生成入口 */
    public void spawnParticle() { spawnParticleInternal(); }

    /** 发射器进入 removed/expired 状态 */
    public void setRemoved(boolean removed) { this.removed = removed; }

    /** 发射器进入/退出 active 状态 */
    public void setActive(boolean active) { this.active = active; }

    /** 发射器进入 sleeping 状态 */
    public void setSleeping(boolean sleeping) { this.sleeping = sleeping; }

    /** 获取/设置 sleep 剩余时间 */
    public float getSleepTimer() { return sleepTimer; }
    public void setSleepTimer(float t) { this.sleepTimer = t; }

    /** 重置 Instant rate 的 hasEmitted 标志（新循环时调用） */
    public void resetHasEmittedInstant() { this.hasEmittedInstant = false; }

    /** 获取/设置 spawn 累加器 */
    public float getSpawnAccumulator() { return spawnAccumulator; }
    public void setSpawnAccumulator(float v) { this.spawnAccumulator = v; }

    /** 重置事件追踪状态（新循环时调用） */
    public void resetEventTracking() {
        this.lastTimelineIndex = 0;
        this.lastTravelDistIndex = 0;
        this.travelDistance = 0;
        this.hasPrevPosition = false;
        Arrays.fill(loopingTravelDistAccum, 0f);
    }

    /** 获取 emitterAge */
    public float getEmitterAge() { return emitterAge; }
    public void setEmitterAge(float age) { this.emitterAge = age; }

    /** 获取 emitterLifetime */
    public float getEmitterLifetime() { return emitterLifetime; }
    public void setEmitterLifetime(float lt) { this.emitterLifetime = lt; }

    /** 触发发射器创建事件（由 Runtime 组件调用） */
    public void fireCreationEvents() { fireEmitterCreationEvents(); }

    /** 触发发射器过期事件（由 Runtime 组件调用） */
    public void fireExpirationEvents() { fireEmitterExpirationEvents(); }

    /** 绑定 emitter 上下文 + 曲线变量（由 Runtime 组件在 apply 时调用） */
    public void bindContextAndCurves() { bindEmitterContext(); }

    // ===== 向后兼容的旧方法 =====

    public void restart() {
        active = true;
        removed = false;
        sleeping = false;
        sleepTimer = 0;
        particles.clear();
        hasTransform = false;
        emitterTransform.identity();
        worldTransform.identity();
        startEmitterCycle();
    }

    public void setExternalParticleManagement(@Nullable ParticleSpawnCallback callback) {
        this.spawnCallback = callback;
        this.externalParticleManagement = callback != null;
    }

    public void setWorldSpaceParticleCallback(@Nullable ParticleSpawnCallback callback) {
        this.spawnCallback = callback;
        this.externalParticleManagement = false;
    }

    public void enableFPMode() {
        FPEmitterLocalSpace fpSpace = definition.findComponent(FPEmitterLocalSpace.class);
        if (fpSpace != null) {
            this.fpMode = true;
            this.fpLocalPosition = fpSpace.position();
            this.fpLocalRotation = fpSpace.position() && fpSpace.rotation();
            this.fpLocalVelocity = fpSpace.velocity();
            this.fpToWorld = fpSpace.toWorld();
        }
    }

    public boolean isFPMode() {
        return fpMode;
    }

    // ==================== 事件系统 ====================

    /**
     * 设置事件执行上下文。必须在需要事件功能时由外部调用设置。
     */
    public void setEventContext(@Nullable EventExecutor.EventContext context) {
        this.eventContext = context;
    }

    @Nullable
    public EventExecutor.EventContext getEventContext() {
        return eventContext;
    }

    private void fireEmitterCreationEvents() {
        if (lifetimeEventsComponent == null || eventContext == null) return;
        EventExecutor.fireEvents(lifetimeEventsComponent.creationEvent(), definition, eventContext);
    }

    private void fireEmitterExpirationEvents() {
        if (lifetimeEventsComponent == null || eventContext == null) return;
        EventExecutor.fireEvents(lifetimeEventsComponent.expirationEvent(), definition, eventContext);
    }

    /**
     * 每帧检查发射器 timeline、travel_distance 和 looping_travel_distance 事件。
     */
    private void updateEmitterLifetimeEvents() {
        if (lifetimeEventsComponent == null || eventContext == null) return;

        // timeline 事件：按时间顺序检查
        TreeMap<Float, List<String>> timeline = lifetimeEventsComponent.timeline();
        if (!timeline.isEmpty()) {
            int idx = 0;
            for (Map.Entry<Float, List<String>> entry : timeline.entrySet()) {
                if (idx < lastTimelineIndex) {
                    idx++;
                    continue;
                }
                if (emitterAge >= entry.getKey()) {
                    lastTimelineIndex = idx + 1;
                    EventExecutor.fireEvents(entry.getValue(), definition, eventContext);
                }
                idx++;
            }
        }

        // 计算移动距离
        updateTravelDistance();

        // travel_distance_events：按距离顺序检查
        TreeMap<Float, List<String>> travelDistEvents = lifetimeEventsComponent.travelDistanceEvents();
        if (!travelDistEvents.isEmpty()) {
            int idx = 0;
            for (Map.Entry<Float, List<String>> entry : travelDistEvents.entrySet()) {
                if (idx < lastTravelDistIndex) {
                    idx++;
                    continue;
                }
                if (travelDistance >= entry.getKey()) {
                    lastTravelDistIndex = idx + 1;
                    EventExecutor.fireEvents(entry.getValue(), definition, eventContext);
                }
                idx++;
            }
        }

        // looping_travel_distance_events
        List<EmitterLifetimeEvents.LoopingTravelDistanceEvent> loopingEvents = lifetimeEventsComponent.loopingTravelDistanceEvents();
        for (int i = 0; i < loopingEvents.size(); i++) {
            EmitterLifetimeEvents.LoopingTravelDistanceEvent loopEvent = loopingEvents.get(i);
            if (travelDistance - loopingTravelDistAccum[i] >= loopEvent.distance()) {
                loopingTravelDistAccum[i] = travelDistance;
                EventExecutor.fireEvents(loopEvent.effects(), definition, eventContext);
            }
        }
    }

    /**
     * 根据发射器世界变换矩阵的平移分量计算移动距离。
     */
    private void updateTravelDistance() {
        float curX = worldTransform.m30();
        float curY = worldTransform.m31();
        float curZ = worldTransform.m32();
        if (hasPrevPosition) {
            float dx = curX - prevEmitterX;
            float dy = curY - prevEmitterY;
            float dz = curZ - prevEmitterZ;
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 0) {
                travelDistance += dist;
            }
        }
        prevEmitterX = curX;
        prevEmitterY = curY;
        prevEmitterZ = curZ;
        hasPrevPosition = true;
    }

    private void fireParticleCreationEvents(ParticleInstance p) {
        if (particleLifetimeEventsComponent == null || eventContext == null) return;
        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);
        EventExecutor.fireEvents(particleLifetimeEventsComponent.creationEvent(), definition, eventContext);
    }

    /**
     * 触发粒子过期事件。也可由外部（如 {@link SnowStormParticle}）调用。
     */
    public void fireParticleExpirationEvents(ParticleInstance p) {
        if (particleLifetimeEventsComponent == null || eventContext == null) return;
        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);
        EventExecutor.fireEvents(particleLifetimeEventsComponent.expirationEvent(), definition, eventContext);
    }

    /**
     * 检查粒子 timeline 事件。也可由外部（如 {@link SnowStormParticle}）调用。
     */
    public void updateParticleTimelineEvents(ParticleInstance p) {
        if (particleLifetimeEventsComponent == null || eventContext == null) return;
        TreeMap<Float, List<String>> timeline = particleLifetimeEventsComponent.timeline();
        if (timeline.isEmpty()) return;

        int idx = 0;
        for (Map.Entry<Float, List<String>> entry : timeline.entrySet()) {
            if (idx < p.lastTimelineIndex) {
                idx++;
                continue;
            }
            if (p.age >= entry.getKey()) {
                p.lastTimelineIndex = idx + 1;
                EventExecutor.fireEvents(entry.getValue(), definition, eventContext);
            }
            idx++;
        }
    }

    /**
     * 触发碰撞事件。由 {@link SnowStormParticle} 在碰撞检测后调用。
     *
     * @param p     碰撞的粒子
     * @param speed 碰撞时的速度大小（blocks/second）
     */
    public void fireCollisionEvents(ParticleInstance p, float speed) {
        if (eventContext == null) return;
        ParticleMotionCollision collision = definition.findComponent(ParticleMotionCollision.class);
        if (collision == null || collision.events().isEmpty()) return;

        molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);
        for (ParticleMotionCollision.CollisionEvent event : collision.events()) {
            if (speed >= event.minSpeed()) {
                List<IEventNode> nodes = definition.getEvents().get(event.event());
                if (nodes != null) {
                    for (IEventNode node : nodes) {
                        EventExecutor.execute(node, eventContext);
                    }
                }
            }
        }
    }

    private static float evaluateKillPlane(ParticleLifetimeKillPlane plane, ParticleInstance p) {
        return plane.a() * p.x + plane.b() * p.y + plane.c() * p.z + plane.d();
    }
}
