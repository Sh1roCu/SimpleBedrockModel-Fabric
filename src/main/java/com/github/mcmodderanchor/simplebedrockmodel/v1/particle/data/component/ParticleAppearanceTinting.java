package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 粒子颜色/着色组件。对应 "minecraft:particle_appearance_tinting"。
 */
public sealed interface ParticleAppearanceTinting extends IParticleComponent {

    record StaticColor(MolangExpression r, MolangExpression g, MolangExpression b,
                       @Nullable MolangExpression a) implements ParticleAppearanceTinting {}

    record GradientColor(MolangExpression interpolant, float[] stops, float[][] colors) implements ParticleAppearanceTinting {}

    static ParticleAppearanceTinting fromJson(JsonObject obj, ParticleMolangEnvironment molang) {
        if (obj.has("color")) {
            JsonElement colorElem = obj.get("color");

            if (colorElem.isJsonArray()) {
                JsonArray arr = colorElem.getAsJsonArray();
                MolangExpression r = molang.compile(molangFromElement(arr.get(0), "1"));
                MolangExpression g = molang.compile(molangFromElement(arr.get(1), "1"));
                MolangExpression b = molang.compile(molangFromElement(arr.get(2), "1"));
                MolangExpression a = arr.size() > 3 ? molang.compile(molangFromElement(arr.get(3), "1")) : null;
                return new StaticColor(r, g, b, a);
            }

            if (colorElem.isJsonObject()) {
                JsonObject colorObj = colorElem.getAsJsonObject();
                if (colorObj.has("interpolant")) {
                    return parseGradientColor(colorObj, molang);
                }
                MolangExpression r = molang.compile(getMolang(colorObj, "r", "1"));
                MolangExpression g = molang.compile(getMolang(colorObj, "g", "1"));
                MolangExpression b = molang.compile(getMolang(colorObj, "b", "1"));
                MolangExpression a = colorObj.has("a") ? molang.compile(getMolang(colorObj, "a", "1")) : null;
                return new StaticColor(r, g, b, a);
            }

            if (colorElem.isJsonPrimitive() && colorElem.getAsJsonPrimitive().isString()) {
                float[] rgba = parseHexColor(colorElem.getAsString());
                return new StaticColor(
                        MolangExpression.constant(rgba[0]), MolangExpression.constant(rgba[1]),
                        MolangExpression.constant(rgba[2]), MolangExpression.constant(rgba[3]));
            }
        }
        return new StaticColor(MolangExpression.constant(1), MolangExpression.constant(1),
                MolangExpression.constant(1), null);
    }

    private static GradientColor parseGradientColor(JsonObject obj, ParticleMolangEnvironment molang) {
        MolangExpression interpolant = molang.compile(getMolang(obj, "interpolant", "0"));
        JsonObject gradient = obj.getAsJsonObject("gradient");
        if (gradient == null) {
            return new GradientColor(interpolant, new float[]{0}, new float[][]{{1, 1, 1, 1}});
        }

        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(gradient.entrySet());
        entries.sort(Comparator.comparingDouble(a -> Double.parseDouble(a.getKey())));

        float[] stops = new float[entries.size()];
        float[][] colors = new float[entries.size()][4];
        for (int i = 0; i < entries.size(); i++) {
            stops[i] = Float.parseFloat(entries.get(i).getKey());
            JsonElement colorElem = entries.get(i).getValue();
            if (colorElem.isJsonPrimitive() && colorElem.getAsJsonPrimitive().isString()) {
                colors[i] = parseHexColor(colorElem.getAsString());
            } else if (colorElem.isJsonArray()) {
                JsonArray arr = colorElem.getAsJsonArray();
                colors[i][0] = arr.get(0).getAsFloat();
                colors[i][1] = arr.get(1).getAsFloat();
                colors[i][2] = arr.get(2).getAsFloat();
                colors[i][3] = arr.size() > 3 ? arr.get(3).getAsFloat() : 1f;
            }
        }
        return new GradientColor(interpolant, stops, colors);
    }
}
