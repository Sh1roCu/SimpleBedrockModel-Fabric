package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.tinting;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;

/**
 * 渐变颜色。对应 "minecraft:particle_appearance_tinting" 中的 gradient。
 */
public record ParticleTintingGradient(MolangExpression interpolant, float[] stops,
                                       MolangExpression[][] colors)
        implements IParticleComponentDefinition, IParticleComponent {

    @Override public int order() { return 210; }
    @Override public boolean requireUpdate() { return true; }
}
