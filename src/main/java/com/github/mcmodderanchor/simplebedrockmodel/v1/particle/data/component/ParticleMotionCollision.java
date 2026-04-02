package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;
import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子碰撞运动组件。对应 "minecraft:particle_motion_collision"。
 * <p>
 * 第一人称模拟粒子此组件无效
 *
 * @param enabled                  是否启用碰撞（Molang 布尔表达式），null 时默认启用
 * @param collisionDrag            碰撞阻力
 * @param coefficientOfRestitution 弹性系数
 * @param collisionRadius          碰撞半径
 * @param expireOnContact          碰撞时是否销毁粒子
 * @param events                   碰撞触发的事件列表
 */
public record ParticleMotionCollision(
        @Nullable MolangExpression enabled,
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact,
        List<CollisionEvent> events
) implements IParticleComponent {

    /**
     * 碰撞事件。
     *
     * @param event    事件名称
     * @param minSpeed 触发碰撞事件的最小速度
     */
    public record CollisionEvent(String event, float minSpeed) {}

    public static ParticleMotionCollision fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        MolangExpression enabled = null;
        if (obj.has("enabled")) {
            String expr = getMolang(obj, "enabled", "1");
            enabled = molang.compile(expr);
        }
        float drag = obj.has("collision_drag") ? obj.get("collision_drag").getAsFloat() : 0;
        float restitution = obj.has("coefficient_of_restitution") ? obj.get("coefficient_of_restitution").getAsFloat() : 0;
        float radius = obj.has("collision_radius") ? obj.get("collision_radius").getAsFloat() : 0;
        boolean expire = getBoolean(obj, "expire_on_contact", false);
        List<CollisionEvent> events = parseEvents(obj);
        return new ParticleMotionCollision(enabled, drag, restitution, radius, expire, events);
    }

    private static List<CollisionEvent> parseEvents(JsonObject obj) {
        if (!obj.has("events")) return List.of();
        JsonElement elem = obj.get("events");
        if (!elem.isJsonArray()) return List.of();
        JsonArray arr = elem.getAsJsonArray();
        List<CollisionEvent> list = new ArrayList<>();
        for (JsonElement e : arr) {
            if (e.isJsonObject()) {
                JsonObject eventObj = e.getAsJsonObject();
                String event = eventObj.has("event") ? eventObj.get("event").getAsString() : "";
                float minSpeed = eventObj.has("min_speed") ? eventObj.get("min_speed").getAsFloat() : 2f;
                if (!event.isEmpty()) {
                    list.add(new CollisionEvent(event, minSpeed));
                }
            }
        }
        return list;
    }
}
