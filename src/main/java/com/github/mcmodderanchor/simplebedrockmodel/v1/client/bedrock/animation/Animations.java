package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.AnimationBone;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.AnimationKeyframes;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockAnimationFile;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo.BedrockAnimationPOJO;
import com.maydaymemory.mae.basic.*;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Animations {
    public static BedrockAnimation createAnimation(String name, BedrockAnimationPOJO pojo, BoneIndexProvider indexProvider) {
        BedrockAnimation animation = new BedrockAnimation(name);
        if (pojo.getBones() != null) {
            for (Map.Entry<String, AnimationBone> entry1 : pojo.getBones().entrySet()) {
                int boneIndex = indexProvider.getIndex(entry1.getKey());
                AnimationBone bone = entry1.getValue();
                if (boneIndex >= 0) {
                    // 这里导出成基岩版模型之后位移 x 轴会逆转（yz 平面对称变成左手系等效位移），现在我们逆转回来
                    ArrayInterpolatableChannel<Vector3fc> translationChannel = parseChannel(bone.getPosition(), false, -1, 1, 1);
                    // 这里导出成基岩版模型之后旋转会 z 轴对称变成左手系等效旋转，现在我们逆转回来
                    ArrayInterpolatableChannel<Vector3fc> rotationChannel = parseChannel(bone.getRotation(), true, -1, -1, 1);
                    ArrayInterpolatableChannel<Vector3fc> scaleChannel = parseChannel(bone.getScale(), false, 1, 1, 1);
                    animation.setTranslationChannel(boneIndex, translationChannel);
                    animation.setRotationChannel(boneIndex, rotationChannel);
                    animation.setScaleChannel(boneIndex, scaleChannel);
                }
            }
        }
        return animation;
    }

    public static List<BedrockAnimation> createAnimation(BedrockAnimationFile pojo, BoneIndexProvider indexProvider) {
        List<BedrockAnimation> animations = new ArrayList<>();
        if (pojo.getAnimations() != null) {
            for (Map.Entry<String, BedrockAnimationPOJO> entry : pojo.getAnimations().entrySet()) {
                animations.add(createAnimation(entry.getKey(), entry.getValue(), indexProvider));
            }
        }
        return animations;
    }

    private static ArrayInterpolatableChannel<Vector3fc> parseChannel(AnimationKeyframes keyframes, boolean toRadian,
                                                                      float x, float y, float z) {
        if (keyframes == null) {
            return null;
        }
        ArrayList<InterpolatableKeyframe<Vector3fc>> array = new ArrayList<>();
        keyframes.getKeyframes().forEach((timeS, keyframe) -> {
            array.add(parseKeyframe((float)(double)timeS, keyframe, toRadian, x, y, z));
        });
        return new ArrayInterpolatableChannel<>(array);
    }


    private static Vector3fKeyframe parseKeyframe(float timeS, AnimationKeyframes.Keyframe keyframe, boolean toRadian,
                                                  float x, float y, float z) {
        Interpolator<Vector3fc> interpolator;
        Vector3f pre, post;
        if (keyframe.getData() != null) {
            pre = post = keyframe.getData();
        } else {
            pre = keyframe.getPre() == null ? keyframe.getPost() : keyframe.getPre();
            post = keyframe.getPost() == null ? keyframe.getPre() : keyframe.getPost();
        }
        if (pre == post) {
            pre.mul(x, y, z);
        } else {
            pre.mul(x, y, z);
            post.mul(x, y, z);
        }
        if (toRadian) {
            if (pre == post) {
                pre = post = new Vector3f((float) Math.toRadians(pre.x()), (float) Math.toRadians(pre.y()), (float) Math.toRadians(pre.z()));
            } else {
                pre = new Vector3f((float) Math.toRadians(pre.x()), (float) Math.toRadians(pre.y()), (float) Math.toRadians(pre.z()));
                post = new Vector3f((float) Math.toRadians(post.x()), (float) Math.toRadians(post.y()), (float) Math.toRadians(post.z()));
            }
        }
        if ("catmullrom".equals(keyframe.getLerpMode())) {
            interpolator = Vector3fCubicSplineInterpolator.INSTANCE;
        } else {
            interpolator = Vector3fLinearInterpolator.INSTANCE;
        }
        return new Vector3fKeyframe(timeS, pre, post, interpolator);
    }
}
