package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * 粒子平面过期组件。对应 "minecraft:particle_lifetime_kill_plane"。
 * <p>
 * 当粒子穿过平面 {@code A*x + B*y + C*z + D = 0} 时过期。
 *
 * @param a 平面方程系数 A
 * @param b 平面方程系数 B
 * @param c 平面方程系数 C
 * @param d 平面方程系数 D
 */
public record ParticleLifetimeKillPlane(float a, float b, float c, float d)
        implements IParticleComponentDefinition, IParticleComponent {

    @Override public int order() { return 320; }

    @Override public boolean requireUpdate() { return true; }

    public static ParticleLifetimeKillPlane fromJson(JsonElement value) {
        if (!value.isJsonArray()) throw new JsonParseException("particle_lifetime_kill_plane must be a JSON array");
        JsonArray arr = value.getAsJsonArray();
        if (arr.size() != 4) throw new JsonParseException("particle_lifetime_kill_plane array must have 4 elements");
        return new ParticleLifetimeKillPlane(
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat()
        );
    }
}
