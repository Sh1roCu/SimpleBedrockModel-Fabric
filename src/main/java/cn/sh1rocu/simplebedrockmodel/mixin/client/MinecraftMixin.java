package cn.sh1rocu.simplebedrockmodel.mixin.client;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import cn.sh1rocu.simplebedrockmodel.api.event.RenderTickEvent;
import cn.sh1rocu.simplebedrockmodel.util.client.MinecraftUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.ImmediateStaticMeshRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.WorldMeshRenderer;
import net.minecraft.client.Minecraft;
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
    @Nullable
    public ClientLevel level;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V"))
    private void sbm$onInit(CallbackInfo ci) {
        RegisterClientReloadListenersEvent.EVENT.invoker().post(new RegisterClientReloadListenersEvent(this.resourceManager));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void sbm$renderTickStart(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.START, MinecraftUtil.getPartialTick()));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 4, shift = At.Shift.AFTER))
    private void sbm$renderTickEnd(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.END, MinecraftUtil.getPartialTick()));
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void sbm$setLevel(ClientLevel levelClient, CallbackInfo ci) {
        if (this.level != null) {
            WorldMeshRenderer.onLevelUnload(this.level);
            ImmediateStaticMeshRenderer.onLevelUnload(this.level);
        }
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V",
            shift = At.Shift.AFTER))
    private void sbm$clearLevel(Screen screen, CallbackInfo ci) {
        if (this.level != null) {
            WorldMeshRenderer.onLevelUnload(this.level);
            ImmediateStaticMeshRenderer.onLevelUnload(this.level);
        }
    }
}
