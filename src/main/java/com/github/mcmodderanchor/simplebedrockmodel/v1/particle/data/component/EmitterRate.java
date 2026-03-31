package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 发射速率组件。
 */
public sealed interface EmitterRate extends IEmitterComponent {

    record Instant(MolangExpression amount) implements EmitterRate { }

    record Steady(MolangExpression spawnRate, MolangExpression maxParticles) implements EmitterRate { }

    static EmitterRate fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        return switch (key) {
            case "minecraft:emitter_rate_instant" -> new Instant(
                    molang.compile(getMolang(obj, "num_particles", "10")));
            case "minecraft:emitter_rate_steady" -> new Steady(
                    molang.compile(getMolang(obj, "spawn_rate", "1")),
                    molang.compile(getMolang(obj, "max_particles", "50")));
            default -> throw new IllegalArgumentException("Unknown emitter rate key: " + key);
        };
    }
}
