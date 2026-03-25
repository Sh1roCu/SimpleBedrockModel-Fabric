package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.*;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基岩版粒子效果 JSON 反序列化器。
 * <p>
 * 解析格式：
 * <pre>
 * {
 *   "format_version": "1.10.0",
 *   "particle_effect": {
 *     "description": { "identifier": "...", "basic_render_parameters": { ... } },
 *     "components": { ... }
 *   }
 * }
 * </pre>
 */
public class ParticleEffectDeserializer implements JsonDeserializer<ParticleEffectDefinition> {

    @Override
    public ParticleEffectDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject root = json.getAsJsonObject();
        JsonObject effect = root.getAsJsonObject("particle_effect");
        if (effect == null) throw new JsonParseException("Missing 'particle_effect'");

        // description
        JsonObject descObj = effect.getAsJsonObject("description");
        ParticleDescription description = parseDescription(descObj);

        // components
        JsonObject compObj = effect.getAsJsonObject("components");
        List<IParticleComponent> components = compObj != null ? parseComponents(compObj) : List.of();

        return new ParticleEffectDefinition(description.getIdentifier(), description, components);
    }

    private ParticleDescription parseDescription(JsonObject obj) {
        String id = obj.get("identifier").getAsString();
        ResourceLocation identifier = new ResourceLocation(id);

        JsonObject renderParams = obj.getAsJsonObject("basic_render_parameters");
        ParticleDescription.Material material = ParticleDescription.Material.PARTICLES_BLEND;
        ResourceLocation texture = new ResourceLocation("minecraft", "textures/particle/generic_0.png");
        int texW = 0, texH = 0;

        if (renderParams != null) {
            String mat = getString(renderParams, "material", "particles_blend");
            material = switch (mat) {
                case "particles_opaque" -> ParticleDescription.Material.PARTICLES_OPAQUE;
                case "particles_alpha" -> ParticleDescription.Material.PARTICLES_ALPHA;
                case "particles_add" -> ParticleDescription.Material.PARTICLES_ADD;
                default -> ParticleDescription.Material.PARTICLES_BLEND;
            };
            String texRaw = getString(renderParams, "texture", "textures/particle/generic_0");
            texture = resolveTexturePath(texRaw);
        }

        return new ParticleDescription(identifier, material, texture, texW, texH);
    }

    private List<IParticleComponent> parseComponents(JsonObject obj) {
        List<IParticleComponent> components = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            IParticleComponent component = parseComponent(key, value);
            if (component != null) {
                components.add(component);
            }
        }
        return components;
    }

    @Nullable
    private IParticleComponent parseComponent(String key, JsonElement value) {
        return switch (key) {
            case "minecraft:emitter_rate_instant" -> parseEmitterRateInstant(value.getAsJsonObject());
            case "minecraft:emitter_rate_steady" -> parseEmitterRateSteady(value.getAsJsonObject());
            case "minecraft:emitter_lifetime_looping" -> parseEmitterLifetimeLooping(value.getAsJsonObject());
            case "minecraft:emitter_lifetime_once" -> parseEmitterLifetimeOnce(value.getAsJsonObject());
            case "minecraft:emitter_shape_point" -> parseEmitterShapePoint(value.getAsJsonObject());
            case "minecraft:emitter_shape_sphere" -> parseEmitterShapeSphere(value.getAsJsonObject());
            case "minecraft:emitter_shape_box" -> parseEmitterShapeBox(value.getAsJsonObject());
            case "minecraft:particle_appearance_billboard" -> parseAppearanceBillboard(value.getAsJsonObject());
            case "minecraft:particle_motion_dynamic" -> parseMotionDynamic(value.getAsJsonObject());
            case "minecraft:particle_motion_parametric" -> parseMotionParametric(value.getAsJsonObject());
            case "minecraft:particle_lifetime_expression" -> parseLifetimeExpression(value.getAsJsonObject());
            case "minecraft:particle_initial_speed" -> parseInitialSpeed(value);
            case "minecraft:particle_appearance_tinting" -> parseAppearanceTinting(value.getAsJsonObject());
            default -> null; // 未知组件，跳过
        };
    }

    // ---- Emitter Rate ----

    private EmitterRate.Instant parseEmitterRateInstant(JsonObject obj) {
        String amount = getMolang(obj, "num_particles", "10");
        return new EmitterRate.Instant(amount);
    }

    private EmitterRate.Steady parseEmitterRateSteady(JsonObject obj) {
        String spawnRate = getMolang(obj, "spawn_rate", "1");
        String maxParticles = getMolang(obj, "max_particles", "50");
        return new EmitterRate.Steady(spawnRate, maxParticles);
    }

    // ---- Emitter Lifetime ----

    private EmitterLifetime.Looping parseEmitterLifetimeLooping(JsonObject obj) {
        String activeTime = getMolang(obj, "active_time", "1");
        String sleepTime = getMolang(obj, "sleep_time", "0");
        return new EmitterLifetime.Looping(activeTime, sleepTime);
    }

    private EmitterLifetime.Once parseEmitterLifetimeOnce(JsonObject obj) {
        String activeTime = getMolang(obj, "active_time", "1");
        return new EmitterLifetime.Once(activeTime);
    }

    // ---- Emitter Shape ----

    private EmitterShape.Point parseEmitterShapePoint(JsonObject obj) {
        String[] offset = getMolangArray3(obj, "offset", "0", "0", "0");
        String[] direction = parseShapeDirection(obj);
        return new EmitterShape.Point(offset, direction);
    }

    private EmitterShape.Sphere parseEmitterShapeSphere(JsonObject obj) {
        String[] offset = getMolangArray3(obj, "offset", "0", "0", "0");
        String radius = getMolang(obj, "radius", "1");
        boolean surfaceOnly = obj.has("surface_only") && obj.get("surface_only").getAsBoolean();
        String[] direction = parseShapeDirection(obj);
        return new EmitterShape.Sphere(offset, radius, surfaceOnly, direction);
    }

    private EmitterShape.Box parseEmitterShapeBox(JsonObject obj) {
        String[] offset = getMolangArray3(obj, "offset", "0", "0", "0");
        String[] halfDimensions = getMolangArray3(obj, "half_dimensions", "0.5", "0.5", "0.5");
        boolean surfaceOnly = obj.has("surface_only") && obj.get("surface_only").getAsBoolean();
        String[] direction = parseShapeDirection(obj);
        return new EmitterShape.Box(offset, halfDimensions, surfaceOnly, direction);
    }

    /**
     * 解析形状的 direction 字段。
     * <ul>
     *   <li>缺失或 {@code "outwards"} / {@code "inwards"} → 返回 null（运行时根据位置计算）</li>
     *   <li>数组 [x, y, z] → 返回 Molang 表达式数组</li>
     * </ul>
     */
    @Nullable
    private static String[] parseShapeDirection(JsonObject obj) {
        if (!obj.has("direction")) return null;
        JsonElement elem = obj.get("direction");
        // "outwards" / "inwards" 字符串 → null
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) return null;
        // 数组
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            return new String[]{
                    molangFromElement(arr.size() > 0 ? arr.get(0) : null, "0"),
                    molangFromElement(arr.size() > 1 ? arr.get(1) : null, "0"),
                    molangFromElement(arr.size() > 2 ? arr.get(2) : null, "0")
            };
        }
        return null;
    }

    // ---- Particle Appearance Billboard ----

    private ParticleAppearanceBillboard parseAppearanceBillboard(JsonObject obj) {
        // size
        String[] size = getMolangArray(obj, "size", 2, "0.1", "0.1");

        // facing_camera_mode
        String modeStr = getString(obj, "facing_camera_mode", "rotate_xyz");
        ParticleAppearanceBillboard.FaceCameraMode mode = parseFaceCameraMode(modeStr);

        // uv
        ParticleAppearanceBillboard.UVConfig uv = parseUVConfig(obj.has("uv") ? obj.getAsJsonObject("uv") : null);

        return new ParticleAppearanceBillboard(size, mode, uv);
    }

    private ParticleAppearanceBillboard.FaceCameraMode parseFaceCameraMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "rotate_y" -> ParticleAppearanceBillboard.FaceCameraMode.ROTATE_Y;
            case "lookat_xyz" -> ParticleAppearanceBillboard.FaceCameraMode.LOOKAT_XYZ;
            case "lookat_y" -> ParticleAppearanceBillboard.FaceCameraMode.LOOKAT_Y;
            case "lookat_direction" -> ParticleAppearanceBillboard.FaceCameraMode.LOOKAT_DIRECTION;
            case "direction_x" -> ParticleAppearanceBillboard.FaceCameraMode.DIRECTION_X;
            case "direction_y" -> ParticleAppearanceBillboard.FaceCameraMode.DIRECTION_Y;
            case "direction_z" -> ParticleAppearanceBillboard.FaceCameraMode.DIRECTION_Z;
            case "emitter_transform_xy" -> ParticleAppearanceBillboard.FaceCameraMode.EMITTER_TRANSFORM_XY;
            case "emitter_transform_xz" -> ParticleAppearanceBillboard.FaceCameraMode.EMITTER_TRANSFORM_XZ;
            case "emitter_transform_yz" -> ParticleAppearanceBillboard.FaceCameraMode.EMITTER_TRANSFORM_YZ;
            default -> ParticleAppearanceBillboard.FaceCameraMode.ROTATE_XYZ;
        };
    }

    private ParticleAppearanceBillboard.UVConfig parseUVConfig(@Nullable JsonObject uvObj) {
        if (uvObj == null) {
            return new ParticleAppearanceBillboard.UVConfig("0", "0", "1", "1", 1, 1);
        }
        int texW = uvObj.has("texture_width") ? uvObj.get("texture_width").getAsInt() : 1;
        int texH = uvObj.has("texture_height") ? uvObj.get("texture_height").getAsInt() : 1;

        // 静态 UV
        if (uvObj.has("uv")) {
            JsonElement uvElem = uvObj.get("uv");
            if (uvElem.isJsonArray()) {
                JsonArray arr = uvElem.getAsJsonArray();
                String u = molangFromElement(arr.get(0), "0");
                String v = molangFromElement(arr.get(1), "0");
                String w = "1";
                String h = "1";
                if (uvObj.has("uv_size")) {
                    JsonArray sizeArr = uvObj.getAsJsonArray("uv_size");
                    w = molangFromElement(sizeArr.get(0), "1");
                    h = molangFromElement(sizeArr.get(1), "1");
                }
                return new ParticleAppearanceBillboard.UVConfig(u, v, w, h, texW, texH);
            }
        }
        return new ParticleAppearanceBillboard.UVConfig("0", "0", String.valueOf(texW), String.valueOf(texH), texW, texH);
    }

    // ---- Particle Motion ----

    private ParticleMotion.Dynamic parseMotionDynamic(JsonObject obj) {
        String[] accel = obj.has("linear_acceleration") ? getMolangArray3(obj, "linear_acceleration", "0", "0", "0") : null;
        String drag = obj.has("linear_drag_coefficient") ? getMolang(obj, "linear_drag_coefficient", "0") : null;
        return new ParticleMotion.Dynamic(accel, drag);
    }

    private ParticleMotion.Parametric parseMotionParametric(JsonObject obj) {
        String[] pos = obj.has("relative_position") ? getMolangArray3(obj, "relative_position", "0", "0", "0") : null;
        String[] dir = obj.has("direction") ? getMolangArray3(obj, "direction", "0", "0", "0") : null;
        return new ParticleMotion.Parametric(pos, dir);
    }

    // ---- Particle Lifetime ----

    private ParticleLifetimeExpression parseLifetimeExpression(JsonObject obj) {
        String maxLifetime = getMolang(obj, "max_lifetime", "1");
        String expiration = obj.has("expiration_expression") ? getMolang(obj, "expiration_expression", "0") : null;
        return new ParticleLifetimeExpression(maxLifetime, expiration);
    }

    // ---- Particle Initial Speed ----

    private ParticleInitialSpeed parseInitialSpeed(JsonElement value) {
        // 可以是数字或 Molang 字符串
        String speed = molangFromElement(value, "0");
        return new ParticleInitialSpeed(speed);
    }

    // ---- Particle Appearance Tinting ----

    private ParticleAppearanceTinting parseAppearanceTinting(JsonObject obj) {
        if (obj.has("color")) {
            JsonElement colorElem = obj.get("color");

            // 颜色可以是 [r, g, b, a] 数组（Molang 表达式）
            if (colorElem.isJsonArray()) {
                JsonArray arr = colorElem.getAsJsonArray();
                String r = molangFromElement(arr.get(0), "1");
                String g = molangFromElement(arr.get(1), "1");
                String b = molangFromElement(arr.get(2), "1");
                String a = arr.size() > 3 ? molangFromElement(arr.get(3), "1") : null;
                return new ParticleAppearanceTinting.StaticColor(r, g, b, a);
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
                return new ParticleAppearanceTinting.StaticColor(r, g, b, a);
            }

            // 颜色可以是 "#RRGGBB" 字符串
            if (colorElem.isJsonPrimitive() && colorElem.getAsJsonPrimitive().isString()) {
                float[] rgba = parseHexColor(colorElem.getAsString());
                return new ParticleAppearanceTinting.StaticColor(
                        String.valueOf(rgba[0]), String.valueOf(rgba[1]),
                        String.valueOf(rgba[2]), String.valueOf(rgba[3]));
            }
        }
        return new ParticleAppearanceTinting.StaticColor("1", "1", "1", null);
    }

    private ParticleAppearanceTinting.GradientColor parseGradientColor(JsonObject obj) {
        String interpolant = getMolang(obj, "interpolant", "0");
        JsonObject gradient = obj.getAsJsonObject("gradient");
        if (gradient == null) {
            return new ParticleAppearanceTinting.GradientColor(interpolant, new float[][]{{1, 1, 1, 1}});
        }

        // 渐变是 { "0.0": "#RRGGBB", "1.0": "#RRGGBB" } 或 { "0.0": [r,g,b,a], ... }
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(gradient.entrySet());
        entries.sort((a, b) -> Double.compare(Double.parseDouble(a.getKey()), Double.parseDouble(b.getKey())));

        float[][] colors = new float[entries.size()][4];
        for (int i = 0; i < entries.size(); i++) {
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
        return new ParticleAppearanceTinting.GradientColor(interpolant, colors);
    }

    // ---- Utility ----

    /**
     * 将基岩版纹理路径转换为 Java 版 ResourceLocation。
     * <p>
     * 基岩版路径格式多样：
     * <ul>
     *   <li>{@code "textures/particle/particles"} — 无命名空间，已有 textures/ 前缀</li>
     *   <li>{@code "particlestorm:particle/bomb_spark"} — 有命名空间，缺少 textures/ 前缀</li>
     *   <li>{@code "textures/particle/particles.png"} — 偶尔带 .png 后缀</li>
     * </ul>
     * 统一转换为 {@code namespace:textures/xxx.png} 格式。
     */
    private static ResourceLocation resolveTexturePath(String raw) {
        // 分离命名空间
        String namespace = "minecraft";
        String path = raw;
        int colonIdx = raw.indexOf(':');
        if (colonIdx >= 0) {
            namespace = raw.substring(0, colonIdx);
            path = raw.substring(colonIdx + 1);
        }

        // 确保 path 以 textures/ 开头
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }

        // 确保 .png 后缀
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }

        return new ResourceLocation(namespace, path);
    }

    private static String getMolang(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key)) return defaultValue;
        return molangFromElement(obj.get(key), defaultValue);
    }

    private static String molangFromElement(JsonElement elem, String defaultValue) {
        if (elem == null || elem.isJsonNull()) return defaultValue;
        if (elem.isJsonPrimitive()) {
            JsonPrimitive prim = elem.getAsJsonPrimitive();
            if (prim.isString()) return prim.getAsString();
            if (prim.isNumber()) return String.valueOf(prim.getAsNumber());
        }
        return defaultValue;
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key)) return defaultValue;
        JsonElement elem = obj.get(key);
        return elem.isJsonPrimitive() ? elem.getAsString() : defaultValue;
    }

    private static String[] getMolangArray3(JsonObject obj, String key, String d0, String d1, String d2) {
        if (!obj.has(key)) return new String[]{d0, d1, d2};
        JsonElement elem = obj.get(key);
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            return new String[]{
                    molangFromElement(arr.size() > 0 ? arr.get(0) : null, d0),
                    molangFromElement(arr.size() > 1 ? arr.get(1) : null, d1),
                    molangFromElement(arr.size() > 2 ? arr.get(2) : null, d2)
            };
        }
        return new String[]{d0, d1, d2};
    }

    private static String[] getMolangArray(JsonObject obj, String key, int size, String... defaults) {
        if (!obj.has(key)) return defaults.clone();
        JsonElement elem = obj.get(key);
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            String[] result = new String[size];
            for (int i = 0; i < size; i++) {
                result[i] = molangFromElement(i < arr.size() ? arr.get(i) : null,
                        i < defaults.length ? defaults[i] : "0");
            }
            return result;
        }
        return defaults.clone();
    }

    private static float[] parseHexColor(String hex) {
        hex = hex.replace("#", "");
        // 基岩版颜色格式为 AARRGGBB
        if (hex.length() > 6) {
            float a = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
            float r = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
            float g = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
            float b = Integer.parseInt(hex.substring(6, 8), 16) / 255f;
            return new float[]{r, g, b, a};
        }
        // RRGGBB（无 alpha）
        float r = Integer.parseInt(hex.substring(0, 2), 16) / 255f;
        float g = Integer.parseInt(hex.substring(2, 4), 16) / 255f;
        float b = Integer.parseInt(hex.substring(4, 6), 16) / 255f;
        return new float[]{r, g, b, 1f};
    }
}
