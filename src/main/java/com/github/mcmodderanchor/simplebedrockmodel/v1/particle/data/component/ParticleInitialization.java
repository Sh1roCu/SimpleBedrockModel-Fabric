package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子初始化组件。对应 "minecraft:particle_initialization"。
 * <p>
 * {@code per_render_expression} 在每帧每个粒子渲染时执行。
 * {@code per_update_expression} 在每帧每个粒子 tick 时执行。
 */
public record ParticleInitialization(
        @Nullable MolangExpression perRenderExpression,
        @Nullable MolangExpression perUpdateExpression
) implements IParticleComponent {

    public static ParticleInitialization fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        MolangExpression perRender = null;
        MolangExpression perUpdate = null;
        if (obj.has("per_render_expression")) {
            String expr = getMolang(obj, "per_render_expression", "");
            if (!expr.isEmpty()) perRender = molang.compile(expr);
        }
        if (obj.has("per_update_expression")) {
            String expr = getMolang(obj, "per_update_expression", "");
            if (!expr.isEmpty()) perUpdate = molang.compile(expr);
        }
        return new ParticleInitialization(perRender, perUpdate);
    }
}
