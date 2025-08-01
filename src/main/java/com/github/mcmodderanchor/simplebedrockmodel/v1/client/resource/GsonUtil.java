package com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.AnimationKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.CubesItem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.SoundEffectKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource.serialize.AnimationKeyframesSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource.serialize.SoundEffectKeyframesSerializer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.resource.serialize.Vector3fSerializer;
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
            .registerTypeAdapter(SoundEffectKeyframes.class, new SoundEffectKeyframesSerializer())
            .create();
}
