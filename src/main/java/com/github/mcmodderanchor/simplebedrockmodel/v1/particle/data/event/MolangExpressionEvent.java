package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.event;

/**
 * Molang 表达式事件节点。触发时执行 Molang 表达式。
 *
 * @param expression Molang 表达式字符串（运行时编译执行）
 */
public record MolangExpressionEvent(String expression) implements IEventNode {
}
