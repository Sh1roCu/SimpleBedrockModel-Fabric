package com.github.mcmodderanchor.simplebedrockmodel.v1.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.BeforeRenderHandEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    public void beforeHandRender(float pPartialTicks, PoseStack pPoseStack, MultiBufferSource.BufferSource pBuffer, LocalPlayer pPlayerEntity, int pCombinedLight, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new BeforeRenderHandEvent(pPoseStack, pPartialTicks));
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTickHead(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack realMainStack = player.getMainHandItem();

        // 检查是否需要介入过渡逻辑
        if (this.mainHandItem != realMainStack) {
            // 询问 Handler：当前过渡状态应该如何处理？
            // 返回值 true: 表示 Handler 正在进行自定义过渡（如播放 Putaway 动画），
            //             需要 Mixin 阻止原版切换物品。
            // 返回值 false: 表示 Handler 已完成过渡或不关心，交还给原版处理。
            boolean isAnimating = FirstPersonRenderHandler.updateTransition(
                    this.mainHandItem,
                    realMainStack
            );

            if (isAnimating) {
                // 锁定高度，防止原版 tick 逻辑将高度降到 0 从而触发强制换手，以此保持旧物品
                this.mainHandHeight = 1.0F;
                this.oMainHandHeight = 1.0F;
                ci.cancel();
                return;
            }

            // 如果 Handler 返回 false (动画结束，或者不需要自定义过渡)
            // 我们检查是否需要“瞬间切换”来衔接下一阶段
            if (FirstPersonRenderHandler.shouldInstantSwap()) {
                this.mainHandItem = realMainStack;
                // 如果下一阶段是原版 Draw，设为 0 让其自然上升
                // 如果下一阶段是自定义 Draw，设为 1 并由 Handler 接管渲染
                this.mainHandHeight = FirstPersonRenderHandler.getTargetHeight();
                this.oMainHandHeight = this.mainHandHeight;
            }

            // 如果既不是正在动画，也不需要瞬间切换，代码继续向下执行。
            // 原版逻辑会接手：降低高度 -> 切换物品 -> 升高高度。
        }
    }
}
