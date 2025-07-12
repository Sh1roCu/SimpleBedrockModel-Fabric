package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.BasicAnimation;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;

public class BedrockAnimation extends BasicAnimation {
    public BedrockAnimation(String name) {
        super(name, new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);
    }
}
