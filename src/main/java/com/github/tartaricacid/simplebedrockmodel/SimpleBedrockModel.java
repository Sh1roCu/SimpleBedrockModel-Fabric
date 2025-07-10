package com.github.tartaricacid.simplebedrockmodel;

import com.github.tartaricacid.simplebedrockmodel.example.init.ModBlockEntityTypes;
import com.github.tartaricacid.simplebedrockmodel.example.init.ModBlocks;
import com.github.tartaricacid.simplebedrockmodel.example.init.ModCreativeModeTabs;
import com.github.tartaricacid.simplebedrockmodel.example.init.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SimpleBedrockModel.MOD_ID)
public class SimpleBedrockModel {
    public static final String MOD_ID = "simplebedrockmodel";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public SimpleBedrockModel() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(eventBus);
        ModBlocks.BLOCKS.register(eventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPE.register(eventBus);
        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(eventBus);
    }
}
