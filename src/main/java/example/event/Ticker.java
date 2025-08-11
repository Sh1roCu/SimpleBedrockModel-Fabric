package example.event;

import example.animation.FPGunAnimationInstance;
import example.animation.GunAnimationGraph;
import example.capability.IFPGunAnimationCapability;
import example.capability.ModCapability;
import example.item.GunItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Ticker {
    private static int oldHotBarSelected = -1;

    @SubscribeEvent
    public static void onServerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            Player player = event.player;
            Inventory inventory = player.getInventory();
            // 需要先更新 animationGraph 再 tick，保持逻辑严密
            LazyOptional<IFPGunAnimationCapability> capabilityLazyOptional = player.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY);
            if (oldHotBarSelected != inventory.selected) {
                ItemStack selected = inventory.getSelected();
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
            capabilityLazyOptional.ifPresent(capability -> {
                GunAnimationGraph animationGraph = capability.getAnimationInstance().getAnimationGraph();
                if (animationGraph != null) {
                    animationGraph.tick();
                }
            });
        }
    }
}
