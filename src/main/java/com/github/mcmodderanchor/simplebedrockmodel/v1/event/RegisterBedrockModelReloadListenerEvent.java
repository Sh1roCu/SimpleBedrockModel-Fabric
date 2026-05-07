package com.github.mcmodderanchor.simplebedrockmodel.v1.event;

import cn.sh1rocu.simplebedrockmodel.api.event.BaseEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RegisterBedrockModelReloadListenerEvent extends BaseEvent {
    private final List<Consumer<Map<ResourceLocation, BedrockModel>>> listeners = new ArrayList<>();

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    });

    public void register(Consumer<Map<ResourceLocation, BedrockModel>> listener) {
        this.listeners.add(listener);
    }

    public List<Consumer<Map<ResourceLocation, BedrockModel>>> getListeners() {
        return listeners;
    }

    public interface Callback {
        void post(RegisterBedrockModelReloadListenerEvent event);
    }
}
