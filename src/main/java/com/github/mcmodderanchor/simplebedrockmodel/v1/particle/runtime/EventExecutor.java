package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 粒子事件执行引擎。
 * <p>
 * 根据 {@link IEventNode} 的具体类型分发执行逻辑。
 */
public final class EventExecutor {
    private static final Random RANDOM = new Random();

    private EventExecutor() {}

    /**
     * 事件执行上下文。
     *
     * @param emitter  所属发射器实例
     * @param molang   Molang 环境
     * @param level    客户端世界（用于播放音效、创建子发射器）
     * @param position 当前位置（用于音效定位和子发射器位置）
     */
    public record EventContext(
            ParticleEmitterInstance emitter,
            ParticleMolangEnvironment molang,
            @Nullable ClientLevel level,
            @Nullable Vec3 position
    ) {}

    /**
     * 按事件名称列表触发事件。
     * <p>
     * 从 {@link ParticleEffectDefinition#getEvents()} 中查找事件名对应的节点列表并逐一执行。
     */
    public static void fireEvents(List<String> eventNames, ParticleEffectDefinition definition, EventContext ctx) {
        if (eventNames.isEmpty()) return;
        Map<String, List<IEventNode>> allEvents = definition.getEvents();
        if (allEvents.isEmpty()) return;

        for (String name : eventNames) {
            List<IEventNode> nodes = allEvents.get(name);
            if (nodes == null) continue;
            for (IEventNode node : nodes) {
                execute(node, ctx);
            }
        }
    }

    /**
     * 执行单个事件节点。
     */
    public static void execute(IEventNode node, EventContext ctx) {
        if (node instanceof EventSequence seq) {
            for (IEventNode child : seq.nodes()) {
                execute(child, ctx);
            }
        } else if (node instanceof EventRandomize randomize) {
            executeRandomize(randomize, ctx);
        } else if (node instanceof ParticleEffectEvent effect) {
            executeParticleEffect(effect, ctx);
        } else if (node instanceof SoundEffectEvent sound) {
            executeSoundEffect(sound, ctx);
        } else if (node instanceof MolangExpressionEvent expr) {
            executeMolangExpression(expr, ctx);
        } else if (node instanceof EventLog log) {
            executeLog(log, ctx);
        }
    }

    private static void executeRandomize(EventRandomize randomize, EventContext ctx) {
        List<EventRandomize.WeightedEntry> entries = randomize.entries();
        if (entries.isEmpty()) return;

        float totalWeight = 0;
        for (EventRandomize.WeightedEntry entry : entries) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) return;

        float roll = RANDOM.nextFloat() * totalWeight;
        float cumulative = 0;
        for (EventRandomize.WeightedEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                execute(entry.node(), ctx);
                return;
            }
        }
        // 浮点精度兜底：执行最后一个
        execute(entries.get(entries.size() - 1).node(), ctx);
    }

    private static void executeParticleEffect(ParticleEffectEvent effect, EventContext ctx) {
        if (ctx.level == null || ctx.position == null) return;

        // 执行 pre_effect_expression
        if (effect.preEffectExpression() != null) {
            try {
                MolangExpression compiled = ctx.molang.compile(effect.preEffectExpression());
                compiled.evaluate(ctx.molang.getContext());
            } catch (Exception e) {
                SimpleBedrockModel.LOGGER.warn("Failed to evaluate pre_effect_expression: {}", effect.preEffectExpression(), e);
            }
        }

        // 查找子粒子效果定义
        ResourceLocation effectId = new ResourceLocation(effect.effect());
        ParticleEffectDefinition childDef = ParticleDefinitionLoader.getInstance().getDefinition(effectId);
        if (childDef == null) {
            SimpleBedrockModel.LOGGER.debug("Particle effect event references unknown effect: {}", effect.effect());
            return;
        }

        WorldEmitterManager.getInstance().addEmitter(ctx.level, ctx.position, Vec3.ZERO, childDef);
    }

    private static void executeSoundEffect(SoundEffectEvent sound, EventContext ctx) {
        if (ctx.level == null || ctx.position == null) return;
        String eventName = sound.eventName();
        if (eventName == null || eventName.isEmpty()) return;

        try {
            ResourceLocation soundId = new ResourceLocation(eventName);
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            ctx.level.playLocalSound(
                    ctx.position.x, ctx.position.y, ctx.position.z,
                    soundEvent, SoundSource.AMBIENT, 1.0f, 1.0f, false
            );
        } catch (Exception e) {
            SimpleBedrockModel.LOGGER.warn("Failed to play sound effect: {}", eventName, e);
        }
    }

    private static void executeMolangExpression(MolangExpressionEvent expr, EventContext ctx) {
        String exprStr = expr.expression();
        if (exprStr == null || exprStr.isEmpty()) return;
        try {
            MolangExpression compiled = ctx.molang.compile(exprStr);
            compiled.evaluate(ctx.molang.getContext());
        } catch (Exception e) {
            SimpleBedrockModel.LOGGER.warn("Failed to evaluate event molang expression: {}", exprStr, e);
        }
    }

    private static void executeLog(EventLog log, EventContext ctx) {
        SimpleBedrockModel.LOGGER.info("[ParticleEvent] {}", log.message());
    }
}
