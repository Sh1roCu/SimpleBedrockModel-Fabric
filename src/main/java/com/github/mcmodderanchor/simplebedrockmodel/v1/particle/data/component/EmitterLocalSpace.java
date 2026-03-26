package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 发射器局部空间组件。对应 "minecraft:emitter_local_space"。
 * <p>
 * 控制粒子是否在发射器的局部坐标系中模拟。
 *
 * @param position 粒子位置是否跟随发射器
 * @param rotation 粒子是否跟随发射器旋转
 * @param velocity 是否将发射器速度添加到粒子初速度
 */
public record EmitterLocalSpace(boolean position, boolean rotation, boolean velocity) implements IEmitterComponent {
}
