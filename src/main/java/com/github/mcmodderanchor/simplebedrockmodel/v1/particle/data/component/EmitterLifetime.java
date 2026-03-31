package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 发射器生命周期组件。
 * <ul>
 *   <li>{@link Looping} — 循环发射，每个周期结束后重新开始</li>
 *   <li>{@link Once} — 发射一次后停止</li>
 * </ul>
 */
public sealed interface EmitterLifetime extends IEmitterComponent {

    /** 活跃时间（秒，Molang 表达式字符串） */
    String activeTime();

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

    static EmitterLifetime fromJson(String key, JsonElement value) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:emitter_lifetime_looping" -> new Looping(
                    getMolang(obj, "active_time", "1"),
                    getMolang(obj, "sleep_time", "0"));
            case "minecraft:emitter_lifetime_once" -> new Once(getMolang(obj, "active_time", "1"));
            default -> throw new IllegalArgumentException("Unknown emitter lifetime key: " + key);
        };
    }
}
