package com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.exclusion.ClientOnly;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class BonesItem {
    @SerializedName("cubes")
    @ClientOnly
    private CubesItem[] cubes;

    @SerializedName("name")
    private String name;

    @SerializedName("pivot")
    private float[] pivot;

    @SerializedName("rotation")
    private float[] rotation;

    @SerializedName("parent")
    private String parent;

    @SerializedName("mirror")
    @ClientOnly
    private boolean mirror = false;

    @Nullable
    public CubesItem[] getCubes() {
        return cubes;
    }

    public String getName() {
        return name;
    }

    public float[] getPivot() {
        return pivot;
    }

    public float[] getRotation() {
        return rotation;
    }

    public String getParent() {
        return parent;
    }

    public boolean isMirror() {
        return mirror;
    }
}