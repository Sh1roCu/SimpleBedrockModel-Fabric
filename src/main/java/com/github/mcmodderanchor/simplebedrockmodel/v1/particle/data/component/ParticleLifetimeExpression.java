package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getMolang;

/**
 * 粒子生命周期组件。对应 "minecraft:particle_lifetime_expression"。
 * @param maxLifetime 最大生命周期（秒，Molang 表达式字符串）
 * @param expirationExpression 过期条件表达式，非零时粒子死亡（可为 null）
 */
public record ParticleLifetimeExpression(
        String maxLifetime,
        @Nullable String expirationExpression
) implements IParticleComponent {

    public static ParticleLifetimeExpression fromJson(JsonObject obj) {
        String maxLifetime = getMolang(obj, "max_lifetime", "1");
        String expiration = obj.has("expiration_expression") ? getMolang(obj, "expiration_expression", "0") : null;
        return new ParticleLifetimeExpression(maxLifetime, expiration);
    }
}
