package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import org.jetbrains.annotations.Nullable;

/**
 * 预编译的 Molang 表达式缓存。对同一个 definition + engine 只需编译一次。
 */
public record CompiledExpressions(
        MolangExpression emitterActiveTime,
        @Nullable MolangExpression emitterSleepTime,
        @Nullable RateCompiled rate,
        ShapeCompiled shape,
        @Nullable MolangExpression initialSpeed,
        MolangExpression particleMaxLifetime,
        @Nullable MolangExpression expirationExpr,
        @Nullable AppearanceCompiled appearance,
        @Nullable ColorCompiled color,
        @Nullable MotionCompiled motion,
        @Nullable ParametricMotionCompiled parametricMotion,
        @Nullable SpinCompiled spin,
        @Nullable MolangExpression perRenderExpression,
        @Nullable MolangExpression perUpdateExpression
) {

    // ---- Emitter Rate ----

    public sealed interface RateCompiled {
        record Instant(MolangExpression amount) implements RateCompiled {}
        record Steady(MolangExpression spawnRate, MolangExpression maxParticles) implements RateCompiled {}
    }

    // ---- Shape ----

    public record ShapeCompiled(
            MolangExpression[] offset,
            @Nullable MolangExpression[] direction,
            @Nullable MolangExpression radius,
            @Nullable MolangExpression[] halfDimensions
    ) {}

    // ---- Appearance ----

    public record AppearanceCompiled(
            MolangExpression sizeW,
            MolangExpression sizeH,
            @Nullable UVCompiled uv
    ) {}

    public sealed interface UVCompiled {
        record Static(MolangExpression u, MolangExpression v,
                      MolangExpression w, MolangExpression h,
                      float texW, float texH) implements UVCompiled {}

        record Flipbook(MolangExpression[] baseUV, MolangExpression[] sizeUV,
                        MolangExpression[] stepUV, MolangExpression maxFrame,
                        float fps, boolean stretch, boolean loop,
                        int texW, int texH) implements UVCompiled {}
    }

    // ---- Color ----

    public sealed interface ColorCompiled {
        record Static(MolangExpression r, MolangExpression g,
                      MolangExpression b, @Nullable MolangExpression a) implements ColorCompiled {}
        record Gradient(MolangExpression interpolant) implements ColorCompiled {}
    }

    // ---- Motion ----

    public record MotionCompiled(
            @Nullable MolangExpression accelX,
            @Nullable MolangExpression accelY,
            @Nullable MolangExpression accelZ,
            @Nullable MolangExpression dragCoefficient,
            @Nullable MolangExpression rotationAcceleration,
            @Nullable MolangExpression rotationDragCoefficient
    ) {}

    // ---- Initial Spin ----

    public record SpinCompiled(
            MolangExpression rotation,
            MolangExpression rotationRate
    ) {}

    // ---- Parametric Motion ----

    public record ParametricMotionCompiled(
            @Nullable MolangExpression[] relativePosition,
            @Nullable MolangExpression[] direction,
            @Nullable MolangExpression rotation
    ) {}

    // ---- Factory ----

    public static CompiledExpressions compile(ParticleEffectDefinition def, ParticleMolangEnvironment molang) {
        // Emitter Lifetime
        MolangExpression emitterActiveTime;
        MolangExpression emitterSleepTime = null;
        EmitterLifetime lifetime = def.findComponent(EmitterLifetime.class);
        if (lifetime != null) {
            emitterActiveTime = molang.compile(lifetime.activeTime());
            if (lifetime instanceof EmitterLifetime.Looping looping) {
                emitterSleepTime = molang.compile(looping.sleepTime());
            }
        } else {
            emitterActiveTime = MolangExpression.constant(Float.MAX_VALUE);
        }

        // Emitter Rate
        RateCompiled rate = null;
        EmitterRate rateComp = def.findComponent(EmitterRate.class);
        if (rateComp instanceof EmitterRate.Instant instant) {
            rate = new RateCompiled.Instant(molang.compile(instant.amount()));
        } else if (rateComp instanceof EmitterRate.Steady steady) {
            rate = new RateCompiled.Steady(molang.compile(steady.spawnRate()), molang.compile(steady.maxParticles()));
        }

        // Shape
        ShapeCompiled shape;
        EmitterShape shapeComp = def.findComponent(EmitterShape.class);
        if (shapeComp != null) {
            MolangExpression[] offset = compileArray3(molang, shapeComp.offset());
            MolangExpression[] direction = shapeComp.direction() != null ? compileArray3(molang, shapeComp.direction()) : null;
            MolangExpression radius = null;
            if (shapeComp instanceof EmitterShape.Sphere sphere) {
                radius = molang.compile(sphere.radius());
            } else if (shapeComp instanceof EmitterShape.Disc disc) {
                radius = molang.compile(disc.radius());
            }
            MolangExpression[] halfDims = shapeComp instanceof EmitterShape.Box box ? compileArray3(molang, box.halfDimensions()) : null;
            shape = new ShapeCompiled(offset, direction, radius, halfDims);
        } else {
            shape = new ShapeCompiled(
                    new MolangExpression[]{MolangExpression.zero(), MolangExpression.zero(), MolangExpression.zero()},
                    null, null, null);
        }

        // Initial Speed
        ParticleInitialSpeed speedComp = def.findComponent(ParticleInitialSpeed.class);
        MolangExpression initialSpeed = speedComp != null ? molang.compile(speedComp.speed()) : null;

        // Particle Lifetime
        MolangExpression particleMaxLifetime;
        MolangExpression expirationExpr = null;
        ParticleLifetimeExpression lifetimeExpr = def.findComponent(ParticleLifetimeExpression.class);
        if (lifetimeExpr != null) {
            particleMaxLifetime = molang.compile(lifetimeExpr.maxLifetime());
            if (lifetimeExpr.expirationExpression() != null) {
                expirationExpr = molang.compile(lifetimeExpr.expirationExpression());
            }
        } else {
            particleMaxLifetime = MolangExpression.constant(1);
        }

        // Appearance
        AppearanceCompiled appearance = null;
        ParticleAppearanceBillboard billboard = def.findComponent(ParticleAppearanceBillboard.class);
        if (billboard != null) {
            MolangExpression sizeW = molang.compile(billboard.size()[0]);
            MolangExpression sizeH = molang.compile(billboard.size()[1]);
            UVCompiled uv = null;
            if (billboard.flipbook() != null) {
                var fb = billboard.flipbook();
                uv = new UVCompiled.Flipbook(
                        new MolangExpression[]{molang.compile(fb.baseUV()[0]), molang.compile(fb.baseUV()[1])},
                        new MolangExpression[]{molang.compile(fb.sizeUV()[0]), molang.compile(fb.sizeUV()[1])},
                        new MolangExpression[]{molang.compile(fb.stepUV()[0]), molang.compile(fb.stepUV()[1])},
                        molang.compile(fb.maxFrame()),
                        fb.framesPerSecond(), fb.stretchToLifetime(), fb.loop(),
                        fb.textureWidth(), fb.textureHeight());
            } else if (billboard.uv() != null) {
                var uvData = billboard.uv();
                uv = new UVCompiled.Static(
                        molang.compile(uvData.u()), molang.compile(uvData.v()),
                        molang.compile(uvData.width()), molang.compile(uvData.height()),
                        uvData.textureWidth(), uvData.textureHeight());
            }
            appearance = new AppearanceCompiled(sizeW, sizeH, uv);
        }

        // Color
        ColorCompiled color = null;
        ParticleAppearanceTinting tinting = def.findComponent(ParticleAppearanceTinting.class);
        if (tinting instanceof ParticleAppearanceTinting.StaticColor sc) {
            color = new ColorCompiled.Static(
                    molang.compile(sc.r()), molang.compile(sc.g()), molang.compile(sc.b()),
                    sc.a() != null ? molang.compile(sc.a()) : null);
        } else if (tinting instanceof ParticleAppearanceTinting.GradientColor gc) {
            color = new ColorCompiled.Gradient(molang.compile(gc.interpolant()));
        }

        // Motion
        MotionCompiled motion = null;
        ParametricMotionCompiled parametricMotion = null;
        ParticleMotion motionComp = def.findComponent(ParticleMotion.class);
        if (motionComp instanceof ParticleMotion.Dynamic dynamic) {
            MolangExpression ax = null, ay = null, az = null;
            if (dynamic.linearAcceleration() != null) {
                MolangExpression[] accel = compileArray3(molang, dynamic.linearAcceleration());
                ax = accel[0]; ay = accel[1]; az = accel[2];
            }
            motion = new MotionCompiled(ax, ay, az,
                    dynamic.linearDragCoefficient() != null ? molang.compile(dynamic.linearDragCoefficient()) : null,
                    dynamic.rotationAcceleration() != null ? molang.compile(dynamic.rotationAcceleration()) : null,
                    dynamic.rotationDragCoefficient() != null ? molang.compile(dynamic.rotationDragCoefficient()) : null);
        } else if (motionComp instanceof ParticleMotion.Parametric parametric) {
            MolangExpression[] relPos = parametric.relativePosition() != null ? compileArray3(molang, parametric.relativePosition()) : null;
            MolangExpression[] dir = parametric.direction() != null ? compileArray3(molang, parametric.direction()) : null;
            MolangExpression rot = parametric.rotation() != null ? molang.compile(parametric.rotation()) : null;
            parametricMotion = new ParametricMotionCompiled(relPos, dir, rot);
        }

        // Initial Spin
        SpinCompiled spin = null;
        ParticleInitialSpin spinComp = def.findComponent(ParticleInitialSpin.class);
        if (spinComp != null) {
            spin = new SpinCompiled(molang.compile(spinComp.rotation()), molang.compile(spinComp.rotationRate()));
        }

        // Initialization
        MolangExpression perRenderExpression = null;
        MolangExpression perUpdateExpression = null;
        ParticleInitialization init = def.findComponent(ParticleInitialization.class);
        if (init != null) {
            if (init.perRenderExpression() != null && !init.perRenderExpression().isEmpty()) {
                perRenderExpression = molang.compile(init.perRenderExpression());
            }
            if (init.perUpdateExpression() != null && !init.perUpdateExpression().isEmpty()) {
                perUpdateExpression = molang.compile(init.perUpdateExpression());
            }
        }

        return new CompiledExpressions(
                emitterActiveTime, emitterSleepTime, rate, shape,
                initialSpeed, particleMaxLifetime, expirationExpr,
                appearance, color, motion, parametricMotion, spin,
                perRenderExpression, perUpdateExpression);
    }

    private static MolangExpression[] compileArray3(ParticleMolangEnvironment molang, String[] exprs) {
        return new MolangExpression[]{
                molang.compile(exprs[0]),
                molang.compile(exprs[1]),
                molang.compile(exprs[2])
        };
    }
}
