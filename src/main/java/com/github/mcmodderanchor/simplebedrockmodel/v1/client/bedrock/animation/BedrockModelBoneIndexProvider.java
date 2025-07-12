package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;

import java.util.HashMap;

public class BedrockModelBoneIndexProvider implements BoneIndexProvider{
    private final HashMap<String, BedrockBone> boneMap;

    public BedrockModelBoneIndexProvider(BedrockModel model) {
        this.boneMap = model.getBoneMap();
    }

    @Override
    public int getIndex(String boneName) {
        BedrockBone bone = boneMap.get(boneName);
        return bone == null ? -1 : bone.index;
    }
}
