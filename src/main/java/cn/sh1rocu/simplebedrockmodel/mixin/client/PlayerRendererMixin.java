package cn.sh1rocu.simplebedrockmodel.mixin.client;

import cn.sh1rocu.simplebedrockmodel.api.event.RenderArmEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void sbm$onRenderLeftArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        var event = new RenderArmEvent(poseStack, buffer, packedLight, player, HumanoidArm.LEFT);
        if (event.isCanceled()) ci.cancel();
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void sbm$onRenderRightArm(PoseStack poseStack, MultiBufferSource buffer,
                                      int packedLight, AbstractClientPlayer player, CallbackInfo ci) {
        var event = new RenderArmEvent(poseStack, buffer, packedLight, player, HumanoidArm.RIGHT);
        if (event.isCanceled()) ci.cancel();
    }
}