package com.github.tartaricacid.simplebedrockmodel.example.client.event;

import com.github.tartaricacid.simplebedrockmodel.example.client.render.blockentity.TestBlockEntityRenderer;
import com.github.tartaricacid.simplebedrockmodel.example.init.ModBlockEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BlockEntityRenderersRegisterHandler {
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntityTypes.TEST.get(), TestBlockEntityRenderer::new);
    }
}
