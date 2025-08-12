package example.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockModelEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.resource.RawResourceLoaders;
import example.init.ExampleModRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class KnownResources {
    public static final ArrayList<ResourceLocation> ANIMATION_AND_MODEL = new ArrayList<>();
    public static final ArrayList<ResourceLocation> MODEL = new ArrayList<>();

    public static final ResourceLocation TEST = registerAnimationAndModel(new ResourceLocation(ExampleModRegister.MOD_ID, "test"));
    public static final ResourceLocation DEAGLE = registerAnimationAndModel(new ResourceLocation(ExampleModRegister.MOD_ID, "deagle"));

    private static ResourceLocation registerAnimationAndModel(ResourceLocation location) {
        ANIMATION_AND_MODEL.add(location);
        return location;
    }

    private static ResourceLocation registerModel(ResourceLocation location) {
        MODEL.add(location);
        return location;
    }

    @SubscribeEvent
    public static void onAnimationRegister(RegisterBedrockAnimationEvent event) {
        for (ResourceLocation resourceLocation : ANIMATION_AND_MODEL) {
            event.register(resourceLocation, resourceLocation, RawResourceLoaders.COMMON_LOADER);
        }
    }

    @SubscribeEvent
    public static void onModelRegister(RegisterBedrockModelEvent event) {
        for (ResourceLocation resourceLocation : MODEL) {
            event.register(resourceLocation, RawResourceLoaders.COMMON_LOADER);
        }
        for (ResourceLocation resourceLocation : ANIMATION_AND_MODEL) {
            event.register(resourceLocation, RawResourceLoaders.COMMON_LOADER);
        }
    }
}
