package com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.pojo;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import org.joml.Vector3f;

import javax.annotation.Nullable;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class AnimationKeyframes {
    private final Double2ObjectRBTreeMap<Keyframe> keyframes;

    public AnimationKeyframes(Double2ObjectRBTreeMap<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public Double2ObjectRBTreeMap<Keyframe> getKeyframes() {
        return keyframes;
    }

    public static class Keyframe {
        private final Vector3f pre;
        private final Vector3f post;
        private final Vector3f data;
        private final String lerpMode;

        public Keyframe (@Nullable Vector3f pre, @Nullable Vector3f post,
                         @Nullable Vector3f data, @Nullable String lerpMode) {
            this.pre = pre;
            this.post = post;
            this.data = data;
            this.lerpMode = lerpMode;
        }

        public Vector3f getPre() {
            return pre;
        }

        public Vector3f getPost() {
            return post;
        }

        public Vector3f getData() {
            return data;
        }

        public String getLerpMode() {
            return lerpMode;
        }
    }
}
