package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

/**
 * 粒子外观组件 — Billboard 模式。
 * 对应 "minecraft:particle_appearance_billboard"。
 */
public record ParticleAppearanceBillboard(
        /** 粒子尺寸 [width, height]（Molang 表达式字符串） */
        String[] size,
        /** 朝向模式 */
        FaceCameraMode faceCameraMode,
        /** UV 配置 */
        UVConfig uv
) implements IParticleComponent {

    public enum FaceCameraMode {
        ROTATE_XYZ,
        ROTATE_Y,
        LOOKAT_XYZ,
        LOOKAT_Y,
        LOOKAT_DIRECTION,
        DIRECTION_X,
        DIRECTION_Y,
        DIRECTION_Z,
        EMITTER_TRANSFORM_XY,
        EMITTER_TRANSFORM_XZ,
        EMITTER_TRANSFORM_YZ
    }

    /**
     * 静态 UV 配置。
     * @param u U 偏移（Molang）
     * @param v V 偏移（Molang）
     * @param width UV 宽度（Molang）
     * @param height UV 高度（Molang）
     * @param textureWidth 纹理总宽度（像素）
     * @param textureHeight 纹理总高度（像素）
     */
    public record UVConfig(
            String u, String v,
            String width, String height,
            int textureWidth, int textureHeight
    ) {}
}
