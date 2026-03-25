package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
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

    public ParticleEmitterInstance(ParticleEffectDefinition definition, ParticleMolangEnvironment molang) {
        this.definition = definition;
        this.molang = molang;
        this.compiled = new CompiledExpressions(definition, molang);

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
     * 更新发射器和所有粒子。
     * @param dt 时间步长（秒）
     */
    public void tick(float dt) {
        if (!active) return;

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

        // 初始位置（由形状决定）
        applyShape(p);

        // 初始速度
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

        particles.add(p);
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
        if (shape instanceof EmitterShape.Point point && point.direction() != null) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else if (shape instanceof EmitterShape.Sphere sphere && sphere.direction() != null) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else if (shape instanceof EmitterShape.Box box && box.direction() != null) {
            dx = (float) compiled.shapeDirection[0].evaluate();
            dy = (float) compiled.shapeDirection[1].evaluate();
            dz = (float) compiled.shapeDirection[2].evaluate();
        } else {
            // "outwards" — 从发射器中心向外
            dx = p.x;
            dy = p.y;
            dz = p.z;
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

    private void applyAppearance(ParticleInstance p) {
        if (compiled.particleSizeW != null) {
            p.width = (float) compiled.particleSizeW.evaluate();
            p.height = (float) compiled.particleSizeH.evaluate();
        }

        // UV
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
            applyGradientColor(p, gradient.colors(), t);
        }
    }

    private void applyGradientColor(ParticleInstance p, float[][] colors, float t) {
        if (colors.length == 0) return;
        if (colors.length == 1) {
            p.r = colors[0][0]; p.g = colors[0][1]; p.b = colors[0][2]; p.a = colors[0][3];
            return;
        }
        t = Math.max(0, Math.min(1, t));
        float segment = t * (colors.length - 1);
        int idx = Math.min((int) segment, colors.length - 2);
        float frac = segment - idx;
        float[] c0 = colors[idx];
        float[] c1 = colors[idx + 1];
        p.r = c0[0] + (c1[0] - c0[0]) * frac;
        p.g = c0[1] + (c1[1] - c0[1]) * frac;
        p.b = c0[2] + (c1[2] - c0[2]) * frac;
        p.a = c0[3] + (c1[3] - c0[3]) * frac;
    }

    private void updateParticles(float dt) {
        ParticleMotion motion = definition.findComponent(ParticleMotion.class);

        for (int i = particles.size() - 1; i >= 0; i--) {
            ParticleInstance p = particles.get(i);
            molang.bindParticle(p.age, p.maxLifetime, p.random1, p.random2, p.random3, p.random4);

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

            if (!p.alive) {
                particles.remove(i);
                recycleParticle(p);
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
        bindEmitterContext();
        emitterLifetime = (float) compiled.emitterActiveTime.evaluate();
    }

    public ParticleEffectDefinition getDefinition() {
        return definition;
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
            } else {
                particleSizeW = particleSizeH = null;
                uvU = uvV = uvW = uvH = null;
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
            } else {
                accelX = accelY = accelZ = null;
                dragCoefficient = null;
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
