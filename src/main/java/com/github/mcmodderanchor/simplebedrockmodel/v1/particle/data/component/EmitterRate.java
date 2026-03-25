package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 发射速率组件。
 * <ul>
 *   <li>{@link Instant} — 一次性发射指定数量的粒子</li>
 *   <li>{@link Steady} — 以固定速率持续发射粒子</li>
 * </ul>
 */
public sealed interface EmitterRate extends IEmitterComponent {

    /**
     * 一次性发射。对应 "minecraft:emitter_rate_instant"。
     * @param amount 发射数量（Molang 表达式字符串）
     */
    record Instant(String amount) implements EmitterRate {}

    /**
     * 持续发射。对应 "minecraft:emitter_rate_steady"。
     * @param spawnRate 每秒发射数量（Molang 表达式字符串）
     * @param maxParticles 最大粒子数（Molang 表达式字符串）
     */
    record Steady(String spawnRate, String maxParticles) implements EmitterRate {}
}
