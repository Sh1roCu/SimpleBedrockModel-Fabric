package com.github.mcmodderanchor.simplebedrockmodel.v1.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandRenderer.class, priority = 2000)
public class ItemInHandRendererMixin {

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTickHead(CallbackInfo ci) {
        if (FirstPersonRenderHandler.shouldLockVanilla()) {
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
            ci.cancel();
        }
    }
}
