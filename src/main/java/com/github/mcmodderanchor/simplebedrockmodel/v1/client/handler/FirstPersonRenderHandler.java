package com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClock;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.time.AnimationClocks;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPGeoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class FirstPersonRenderHandler {
    private static final AnimationClock CLOCK = AnimationClocks.client();

    private static int realSelectedSlot = -1;
    private static ItemStack realMainHand = ItemStack.EMPTY;

    private static boolean transitioning = false;

    private static IFPAnimationInstance activeInstance = null;
    private static IFPAnimationInstance previousInstance = null;

    private static ItemStack pendingTarget = ItemStack.EMPTY;

    private static long switchStartTime = 0L;
    private static long currentSheatheDuration = 0L;

    private static boolean lockVanilla = false;
    private static boolean nextIsCustom = false;

    private static boolean forceHandSwapFlag = false;

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        realSelectedSlot = -1;
        realMainHand = ItemStack.EMPTY;
        transitioning = false;
        activeInstance = null;
        previousInstance = null;
        pendingTarget = ItemStack.EMPTY;
        lockVanilla = false;
        nextIsCustom = false;
        forceHandSwapFlag = false;
        switchStartTime = 0L;
        currentSheatheDuration = 0L;
    }

    @SubscribeEvent
    public static void onRenderHand(SwapItemWithOffHand event) {
        forceHandSwapFlag = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !CLOCK.shouldTick()) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int newSlot = player.getInventory().selected;
        ItemStack newMain = player.getMainHandItem();

        boolean slotChanged = newSlot != realSelectedSlot || forceHandSwapFlag;
        boolean itemChanged = !isSameItemStacks(realMainHand, newMain);
        forceHandSwapFlag = false;

        realSelectedSlot = newSlot;
        realMainHand = newMain;

        if (slotChanged) {
            onSlotChanged(newMain);
        } else if (itemChanged) {
            onItemChangedInSameSlot(newMain);
        }

        if (activeInstance != null) {
            activeInstance.updateItem(newMain);
        }

        tickStates();
    }

    private static void onSlotChanged(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);

        if (transitioning) {
            return;
        }

        boolean oldIsCustom = activeInstance != null;

        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);

        if (oldIsCustom) {
            transitioning = true;
            lockVanilla = true;
            switchStartTime = CLOCK.nowMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());
            previousInstance.triggerPutAway();
        } else {
            transitioning = false;
            lockVanilla = false;
        }
    }

    private static void onItemChangedInSameSlot(ItemStack newStack) {
        pendingTarget = newStack;
        nextIsCustom = isCustomItem(newStack);

        if (transitioning) {
            return;
        }

        boolean oldIsCustom = activeInstance != null;

        previousInstance = activeInstance;
        activeInstance = createInstance(newStack);

        if (oldIsCustom) {
            transitioning = true;
            lockVanilla = true;
            switchStartTime = CLOCK.nowMillis();
            currentSheatheDuration = calculateSheatheDuration(previousInstance.currentItem());
            previousInstance.triggerPutAway();
        } else {
            lockVanilla = false;
        }
    }

    private static void tickStates() {
        if (transitioning && getSheatheProgress() >= 1.0f) {
            transitioning = false;
            lockVanilla = false;
            activeInstance = createInstance(pendingTarget);
            previousInstance = null;
        }
    }

    @SubscribeEvent
    public static void tickAnimation(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !CLOCK.shouldTick()) {
            return;
        }
        IFPAnimationInstance ani = getActiveAnimationInstance();
        if (ani != null) {
            ani.triggerDraw();
            ani.tick(event.renderTickTime);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        IFPAnimationInstance inst = getActiveAnimationInstance();
        if (inst == null) {
            return;
        }

        ItemStack stack = inst.currentItem();
        if (stack.isEmpty()) {
            return;
        }

        getRenderer(stack).ifPresent(renderer -> {
            if (event.getHand() == InteractionHand.OFF_HAND) {
                if (renderer.blockOffhandRender()) {
                    event.setCanceled(true);
                }
                return;
            }

            ItemDisplayContext transformType = event.getHand() == InteractionHand.MAIN_HAND
                    ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            renderer.renderFirstPerson(
                    player,
                    stack,
                    transformType,
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight(),
                    event.getPartialTick()
            );
            event.setCanceled(true);
        });
    }

    public static boolean shouldLockVanilla() {
        return lockVanilla;
    }

    public static float getTargetHeight() {
        return nextIsCustom ? 1.0F : 0.0F;
    }

    public static IFPAnimationInstance getActiveAnimationInstance() {
        return transitioning ? previousInstance : activeInstance;
    }

    private static IFPAnimationInstance createInstance(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.createAnimationInstance(stack, Minecraft.getInstance().getCameraEntity()))
                .orElse(null);
    }

    private static boolean isCustomItem(ItemStack stack) {
        return getRenderer(stack).isPresent();
    }

    private static long calculateSheatheDuration(ItemStack stack) {
        return getRenderer(stack)
                .map(r -> r.getPutAwayDuration(stack))
                .orElse(0L);
    }

    private static float getSheatheProgress() {
        if (currentSheatheDuration <= 0) {
            return 1.0f;
        }
        long elapsed = CLOCK.nowMillis() - switchStartTime;
        return Math.min(1.0f, (float) elapsed / currentSheatheDuration);
    }

    private static Optional<IFPGeoItemRenderer> getRenderer(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (IClientItemExtensions.of(stack.getItem()).getCustomRenderer() instanceof IFPGeoItemRenderer renderer) {
            return Optional.of(renderer);
        }
        return Optional.empty();
    }

    private static boolean isSameItemStacks(ItemStack oldStack, ItemStack newStack) {
        if (oldStack == newStack) {
            return true;
        }
        if (oldStack.isEmpty() && newStack.isEmpty()) {
            return true;
        }
        if (oldStack.isEmpty() || newStack.isEmpty()) {
            return false;
        }

        return getRenderer(oldStack)
                .map(r -> r.isSameItem(oldStack, newStack))
                .orElseGet(() -> ItemStack.isSameItem(oldStack, newStack));
    }
}
