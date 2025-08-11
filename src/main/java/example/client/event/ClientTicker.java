package example.client.event;

import example.animation.FPGunAnimationInstance;
import example.animation.GunAnimationGraph;
import example.capability.IFPGunAnimationCapability;
import example.capability.ModCapability;
import example.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientTicker {
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).ifPresent(capability -> {
                    GunAnimationGraph animationGraph = capability.getAnimationInstance().getAnimationGraph();
                    if (animationGraph != null) {
                        animationGraph.tick();
                    }
                });
            }
        }
    }

    private static int oldHotBarSelected = -1;

    @SubscribeEvent
    public static void onPlayerChangeSelect(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            Inventory inventory = player.getInventory();
            // 这里就先简单地判断有没有切换选中的格子，用于测试。
            if (oldHotBarSelected != inventory.selected) {
                ItemStack selected = inventory.getSelected();
                LazyOptional<IFPGunAnimationCapability> capabilityLazyOptional = player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY);
                if (selected.getItem() instanceof GunItem gunItem) {
                    capabilityLazyOptional.ifPresent(capability -> {
                        FPGunAnimationInstance animationInstance = capability.getAnimationInstance();
                        animationInstance.updateAnimationGraphAndDraw(gunItem.getAnimationGraph(animationInstance));
                    });
                } else {
                    capabilityLazyOptional.ifPresent(capability -> {
                        capability.getAnimationInstance().updateAnimationGraphAndDraw(null);
                    });
                }
                oldHotBarSelected = inventory.selected;
            }
        }
    }
}
