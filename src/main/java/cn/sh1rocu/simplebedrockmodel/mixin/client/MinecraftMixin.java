package cn.sh1rocu.simplebedrockmodel.mixin.client;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import cn.sh1rocu.simplebedrockmodel.api.event.RenderTickEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
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

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V"))
    private void sbm$onInit(CallbackInfo ci) {
        RegisterClientReloadListenersEvent.EVENT.invoker().post(new RegisterClientReloadListenersEvent(this.resourceManager));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void sbm$renderTickStart(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.START, this.timer));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 4, shift = At.Shift.AFTER))
    private void sbm$renderTickEnd(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.END, this.timer));
    }
}
