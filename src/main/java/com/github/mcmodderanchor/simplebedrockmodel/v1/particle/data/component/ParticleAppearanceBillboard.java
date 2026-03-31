package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 粒子外观组件 — Billboard 模式。
 * 对应 "minecraft:particle_appearance_billboard"。
 * @param size 粒子尺寸 [width, height]（Molang 表达式字符串）
 * @param faceCameraMode 朝向模式
 * @param uv 静态 UV 配置（flipbook 为 null 时使用）
 * @param flipbook Flipbook UV 动画配置，可为 null
 */
public record ParticleAppearanceBillboard(
        String[] size,
        FaceCameraMode faceCameraMode,
        @Nullable UVConfig uv,
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

    public static ParticleAppearanceBillboard fromJson(JsonObject obj) {
        // size
        String[] size = getMolangArray(obj, "size", 2, "0.1", "0.1");

        // facing_camera_mode
        String modeStr = getString(obj, "facing_camera_mode", "rotate_xyz");
        FaceCameraMode mode = parseFaceCameraMode(modeStr);

        // uv
        JsonObject uvObj = obj.has("uv") ? obj.getAsJsonObject("uv") : null;
        UVConfig uv = null;
        FlipbookConfig flipbook = null;

        if (uvObj != null && uvObj.has("flipbook")) {
            flipbook = parseFlipbookConfig(uvObj);
        } else {
            uv = parseUVConfig(uvObj);
        }

        return new ParticleAppearanceBillboard(size, mode, uv, flipbook);
    }

    private static FaceCameraMode parseFaceCameraMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "rotate_y" -> FaceCameraMode.ROTATE_Y;
            case "lookat_xyz" -> FaceCameraMode.LOOKAT_XYZ;
            case "lookat_y" -> FaceCameraMode.LOOKAT_Y;
            case "lookat_direction" -> FaceCameraMode.LOOKAT_DIRECTION;
            case "direction_x" -> FaceCameraMode.DIRECTION_X;
            case "direction_y" -> FaceCameraMode.DIRECTION_Y;
            case "direction_z" -> FaceCameraMode.DIRECTION_Z;
            case "emitter_transform_xy" -> FaceCameraMode.EMITTER_TRANSFORM_XY;
            case "emitter_transform_xz" -> FaceCameraMode.EMITTER_TRANSFORM_XZ;
            case "emitter_transform_yz" -> FaceCameraMode.EMITTER_TRANSFORM_YZ;
            default -> FaceCameraMode.ROTATE_XYZ;
        };
    }

    private static UVConfig parseUVConfig(@Nullable JsonObject uvObj) {
        if (uvObj == null) {
            return new UVConfig("0", "0", "1", "1", 1, 1);
        }
        int texW = uvObj.has("texture_width") ? uvObj.get("texture_width").getAsInt() : 1;
        int texH = uvObj.has("texture_height") ? uvObj.get("texture_height").getAsInt() : 1;

        // 静态 UV
        if (uvObj.has("uv")) {
            JsonElement uvElem = uvObj.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray arr = uvElem.getAsJsonArray();
                String u = molangFromElement(arr.get(0), "0");
                String v = molangFromElement(arr.get(1), "0");
                String w = "1";
                String h = "1";
                if (uvObj.has("uv_size")) {
                    JsonArray sizeArr = uvObj.getAsJsonArray("uv_size");
                    w = molangFromElement(sizeArr.get(0), "1");
                    h = molangFromElement(sizeArr.get(1), "1");
                }
                return new UVConfig(u, v, w, h, texW, texH);
            }
        }
        return new UVConfig("0", "0", String.valueOf(texW), String.valueOf(texH), texW, texH);
    }

    private static FlipbookConfig parseFlipbookConfig(JsonObject uvObj) {
        int texW = uvObj.has("texture_width") ? uvObj.get("texture_width").getAsInt() : 1;
        int texH = uvObj.has("texture_height") ? uvObj.get("texture_height").getAsInt() : 1;
        JsonObject fb = uvObj.getAsJsonObject("flipbook");
        String[] baseUV = getMolangArray(fb, "base_UV", 2, "0", "0");
        String[] sizeUV = getMolangArray(fb, "size_UV", 2, "1", "1");
        String[] stepUV = getMolangArray(fb, "step_UV", 2, "0", "0");
        float fps = fb.has("frames_per_second") ? fb.get("frames_per_second").getAsFloat() : 1;
        String maxFrame = getMolang(fb, "max_frame", "1");
        boolean stretch = getBoolean(fb, "stretch_to_lifetime", false);
        boolean loop = getBoolean(fb, "loop", false);
        return new FlipbookConfig(baseUV, sizeUV, stepUV, fps, maxFrame, stretch, loop, texW, texH);
    }
}
