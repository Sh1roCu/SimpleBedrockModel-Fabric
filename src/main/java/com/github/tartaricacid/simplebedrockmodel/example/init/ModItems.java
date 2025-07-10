package com.github.tartaricacid.simplebedrockmodel.example.init;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SimpleBedrockModel.MOD_ID);

    public static final RegistryObject<BlockItem> TEST_BLOCK_ITEM = ITEMS.register("test", () ->
            new BlockItem(ModBlocks.TEST.get(), new Item.Properties())
    );
}
