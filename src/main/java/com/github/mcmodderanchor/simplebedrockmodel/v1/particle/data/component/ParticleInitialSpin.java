package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子初始自旋组件。对应 "minecraft:particle_initial_spin"。
 */
public record ParticleInitialSpin(MolangExpression rotation, MolangExpression rotationRate) implements IParticleComponent {

    public static ParticleInitialSpin fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        return new ParticleInitialSpin(
                molang.compile(getMolang(obj, "rotation", "0")),
                molang.compile(getMolang(obj, "rotation_rate", "0")));
    }
}
