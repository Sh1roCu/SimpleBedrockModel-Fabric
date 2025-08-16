package cn.sh1rocu.simplebedrockmodel.client;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import com.github.tartaricacid.simplebedrockmodel.client.compat.sodium.SodiumCompat;
import com.github.tartaricacid.simplebedrockmodel.client.manager.BedrockEntityModelRegister;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleBedrockModelClient implements ClientModInitializer {
    public static final String MOD_ID = "simplebedrockmodel";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        RegisterClientReloadListenersEvent.CALLBACK.register(BedrockEntityModelRegister::onRegisterClientReloadListenersEvent);
        SodiumCompat.init();
    }
}
