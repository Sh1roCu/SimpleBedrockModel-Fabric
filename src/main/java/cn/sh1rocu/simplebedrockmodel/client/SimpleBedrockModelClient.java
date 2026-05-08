package cn.sh1rocu.simplebedrockmodel.client;

import cn.sh1rocu.simplebedrockmodel.api.event.*;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.ClientSetupEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.BeforeRenderHandEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.ClientAnimationClockTicker;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.RenderItemInHandBobEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.CameraEventHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonArmorHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.network.NetworkHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.debug.ParticleDebugCommand;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.debug.ParticleDebugRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.ReloadListenersRegister;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class SimpleBedrockModelClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        NetworkHandler.registerS2CReceivers();
        ReloadListenersRegister.BedrockModelClientRegister.onRegisterReloadListener();

        ClientSetupEvent.onClientSetup();

        ClientPlayConnectionEvents.DISCONNECT.register(ClientAnimationClockTicker::onLoggingOut);
        RenderTickEvent.EVENT.register(ClientAnimationClockTicker::onRenderTick);

        RenderItemInHandBobEvent.BOB_VIEW.register(CameraEventHandler::cancelItemInHandViewBobbing);
        ViewportEvent.CAMERA.register(CameraEventHandler::applyLevelCameraAnimation);
        BeforeRenderHandEvent.EVENT.register(CameraEventHandler::applyItemInHandCameraAnimation);

        RenderArmEvent.EVENT.register(FirstPersonArmorHandler::onRenderArm);

        ClientPlayConnectionEvents.DISCONNECT.register(FirstPersonRenderHandler::onPlayerLoggedOut);
        SwapItemWithOffHand.EVENT.register(FirstPersonRenderHandler::onRenderHand);
        ClientTickEvents.START_CLIENT_TICK.register(FirstPersonRenderHandler::onClientTick);
        RenderTickEvent.EVENT.register(FirstPersonRenderHandler::tickAnimation);
        RenderHandEvent.EVENT.register(FirstPersonRenderHandler::onRenderHand);

        ClientCommandRegistrationCallback.EVENT.register(ParticleDebugCommand::onRegisterClientCommands);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ParticleDebugRenderer::onRenderLevelStage);

        ViewportEvent.CAMERA.register(BaseEvent.LOWEST, CameraStateCache::onComputeCameraAngles);

        ClientTickEvents.START_CLIENT_TICK.register(WorldEmitterManager::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register(WorldEmitterManager::onLoggingOut);

    }
}
