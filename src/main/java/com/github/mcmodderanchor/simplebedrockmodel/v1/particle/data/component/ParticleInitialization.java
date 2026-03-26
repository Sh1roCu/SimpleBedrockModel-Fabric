package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 粒子初始化组件。对应 "minecraft:particle_initialization"。
 * <p>
 * {@code per_render_expression} 在每帧每个粒子更新时执行，
 * 通常用于设置 variable.xxx 供其他组件（如 size）引用。
 *
 * @param perRenderExpression 每帧执行的 Molang 表达式，可为 null
 */
public record ParticleInitialization(@Nullable String perRenderExpression) implements IParticleComponent {
}
