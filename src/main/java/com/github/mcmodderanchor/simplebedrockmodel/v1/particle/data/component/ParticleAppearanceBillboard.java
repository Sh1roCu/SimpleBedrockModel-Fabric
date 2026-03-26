package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import javax.annotation.Nullable;

/**
 * 粒子外观组件 — Billboard 模式。
 * 对应 "minecraft:particle_appearance_billboard"。
 */
public record ParticleAppearanceBillboard(
        /** 粒子尺寸 [width, height]（Molang 表达式字符串） */
        String[] size,
        /** 朝向模式 */
        FaceCameraMode faceCameraMode,
        /** 静态 UV 配置（flipbook 为 null 时使用） */
        @Nullable UVConfig uv,
        /** Flipbook UV 动画配置，可为 null */
        @Nullable FlipbookConfig flipbook
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

    /**
     * Flipbook UV 动画配置。
     * @param baseUV 起始 UV [u, v]（Molang）
     * @param sizeUV 每帧尺寸 [w, h]（Molang）
     * @param stepUV 每帧步进 [du, dv]（Molang）
     * @param framesPerSecond 帧率
     * @param maxFrame 最大帧数（Molang）
     * @param stretchToLifetime 是否拉伸到粒子生命周期
     * @param loop 是否循环
     * @param textureWidth 纹理总宽度（像素）
     * @param textureHeight 纹理总高度（像素）
     */
    public record FlipbookConfig(
            String[] baseUV, String[] sizeUV, String[] stepUV,
            float framesPerSecond, String maxFrame,
            boolean stretchToLifetime, boolean loop,
            int textureWidth, int textureHeight
    ) {}
}
