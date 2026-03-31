package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;

/**
 * 粒子碰撞运动组件。对应 "minecraft:particle_motion_collision"。
 * <p>
 * 第一人称模拟粒子此组件无效
 *
 * @param collisionDrag 碰撞阻力
 * @param coefficientOfRestitution 弹性系数
 * @param collisionRadius 碰撞半径
 * @param expireOnContact 碰撞时是否销毁粒子
 */
public record ParticleMotionCollision(
        float collisionDrag,
        float coefficientOfRestitution,
        float collisionRadius,
        boolean expireOnContact
) implements IParticleComponent {

    public static ParticleMotionCollision fromJson(JsonObject obj) {
        float drag = obj.has("collision_drag") ? obj.get("collision_drag").getAsFloat() : 0;
        float restitution = obj.has("coefficient_of_restitution") ? obj.get("coefficient_of_restitution").getAsFloat() : 1;
        float radius = obj.has("collision_radius") ? obj.get("collision_radius").getAsFloat() : 0.1f;
        boolean expire = getBoolean(obj, "expire_on_contact", false);
        return new ParticleMotionCollision(drag, restitution, radius, expire);
    }
}
