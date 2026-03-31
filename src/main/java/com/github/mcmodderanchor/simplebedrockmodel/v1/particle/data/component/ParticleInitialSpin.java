package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子初始自旋组件。对应 "minecraft:particle_initial_spin"。
 *
 * @param rotation     初始旋转角度（Molang，度）
 * @param rotationRate 旋转速率（Molang，度/秒）
 */
public record ParticleInitialSpin(String rotation, String rotationRate) implements IParticleComponent {

    public static ParticleInitialSpin fromJson(JsonObject obj) {
        return new ParticleInitialSpin(
                getMolang(obj, "rotation", "0"),
                getMolang(obj, "rotation_rate", "0"));
    }
}
