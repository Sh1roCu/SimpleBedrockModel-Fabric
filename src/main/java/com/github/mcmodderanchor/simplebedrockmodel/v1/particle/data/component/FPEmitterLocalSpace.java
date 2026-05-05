package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonObject;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.getBoolean;

/**
 * 第一人称发射器局部空间组件。对应 "sbm:fp_emitter_local_space"。
 * <p>
 * 在第一人称管线中覆盖 {@link EmitterLocalSpace} 的行为。
 * 当 {@code position=true} 时，粒子在发射时使用定位器位置，
 * 但发射后脱离定位器（不再跟随模型变换）。
 * <p>
 * {@code toWorld} 控制脱离后的粒子去向：
 * <ul>
 *   <li>{@code true}：投放到世界的 ParticleEngine，由原版管线管理（与 position=false 行为一致）</li>
 *   <li>{@code false}：留在第一人称管线内部，仍跟随摄像机但不跟随定位器</li>
 * </ul>
 *
 * @param position 粒子位置是否跟随发射器（FP 模式下为"发射时定位，发射后脱离"）
 * @param rotation 粒子是否跟随发射器旋转
 * @param velocity 是否将发射器速度添加到粒子初速度
 * @param toWorld  脱离定位器后是否投放到世界（仅 position=true 时有效）
 */
public record FPEmitterLocalSpace(boolean position, boolean rotation, boolean velocity, boolean toWorld)
        implements IEmitterComponentDefinition, IEmitterComponent {

    @Override
    public int order() { return 410; }

    public static FPEmitterLocalSpace fromJson(JsonObject obj) {
        return new FPEmitterLocalSpace(
                getBoolean(obj, "position", false),
                getBoolean(obj, "rotation", false),
                getBoolean(obj, "velocity", false),
                getBoolean(obj, "to_world", false));
    }
}
