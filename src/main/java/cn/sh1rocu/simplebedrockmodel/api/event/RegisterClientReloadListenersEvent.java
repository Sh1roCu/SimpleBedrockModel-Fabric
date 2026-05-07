package cn.sh1rocu.simplebedrockmodel.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

public class RegisterClientReloadListenersEvent extends BaseEvent {
    private final ReloadableResourceManager resourceManager;

    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    });

    public interface Callback {
        void post(RegisterClientReloadListenersEvent event);
    }

    public RegisterClientReloadListenersEvent(ReloadableResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void registerReloadListener(PreparableReloadListener reloadListener) {
        resourceManager.registerReloadListener(reloadListener);
    }
}
