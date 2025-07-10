package com.github.tartaricacid.simplebedrockmodel.example.client.resource;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.animation.Animations;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.animation.BedrockAnimation;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.animation.BedrockModelBoneIndexProvider;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockModel;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockAnimationFile;
import com.github.tartaricacid.simplebedrockmodel.client.resource.manager.BedrockAnimationRegister;
import com.github.tartaricacid.simplebedrockmodel.client.resource.manager.BedrockAnimationRegisterEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class BedrockAnimationLoader {
    private static final Map<ResourceLocation, Function<BedrockAnimationFile, Map<String, BedrockAnimation>>> ALL_ANIMATIONS = Maps.newHashMap();

    public static final ResourceLocation TEST = registerSimpleBedrockAnimation(BedrockModelLoader.TEST, "test");

    public static ResourceLocation registerSimpleBedrockAnimation(ResourceLocation model, String name) {
        return registerSimpleBedrockAnimation(model, ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "bedrock/" + name));
    }

    @SuppressWarnings("unchecked")
    public static ResourceLocation registerSimpleBedrockAnimation(ResourceLocation modelLocation, ResourceLocation location) {
        return registerBedrockAnimation(location, pojo -> {
            BedrockModel model = BedrockModelLoader.getModel(modelLocation);
            if (model == null) {
                SimpleBedrockModel.LOGGER.error("Could not find model for {}", modelLocation);
                return null;
            }
            List<BedrockAnimation> animation = Animations.createAnimation(pojo, new BedrockModelBoneIndexProvider(model));
            return ImmutableMap.ofEntries(
                    animation.stream()
                            .map(a -> Map.entry(a.getName(), a))
                            .toArray(Map.Entry[]::new)
            );
        });
    }

    public static ResourceLocation registerBedrockAnimation(ResourceLocation location, Function<BedrockAnimationFile, Map<String, BedrockAnimation>> animation) {
        ALL_ANIMATIONS.put(location, animation);
        return location;
    }

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockAnimationRegisterEvent event) {
        ALL_ANIMATIONS.forEach(event::register);
        ALL_ANIMATIONS.clear();
    }

    public static Map<String, BedrockAnimation> getAnimations(ResourceLocation location) {
        return BedrockAnimationRegister.INSTANCE.getAnimations(location);
    }
}
