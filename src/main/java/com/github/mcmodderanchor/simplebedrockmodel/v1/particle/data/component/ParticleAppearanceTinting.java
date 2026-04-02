package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

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

    /**
     * 渐变颜色。每个颜色停靠点的 RGBA 通道均为 {@link MolangExpression}，支持动态求值。
     *
     * @param interpolant 插值因子（Molang 表达式）
     * @param stops       停靠点位置数组
     * @param colors      每个停靠点的颜色，每行 4 个 MolangExpression：[r, g, b, a]
     */
    record GradientColor(MolangExpression interpolant, float[] stops, MolangExpression[][] colors) implements ParticleAppearanceTinting {}

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
        JsonElement gradientElem = obj.get("gradient");

        // gradient 可以是对象（key 为停靠点）或数组（自动等分）
        if (gradientElem == null) {
            return new GradientColor(interpolant, new float[]{0},
                    new MolangExpression[][]{{MolangExpression.constant(1), MolangExpression.constant(1),
                            MolangExpression.constant(1), MolangExpression.constant(1)}});
        }

        List<Float> stopList = new ArrayList<>();
        List<MolangExpression[]> colorList = new ArrayList<>();

        if (gradientElem.isJsonObject()) {
            JsonObject gradient = gradientElem.getAsJsonObject();
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(gradient.entrySet());
            entries.sort(Comparator.comparingDouble(a -> Double.parseDouble(a.getKey())));
            for (Map.Entry<String, JsonElement> entry : entries) {
                stopList.add(Float.parseFloat(entry.getKey()));
                colorList.add(parseColorField(entry.getValue(), molang));
            }
        } else if (gradientElem.isJsonArray()) {
            JsonArray arr = gradientElem.getAsJsonArray();
            int size = arr.size();
            for (int i = 0; i < size; i++) {
                stopList.add(size > 1 ? (float) i / (size - 1) : 0f);
                colorList.add(parseColorField(arr.get(i), molang));
            }
        }

        float[] stops = new float[stopList.size()];
        for (int i = 0; i < stopList.size(); i++) stops[i] = stopList.get(i);
        return new GradientColor(interpolant, stops, colorList.toArray(new MolangExpression[0][]));
    }

    /**
     * 解析单个颜色字段为 MolangExpression[4]（r, g, b, a）。
     * 支持十六进制字符串、数组（每个元素可为数字或 Molang 表达式）。
     */
    private static MolangExpression[] parseColorField(JsonElement elem, ParticleMolangEnvironment molang) {
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            float[] rgba = parseHexColor(elem.getAsString());
            return new MolangExpression[]{
                    MolangExpression.constant(rgba[0]), MolangExpression.constant(rgba[1]),
                    MolangExpression.constant(rgba[2]), MolangExpression.constant(rgba[3])};
        }
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            MolangExpression r = molang.compile(molangFromElement(arr.get(0), "1"));
            MolangExpression g = molang.compile(molangFromElement(arr.get(1), "1"));
            MolangExpression b = molang.compile(molangFromElement(arr.get(2), "1"));
            MolangExpression a = arr.size() > 3 ? molang.compile(molangFromElement(arr.get(3), "1")) : MolangExpression.constant(1);
            return new MolangExpression[]{r, g, b, a};
        }
        return new MolangExpression[]{
                MolangExpression.constant(1), MolangExpression.constant(1),
                MolangExpression.constant(1), MolangExpression.constant(1)};
    }
}
