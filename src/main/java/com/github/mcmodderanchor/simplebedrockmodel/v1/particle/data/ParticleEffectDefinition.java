package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

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
    private final Map<Class<? extends IParticleComponent>, IParticleComponent> componentMap;

    public ParticleEffectDefinition(ResourceLocation identifier, ParticleDescription description, List<IParticleComponent> components) {
        this.identifier = identifier;
        this.description = description;
        this.componentMap = buildComponentMap(components);
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public ParticleDescription getDescription() {
        return description;
    }

    /**
     * 按类型查找组件。O(1) 查找。
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IParticleComponent> T findComponent(Class<T> type) {
        return (T) componentMap.get(type);
    }

    /**
     * 构建组件类型映射。
     * <p>
     * 对于 sealed interface（如 {@link EmitterRate}、{@link EmitterLifetime} 等），
     * 同时注册具体实现类和父接口两个 key，使得通过父接口也能查找到组件。
     */
    private static Map<Class<? extends IParticleComponent>, IParticleComponent> buildComponentMap(List<IParticleComponent> components) {
        Map<Class<? extends IParticleComponent>, IParticleComponent> map = new HashMap<>();
        for (IParticleComponent c : components) {
            Class<? extends IParticleComponent> clazz = c.getClass();
            map.put(clazz, c);
            // 注册 sealed interface 父类型，使 findComponent(EmitterRate.class) 等调用能命中
            for (Class<?> iface : clazz.getInterfaces()) {
                if (IParticleComponent.class.isAssignableFrom(iface) && iface != IParticleComponent.class && iface != IEmitterComponent.class) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IParticleComponent> parentType = (Class<? extends IParticleComponent>) iface;
                    map.putIfAbsent(parentType, c);
                }
            }
            // 处理 record 实现 sealed interface 的情况（enclosing class）
            Class<?> enclosing = clazz.getEnclosingClass();
            if (enclosing != null && IParticleComponent.class.isAssignableFrom(enclosing) && enclosing != IParticleComponent.class && enclosing != IEmitterComponent.class) {
                @SuppressWarnings("unchecked")
                Class<? extends IParticleComponent> parentType = (Class<? extends IParticleComponent>) enclosing;
                map.putIfAbsent(parentType, c);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
