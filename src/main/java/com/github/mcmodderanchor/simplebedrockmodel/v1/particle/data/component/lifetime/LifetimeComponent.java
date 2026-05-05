package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.lifetime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.IEmitterComponentDefinition;

/** 标记接口：所有 lifetime 组件实现此接口 */
public interface LifetimeComponent extends IEmitterComponentDefinition {
    /** 活跃时间表达式。Looping/Once 从 record 字段继承，Expression 返回 Float.MAX_VALUE。 */
    MolangExpression activeTime();
}
