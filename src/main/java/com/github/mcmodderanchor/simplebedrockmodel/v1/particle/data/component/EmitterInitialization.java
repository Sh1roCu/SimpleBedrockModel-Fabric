package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 发射器初始化组件。对应 "minecraft:emitter_initialization"。
 * <p>
 * 注：这是发射器级别的初始化，粒子级别见的 {@link ParticleInitialization} 。
 * <ul>
 *   <li>{@code creationExpression} — 发射器创建时执行一次</li>
 *   <li>{@code perUpdateExpression} — 发射器每帧更新时执行</li>
 * </ul>
 */
public record EmitterInitialization(
        @Nullable MolangExpression creationExpression,
        @Nullable MolangExpression perUpdateExpression
) implements IEmitterComponentDefinition, IEmitterComponent {

    @Override
    public int order() { return 500; }

    @Override
    public boolean requireUpdate() { return perUpdateExpression != null; }

    public static EmitterInitialization fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        MolangExpression creation = null;
        MolangExpression perUpdate = null;
        if (obj.has("creation_expression")) {
            String expr = getMolang(obj, "creation_expression", "");
            if (!expr.isEmpty()) creation = molang.compile(expr);
        }
        if (obj.has("per_update_expression")) {
            String expr = getMolang(obj, "per_update_expression", "");
            if (!expr.isEmpty()) perUpdate = molang.compile(expr);
        }
        return new EmitterInitialization(creation, perUpdate);
    }
}
