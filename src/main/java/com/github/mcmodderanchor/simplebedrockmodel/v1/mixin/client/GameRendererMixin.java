package com.github.mcmodderanchor.simplebedrockmodel.v1.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.RenderItemInHandBobEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.RenderLevelBobEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private boolean sbm$useFovSetting;

    @Shadow
    public abstract Minecraft getMinecraft();


    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onBobHurt(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
        boolean cancel;
        if (!sbm$useFovSetting) {
            var event = new RenderItemInHandBobEvent.BobHurt();
            RenderItemInHandBobEvent.BOB_HURT.invoker().post(event);
            cancel = event.isCanceled();
        } else {
            var event = new RenderLevelBobEvent.BobHurt();
            RenderLevelBobEvent.BOB_HURT.invoker().post(event);
            cancel = event.isCanceled();
        }
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBobView(PoseStack pMatrixStack, float pPartialTicks, CallbackInfo ci) {
        boolean cancel;
        if (!sbm$useFovSetting) {
            var event = new RenderItemInHandBobEvent.BobView();
            RenderItemInHandBobEvent.BOB_VIEW.invoker().post(event);
            cancel = event.isCanceled();
        } else {
            var event = new RenderLevelBobEvent.BobView();
            RenderLevelBobEvent.BOB_VIEW.invoker().post(event);
            cancel = event.isCanceled();
        }
        if (cancel) {
            ci.cancel();
        }
    }

    /**
     * 是一个 hack 实现。因为 getFov 这个方法只有在构建 投影矩阵 的时候调用。
     * 因此可以根据 getFov 中的 pUseFovSetting 来判断当前准备渲染 Level 还是渲染 HandWithItem 。
     * 至于为什么不直接对 renderItemInHand 这个方法 mixin ，是因为安装了 Optifine 之后，这个方法的内容被大幅度修改了。
     */
    @Inject(method = "getFov", at = @At("HEAD"))
    public void switchRenderType(Camera pActiveRenderInfo, float pPartialTicks, boolean pUseFOVSetting, CallbackInfoReturnable<Double> cir) {
        this.sbm$useFovSetting = pUseFOVSetting;
    }
}
