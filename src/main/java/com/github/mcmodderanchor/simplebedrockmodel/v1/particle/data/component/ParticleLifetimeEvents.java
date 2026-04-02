package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * 粒子生命周期事件组件。对应 "minecraft:particle_lifetime_events"。
 *
 * @param creationEvent   粒子创建时触发的事件名列表
 * @param expirationEvent 粒子过期时触发的事件名列表
 * @param timeline        时间轴事件，key 为时间（秒），value 为事件名列表
 */
public record ParticleLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        TreeMap<Float, List<String>> timeline
) implements IParticleComponent {

    public static ParticleLifetimeEvents fromJson(JsonObject obj) {
        List<String> creation = parseEventList(obj, "creation_event");
        List<String> expiration = parseEventList(obj, "expiration_event");
        TreeMap<Float, List<String>> timeline = parseTimeline(obj);
        return new ParticleLifetimeEvents(creation, expiration, timeline);
    }

    private static List<String> parseEventList(JsonObject obj, String key) {
        if (!obj.has(key)) return List.of();
        JsonElement elem = obj.get(key);
        if (elem.isJsonPrimitive()) return List.of(elem.getAsString());
        if (elem.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement e : elem.getAsJsonArray()) list.add(e.getAsString());
            return list;
        }
        return List.of();
    }

    private static TreeMap<Float, List<String>> parseTimeline(JsonObject obj) {
        if (!obj.has("timeline")) return new TreeMap<>();
        JsonObject timelineObj = obj.getAsJsonObject("timeline");
        if (timelineObj == null) return new TreeMap<>();
        TreeMap<Float, List<String>> map = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : timelineObj.entrySet()) {
            float time = Float.parseFloat(entry.getKey());
            JsonElement val = entry.getValue();
            if (val.isJsonPrimitive()) {
                map.put(time, List.of(val.getAsString()));
            } else if (val.isJsonArray()) {
                List<String> list = new ArrayList<>();
                for (JsonElement e : val.getAsJsonArray()) list.add(e.getAsString());
                map.put(time, list);
            }
        }
        return map;
    }
}
