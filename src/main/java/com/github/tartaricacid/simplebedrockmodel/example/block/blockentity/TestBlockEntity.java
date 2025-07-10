package com.github.tartaricacid.simplebedrockmodel.example.block.blockentity;

import com.github.tartaricacid.simplebedrockmodel.example.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockEntity extends BlockEntity {
    public TestBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntityTypes.TEST.get(), pPos, pBlockState);
    }
}
