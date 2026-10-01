package cn.sh1rocu.simplebedrockmodel;

import cn.sh1rocu.simplebedrockmodel.api.event.BaseEvent;
import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.ReloadListenersRegister;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.command.WorldMeshCommands;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.WorldMeshLifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class SimpleBedrockModel implements ModInitializer {
    @Override
    public void onInitialize() {
        com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel.setUp();

        WorldMeshCommands.register();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ReloadListenersRegister.BedrockModelServerRegister.onRegisterReloadListener();
        }

        RegisterClientReloadListenersEvent.EVENT.register(BaseEvent.HIGHEST, WorldMeshLifecycle::registerReloadListener);
    }
}
