package example.resource;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

public class BedrockModelRegister {
    public static BedrockModelRegister INSTANCE = new BedrockModelRegister();

    private final BedrockModelResourceSet resourceSet;

    private BedrockModelRegister() {
        resourceSet = new BedrockModelResourceSet(KnownResources.MODELS, FMLLoader.getDist());
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = SimpleBedrockModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class BedrockModelClientRegister {
        @SubscribeEvent
        public static void onRegisterReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(INSTANCE.resourceSet);
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @Mod.EventBusSubscriber(modid = SimpleBedrockModel.MOD_ID, value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BedrockModelServerRegister {
        @SubscribeEvent
        public static void onRegisterReloadListener(AddReloadListenerEvent event) {
            event.addListener(INSTANCE.resourceSet);
        }
    }

    public BedrockModel getModel(ResourceLocation resourceLocation) {
        return resourceSet.getModel(resourceLocation);
    }
}
