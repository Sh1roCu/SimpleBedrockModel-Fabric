package cn.sh1rocu.simplebedrockmodel.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.WorldMeshRenderer;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"))
    public void sbm$afterBE(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline,
                            Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci,
                            @Local ProfilerFiller profiler, @Local Frustum frustum) {
        WorldMeshRenderer.onRenderStage(camera, frustum, poseStack, projectionMatrix);
    }
}
