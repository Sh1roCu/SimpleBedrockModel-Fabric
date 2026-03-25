package com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation;

import com.maydaymemory.mae.basic.BaseKeyframe;
import com.maydaymemory.mae.basic.InterpolatableKeyframe;
import com.maydaymemory.mae.basic.Interpolator;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import team.unnamed.mocha.runtime.MochaFunction;

/**
 * 支持 Molang 表达式的 Vector3f 关键帧。
 * 每次获取 pre/post 值时，会实时求值 Molang 表达式。
 */
public class MolangVector3fKeyframe extends BaseKeyframe<Vector3fc> implements InterpolatableKeyframe<Vector3fc> {
    private final MochaFunction[] preFunctions;
    private final MochaFunction[] postFunctions;
    private final Interpolator<Vector3fc> interpolator;

    /**
     * @param timeS         关键帧时间（秒）
     * @param preFunctions  pre 值的 3 个 Molang 函数 (x, y, z)
     * @param postFunctions post 值的 3 个 Molang 函数 (x, y, z)
     * @param interpolator  插值器
     */
    public MolangVector3fKeyframe(float timeS,
                                  MochaFunction[] preFunctions,
                                  MochaFunction[] postFunctions,
                                  Interpolator<Vector3fc> interpolator) {
        super(timeS);
        this.preFunctions = preFunctions;
        this.postFunctions = postFunctions;
        this.interpolator = interpolator;
    }

    @Override
    public Vector3fc getPre() {
        return evaluate(preFunctions);
    }

    @Override
    public Vector3fc getPost() {
        return evaluate(postFunctions);
    }

    @Override
    public Interpolator<Vector3fc> getInterpolator() {
        return interpolator;
    }

    @Override
    public Vector3fc getValue() {
        return getPre();
    }

    private static Vector3fc evaluate(MochaFunction[] functions) {
        return new Vector3f(
                (float) functions[0].evaluate(),
                (float) functions[1].evaluate(),
                (float) functions[2].evaluate()
        );
    }
}
