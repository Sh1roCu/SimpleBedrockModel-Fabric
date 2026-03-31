package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 粒子颜色/着色组件。对应 "minecraft:particle_appearance_tinting"。
 */
public sealed interface ParticleAppearanceTinting extends IParticleComponent {

    /**
     * 静态颜色。
     * @param r 红色通道（Molang，0~1）
     * @param g 绿色通道（Molang，0~1）
     * @param b 蓝色通道（Molang，0~1）
     * @param a 透明度通道（Molang，0~1），可为 null 表示 1.0
     */
    record StaticColor(String r, String g, String b, @Nullable String a) implements ParticleAppearanceTinting {}

    /**
     * 渐变颜色。
     * @param interpolant 插值因子（Molang）
     * @param stops 每个颜色对应的位置值（与 colors 一一对应）
     * @param colors 颜色数组，每个元素为 [r, g, b, a] 的 float 数组
     */
    record GradientColor(String interpolant, float[] stops, float[][] colors) implements ParticleAppearanceTinting {}

    static ParticleAppearanceTinting fromJson(JsonObject obj) {
        if (obj.has("color")) {
            JsonElement colorElem = obj.get("color");

            // 颜色可以是 [r, g, b, a] 数组（Molang 表达式）
            if (colorElem.isJsonArray()) {
                JsonArray arr = colorElem.getAsJsonArray();
                String r = molangFromElement(arr.get(0), "1");
                String g = molangFromElement(arr.get(1), "1");
                String b = molangFromElement(arr.get(2), "1");
                String a = arr.size() > 3 ? molangFromElement(arr.get(3), "1") : null;
                return new StaticColor(r, g, b, a);
            }

            // 颜色可以是对象 { "r": ..., "g": ..., "b": ..., "a": ... }
            if (colorElem.isJsonObject()) {
                JsonObject colorObj = colorElem.getAsJsonObject();

                // 检查是否是渐变
                if (colorObj.has("interpolant")) {
                    return parseGradientColor(colorObj);
                }

                String r = getMolang(colorObj, "r", "1");
                String g = getMolang(colorObj, "g", "1");
                String b = getMolang(colorObj, "b", "1");
                String a = colorObj.has("a") ? getMolang(colorObj, "a", "1") : null;
                return new StaticColor(r, g, b, a);
            }

            // 颜色可以是 "#RRGGBB" 字符串
            if (colorElem.isJsonPrimitive() && colorElem.getAsJsonPrimitive().isString()) {
                float[] rgba = parseHexColor(colorElem.getAsString());
                return new StaticColor(
                        String.valueOf(rgba[0]), String.valueOf(rgba[1]),
                        String.valueOf(rgba[2]), String.valueOf(rgba[3]));
            }
        }
        return new StaticColor("1", "1", "1", null);
    }

    private static GradientColor parseGradientColor(JsonObject obj) {
        String interpolant = getMolang(obj, "interpolant", "0");
        JsonObject gradient = obj.getAsJsonObject("gradient");
        if (gradient == null) {
            return new GradientColor(interpolant, new float[]{0}, new float[][]{{1, 1, 1, 1}});
        }

        // 渐变是 { "0.0": "#RRGGBB", "1.0": "#RRGGBB" } 或 { "0.0": [r,g,b,a], ... }
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(gradient.entrySet());
        entries.sort((a, b) -> Double.compare(Double.parseDouble(a.getKey()), Double.parseDouble(b.getKey())));

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
