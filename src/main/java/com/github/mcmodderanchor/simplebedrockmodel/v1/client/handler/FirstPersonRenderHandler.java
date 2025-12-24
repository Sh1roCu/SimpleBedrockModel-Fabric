package com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPGeoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class FirstPersonRenderHandler {
    private static IFPAnimationInstance previousInstance = null;
    private static IFPAnimationInstance currentInstance = null;

    // 切换动画的时间控制
    private static long switchStartTime;
    private static long currentSheatheDuration = 0;

    // 过渡状态控制
    private static boolean isTransitioning = false;
    private static ItemStack pendingTarget = ItemStack.EMPTY;
    private static boolean wasLastCustom = false;
    private static boolean nextIsCustom = false;

    // 记录上一次的热键选中下标，用于检测切换物品栏（序号）
    private static int lastSelectedHotbarIndex = -1;

    // 手动触发主副手互换的信号（外部可调用）
    private static boolean forceHandSwapFlag = false;

    /**
     * 由 Mixin 在 tick 中调用，用于控制物品切换过渡
     * @param currentStack 当前渲染的物品（旧物品）
     * @param targetStack 玩家实际持有的物品（新物品）
     * @return true 表示正在播放自定义过渡动画，需要阻止原版逻辑；false 表示可以放行
     */
    public static boolean updateTransition(ItemStack currentStack, ItemStack targetStack) {
        // 检测是否是新的切换请求
        LocalPlayer player = Minecraft.getInstance().player;
        int selectedIndex = player != null ? player.getInventory().selected : -1;
        boolean slotChanged = selectedIndex != lastSelectedHotbarIndex || forceHandSwapFlag;

        if (forceHandSwapFlag) forceHandSwapFlag = false;

        // If slot changed, treat it as a new item no matter what renderer says
        boolean pendingEqualsTarget = isSameItemStacks(pendingTarget, targetStack, slotChanged);

        if (!pendingEqualsTarget) {
            if (isTransitioning) {
                // A -> B (进行中) -> C
                // 如果当前正在播放自定义收起动画，我们不应该打断它。
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
                // 每次调用都更新记录的选中下标
                lastSelectedHotbarIndex = selectedIndex;
                return false; // 通知mixin放行，切换到新物品
            }
        }

        // 进行超级替换
        if (currentInstance != null) {
            currentInstance.updateItem(targetStack);
        }
        // 每次调用都更新记录的选中下标
        lastSelectedHotbarIndex = selectedIndex;
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

        currentInstance = createCurrentInstance(target);
    }

    private static IFPAnimationInstance createCurrentInstance(ItemStack target) {
        return getRenderer(target).map(r -> {
            return r.createAnimationInstance(target, Minecraft.getInstance().getCameraEntity());
        }).orElse(null);
    }

    private static void initializeTransition(ItemStack current, ItemStack target) {
        pendingTarget = target;
        switchStartTime = System.currentTimeMillis();

        boolean oldIsCustom = isCustomItem(current);
        nextIsCustom = isCustomItem(target);

        // 归档旧实例
        previousInstance = currentInstance;

        currentInstance = createCurrentInstance(target);

        // 只有当旧物品是自定义物品时，我们才拦截并播放自定义收起动画
        if (oldIsCustom) {
            isTransitioning = true;
            currentSheatheDuration = calculateSheatheDuration(current);
            // 触发旧实例的收起动画
            if (previousInstance != null) {
                previousInstance.triggerPutAway();
            }
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
        // 如果下一个是自定义物品，返回 1.0(由 Handler 完全接管渲染，跳过原版 Draw 上浮)
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

    public static Optional<IFPGeoItemRenderer> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof IFPGeoItemRenderer renderer) {
            return Optional.of(renderer);
        }
        return Optional.empty();
    }

    @SubscribeEvent
    public static void onRenderHand(SwapItemWithOffHand event) {
        forceHandSwapFlag = true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        IFPAnimationInstance animationInstance = getActiveAnimationInstance();

        if (animationInstance != null) {
            ItemStack stack = animationInstance.currentItem();
            if (!stack.isEmpty()) {
                getRenderer(stack).ifPresent(renderer -> {
                    if (event.getHand() != InteractionHand.MAIN_HAND && renderer.blockOffhandRender()) {
                        event.setCanceled(true);
                        return;
                    }

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
                currentInstance = createCurrentInstance(heldItem);
                previousInstance = null;
                pendingTarget = heldItem;
            }
        }

        if (!isTransitioning && currentInstance != null) {
            // 触发 Draw 动画
            currentInstance.triggerDraw();
        }

        var ani = getActiveAnimationInstance();
        if (ani != null) {
            ani.tick(event.renderTickTime);
        }
    }

    public static IFPAnimationInstance getActiveAnimationInstance() {
        return isTransitioning ? previousInstance : currentInstance;
    }

    private static boolean isSameItemStacks(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return false;
        if (oldStack == newStack) return true;
        if (oldStack.isEmpty() && newStack.isEmpty()) return true;
        if (oldStack.isEmpty() || newStack.isEmpty()) return false;

        // If oldStack has a renderer, prefer its comparison logic
        Optional<IFPGeoItemRenderer> opt = getRenderer(oldStack);
        return opt.map(abstractGeoItemRenderer -> abstractGeoItemRenderer.isSameItem(oldStack, newStack))
                .orElseGet(() -> ItemStack.isSameItem(oldStack, newStack));
    }
}