package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.google.gson.JsonElement;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 粒子组件注册表。
 * <p>
 * 维护 JSON key（如 {@code "minecraft:emitter_rate_instant"}）到组件解析函数的映射。
 * 每个组件类通过 {@code fromJson} 静态方法自行负责解析逻辑。
 */
public final class ParticleComponentRegistry {

    private static final Map<String, BiFunction<String, JsonElement, IParticleComponent>> DESERIALIZERS = new HashMap<>();

    static {
        // Emitter Rate
        register("minecraft:emitter_rate_instant", EmitterRate::fromJson);
        register("minecraft:emitter_rate_steady", EmitterRate::fromJson);

        // Emitter Lifetime
        register("minecraft:emitter_lifetime_looping", EmitterLifetime::fromJson);
        register("minecraft:emitter_lifetime_once", EmitterLifetime::fromJson);

        // Emitter Local Space
        register("minecraft:emitter_local_space", (key, elem) -> EmitterLocalSpace.fromJson(elem.getAsJsonObject()));
        register("sbm:fp_emitter_local_space", (key, elem) -> FPEmitterLocalSpace.fromJson(elem.getAsJsonObject()));

        // Emitter Shape
        register("minecraft:emitter_shape_point", EmitterShape::fromJson);
        register("minecraft:emitter_shape_sphere", EmitterShape::fromJson);
        register("minecraft:emitter_shape_box", EmitterShape::fromJson);
        register("minecraft:emitter_shape_disc", EmitterShape::fromJson);
        register("minecraft:emitter_shape_entity_aabb", EmitterShape::fromJson);

        // Particle Appearance
        register("minecraft:particle_appearance_billboard", (key, elem) -> ParticleAppearanceBillboard.fromJson(elem.getAsJsonObject()));
        register("minecraft:particle_appearance_tinting", (key, elem) -> ParticleAppearanceTinting.fromJson(elem.getAsJsonObject()));

        // Particle Motion
        register("minecraft:particle_motion_dynamic", ParticleMotion::fromJson);
        register("minecraft:particle_motion_parametric", ParticleMotion::fromJson);
        register("minecraft:particle_motion_collision", (key, elem) -> ParticleMotionCollision.fromJson(elem.getAsJsonObject()));

        // Particle Lifetime
        register("minecraft:particle_lifetime_expression", (key, elem) -> ParticleLifetimeExpression.fromJson(elem.getAsJsonObject()));

        // Particle Initial
        register("minecraft:particle_initial_speed", (key, elem) -> ParticleInitialSpeed.fromJson(elem));
        register("minecraft:particle_initial_spin", (key, elem) -> ParticleInitialSpin.fromJson(elem.getAsJsonObject()));
        register("minecraft:particle_initialization", (key, elem) -> ParticleInitialization.fromJson(elem.getAsJsonObject()));
    }

    private ParticleComponentRegistry() {}

    private static void register(String key, BiFunction<String, JsonElement, IParticleComponent> deserializer) {
        DESERIALIZERS.put(key, deserializer);
    }

    /**
     * 根据 JSON key 解析对应的组件。
     *
     * @param key   组件的 JSON key（如 {@code "minecraft:emitter_rate_instant"}）
     * @param value 组件的 JSON 值
     * @return 解析后的组件实例，未知 key 返回 null
     */
    @Nullable
    public static IParticleComponent deserialize(String key, JsonElement value) {
        var deserializer = DESERIALIZERS.get(key);
        if (deserializer == null) return null;
        return deserializer.apply(key, value);
    }
}
