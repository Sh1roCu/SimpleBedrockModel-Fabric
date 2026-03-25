package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 对应基岩版 particle_effect JSON 的顶层定义。
 * <pre>
 * {
 *   "format_version": "1.10.0",
 *   "particle_effect": {
 *     "description": { ... },
 *     "components": { ... }
 *   }
 * }
 * </pre>
 */
public class ParticleEffectDefinition {
    private final ResourceLocation identifier;
    private final ParticleDescription description;
    private final List<IParticleComponent> components;

    public ParticleEffectDefinition(ResourceLocation identifier, ParticleDescription description, List<IParticleComponent> components) {
        this.identifier = identifier;
        this.description = description;
        this.components = components;
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public ParticleDescription getDescription() {
        return description;
    }

    public List<IParticleComponent> getComponents() {
        return components;
    }

    /**
     * 按类型查找第一个匹配的组件。
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IParticleComponent> T findComponent(Class<T> type) {
        for (IParticleComponent c : components) {
            if (type.isInstance(c)) return (T) c;
        }
        return null;
    }
}
