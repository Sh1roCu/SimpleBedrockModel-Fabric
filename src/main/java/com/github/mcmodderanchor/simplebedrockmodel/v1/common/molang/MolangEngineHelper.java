package com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang;

import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;

/**
 * 创建和管理 MolangEngine 的工具类。
 */
public final class MolangEngineHelper {
    private MolangEngineHelper() {}

    /**
     * 创建一个标准的 MochaEngine，预配置 math 绑定。
     * query / variable 命名空间留空，由使用者通过 {@link #bindContext} 绑定。
     */
    public static MochaEngine<?> createEngine() {
        return MochaEngine.create(null, builder -> {
            builder.set("math", MolangMathBinding.INSTANCE);
        });
    }

    /**
     * 将 query binding 注册到 engine scope 上。
     * 通常在 engine 创建后调用一次。
     */
    public static void registerQueryBinding(MochaEngine<?> engine, DynamicQueryBinding queryBinding) {
        engine.scope().set("query", queryBinding);
        engine.scope().set("q", queryBinding);
    }

    /**
     * 在求值前调用，将 MolangContext 的 variable 存储绑定到 engine scope。
     * 这样每个 context 的 variable 命名空间是独立的。
     */
    public static void bindContext(MochaEngine<?> engine, MolangContext<?> context) {
        MutableObjectBinding vars = context.getVariableStorage();
        engine.scope().set("variable", vars);
        engine.scope().set("v", vars);
    }
}
