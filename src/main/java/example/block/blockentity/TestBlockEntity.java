package example.block.blockentity;

import com.maydaymemory.mae.control.runner.AnimationRunner;
import example.init.ExampleModRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TestBlockEntity extends BlockEntity {
    private static final String[] ANIMATIONS = new String[]{
            "治疗魔法", "待机初始帧", "跑步", "抓取", "抓取 未命中", "跑步—>冲刺轻击",
            "走路", "待机", "待机—>防空技能", "待机—>蹲击", "待机—>推击", "待机—>轻击",
            "待机—>重击", "待机—>暗能量球", "待机—>格林爆破", "待机—>治疗魔法蓄力",
            "治疗魔法蓄力ing", "治疗魔法蓄力—>被打断（待机）", "治疗魔法蓄力—>治疗魔法释放"
    };

    @OnlyIn(Dist.CLIENT)
    public AnimationRunner animationRunner;
    private int currentAnimationIndex = 0;

    public TestBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleModRegister.TEST_BLOCK_ENTITY_TYPE, pos, state);
    }

    public String currentAnimation() {
        return ANIMATIONS[currentAnimationIndex];
    }

    public String nextAnimation() {
        currentAnimationIndex = (currentAnimationIndex + 1) % ANIMATIONS.length;
        return ANIMATIONS[currentAnimationIndex];
    }
}
