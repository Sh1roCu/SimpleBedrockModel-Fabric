package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 粒子颜色/着色组件。对应 "minecraft:particle_appearance_tinting"。
 */
public sealed interface ParticleAppearanceTinting extends IParticleComponent {

    /**
     * 静态颜色。
     * @param r 红色通道（Molang，0~1）
     * @param g 绿色通道（Molang，0~1）
     * @param b 蓝色通道（Molang，0~1）
     * @param a 透明度通道（Molang，0~1），可为 null 表示 1.0
     */
    record StaticColor(String r, String g, String b, @Nullable String a) implements ParticleAppearanceTinting {}

    /**
     * 渐变颜色。
     * @param interpolant 插值因子（Molang）
     * @param stops 每个颜色对应的位置值（与 colors 一一对应）
     * @param colors 颜色数组，每个元素为 [r, g, b, a] 的 float 数组
     */
    record GradientColor(String interpolant, float[] stops, float[][] colors) implements ParticleAppearanceTinting {}
}
