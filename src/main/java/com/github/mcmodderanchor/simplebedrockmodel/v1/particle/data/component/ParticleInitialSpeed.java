package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonElement;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.molangFromElement;

/**
 * 粒子初始速度组件。对应 "minecraft:particle_initial_speed"。
 * @param speed 初始速度标量（Molang 表达式字符串），沿发射方向
 */
public record ParticleInitialSpeed(String speed) implements IParticleComponent {

    public static ParticleInitialSpeed fromJson(JsonElement value) {
        return new ParticleInitialSpeed(molangFromElement(value, "0"));
    }
}
