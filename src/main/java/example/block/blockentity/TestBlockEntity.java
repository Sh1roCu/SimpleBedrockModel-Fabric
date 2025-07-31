package example.block.blockentity;

import com.maydaymemory.mae.control.misc.RealtimeVelocityEstimatorNode;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import example.client.animation.TestAnimationContext;
import example.init.ExampleModRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TestBlockEntity extends BlockEntity {
    @OnlyIn(Dist.CLIENT)
    public AnimationStateMachine<TestAnimationContext> stateMachine;

    @OnlyIn(Dist.CLIENT)
    public RealtimeVelocityEstimatorNode velocityEstimatorNode;

    public TestBlockEntity(BlockPos pos, BlockState state) {
        super(ExampleModRegister.TEST_BLOCK_ENTITY_TYPE, pos, state);
    }

    @OnlyIn(Dist.CLIENT)
    public void nextAnimation() {
        stateMachine.getContext().needTransition = true;
    }

    @OnlyIn(Dist.CLIENT)
    public void tick() {
        velocityEstimatorNode.tick();
        stateMachine.tick();
    }
}
