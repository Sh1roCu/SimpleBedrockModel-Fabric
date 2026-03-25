package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 粒子生命周期组件。对应 "minecraft:particle_lifetime_expression"。
 * @param maxLifetime 最大生命周期（秒，Molang 表达式字符串）
 * @param expirationExpression 过期条件表达式，非零时粒子死亡（可为 null）
 */
public record ParticleLifetimeExpression(
        String maxLifetime,
        @Nullable String expirationExpression
) implements IParticleComponent {
}
