package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonElement;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 粒子组件注册表。
 * <p>
 * 维护 JSON key 到组件解析函数的映射。
 * 解析时接收 {@link ParticleMolangEnvironment}，在反序列化阶段直接编译 Molang 表达式。
 */
public final class ParticleComponentRegistry {

    @FunctionalInterface
    public interface ComponentDeserializer {
        IParticleComponent deserialize(String key, JsonElement value, ParticleMolangEnvironment molang);
    }

    private static final Map<String, ComponentDeserializer> DESERIALIZERS = new HashMap<>();

    static {
        // Emitter Rate
        register("minecraft:emitter_rate_instant", EmitterRate::fromJson);
        register("minecraft:emitter_rate_steady", EmitterRate::fromJson);

        // Emitter Lifetime
        register("minecraft:emitter_lifetime_looping", EmitterLifetime::fromJson);
        register("minecraft:emitter_lifetime_once", EmitterLifetime::fromJson);

        // Emitter Local Space (不含 Molang 表达式，忽略 molang 参数)
        register("minecraft:emitter_local_space", (key, elem, molang) -> EmitterLocalSpace.fromJson(elem.getAsJsonObject()));
        register("sbm:fp_emitter_local_space", (key, elem, molang) -> FPEmitterLocalSpace.fromJson(elem.getAsJsonObject()));

        // Emitter Shape
        register("minecraft:emitter_shape_point", EmitterShape::fromJson);
        register("minecraft:emitter_shape_sphere", EmitterShape::fromJson);
        register("minecraft:emitter_shape_box", EmitterShape::fromJson);
        register("minecraft:emitter_shape_disc", EmitterShape::fromJson);
        register("minecraft:emitter_shape_entity_aabb", EmitterShape::fromJson);

        // Particle Appearance
        register("minecraft:particle_appearance_billboard", (key, elem, molang) -> ParticleAppearanceBillboard.fromJson(elem.getAsJsonObject(), molang));
        register("minecraft:particle_appearance_tinting", (key, elem, molang) -> ParticleAppearanceTinting.fromJson(elem.getAsJsonObject(), molang));

        // Particle Motion
        register("minecraft:particle_motion_dynamic", ParticleMotion::fromJson);
        register("minecraft:particle_motion_parametric", ParticleMotion::fromJson);
        register("minecraft:particle_motion_collision", (key, elem, molang) -> ParticleMotionCollision.fromJson(elem.getAsJsonObject()));

        // Particle Lifetime
        register("minecraft:particle_lifetime_expression", (key, elem, molang) -> ParticleLifetimeExpression.fromJson(elem.getAsJsonObject(), molang));

        // Particle Initial
        register("minecraft:particle_initial_speed", (key, elem, molang) -> ParticleInitialSpeed.fromJson(elem, molang));
        register("minecraft:particle_initial_spin", (key, elem, molang) -> ParticleInitialSpin.fromJson(elem.getAsJsonObject(), molang));
        register("minecraft:particle_initialization", (key, elem, molang) -> ParticleInitialization.fromJson(elem.getAsJsonObject(), molang));
    }

    private ParticleComponentRegistry() {}

    private static void register(String key, ComponentDeserializer deserializer) {
        DESERIALIZERS.put(key, deserializer);
    }

    @Nullable
    public static IParticleComponent deserialize(String key, JsonElement value, ParticleMolangEnvironment molang) {
        var deserializer = DESERIALIZERS.get(key);
        if (deserializer == null) return null;
        return deserializer.deserialize(key, value, molang);
    }
}
