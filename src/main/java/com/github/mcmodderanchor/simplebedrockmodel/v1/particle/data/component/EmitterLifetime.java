package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
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

    /** 活跃时间（秒） */
    MolangExpression activeTime();

    default float activeTime(MolangContext<?> ctx) {
        return (float) activeTime().evaluate(ctx);
    }

    /**
     * 循环发射。对应 "minecraft:emitter_lifetime_looping"。
     */
    record Looping(MolangExpression activeTime, MolangExpression sleepTime) implements EmitterLifetime {
        public float sleepTime(MolangContext<?> ctx) {
            return (float) sleepTime().evaluate(ctx);
        }
    }

    /**
     * 单次发射。对应 "minecraft:emitter_lifetime_once"。
     */
    record Once(MolangExpression activeTime) implements EmitterLifetime {}

    static EmitterLifetime fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:emitter_lifetime_looping" -> new Looping(
                    molang.compile(getMolang(obj, "active_time", "1")),
                    molang.compile(getMolang(obj, "sleep_time", "0")));
            case "minecraft:emitter_lifetime_once" -> new Once(
                    molang.compile(getMolang(obj, "active_time", "1")));
            default -> throw new IllegalArgumentException("Unknown emitter lifetime key: " + key);
        };
    }
}
