package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.molang.runtime.MolangExpression;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleMolangEnvironment;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Random;

import static com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleJsonUtils.*;

/**
 * 发射器形状组件，决定粒子的初始位置和方向。
 */
public sealed interface EmitterShape extends IEmitterComponent {
    enum PlaneNormal {
        X, Y, Z, CUSTOM
    }

    MolangExpression[] offset();

    @Nullable
    MolangExpression[] direction();

    DirectionMode directionMode();

    /**
     * 采样粒子初始位置，写入 p.x / p.y / p.z。
     */
    void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random);

    /**
     * 根据 directionMode 和 speed 计算初始速度，写入 p.vx / p.vy / p.vz。
     * 默认实现适用于所有形状。
     */
    default void applyDirection(ParticleInstance p, MolangContext<?> ctx, Random random, float speed) {
        if (speed == 0) return;
        float dx, dy, dz;
        DirectionMode dirMode = directionMode();
        if (dirMode == DirectionMode.CUSTOM && direction() != null) {
            dx = (float) direction()[0].evaluate(ctx);
            dy = (float) direction()[1].evaluate(ctx);
            dz = (float) direction()[2].evaluate(ctx);
        } else {
            float ox = (float) offset()[0].evaluate(ctx);
            float oy = (float) offset()[1].evaluate(ctx);
            float oz = (float) offset()[2].evaluate(ctx);
            dx = p.x - ox;
            dy = p.y - oy;
            dz = p.z - oz;
        }
        if (dirMode == DirectionMode.INWARDS) {
            dx = -dx;
            dy = -dy;
            dz = -dz;
        }
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0.0001f) {
            dx /= len;
            dy /= len;
            dz /= len;
        } else {
            float theta = (float) (random.nextFloat() * Math.PI * 2);
            float phi = (float) (Math.acos(2 * random.nextFloat() - 1));
            dx = (float) (Math.sin(phi) * Math.cos(theta));
            dy = (float) Math.cos(phi);
            dz = (float) (Math.sin(phi) * Math.sin(theta));
        }
        p.vx = dx * speed;
        p.vy = dy * speed;
        p.vz = dz * speed;
    }

    enum DirectionMode {OUTWARDS, INWARDS, CUSTOM}

    record Point(MolangExpression[] offset, @Nullable MolangExpression[] direction,
                 DirectionMode directionMode) implements EmitterShape {

        @Override
        public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
            p.x = (float) offset[0].evaluate(ctx);
            p.y = (float) offset[1].evaluate(ctx);
            p.z = (float) offset[2].evaluate(ctx);
        }
    }

    record Sphere(MolangExpression[] offset, MolangExpression radius, boolean surfaceOnly,
                  @Nullable MolangExpression[] direction, DirectionMode directionMode) implements EmitterShape {

        @Override
        public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
            float ox = (float) offset[0].evaluate(ctx);
            float oy = (float) offset[1].evaluate(ctx);
            float oz = (float) offset[2].evaluate(ctx);
            float r = (float) radius.evaluate(ctx);
            float theta = (float) (random.nextFloat() * Math.PI * 2);
            float phi = (float) (Math.acos(2 * random.nextFloat() - 1));
            float dist = surfaceOnly ? r : r * (float) Math.cbrt(random.nextFloat());
            p.x = ox + dist * (float) (Math.sin(phi) * Math.cos(theta));
            p.y = oy + dist * (float) Math.cos(phi);
            p.z = oz + dist * (float) (Math.sin(phi) * Math.sin(theta));
        }
    }

    record Box(MolangExpression[] offset, MolangExpression[] halfDimensions, boolean surfaceOnly,
               @Nullable MolangExpression[] direction, DirectionMode directionMode) implements EmitterShape {

        @Override
        public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
            float ox = (float) offset[0].evaluate(ctx);
            float oy = (float) offset[1].evaluate(ctx);
            float oz = (float) offset[2].evaluate(ctx);
            float hx = (float) halfDimensions[0].evaluate(ctx);
            float hy = (float) halfDimensions[1].evaluate(ctx);
            float hz = (float) halfDimensions[2].evaluate(ctx);
            if (surfaceOnly) {
                int face = random.nextInt(6);
                float u = random.nextFloat() * 2 - 1, v = random.nextFloat() * 2 - 1;
                switch (face) {
                    case 0 -> { p.x = ox + hx; p.y = oy + u * hy; p.z = oz + v * hz; }
                    case 1 -> { p.x = ox - hx; p.y = oy + u * hy; p.z = oz + v * hz; }
                    case 2 -> { p.y = oy + hy; p.x = ox + u * hx; p.z = oz + v * hz; }
                    case 3 -> { p.y = oy - hy;  p.x = ox + u * hx; p.z = oz + v * hz; }
                    case 4 -> { p.z = oz + hz; p.x = ox + u * hx; p.y = oy + v * hy; }
                    case 5 -> { p.z = oz - hz; p.x = ox + u * hx; p.y = oy + v * hy; }
                }
            } else {
                p.x = ox + (random.nextFloat() * 2 - 1) * hx;
                p.y = oy + (random.nextFloat() * 2 - 1) * hy;
                p.z = oz + (random.nextFloat() * 2 - 1) * hz;
            }
        }
    }

    record Disc(MolangExpression[] offset, MolangExpression radius, PlaneNormal planeNormal,
                boolean surfaceOnly, @Nullable MolangExpression[] direction,
                DirectionMode directionMode) implements EmitterShape {
        @Override
        public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
            float ox = (float) offset[0].evaluate(ctx);
            float oy = (float) offset[1].evaluate(ctx);
            float oz = (float) offset[2].evaluate(ctx);
            float r = (float) radius.evaluate(ctx);
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            float dist = surfaceOnly ? r : r * (float) Math.sqrt(random.nextFloat());
            float lx = dist * (float) Math.cos(angle);
            float lz = dist * (float) Math.sin(angle);
            switch (planeNormal) {
                case Y -> { p.x = ox + lx; p.y = oy; p.z = oz + lz; }
                case X -> {  p.x = ox; p.y = oy + lx; p.z = oz + lz; }
                case Z -> { p.x = ox + lx; p.y = oy + lz; p.z = oz; }
                default -> { p.x = ox + lx; p.y = oy; p.z = oz + lz; }
            }
        }
    }

    record EntityAABB(MolangExpression[] offset, boolean surfaceOnly,
                      @Nullable MolangExpression[] direction, DirectionMode directionMode) implements EmitterShape {
        @Override
        public void applyPosition(ParticleInstance p, MolangContext<?> ctx, Random random) {
            float ox = (float) offset[0].evaluate(ctx);
            float oy = (float) offset[1].evaluate(ctx);
            float oz = (float) offset[2].evaluate(ctx);
            // EntityAABB: 使用默认 1x1x1 盒体（实际尺寸需外部通过 Molang variable 提供）
            p.x = ox + (random.nextFloat() * 2 - 1) * 0.5f;
            p.y = oy + random.nextFloat();
            p.z = oz + (random.nextFloat() * 2 - 1) * 0.5f;
        }
    }

    static EmitterShape fromJson(String key, JsonElement value, ParticleMolangEnvironment molang) {
        JsonObject obj = value.getAsJsonObject();
        MolangExpression[] offset = compileArray3(molang, getMolangArray3(obj, "offset", "0", "0", "0"));
        DirectionParseResult dirResult = parseShapeDirection(obj, molang);

        return switch (key) {
            case "minecraft:emitter_shape_point" -> new Point(offset, dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_sphere" -> new Sphere(offset, molang.compile(getMolang(obj, "radius", "1")),
                    getBoolean(obj, "surface_only", false),
                    dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_box" ->
                    new Box(offset, compileArray3(molang, getMolangArray3(obj, "half_dimensions", "0.5", "0.5", "0.5")),
                            getBoolean(obj, "surface_only", false),
                            dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_disc" -> new Disc(offset, molang.compile(getMolang(obj, "radius", "1")),
                    parsePlaneNormal(obj),
                    getBoolean(obj, "surface_only", false),
                    dirResult.direction, dirResult.mode);
            case "minecraft:emitter_shape_entity_aabb" -> new EntityAABB(offset,
                    getBoolean(obj, "surface_only", false),
                    dirResult.direction, dirResult.mode);
            default -> throw new IllegalArgumentException("Unknown emitter shape key: " + key);
        };
    }

    private static DirectionParseResult parseShapeDirection(JsonObject obj, ParticleMolangEnvironment molang) {
        if (!obj.has("direction")) return new DirectionParseResult(null, DirectionMode.OUTWARDS);
        JsonElement elem = obj.get("direction");
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String str = elem.getAsString().toLowerCase();
            if ("inwards".equals(str)) return new DirectionParseResult(null, DirectionMode.INWARDS);
            return new DirectionParseResult(null, DirectionMode.OUTWARDS);
        }
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            String[] dir = new String[]{
                    molangFromElement(arr.size() > 0 ? arr.get(0) : null, "0"),
                    molangFromElement(arr.size() > 1 ? arr.get(1) : null, "0"),
                    molangFromElement(arr.size() > 2 ? arr.get(2) : null, "0")
            };
            return new DirectionParseResult(compileArray3(molang, dir), DirectionMode.CUSTOM);
        }
        return new DirectionParseResult(null, DirectionMode.OUTWARDS);
    }

    record DirectionParseResult(@Nullable MolangExpression[] direction, DirectionMode mode) {
    }

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
        return PlaneNormal.CUSTOM;
    }

    private static MolangExpression[] compileArray3(ParticleMolangEnvironment molang, String[] exprs) {
        return new MolangExpression[]{
                molang.compile(exprs[0]),
                molang.compile(exprs[1]),
                molang.compile(exprs[2])
        };
    }
}
