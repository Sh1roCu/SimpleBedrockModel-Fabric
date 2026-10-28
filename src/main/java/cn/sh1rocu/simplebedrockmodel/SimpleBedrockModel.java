package cn.sh1rocu.simplebedrockmodel;

import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.ReloadListenersRegister;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class SimpleBedrockModel implements ModInitializer {
    @Override
    public void onInitialize() {
        com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel.setUp();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ReloadListenersRegister.BedrockModelServerRegister.onRegisterReloadListener();
        }
    }
}
