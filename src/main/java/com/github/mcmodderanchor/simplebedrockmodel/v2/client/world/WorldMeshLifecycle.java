package com.github.mcmodderanchor.simplebedrockmodel.v2.client.world;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.ApiStatus;

/**
 * 资源重载的 apply 阶段运行于客户端主线程；世界卸载监听由渲染器的注解订阅器处理。
 */
@ApiStatus.Internal
public class WorldMeshLifecycle {
    private WorldMeshLifecycle() {
    }

    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            WorldMeshRenderer.clearCaches();
            ImmediateStaticMeshRenderer.clear();
        });
    }
}
