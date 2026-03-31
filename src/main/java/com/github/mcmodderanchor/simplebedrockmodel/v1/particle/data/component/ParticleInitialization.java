package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子初始化组件。对应 "minecraft:particle_initialization"。
 * <p>
 * {@code per_render_expression} 在每帧每个粒子更新时执行，
 * 通常用于设置 variable.xxx 供其他组件（如 size）引用。
 * <p>
 * {@code per_update_expression} 在每帧每个粒子 tick 时执行，
 * 在 per_render_expression 之后、其他组件求值之前。
 *
 * @param perRenderExpression 每帧执行的 Molang 表达式，可为 null
 * @param perUpdateExpression 每帧 tick 时执行的 Molang 表达式，可为 null
 */
public record ParticleInitialization(@Nullable String perRenderExpression,
                                     @Nullable String perUpdateExpression) implements IParticleComponent {

    public static ParticleInitialization fromJson(JsonObject obj) {
        String perRender = obj.has("per_render_expression") ? getMolang(obj, "per_render_expression", "") : null;
        String perUpdate = obj.has("per_update_expression") ? getMolang(obj, "per_update_expression", "") : null;
        return new ParticleInitialization(perRender, perUpdate);
    }
}
