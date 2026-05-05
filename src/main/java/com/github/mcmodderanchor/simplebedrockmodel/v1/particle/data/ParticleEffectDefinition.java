package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.shape.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.curve.ParticleCurve;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event.IEventNode;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * 对应基岩版 particle_effect JSON 的顶层定义。
 * <pre>
 * {
 *   "format_version": "1.10.0",
 *   "particle_effect": {
 *     "description": { ... },
 *     "curves": { ... },
 *     "events": { ... },
 *     "components": { ... }
 *   }
 * }
 * </pre>
 */
public class ParticleEffectDefinition {
    private final ResourceLocation identifier;
    private final ParticleDescription description;
    private final Map<Class<? extends IComponent>, IComponent> componentMap;

    // === 新增：预设层 ===
    private final EmitterPreset emitterPreset;
    private final ParticlePreset particlePreset;

    // 预缓存的常用组件引用（标记 Deprecated，请用 emitterPreset().find() / particlePreset().find()）
    @Deprecated @Nullable
    private final EmitterShape shape;

    @Deprecated @Nullable
    private final ParticleInitialSpeed initialSpeed;

    @Deprecated @Nullable
    private final ParticleLifetimeExpression lifetimeExpression;

    @Deprecated @Nullable
    private final ParticleAppearanceBillboard billboard;

    @Deprecated @Nullable
    private final ParticleInitialSpin initialSpin;

    @Deprecated @Nullable
    private final ParticleInitialization initialization;

    // 曲线和事件（顶层字段，非组件）
    private final Map<String, ParticleCurve> curves;
    private final Map<String, List<IEventNode>> events;

    public ParticleEffectDefinition(ResourceLocation identifier, ParticleDescription description,
                                    List<IComponent> components,
                                    @Nullable Map<String, ParticleCurve> curves,
                                    @Nullable Map<String, List<IEventNode>> events) {
        this.identifier = identifier;
        this.description = description;
        this.componentMap = buildComponentMap(components);
        this.curves = curves != null ? curves : Map.of();
        this.events = events != null ? events : Map.of();

        // 分离 emitter 和 particle 定义组件
        List<IEmitterComponentDefinition> emitterComponents = new ArrayList<>();
        List<IParticleComponentDefinition> particleComponents = new ArrayList<>();
        for (IComponent c : components) {
            if (c instanceof IEmitterComponentDefinition ec) {
                emitterComponents.add(ec);
            } else if (c instanceof IParticleComponentDefinition pc) {
                particleComponents.add(pc);
            }
        }

        // 构建预设
        this.emitterPreset = new EmitterPreset(emitterComponents);
        this.particlePreset = new ParticlePreset(particleComponents);

        // 旧字段保留向后兼容（已删除的 sealed interface 类型不再可用）
        this.shape = emitterPreset.find(EmitterShape.class);
        this.initialSpeed = particlePreset.find(ParticleInitialSpeed.class);
        this.lifetimeExpression = particlePreset.find(ParticleLifetimeExpression.class);
        this.billboard = particlePreset.find(ParticleAppearanceBillboard.class);
        this.initialSpin = particlePreset.find(ParticleInitialSpin.class);
        this.initialization = particlePreset.find(ParticleInitialization.class);
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public ParticleDescription getDescription() {
        return description;
    }

    /** 获取发射器预设 */
    public EmitterPreset emitterPreset() { return emitterPreset; }

    /** 获取粒子预设 */
    public ParticlePreset particlePreset() { return particlePreset; }

    @Deprecated @Nullable
    public EmitterShape getShape() {
        return shape;
    }

    @Deprecated @Nullable
    public ParticleInitialSpeed getInitialSpeed() {
        return initialSpeed;
    }

    @Deprecated @Nullable
    public ParticleLifetimeExpression getLifetimeExpression() {
        return lifetimeExpression;
    }

    @Deprecated @Nullable
    public ParticleAppearanceBillboard getBillboard() {
        return billboard;
    }

    @Deprecated @Nullable
    public ParticleInitialSpin getInitialSpin() {
        return initialSpin;
    }

    @Deprecated @Nullable
    public ParticleInitialization getInitialization() {
        return initialization;
    }

    public Map<String, ParticleCurve> getCurves() {
        return curves;
    }

    public Map<String, List<IEventNode>> getEvents() {
        return events;
    }

    /**
     * 按类型查找组件
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IComponent> T findComponent(Class<T> type) {
        return (T) componentMap.get(type);
    }

    /**
     * 构建组件类型映射。
     * <p>
     * 对于 sealed interface（如 {@link EmitterRate}、{@link EmitterLifetime} 等），
     * 同时注册具体实现类和父接口两个 key，使得通过父接口也能查找到组件。
     */
    private static Map<Class<? extends IComponent>, IComponent> buildComponentMap(List<IComponent> components) {
        Map<Class<? extends IComponent>, IComponent> map = new HashMap<>();
        for (IComponent c : components) {
            Class<? extends IComponent> clazz = c.getClass();
            map.put(clazz, c);
            for (Class<?> iface : clazz.getInterfaces()) {
                if (IComponent.class.isAssignableFrom(iface)
                        && iface != IComponent.class
                        && iface != IParticleComponent.class
                        && iface != IEmitterComponent.class
                        && iface != IParticleComponentDefinition.class
                        && iface != IEmitterComponentDefinition.class) {
                    @SuppressWarnings("unchecked")
                    Class<? extends IComponent> parentType = (Class<? extends IComponent>) iface;
                    map.putIfAbsent(parentType, c);
                }
            }
            Class<?> enclosing = clazz.getEnclosingClass();
            if (enclosing != null
                    && IComponent.class.isAssignableFrom(enclosing)
                    && enclosing != IComponent.class
                    && enclosing != IParticleComponent.class
                    && enclosing != IEmitterComponent.class
                    && enclosing != IParticleComponentDefinition.class
                    && enclosing != IEmitterComponentDefinition.class) {
                @SuppressWarnings("unchecked")
                Class<? extends IComponent> parentType = (Class<? extends IComponent>) enclosing;
                map.putIfAbsent(parentType, c);
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
