package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 发射器生命周期组件。
 * <ul>
 *   <li>{@link Looping} — 循环发射，每个周期结束后重新开始</li>
 *   <li>{@link Once} — 发射一次后停止</li>
 * </ul>
 */
public sealed interface EmitterLifetime extends IEmitterComponent {

    /**
     * 循环发射。对应 "minecraft:emitter_lifetime_looping"。
     * @param activeTime 每个循环的活跃时间（秒，Molang）
     * @param sleepTime 循环间的休眠时间（秒，Molang）
     */
    record Looping(String activeTime, String sleepTime) implements EmitterLifetime {}

    /**
     * 单次发射。对应 "minecraft:emitter_lifetime_once"。
     * @param activeTime 活跃时间（秒，Molang）
     */
    record Once(String activeTime) implements EmitterLifetime {}
}
