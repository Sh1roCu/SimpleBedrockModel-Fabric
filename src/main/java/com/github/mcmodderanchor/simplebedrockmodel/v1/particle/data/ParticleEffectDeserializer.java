package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 基岩版粒子效果 JSON 反序列化器。
 * <p>
 * 解析格式：
 * <pre>
 * {
 *   "format_version": "1.10.0",
 *   "particle_effect": {
 *     "description": { "identifier": "...", "basic_render_parameters": { ... } },
 *     "components": { ... }
 *   }
 * }
 * </pre>
 * <p>
 * 各组件的具体解析逻辑由 {@link ParticleComponentRegistry} 分发到各组件类的
 * {@code fromJson} 方法中。
 */
public class ParticleEffectDeserializer implements JsonDeserializer<ParticleEffectDefinition> {

    @Override
    public ParticleEffectDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        JsonObject effect = root.getAsJsonObject("particle_effect");
        if (effect == null) throw new JsonParseException("Missing 'particle_effect'");

        // description
        JsonObject descObj = effect.getAsJsonObject("description");
        ParticleDescription description = parseDescription(descObj);

        // components
        JsonObject compObj = effect.getAsJsonObject("components");
        List<IParticleComponent> components = compObj != null ? parseComponents(compObj) : List.of();

        return new ParticleEffectDefinition(description.getIdentifier(), description, components);
    }

    private ParticleDescription parseDescription(JsonObject obj) {
        String id = obj.get("identifier").getAsString();
        ResourceLocation identifier = new ResourceLocation(id);

        JsonObject renderParams = obj.getAsJsonObject("basic_render_parameters");
        ParticleDescription.Material material = ParticleDescription.Material.PARTICLES_BLEND;
        ResourceLocation texture = new ResourceLocation("minecraft", "textures/particle/generic_0.png");
        int texW = 0, texH = 0;

        if (renderParams != null) {
            String mat = getString(renderParams, "material", "particles_blend");
            material = switch (mat) {
                case "particles_opaque" -> ParticleDescription.Material.PARTICLES_OPAQUE;
                case "particles_alpha" -> ParticleDescription.Material.PARTICLES_ALPHA;
                case "particles_add" -> ParticleDescription.Material.PARTICLES_ADD;
                default -> ParticleDescription.Material.PARTICLES_BLEND;
            };
            String texRaw = getString(renderParams, "texture", "textures/particle/generic_0");
            texture = resolveTexturePath(texRaw);
        }

        return new ParticleDescription(identifier, material, texture, texW, texH);
    }

    private List<IParticleComponent> parseComponents(JsonObject obj) {
        List<IParticleComponent> components = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            IParticleComponent component = ParticleComponentRegistry.deserialize(entry.getKey(), entry.getValue());
            if (component != null) {
                components.add(component);
            }
        }
        return components;
    }
}
