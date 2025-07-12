package example.client.resource;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.bedrock.model.BedrockModel;
import example.client.manager.BedrockModelRegister;
import example.client.manager.BedrockModelRegisterEvent;
import example.init.ExampleModRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BedrockModelLoader {
    public static final ResourceLocation TEST_MODEL = ExampleModRegister.modLoc("bedrock/block/test");

    @SubscribeEvent
    public static void onRegisterBedrockModelRenderers(BedrockModelRegisterEvent event) {
        event.register(TEST_MODEL, BedrockModel::new);
    }

    public static BedrockModel getModel(ResourceLocation location) {
        return BedrockModelRegister.INSTANCE.getModel(location);
    }
}
