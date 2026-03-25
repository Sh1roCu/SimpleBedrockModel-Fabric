package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.DynamicQueryBinding;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.MolangEngineHelper;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.MochaFunction;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.NumberValue;

/**
 * 粒子系统专用的 Molang 运行时环境。
 * <p>
 * 管理一个共享的 {@link MochaEngine}，并提供粒子/发射器相关的变量绑定。
 * 在每次求值前，通过 {@link #bindEmitter}/{@link #bindParticle} 更新当前上下文。
 */
public class ParticleMolangEnvironment {
    private final MochaEngine<?> engine;
    private final MutableObjectBinding variableStorage = new MutableObjectBinding();
    private final DynamicQueryBinding queryBinding = new DynamicQueryBinding();

    // 当前绑定的发射器/粒子状态（由外部在求值前设置）
    private float emitterAge;
    private float emitterLifetime;
    private float particleAge;
    private float particleLifetime;
    private int emitterRandom1;
    private int emitterRandom2;
    private int emitterRandom3;
    private int emitterRandom4;
    private float particleRandom1;
    private float particleRandom2;
    private float particleRandom3;
    private float particleRandom4;

    public ParticleMolangEnvironment() {
        this.engine = MolangEngineHelper.createEngine();
        MolangEngineHelper.registerQueryBinding(engine, queryBinding);
        engine.scope().set("variable", variableStorage);
        engine.scope().set("v", variableStorage);

        // 注册粒子相关的 variable 属性
        registerVariables();
    }

    private void registerVariables() {
        // 发射器变量
        variableStorage.set("emitter_age", NumberValue.of(0));
        variableStorage.set("emitter_lifetime", NumberValue.of(0));
        variableStorage.set("emitter_random_1", NumberValue.of(0));
        variableStorage.set("emitter_random_2", NumberValue.of(0));
        variableStorage.set("emitter_random_3", NumberValue.of(0));
        variableStorage.set("emitter_random_4", NumberValue.of(0));

        // 粒子变量
        variableStorage.set("particle_age", NumberValue.of(0));
        variableStorage.set("particle_lifetime", NumberValue.of(0));
        variableStorage.set("particle_random_1", NumberValue.of(0));
        variableStorage.set("particle_random_2", NumberValue.of(0));
        variableStorage.set("particle_random_3", NumberValue.of(0));
        variableStorage.set("particle_random_4", NumberValue.of(0));
    }

    /**
     * 绑定发射器状态到 Molang 变量。在发射器相关表达式求值前调用。
     */
    public void bindEmitter(float age, float lifetime, int r1, int r2, int r3, int r4) {
        this.emitterAge = age;
        this.emitterLifetime = lifetime;
        this.emitterRandom1 = r1;
        this.emitterRandom2 = r2;
        this.emitterRandom3 = r3;
        this.emitterRandom4 = r4;
        variableStorage.set("emitter_age", NumberValue.of(age));
        variableStorage.set("emitter_lifetime", NumberValue.of(lifetime));
        variableStorage.set("emitter_random_1", NumberValue.of(r1));
        variableStorage.set("emitter_random_2", NumberValue.of(r2));
        variableStorage.set("emitter_random_3", NumberValue.of(r3));
        variableStorage.set("emitter_random_4", NumberValue.of(r4));
    }

    /**
     * 绑定粒子状态到 Molang 变量。在粒子相关表达式求值前调用。
     */
    public void bindParticle(float age, float lifetime, float r1, float r2, float r3, float r4) {
        this.particleAge = age;
        this.particleLifetime = lifetime;
        this.particleRandom1 = r1;
        this.particleRandom2 = r2;
        this.particleRandom3 = r3;
        this.particleRandom4 = r4;
        variableStorage.set("particle_age", NumberValue.of(age));
        variableStorage.set("particle_lifetime", NumberValue.of(lifetime));
        variableStorage.set("particle_random_1", NumberValue.of(r1));
        variableStorage.set("particle_random_2", NumberValue.of(r2));
        variableStorage.set("particle_random_3", NumberValue.of(r3));
        variableStorage.set("particle_random_4", NumberValue.of(r4));
    }

    /**
     * 编译 Molang 表达式字符串为可执行函数。
     */
    public MochaFunction compile(String expression) {
        return engine.prepareEval(expression);
    }

    /**
     * 直接求值 Molang 表达式字符串。
     */
    public double eval(String expression) {
        return engine.eval(expression);
    }

    /**
     * 获取 variable 存储，允许外部设置自定义变量。
     */
    public MutableObjectBinding getVariableStorage() {
        return variableStorage;
    }

    public MochaEngine<?> getEngine() {
        return engine;
    }
}
