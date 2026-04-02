package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * 发射器生命周期事件组件。对应 "minecraft:emitter_lifetime_events"。
 *
 * @param creationEvent              发射器创建时触发的事件名列表
 * @param expirationEvent            发射器过期时触发的事件名列表
 * @param timeline                   时间轴事件，key 为时间（秒），value 为事件名列表
 * @param travelDistanceEvents       移动距离事件，key 为距离，value 为事件名列表
 * @param loopingTravelDistanceEvents 循环移动距离事件
 */
public record EmitterLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        TreeMap<Float, List<String>> timeline,
        TreeMap<Float, List<String>> travelDistanceEvents,
        List<LoopingTravelDistanceEvent> loopingTravelDistanceEvents
) implements IEmitterComponent {

    public record LoopingTravelDistanceEvent(float distance, List<String> effects) {}

    public static EmitterLifetimeEvents fromJson(JsonObject obj) {
        List<String> creation = parseEventList(obj, "creation_event");
        List<String> expiration = parseEventList(obj, "expiration_event");
        TreeMap<Float, List<String>> timeline = parseTimeline(obj, "timeline");
        TreeMap<Float, List<String>> travelDistance = parseTimeline(obj, "travel_distance_events");
        List<LoopingTravelDistanceEvent> looping = parseLoopingTravelDistance(obj);
        return new EmitterLifetimeEvents(creation, expiration, timeline, travelDistance, looping);
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

    private static TreeMap<Float, List<String>> parseTimeline(JsonObject obj, String key) {
        if (!obj.has(key)) return new TreeMap<>();
        JsonObject timelineObj = obj.getAsJsonObject(key);
        if (timelineObj == null) return new TreeMap<>();
        TreeMap<Float, List<String>> map = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : timelineObj.entrySet()) {
            float time = Float.parseFloat(entry.getKey());
            map.put(time, parseEventValue(entry.getValue()));
        }
        return map;
    }

    private static List<String> parseEventValue(JsonElement elem) {
        if (elem.isJsonPrimitive()) return List.of(elem.getAsString());
        if (elem.isJsonArray()) {
            List<String> list = new ArrayList<>();
            for (JsonElement e : elem.getAsJsonArray()) list.add(e.getAsString());
            return list;
        }
        return List.of();
    }

    private static List<LoopingTravelDistanceEvent> parseLoopingTravelDistance(JsonObject obj) {
        if (!obj.has("looping_travel_distance_events")) return List.of();
        JsonArray arr = obj.getAsJsonArray("looping_travel_distance_events");
        if (arr == null) return List.of();
        List<LoopingTravelDistanceEvent> list = new ArrayList<>();
        for (JsonElement elem : arr) {
            JsonObject entry = elem.getAsJsonObject();
            float distance = entry.has("distance") ? entry.get("distance").getAsFloat() : 1f;
            List<String> effects = new ArrayList<>();
            if (entry.has("effects")) {
                for (JsonElement e : entry.getAsJsonArray("effects")) effects.add(e.getAsString());
            }
            list.add(new LoopingTravelDistanceEvent(distance, effects));
        }
        return list;
    }
}
