package com.github.tartaricacid.simplebedrockmodel.example.init;

import com.github.tartaricacid.simplebedrockmodel.SimpleBedrockModel;
import com.github.tartaricacid.simplebedrockmodel.example.block.blockentity.TestBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPE = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SimpleBedrockModel.MOD_ID);

    public static final RegistryObject<BlockEntityType<TestBlockEntity>> TEST = BLOCK_ENTITY_TYPE.register("test", () -> BlockEntityType.Builder.of(TestBlockEntity::new, ModBlocks.TEST.get()).build(null));
}
