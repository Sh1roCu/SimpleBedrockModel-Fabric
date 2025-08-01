package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.BasicAnimation;
import com.maydaymemory.mae.basic.ClipChannel;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import net.minecraft.resources.ResourceLocation;

public class BedrockAnimation extends BasicAnimation {
    public static final int SOUND_CHANNEL_INDEX = 0;

    public BedrockAnimation(String name) {
        super(name, new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    }

    public void setSoundChannel(ClipChannel<ResourceLocation> channel) {
        this.setClipChannel(SOUND_CHANNEL_INDEX, channel);
    }
}
