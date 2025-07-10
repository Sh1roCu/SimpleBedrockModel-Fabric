package com.github.tartaricacid.simplebedrockmodel.client.resource;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.AnimationKeyframes;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.CubesItem;
import com.github.tartaricacid.simplebedrockmodel.client.resource.serialize.AnimationKeyframesSerializer;
import com.github.tartaricacid.simplebedrockmodel.client.resource.serialize.Vector3fSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

public class GsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(AnimationKeyframes.class, new AnimationKeyframesSerializer())
            .create();
}
