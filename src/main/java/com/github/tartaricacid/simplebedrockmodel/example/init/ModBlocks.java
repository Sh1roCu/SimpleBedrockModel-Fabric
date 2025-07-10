package com.github.tartaricacid.simplebedrockmodel.example.init;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import com.github.tartaricacid.simplebedrockmodel.example.block.TestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SimpleBedrockModel.MOD_ID);

    public static final RegistryObject<Block> TEST = BLOCKS.register("test", TestBlock::new);
}
