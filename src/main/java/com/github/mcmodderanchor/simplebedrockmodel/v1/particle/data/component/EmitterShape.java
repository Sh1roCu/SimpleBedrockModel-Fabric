package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 发射器形状组件，决定粒子的初始位置和方向。
 */
public sealed interface EmitterShape extends IEmitterComponent {

    /** 偏移量 [x, y, z]（Molang 表达式字符串） */
    String[] offset();

    /** 方向向量 [x, y, z]（Molang），CUSTOM 模式时非 null */
    @Nullable String[] direction();

    /** 方向模式 */
    DirectionMode directionMode();

    /** 方向模式 */
    enum DirectionMode {
        /** 从发射器中心向外 */
        OUTWARDS,
        /** 从发射器中心向内 */
        INWARDS,
        /** 自定义方向向量 */
        CUSTOM
    }

    /**
     * 点发射。对应 "minecraft:emitter_shape_point"。
     * @param offset 偏移量 [x, y, z]（Molang 表达式字符串）
     * @param direction 初始方向 [x, y, z]（Molang），CUSTOM 模式时使用
     * @param directionMode 方向模式
     */
    record Point(String[] offset, @Nullable String[] direction, DirectionMode directionMode) implements EmitterShape {}

    /**
     * 球体发射。对应 "minecraft:emitter_shape_sphere"。
     * @param offset 中心偏移 [x, y, z]
     * @param radius 半径（Molang）
     * @param surfaceOnly 是否仅在表面发射
     * @param direction 方向 [x, y, z]，CUSTOM 模式时使用
     * @param directionMode 方向模式
     */
    record Sphere(String[] offset, String radius, boolean surfaceOnly, @Nullable String[] direction, DirectionMode directionMode) implements EmitterShape {}

    /**
     * 盒体发射。对应 "minecraft:emitter_shape_box"。
     * @param offset 中心偏移 [x, y, z]
     * @param halfDimensions 半尺寸 [x, y, z]
     * @param surfaceOnly 是否仅在表面发射
     * @param direction 方向 [x, y, z]，CUSTOM 模式时使用
     * @param directionMode 方向模式
     */
    record Box(String[] offset, String[] halfDimensions, boolean surfaceOnly, @Nullable String[] direction, DirectionMode directionMode) implements EmitterShape {}

    /**
     * 圆盘发射。对应 "minecraft:emitter_shape_disc"。
     * @param offset 中心偏移 [x, y, z]
     * @param radius 半径（Molang）
     * @param planeNormal 圆盘法线方向（决定圆盘所在平面）
     * @param surfaceOnly 是否仅在边缘发射
     * @param direction 方向 [x, y, z]，CUSTOM 模式时使用
     * @param directionMode 方向模式
     */
    record Disc(String[] offset, String radius, PlaneNormal planeNormal,
                boolean surfaceOnly, @Nullable String[] direction, DirectionMode directionMode) implements EmitterShape {}

    /**
     * 实体 AABB 发射。对应 "minecraft:emitter_shape_entity_aabb"。
     * <p>
     * 使用实体的碰撞箱作为发射区域。由于本库不直接访问实体数据，
     * 运行时通过 Molang variable 获取实体尺寸。
     * @param offset 中心偏移 [x, y, z]
     * @param surfaceOnly 是否仅在表面发射
     * @param direction 方向 [x, y, z]，CUSTOM 模式时使用
     * @param directionMode 方向模式
     */
    record EntityAABB(String[] offset, boolean surfaceOnly, @Nullable String[] direction, DirectionMode directionMode) implements EmitterShape {}

    /** 圆盘法线方向 */
    enum PlaneNormal {
        X, Y, Z, CUSTOM
    }

    static EmitterShape fromJson(String key, JsonElement value) {
        JsonObject obj = value.getAsJsonObject();
        String[] offset = getMolangArray3(obj, "offset", "0", "0", "0");
        DirectionParseResult dirResult = parseShapeDirection(obj);

        return switch (key) {
            case "minecraft:emitter_shape_point" ->
                    new Point(offset, dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_sphere" ->
                    new Sphere(offset, getMolang(obj, "radius", "1"),
                            getBoolean(obj, "surface_only", false),
                            dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_box" ->
                    new Box(offset, getMolangArray3(obj, "half_dimensions", "0.5", "0.5", "0.5"),
                            getBoolean(obj, "surface_only", false),
                            dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_disc" ->
                    new Disc(offset, getMolang(obj, "radius", "1"),
                            parsePlaneNormal(obj),
                            getBoolean(obj, "surface_only", false),
                            dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_entity_aabb" ->
                    new EntityAABB(offset,
                            getBoolean(obj, "surface_only", false),
                            dirResult.direction, dirResult.mode);
            default -> throw new IllegalArgumentException("Unknown emitter shape key: " + key);
        };
    }

    /**
     * 解析形状的 direction 字段。
     * <ul>
     *   <li>缺失或 {@code "outwards"} → OUTWARDS 模式</li>
     *   <li>{@code "inwards"} → INWARDS 模式</li>
     *   <li>数组 [x, y, z] → CUSTOM 模式</li>
     * </ul>
     */
    private static DirectionParseResult parseShapeDirection(JsonObject obj) {
        if (!obj.has("direction")) return new DirectionParseResult(null, DirectionMode.OUTWARDS);
        JsonElement elem = obj.get("direction");
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String str = elem.getAsString().toLowerCase();
            if ("inwards".equals(str)) {
                return new DirectionParseResult(null, DirectionMode.INWARDS);
            }
            return new DirectionParseResult(null, DirectionMode.OUTWARDS);
        }
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            String[] dir = new String[]{
                    molangFromElement(arr.size() > 0 ? arr.get(0) : null, "0"),
                    molangFromElement(arr.size() > 1 ? arr.get(1) : null, "0"),
                    molangFromElement(arr.size() > 2 ? arr.get(2) : null, "0")
            };
            return new DirectionParseResult(dir, DirectionMode.CUSTOM);
        }
        return new DirectionParseResult(null, DirectionMode.OUTWARDS);
    }

    record DirectionParseResult(@Nullable String[] direction, DirectionMode mode) {}

    /**
     * 解析圆盘的 plane_normal 字段。
     * 支持字符串 "x"/"y"/"z" 或自定义向量数组。
     */
    private static PlaneNormal parsePlaneNormal(JsonObject obj) {
        if (!obj.has("plane_normal")) return PlaneNormal.Y;
        JsonElement elem = obj.get("plane_normal");
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            return switch (elem.getAsString().toLowerCase()) {
                case "x" -> PlaneNormal.X;
                case "z" -> PlaneNormal.Z;
                default -> PlaneNormal.Y;
            };
        }
        // 自定义向量暂按 Y 处理（完整实现需要编译自定义法线向量）
        return PlaneNormal.CUSTOM;
    }
}
