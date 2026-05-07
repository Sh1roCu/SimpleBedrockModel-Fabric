package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render;

import cn.sh1rocu.simplebedrockmodel.api.event.ViewportEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CameraStateCache {

    private static float cameraRoll;

    private CameraStateCache() {
    }

    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        cameraRoll = event.getRoll();
    }

    /**
     * 获取当前帧的 camera roll（度）。
     */
    public static float getCameraRollDegrees() {
        return cameraRoll;
    }

    /**
     * 获取当前帧的 camera roll（弧度）。
     */
    public static float getCameraRollRadians() {
        return (float) Math.toRadians(cameraRoll);
    }
}
