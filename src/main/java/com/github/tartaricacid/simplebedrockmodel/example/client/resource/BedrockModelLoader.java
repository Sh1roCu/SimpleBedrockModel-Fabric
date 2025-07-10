package com.github.tartaricacid.simplebedrockmodel.example.client.resource;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockModel;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.BedrockModelPOJO;
import com.github.tartaricacid.simplebedrockmodel.client.resource.manager.BedrockModelRegister;
import com.github.tartaricacid.simplebedrockmodel.client.resource.manager.BedrockModelRegisterEvent;
import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class BedrockModelLoader {
    private static final Map<ResourceLocation, Function<BedrockModelPOJO, ? extends BedrockModel>> ALL_MODELS = Maps.newHashMap();

    public static final ResourceLocation TEST = registerSimpleBlockModel("test");

    public static ResourceLocation registerSimpleBlockModel(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "bedrock/block/" + name);
        return registerSimpleModel(location);
    }

    public static ResourceLocation registerSimpleEntityModel(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "bedrock/entity/" + name);
        return registerSimpleModel(location);
    }

    public static ResourceLocation registerSimpleModel(ResourceLocation location) {
        return registerModel(location, BedrockModel::new);
    }

    public static ResourceLocation registerBlockModel(String name, Function<BedrockModelPOJO, ? extends BedrockModel> function) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "bedrock/block/" + name);
        return registerModel(location, function);
    }

    public static ResourceLocation registerEntityModel(String name, Function<BedrockModelPOJO, ? extends BedrockModel> function) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(SimpleBedrockModel.MOD_ID, "bedrock/entity/" + name);
        return registerModel(location, function);
    }

    public static ResourceLocation registerModel(ResourceLocation location, Function<BedrockModelPOJO, ? extends BedrockModel> function) {
        ALL_MODELS.put(location, function);
        return location;
    }

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockModelRegisterEvent event) {
        ALL_MODELS.forEach(event::register);
        ALL_MODELS.clear();
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelRegister.INSTANCE.getModel(location);
    }
}
