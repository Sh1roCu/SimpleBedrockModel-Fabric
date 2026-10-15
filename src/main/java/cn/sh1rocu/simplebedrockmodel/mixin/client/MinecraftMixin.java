package cn.sh1rocu.simplebedrockmodel.mixin.client;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import cn.sh1rocu.simplebedrockmodel.api.event.RenderFrameEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.cache.StaticMeshCache;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Final
    private ReloadableResourceManager resourceManager;

    @Shadow
    @Final
    private DeltaTracker.Timer timer;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V"))
    private void sbm$onInit(CallbackInfo ci) {
        RegisterClientReloadListenersEvent.EVENT.invoker().post(new RegisterClientReloadListenersEvent(this.resourceManager));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void sbm$renderTickStart(boolean tick, CallbackInfo ci) {
        RenderFrameEvent.EVENT.invoker().post(new RenderFrameEvent((Minecraft) (Object) this, RenderFrameEvent.Phase.START, this.timer));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = At.Shift.AFTER))
    private void sbm$renderTickEnd(boolean tick, CallbackInfo ci) {
        RenderFrameEvent.EVENT.invoker().post(new RenderFrameEvent((Minecraft) (Object) this, RenderFrameEvent.Phase.END, this.timer));
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void sbm$setLevel(ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        if (this.level != null) {
            WorldMeshRenderer.onLevelUnload(this.level);
            StaticMeshCache.onLevelUnload(this.level);
        }
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER))
    private void sbm$clearLevel(Screen nextScreen, boolean keepResourcePacks, CallbackInfo ci) {
        if (this.level != null) {
            WorldMeshRenderer.onLevelUnload(this.level);
            StaticMeshCache.onLevelUnload(this.level);
        }
    }
}
