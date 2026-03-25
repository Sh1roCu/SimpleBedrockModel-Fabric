package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 发射器形状组件，决定粒子的初始位置和方向。
 */
public sealed interface EmitterShape extends IEmitterComponent {

    /**
     * 点发射。对应 "minecraft:emitter_shape_point"。
     * @param offset 偏移量 [x, y, z]（Molang 表达式字符串）
     * @param direction 初始方向 [x, y, z]（Molang），null 表示 "outwards"
     */
    record Point(String[] offset, @Nullable String[] direction) implements EmitterShape {}

    /**
     * 球体发射。对应 "minecraft:emitter_shape_sphere"。
     * @param offset 中心偏移 [x, y, z]
     * @param radius 半径（Molang）
     * @param surfaceOnly 是否仅在表面发射
     * @param direction 方向 [x, y, z]，null 表示 "outwards"
     */
    record Sphere(String[] offset, String radius, boolean surfaceOnly, @Nullable String[] direction) implements EmitterShape {}

    /**
     * 盒体发射。对应 "minecraft:emitter_shape_box"。
     * @param offset 中心偏移 [x, y, z]
     * @param halfDimensions 半尺寸 [x, y, z]
     * @param surfaceOnly 是否仅在表面发射
     * @param direction 方向 [x, y, z]，null 表示 "outwards"
     */
    record Box(String[] offset, String[] halfDimensions, boolean surfaceOnly, @Nullable String[] direction) implements EmitterShape {}
}
