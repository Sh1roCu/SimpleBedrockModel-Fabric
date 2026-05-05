package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.tinting;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IParticleComponentDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * 静态颜色。对应 "minecraft:particle_appearance_tinting" 中的 color 数组。
 */
public record ParticleTintingStatic(MolangExpression r, MolangExpression g, MolangExpression b,
                                     @Nullable MolangExpression a)
        implements IParticleComponentDefinition, IParticleComponent {

    @Override public int order() { return 210; }
    @Override public boolean requireUpdate() { return false; }
}
