package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 粒子运动组件。
 */
public sealed interface ParticleMotion extends IParticleComponent {

    /**
     * 动力学运动。对应 "minecraft:particle_motion_dynamic"。
     * @param linearAcceleration 线性加速度 [x, y, z]（Molang 表达式字符串）
     * @param linearDragCoefficient 线性阻力系数（Molang），可为 null
     */
    record Dynamic(@Nullable String[] linearAcceleration, @Nullable String linearDragCoefficient) implements ParticleMotion {}

    /**
     * 参数化运动。对应 "minecraft:particle_motion_parametric"。
     * @param relativePosition 相对位置 [x, y, z]（Molang），可为 null
     * @param direction 朝向 [x, y, z]（Molang），可为 null
     */
    record Parametric(@Nullable String[] relativePosition, @Nullable String[] direction) implements ParticleMotion {}
}
