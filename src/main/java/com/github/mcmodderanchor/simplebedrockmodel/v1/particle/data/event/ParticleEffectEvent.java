package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

import org.jetbrains.annotations.Nullable;

/**
 * 粒子效果事件节点。触发时创建子发射器。
 *
 * @param effect              子粒子效果标识符
 * @param type                发射器类型
 * @param preEffectExpression 创建子发射器前执行的 Molang 表达式（原始字符串，运行时编译）
 */
public record ParticleEffectEvent(
        String effect,
        Type type,
        @Nullable String preEffectExpression
) implements IEventNode {

    public enum Type {
        EMITTER,
        EMITTER_BOUND,
        PARTICLE,
        PARTICLE_WITH_VELOCITY;

        public static Type fromString(String str) {
            return switch (str.toLowerCase()) {
                case "emitter_bound" -> EMITTER_BOUND;
                case "particle" -> PARTICLE;
                case "particle_with_velocity" -> PARTICLE_WITH_VELOCITY;
                default -> EMITTER;
            };
        }
    }
}
