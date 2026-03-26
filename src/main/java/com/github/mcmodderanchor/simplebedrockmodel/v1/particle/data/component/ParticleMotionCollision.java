package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 粒子碰撞运动组件。对应 "minecraft:particle_motion_collision"。
 * <p>
 * 只是解析，第一人称就不用这玩意了。
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
}
