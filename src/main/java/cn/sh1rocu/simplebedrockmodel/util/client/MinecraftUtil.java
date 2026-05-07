package cn.sh1rocu.simplebedrockmodel.util.client;

import cn.sh1rocu.simplebedrockmodel.mixin.client.accessor.MinecraftAccessor;
import net.minecraft.client.Minecraft;

public class MinecraftUtil {

    public static float getPartialTick() {
        Minecraft mc = Minecraft.getInstance();
        return mc.isPaused() ? ((MinecraftAccessor) mc).sbm$getPausePartialTick() : mc.getFrameTime();
    }
}
