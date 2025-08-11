package example.capability;

import example.init.ExampleModRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCapability {
    public static Capability<IFPGunAnimationCapability> FPGUN_ANIMATION_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

    @SubscribeEvent
    public static void onEntityCapabilityAttach(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            event.addCapability(new ResourceLocation(ExampleModRegister.MOD_ID, "fpgun_animation"), new FPGunAnimationCapabilityProvider(player));
        }
    }
}
