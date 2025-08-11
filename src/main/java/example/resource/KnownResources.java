package example.resource;

import example.init.ExampleModRegister;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class KnownResources {
    public static final ArrayList<ResourceLocation> ANIMATIONS = new ArrayList<>();
    public static final ArrayList<ResourceLocation> MODELS = new ArrayList<>();

    public static final ResourceLocation TEST = registerAnimationAndModel(new ResourceLocation(ExampleModRegister.MOD_ID, "test"));
    public static final ResourceLocation DEAGLE = registerAnimationAndModel(new ResourceLocation(ExampleModRegister.MOD_ID, "deagle"));

    private static ResourceLocation registerAnimationAndModel(ResourceLocation location) {
        ANIMATIONS.add(location);
        MODELS.add(location);
        return location;
    }
}
