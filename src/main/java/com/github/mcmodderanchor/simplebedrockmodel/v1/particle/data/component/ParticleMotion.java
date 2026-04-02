package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 粒子运动组件。
 */
public sealed interface ParticleMotion extends IParticleComponent {

    /**
     * 对粒子应用运动更新。
     */
    void apply(ParticleInstance p, MolangContext<?> ctx, float dt);

    /**
     * 动力学运动。对应 "minecraft:particle_motion_dynamic"。
     */
    record Dynamic(
            @Nullable MolangExpression[] linearAcceleration,
            @Nullable MolangExpression linearDragCoefficient,
            @Nullable MolangExpression rotationAcceleration,
            @Nullable MolangExpression rotationDragCoefficient
    ) implements ParticleMotion {
        @Override
        public void apply(ParticleInstance p, MolangContext<?> ctx, float dt) {
            if (linearAcceleration != null) {
                p.vx += (float) linearAcceleration[0].evaluate(ctx) * dt;
                p.vy += (float) linearAcceleration[1].evaluate(ctx) * dt;
                p.vz += (float) linearAcceleration[2].evaluate(ctx) * dt;
            }
            if (linearDragCoefficient != null) {
                float factor = Math.max(0, 1f - (float) linearDragCoefficient.evaluate(ctx) * dt);
                p.vx *= factor;
                p.vy *= factor;
                p.vz *= factor;
            }
            if (rotationAcceleration != null) {
                p.rotationRate += (float) rotationAcceleration.evaluate(ctx) * dt;
            }
            if (rotationDragCoefficient != null) {
                float rotFactor = Math.max(0, 1f - (float) rotationDragCoefficient.evaluate(ctx) * dt);
                p.rotationRate *= rotFactor;
            }
        }
    }

    /**
     * 参数化运动。对应 "minecraft:particle_motion_parametric"。
     */
    record Parametric(
            @Nullable MolangExpression[] relativePosition,
            @Nullable MolangExpression[] direction,
            @Nullable MolangExpression rotation
    ) implements ParticleMotion {
        @Override
        public void apply(ParticleInstance p, MolangContext<?> ctx, float dt) {
            if (relativePosition != null) {
                p.x = (float) relativePosition[0].evaluate(ctx);
                p.y = (float) relativePosition[1].evaluate(ctx);
                p.z = (float) relativePosition[2].evaluate(ctx);
            }
            if (rotation != null) {
                p.rotation = (float) rotation.evaluate(ctx);
            }
        }
    }

    static ParticleMotion fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:particle_motion_dynamic" -> {
                MolangExpression[] accel = obj.has("linear_acceleration")
                        ? compileArray3(molang, getMolangArray3(obj, "linear_acceleration", "0", "0", "0")) : null;
                MolangExpression drag = obj.has("linear_drag_coefficient")
                        ? molang.compile(getMolang(obj, "linear_drag_coefficient", "0")) : null;
                MolangExpression rotAccel = obj.has("rotation_acceleration")
                        ? molang.compile(getMolang(obj, "rotation_acceleration", "0")) : null;
                MolangExpression rotDrag = obj.has("rotation_drag_coefficient")
                        ? molang.compile(getMolang(obj, "rotation_drag_coefficient", "0")) : null;
                yield new Dynamic(accel, drag, rotAccel, rotDrag);
            }
            case "minecraft:particle_motion_parametric" -> {
                MolangExpression[] pos = obj.has("relative_position")
                        ? compileArray3(molang, getMolangArray3(obj, "relative_position", "0", "0", "0")) : null;
                MolangExpression[] dir = obj.has("direction")
                        ? compileArray3(molang, getMolangArray3(obj, "direction", "0", "0", "0")) : null;
                MolangExpression rot = obj.has("rotation")
                        ? molang.compile(getMolang(obj, "rotation", "0")) : null;
                yield new Parametric(pos, dir, rot);
            }
            default -> throw new IllegalArgumentException("Unknown particle motion key: " + key);
        };
    }

    private static MolangExpression[] compileArray3(ParticleMolangEnvironment molang, String[] exprs) {
        return new MolangExpression[]{
                molang.compile(exprs[0]),
                molang.compile(exprs[1]),
                molang.compile(exprs[2])
        };
    }
}
