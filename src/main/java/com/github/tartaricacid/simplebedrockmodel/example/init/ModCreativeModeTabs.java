package com.github.tartaricacid.simplebedrockmodel.example.init;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleBedrockModel.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",  () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.quantum_ocean"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .displayItems((parameters, output) -> {
                output.accept(ModItems.TEST_BLOCK_ITEM.get());
            })
            .build());
}
