package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticleEmitterInstance {
    private static final int MAX_PARTICLES = 1000;
    private static final Random RANDOM = new Random();

    private final ParticleEffectDefinition definition;
    private final ParticleMolangEnvironment molang;

    // 发射器状态
    private float emitterAge;
    private float emitterLifetime;
    private boolean active = true;
    private boolean sleeping;
    private float sleepTimer;
    private boolean hasEmittedInstant;
    private float spawnAccumulator;

    private final int emitterRandom1 = RANDOM.nextInt();
    private final int emitterRandom2 = RANDOM.nextInt();
    private final int emitterRandom3 = RANDOM.nextInt();
    private final int emitterRandom4 = RANDOM.nextInt();

    private final List<ParticleInstance> particles = new ArrayList<>();
    private final List<ParticleInstance> pool = new ArrayList<>();

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
    private final EmitterLifetime lifetimeComponent;
    @Nullable
    private final EmitterRate rateComponent;
    @Nullable
    private final EmitterShape shapeComponent;
    @Nullable
    private final ParticleInitialSpeed initialSpeedComponent;
    @Nullable
    private final ParticleLifetimeExpression lifetimeExprComponent;
    @Nullable
    private final ParticleAppearanceBillboard billboardComponent;
    @Nullable
    private final ParticleAppearanceTinting tintingComponent;
    @Nullable
    private final ParticleMotion motionComponent;
    @Nullable
    private final ParticleInitialSpin spinComponent;
    @Nullable
    private final ParticleInitialization initComponent;

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

        this.lifetimeComponent = definition.getLifetime();
        this.rateComponent = definition.getRate();
        this.shapeComponent = definition.getShape();
        this.initialSpeedComponent = definition.getInitialSpeed();
        this.lifetimeExprComponent = definition.getLifetimeExpression();
        this.billboardComponent = definition.getBillboard();
        this.tintingComponent = definition.getTinting();
        this.motionComponent = definition.getMotion();
        this.spinComponent = definition.getInitialSpin();
        this.initComponent = definition.getInitialization();

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

        bindEmitterContext();
        if (lifetimeComponent != null) {
            this.emitterLifetime = lifetimeComponent.activeTime(ctx());
        } else {
            this.emitterLifetime = Float.MAX_VALUE;
        }
    }

    private MolangContext<?> ctx() {
        return molang.getContext();
    }

    public void setEmitterTransform(Matrix4f locatorTransform, Matrix4f worldTransformIn) {
        emitterTransform.set(locatorTransform);
        worldTransform.set(worldTransformIn);
        hasTransform = true;
    }

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

    public void tick(float dt) {
        if (!active) {
            updateParticles(dt);
            return;
        }
        if (sleeping) {
            sleepTimer -= dt;
            if (sleepTimer <= 0) {
                sleeping = false;
                emitterAge = 0;
                hasEmittedInstant = false;
                spawnAccumulator = 0;
                bindEmitterContext();
                if (lifetimeComponent != null) {
                    emitterLifetime = lifetimeComponent.activeTime(ctx());
                }
            }
            updateParticles(dt);
            return;
        }
        emitterAge += dt;
        bindEmitterContext();
        if (emitterAge >= emitterLifetime) {
            handleLifetimeEnd();
        } else {
            emitParticles(dt);
        }
        updateParticles(dt);
    }

    private void handleLifetimeEnd() {
        if (lifetimeComponent instanceof EmitterLifetime.Looping looping) {
            sleeping = true;
            bindEmitterContext();
            sleepTimer = looping.sleepTime(ctx());
            if (sleepTimer <= 0) {
                sleeping = false;
                emitterAge = 0;
                hasEmittedInstant = false;
                spawnAccumulator = 0;
                bindEmitterContext();
                emitterLifetime = lifetimeComponent.activeTime(ctx());
            }
        } else {
            active = false;
        }
    }

    private void emitParticles(float dt) {
        if (rateComponent instanceof EmitterRate.Instant instant) {
            if (!hasEmittedInstant) {
                hasEmittedInstant = true;
                int count = (int) instant.amount().evaluate(ctx());
                for (int i = 0; i < count && particles.size() < MAX_PARTICLES; i++) {
                    spawnParticle();
                }
            }
        } else if (rateComponent instanceof EmitterRate.Steady steady) {
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
        p.random1 = RANDOM.nextFloat();
        p.random2 = RANDOM.nextFloat();
        p.random3 = RANDOM.nextFloat();
        p.random4 = RANDOM.nextFloat();

        applyShape(p);
        applyInitialSpeed(p);

        molang.bindParticle(0, 1, p.random1, p.random2, p.random3, p.random4);
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

        if (spawnCallback != null && (externalParticleManagement || p.worldSpace)) {
            spawnCallback.onParticleSpawned(p);
        } else {
            particles.add(p);
        }
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
            Vector4f worldPos = worldTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
            p.x = worldPos.x;
            p.y = worldPos.y;
            p.z = worldPos.z;
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
                Vector4f worldPos = worldTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
                p.x = worldPos.x;
                p.y = worldPos.y;
                p.z = worldPos.z;
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
                Vector4f modelPos = emitterTransform.transform(new Vector4f(p.x, p.y, p.z, 1));
                p.x = modelPos.x;
                p.y = modelPos.y;
                p.z = modelPos.z;
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
        if (tintingComponent instanceof ParticleAppearanceTinting.StaticColor sc) {
            p.r = (float) sc.r().evaluate(ctx());
            p.g = (float) sc.g().evaluate(ctx());
            p.b = (float) sc.b().evaluate(ctx());
            p.a = sc.a() != null ? (float) sc.a().evaluate(ctx()) : 1f;
        } else if (tintingComponent instanceof ParticleAppearanceTinting.GradientColor gc) {
            float t = (float) gc.interpolant().evaluate(ctx());
            applyGradientColor(p, gc.stops(), gc.colors(), t);
        }
    }

    private void applyGradientColor(ParticleInstance p, float[] stops, float[][] colors, float t) {
        if (colors.length == 0) return;
        if (colors.length == 1) {
            p.r = colors[0][0];
            p.g = colors[0][1];
            p.b = colors[0][2];
            p.a = colors[0][3];
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
        float[] c0 = colors[idx], c1 = colors[idx + 1];
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
        if (motionComponent != null) {
            motionComponent.apply(p, ctx(), dt);
        }

        applyAppearance(p);
        applyTinting(p);
        p.tick(dt);

        // 过期条件
        if (lifetimeExprComponent != null && lifetimeExprComponent.expirationExpression() != null) {
            if (lifetimeExprComponent.expirationExpression().evaluate(ctx()) != 0) {
                p.alive = false;
            }
        }
    }

    private void bindEmitterContext() {
        molang.bindEmitter(emitterAge, emitterLifetime, emitterRandom1, emitterRandom2, emitterRandom3, emitterRandom4);
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
        return !active && particles.isEmpty();
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
        if (lifetimeComponent != null) {
            emitterLifetime = lifetimeComponent.activeTime(ctx());
        } else {
            emitterLifetime = Float.MAX_VALUE;
        }
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
}
