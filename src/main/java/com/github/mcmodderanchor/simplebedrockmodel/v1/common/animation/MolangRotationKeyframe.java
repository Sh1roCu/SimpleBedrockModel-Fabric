package com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation;

import com.maydaymemory.mae.basic.BaseKeyframe;
import com.maydaymemory.mae.basic.InterpolatableKeyframe;
import com.maydaymemory.mae.basic.Interpolator;
import com.maydaymemory.mae.basic.Rotation;
import org.joml.Vector3f;
import team.unnamed.mocha.runtime.MochaFunction;

/**
 * 支持 Molang 表达式的 Rotation 关键帧。
 * 每次获取 pre/post 值时，会实时求值 Molang 表达式并转换为弧度。
 */
public class MolangRotationKeyframe extends BaseKeyframe<Rotation> implements InterpolatableKeyframe<Rotation> {
    private static final float DEGREE_TO_RADIAN = (float) (Math.PI / 180);

    private final MochaFunction[] preFunctions;
    private final MochaFunction[] postFunctions;
    private final float mulX, mulY, mulZ;
    private final Interpolator<Rotation> interpolator;

    /**
     * @param timeS         关键帧时间（秒）
     * @param preFunctions  pre 值的 3 个 Molang 函数 (x, y, z)，求值结果为角度
     * @param postFunctions post 值的 3 个 Molang 函数 (x, y, z)，求值结果为角度
     * @param mulX          x 轴符号乘数（用于坐标系转换）
     * @param mulY          y 轴符号乘数
     * @param mulZ          z 轴符号乘数
     * @param interpolator  插值器
     */
    public MolangRotationKeyframe(float timeS,
                                  MochaFunction[] preFunctions,
                                  MochaFunction[] postFunctions,
                                  float mulX, float mulY, float mulZ,
                                  Interpolator<Rotation> interpolator) {
        super(timeS);
        this.preFunctions = preFunctions;
        this.postFunctions = postFunctions;
        this.mulX = mulX;
        this.mulY = mulY;
        this.mulZ = mulZ;
        this.interpolator = interpolator;
    }

    @Override
    public Rotation getPre() {
        return evaluate(preFunctions);
    }

    @Override
    public Rotation getPost() {
        return evaluate(postFunctions);
    }

    @Override
    public Interpolator<Rotation> getInterpolator() {
        return interpolator;
    }

    @Override
    public Rotation getValue() {
        return getPre();
    }

    private Rotation evaluate(MochaFunction[] functions) {
        float x = (float) functions[0].evaluate() * mulX * DEGREE_TO_RADIAN;
        float y = (float) functions[1].evaluate() * mulY * DEGREE_TO_RADIAN;
        float z = (float) functions[2].evaluate() * mulZ * DEGREE_TO_RADIAN;
        return new Rotation(new Vector3f(x, y, z));
    }
}
