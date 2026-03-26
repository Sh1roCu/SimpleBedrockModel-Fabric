package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 粒子初始自旋组件。对应 "minecraft:particle_initial_spin"。
 *
 * @param rotation     初始旋转角度（Molang，度）
 * @param rotationRate 旋转速率（Molang，度/秒）
 */
public record ParticleInitialSpin(String rotation, String rotationRate) implements IParticleComponent {
}
