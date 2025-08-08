package example.animation;

import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.control.misc.RealtimeVelocityEstimatorNode;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Animation Instance 一般统合了某个游戏对象的所有动画需要的上下文，并且统一负责同步动画状态
 */
public class TestBlockAnimationInstance {
    private final AnimationStateMachine<TestBlockAnimationContext> stateMachine;

    @OnlyIn(Dist.CLIENT)
    private RealtimeVelocityEstimatorNode velocityEstimatorNode;

    public TestBlockAnimationInstance(BlockEntity blockEntity) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            velocityEstimatorNode = new RealtimeVelocityEstimatorNode(ArrayPoseBuilder::new, System::nanoTime);
            stateMachine = new AnimationStateMachine<>(TestBlockStateMachineState.INSTANCE, new TestBlockAnimationContext(velocityEstimatorNode, blockEntity), System::nanoTime);
            velocityEstimatorNode.getPoseSlot().connect(stateMachine.getOutputPort());
        } else {
            stateMachine = new AnimationStateMachine<>(TestBlockStateMachineState.INSTANCE, new TestBlockAnimationContext(null, blockEntity), System::nanoTime);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void renderTick() {
        velocityEstimatorNode.tick();
        stateMachine.tick();
    }

    public void tick() {
        stateMachine.tick();
    }

    public void triggerTransition() {
        stateMachine.getContext().needTransition = true;
    }

    public AnimationStateMachine<TestBlockAnimationContext> getStateMachine() {
        return stateMachine;
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("StateMachineContext", stateMachine.getContext().getUpdateTag());
        return tag;
    }

    public void handleUpdateTag(CompoundTag tag) {
        stateMachine.getContext().handleUpdateTag(tag.getCompound("StateMachineContext"));
    }
}
