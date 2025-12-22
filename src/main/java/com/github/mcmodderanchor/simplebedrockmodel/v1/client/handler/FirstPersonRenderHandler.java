package com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.AbstractGeoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class FirstPersonRenderHandler {
    public static IFPAnimationInstance previousInstance = null;
    public static IFPAnimationInstance currentInstance = null;

    // 切换动画的时间控制
    private static long switchStartTime;
    private static long currentSheatheDuration = 0;

    // 过渡状态控制
    private static boolean isTransitioning = false;
    private static ItemStack pendingTarget = ItemStack.EMPTY;
    private static boolean wasLastCustom = false;
    private static boolean nextIsCustom = false;

    // 标记当前实例的 Draw 动画是否已触发
    private static boolean drawTriggered = false;

    /**
     * 由 Mixin 在 tick 中调用，用于控制物品切换过渡
     * @param currentStack 当前渲染的物品（旧物品）
     * @param targetStack 玩家实际持有的物品（新物品）
     * @return true 表示正在播放自定义过渡动画，需要阻止原版逻辑；false 表示可以放行
     */
    public static boolean updateTransition(ItemStack currentStack, ItemStack targetStack) {
        // 检测是否是新的切换请求
        if (pendingTarget != targetStack) {
            if (isTransitioning) {
                // A -> B (进行中) -> C
                // 如果当前正在播放自定义收起动画，我们不应该打断它。
                // 我们只需要将“下一个要出场的物品”从 B 换成 C。
                updateTargetOnly(targetStack);
            } else {
                // A (稳定) -> B
                // 初始化新实例，旧实例归档，开始过渡
                initializeTransition(currentStack, targetStack);
            }
        }

        // 1. 如果是 Custom -> Any，且处于 Putaway 阶段
        if (isTransitioning) {
            if (!isPutawayFinished()) {
                return true; // 通知mixin继续锁定旧物品
            } else {
                // 2. Putaway 结束
                isTransitioning = false;
                wasLastCustom = true; // 标记刚刚完成的是自定义物品的收起
                return false; // 通知mixin放行，切换到新物品
            }
        }

        // 不是自定义物品让原版处理即可
        wasLastCustom = false;
        return false;
    }

    /**
     * 在过渡进行中更新目标 (A -> B -> C)
     * 保持 A 的收起动画状态不变，只替换 B 为 C
     */
    private static void updateTargetOnly(ItemStack target) {
        pendingTarget = target;
        nextIsCustom = isCustomItem(target);
        drawTriggered = false;

        createCurrentInstance(target);
    }

    private static void createCurrentInstance(ItemStack target) {
        var renderer = getRenderer(target).orElse(null);
        if (renderer != null) {
            currentInstance = renderer.createAnimationInstance(target, Minecraft.getInstance().getCameraEntity());
        } else {
            currentInstance = null;
        }
    }

    private static void initializeTransition(ItemStack current, ItemStack target) {
        pendingTarget = target;
        switchStartTime = System.currentTimeMillis();
        drawTriggered = false; // 重置触发标记

        boolean oldIsCustom = isCustomItem(current);
        nextIsCustom = isCustomItem(target);

        // 归档旧实例
        previousInstance = currentInstance;

        createCurrentInstance(target);

        // 只有当旧物品是自定义物品时，我们才拦截并播放自定义收起动画
        if (oldIsCustom) {
            isTransitioning = true;
            currentSheatheDuration = calculateSheatheDuration(current);
            // 触发旧实例的收起动画
             if (previousInstance != null) previousInstance.triggerPutAway();
        } else {
            isTransitioning = false;
        }
    }

    public static boolean shouldInstantSwap() {
        // 如果是从 Custom 切换过来的（刚刚播放完自定义收起），
        // 我们希望瞬间完成数据切换，以便立刻开始 Draw 动画
        return wasLastCustom;
    }

    public static float getTargetHeight() {
        // 如果下一个是自定义物品，返回 1.0 (由 Handler 完全接管渲染，跳过原版 Draw 上浮)
        // 如果下一个是原版物品，返回 0.0 (让原版播放 Draw 上浮动画)
        return nextIsCustom ? 1.0F : 0.0F;
    }

    // 辅助方法：判断是否为自定义渲染物品
    public static boolean isCustomItem(ItemStack stack) {
        return getRenderer(stack).isPresent();
    }

    // 辅助方法：判断收起动画是否结束
    public static boolean isPutawayFinished() {
        return getSheatheProgress() >= 1.0f;
    }

    public static float getSheatheProgress() {
        if (currentSheatheDuration <= 0) return 1.0f;

        long timePassed = System.currentTimeMillis() - switchStartTime;
        float progress = (float) timePassed / currentSheatheDuration;

        return Math.min(1.0f, progress);
    }

    private static long calculateSheatheDuration(ItemStack stack) {
        return getRenderer(stack).map(renderer -> renderer.getPutAwayDuration(stack)).orElse(0L);
    }

    public static Optional<AbstractGeoItemRenderer<?>> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof AbstractGeoItemRenderer<?> renderer) {
            return Optional.of(renderer);
        }
        return Optional.empty();
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // 仅处理主手
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        IFPAnimationInstance animationInstance = getActiveAnimationInstance();

        if (animationInstance != null) {
            ItemStack stack = animationInstance.currentItem();
            if (!stack.isEmpty()) {
                getRenderer(stack).ifPresent(renderer -> {
                    ItemDisplayContext transformType = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
                    renderer.renderFirstPerson(player, stack, transformType, event.getPoseStack(), event.getMultiBufferSource(),
                            event.getPackedLight(), event.getPartialTick());
                    event.setCanceled(true);
                });
            }
        }
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // 检测是否切回了原物品 (A -> B -> A)
        if (isTransitioning && previousInstance != null) {
            ItemStack heldItem = player.getMainHandItem();
            // 使用引用比较，确保是同一个物品对象（即切回了同一个槽位）
            if (heldItem == previousInstance.currentItem()) {
                isTransitioning = false;
                createCurrentInstance(heldItem);
                previousInstance = null;
                pendingTarget = heldItem;
                drawTriggered = false; // 重置触发器，以便重新播放 Draw
            }
        }

        if (!isTransitioning && currentInstance != null && !drawTriggered) {
            // 触发 Draw 动画
            currentInstance.triggerDraw();
            drawTriggered = true;
        }

        var ani = getActiveAnimationInstance();
        if (ani != null) {
            ani.tick(event.renderTickTime);
        }
    }

    public static IFPAnimationInstance getActiveAnimationInstance() {
        // 如果正在进行收起动画（isTransitioning 为 true），渲染旧的实例
        // 注意：当 isTransitioning 为 true 时，Mixin 锁定了 mainHandItem 为旧物品
        if (isTransitioning) {
            return previousInstance;
        } else {
            return currentInstance;
        }
    }
}
