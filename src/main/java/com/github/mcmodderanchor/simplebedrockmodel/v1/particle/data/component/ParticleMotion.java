package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 粒子运动组件。
 */
public sealed interface ParticleMotion extends IParticleComponent {

    /**
     * 动力学运动。对应 "minecraft:particle_motion_dynamic"。
     * @param linearAcceleration 线性加速度 [x, y, z]（Molang 表达式字符串）
     * @param linearDragCoefficient 线性阻力系数（Molang），可为 null
     * @param rotationAcceleration 旋转加速度（Molang，度/秒²），可为 null
     * @param rotationDragCoefficient 旋转阻力系数（Molang），可为 null
     */
    record Dynamic(@Nullable String[] linearAcceleration, @Nullable String linearDragCoefficient,
                   @Nullable String rotationAcceleration, @Nullable String rotationDragCoefficient) implements ParticleMotion {}

    /**
     * 参数化运动。对应 "minecraft:particle_motion_parametric"。
     * @param relativePosition 相对位置 [x, y, z]（Molang），可为 null
     * @param direction 朝向 [x, y, z]（Molang），可为 null
     * @param rotation 旋转角度（Molang，度），可为 null。直接设置粒子旋转角度（非增量）
     */
    record Parametric(@Nullable String[] relativePosition, @Nullable String[] direction,
                      @Nullable String rotation) implements ParticleMotion {}

    static ParticleMotion fromJson(String key, JsonElement value) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:particle_motion_dynamic" -> {
                String[] accel = obj.has("linear_acceleration") ? getMolangArray3(obj, "linear_acceleration", "0", "0", "0") : null;
                String drag = obj.has("linear_drag_coefficient") ? getMolang(obj, "linear_drag_coefficient", "0") : null;
                String rotAccel = obj.has("rotation_acceleration") ? getMolang(obj, "rotation_acceleration", "0") : null;
                String rotDrag = obj.has("rotation_drag_coefficient") ? getMolang(obj, "rotation_drag_coefficient", "0") : null;
                yield new Dynamic(accel, drag, rotAccel, rotDrag);
            }
            case "minecraft:particle_motion_parametric" -> {
                String[] pos = obj.has("relative_position") ? getMolangArray3(obj, "relative_position", "0", "0", "0") : null;
                String[] dir = obj.has("direction") ? getMolangArray3(obj, "direction", "0", "0", "0") : null;
                String rot = obj.has("rotation") ? getMolang(obj, "rotation", "0") : null;
                yield new Parametric(pos, dir, rot);
            }
            default -> throw new IllegalArgumentException("Unknown particle motion key: " + key);
        };
    }
}
