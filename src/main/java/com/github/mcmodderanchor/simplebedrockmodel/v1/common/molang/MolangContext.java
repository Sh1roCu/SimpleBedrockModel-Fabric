package com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang;

import team.unnamed.mocha.runtime.value.MutableObjectBinding;

import javax.annotation.Nullable;

/**
 * Molang 求值上下文。持有当前关联对象的可变引用和独立的 variable 存储。
 * <p>
 * 使用方式：每个需要独立 Molang 变量空间的实例（如每个实体）持有自己的 MolangContext，
 * 在求值前调用 {@link #bind()} 将当前 context 绑定到共享的 engine scope 上。
 *
 * @param <T> 关联对象的类型（如 Entity、LivingEntity 等）
 */
public class MolangContext<T> {
    private @Nullable T entity;
    private final MutableObjectBinding variableStorage = new MutableObjectBinding();

    public MolangContext() {
    }

    public MolangContext(@Nullable T entity) {
        this.entity = entity;
    }

    @Nullable
    public T getEntity() {
        return entity;
    }

    public void setEntity(@Nullable T entity) {
        this.entity = entity;
    }

    /**
     * 获取此 context 独立的 variable 存储。
     * 对应 Molang 中的 variable / v 命名空间。
     */
    public MutableObjectBinding getVariableStorage() {
        return variableStorage;
    }
}
