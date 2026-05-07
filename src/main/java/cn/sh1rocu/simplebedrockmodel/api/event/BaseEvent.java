package cn.sh1rocu.simplebedrockmodel.api.event;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import net.minecraft.resources.ResourceLocation;

public class BaseEvent {
    public static final ResourceLocation HIGHEST = SimpleBedrockModel.modLoc("highest");
    public static final ResourceLocation HIGH = SimpleBedrockModel.modLoc("high");
    public static final ResourceLocation LOW = SimpleBedrockModel.modLoc("low");
    public static final ResourceLocation LOWEST = SimpleBedrockModel.modLoc("lowest");

    protected boolean isCanceled = false;
}