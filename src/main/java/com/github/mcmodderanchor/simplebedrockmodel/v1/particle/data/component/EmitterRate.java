package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

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

    static EmitterRate fromJson(String key, JsonElement value) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:emitter_rate_instant" -> new Instant(getMolang(obj, "num_particles", "10"));
            case "minecraft:emitter_rate_steady" -> new Steady(
                    getMolang(obj, "spawn_rate", "1"),
                    getMolang(obj, "max_particles", "50"));
            default -> throw new IllegalArgumentException("Unknown emitter rate key: " + key);
        };
    }
}
