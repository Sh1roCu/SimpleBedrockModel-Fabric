package com.github.tartaricacid.simplebedrockmodel.client.manager;

import cn.sh1rocu.simplebedrockmodel.api.event.RegisterClientReloadListenersEvent;
import com.github.tartaricacid.simplebedrockmodel.client.bedrock.AbstractBedrockEntityModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

import java.util.Set;

@Environment(EnvType.CLIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
public class BedrockEntityModelRegister<T extends AbstractBedrockEntityModel<? extends Entity>> {
    public static BedrockEntityModelRegister INSTANCE = null;
    private final BedrockEntityModelSet<T> modelSet;

    private BedrockEntityModelRegister(BedrockEntityModelSet<T> modelSet) {
        this.modelSet = modelSet;
    }

    public static void onRegisterClientReloadListenersEvent(RegisterClientReloadListenersEvent event) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        if (resourceManager instanceof ReloadableResourceManager manager) {
            INSTANCE = new BedrockEntityModelRegister<>(new BedrockEntityModelSet<>());
            BedrockEntityModelRegisterEvent.CALLBACK.invoker().post(new BedrockEntityModelRegisterEvent(INSTANCE.modelSet));
            // 将注册冻结
            INSTANCE.modelSet.immutableKnowLocations();
            // 添加到最前面，避免实体读取模型时模型还没加载完成
            manager.listeners.add(0, INSTANCE.modelSet);
        }
    }

    public AbstractBedrockEntityModel<? extends Entity> getModel(ResourceLocation location) {
        return modelSet.getModels().get(location);
    }

    public Set<ResourceLocation> getAllModelKeys() {
        return modelSet.getModels().keySet();
    }
}