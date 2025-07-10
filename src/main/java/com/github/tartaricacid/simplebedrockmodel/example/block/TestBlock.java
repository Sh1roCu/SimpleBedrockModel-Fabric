package com.github.tartaricacid.simplebedrockmodel.example.block;

import com.github.tartaricacid.simplebedrockmodel.example.block.blockentity.TestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public class TestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TestBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PODZOL)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .lightLevel(s -> 15)
                .noOcclusion()
                .ignitedByLava());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TestBlockEntity(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
