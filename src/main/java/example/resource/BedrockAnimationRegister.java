package example.resource;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Map;

public class BedrockAnimationRegister {
    public static BedrockAnimationRegister INSTANCE = new BedrockAnimationRegister();

    private final BedrockAnimationResourceSet resourceSet;

    private BedrockAnimationRegister() {
        resourceSet = new BedrockAnimationResourceSet(KnownResources.ANIMATIONS, FMLLoader.getDist());;
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = SimpleBedrockModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class BedrockAnimationClientRegister {
        @SubscribeEvent
        public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE.resourceSet);
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @Mod.EventBusSubscriber(modid = SimpleBedrockModel.MOD_ID, value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BedrockAnimationServerRegister {
        @SubscribeEvent
        public static void onRegisterReloadListener(AddReloadListenerEvent event) {
            event.addListener(INSTANCE.resourceSet);
        }
    }

    public Map<String, BedrockAnimation> getAnimations(ResourceLocation resourceLocation) {
        return resourceSet.getAnimations(resourceLocation);
    }
}
