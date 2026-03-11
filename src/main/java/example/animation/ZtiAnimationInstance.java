package example.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleAnimationState;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.SimpleTransition;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;
import example.entity.Zti;

public class ZtiAnimationInstance {
    private final AnimationStateMachine<ZtiAnimationContext> stateMachine;

    public ZtiAnimationInstance(Zti entity) {
        SimpleAnimationState<ZtiAnimationContext> idle = new SimpleAnimationState.Builder<ZtiAnimationContext>()
                .evaluatePose(ZtiAnimationContext::evaluateCurrentPose)
                .build();
        SimpleAnimationState<ZtiAnimationContext> running = new SimpleAnimationState.Builder<ZtiAnimationContext>()
                .evaluatePose(ZtiAnimationContext::evaluateCurrentPose)
                .build();
        SimpleAnimationState<ZtiAnimationContext> attack = new SimpleAnimationState.Builder<ZtiAnimationContext>()
                .evaluatePose(ZtiAnimationContext::evaluateCurrentPose)
                .build();

        new SimpleTransition.Builder<ZtiAnimationContext>()
                .from(idle)
                .target(running)
                .duration(0.2f)
                .predicate(ZtiAnimationContext::isMoving)
                .afterTrigger(ctx -> ctx.playLooping(ctx.runningAnimation()))
                .build();
        new SimpleTransition.Builder<ZtiAnimationContext>()
                .from(running)
                .target(idle)
                .duration(0.2f)
                .predicate(ctx -> !ctx.isMoving())
                .afterTrigger(ctx -> ctx.playLooping(ctx.idleAnimation()))
                .build();
        new SimpleTransition.Builder<ZtiAnimationContext>()
                .from(idle, running)
                .target(attack)
                .duration(0.1f)
                .predicate(ZtiAnimationContext::consumeAttackTrigger)
                .afterTrigger(ctx -> ctx.playOnce(ctx.randomAttackAnimation()))
                .build();
        new SimpleTransition.Builder<ZtiAnimationContext>()
                .from(attack)
                .target(running)
                .duration(0.1f)
                .predicate(ctx -> ctx.isCurrentAnimationFinished() && ctx.isMoving())
                .afterTrigger(ctx -> ctx.playLooping(ctx.runningAnimation()))
                .build();
        new SimpleTransition.Builder<ZtiAnimationContext>()
                .from(attack)
                .target(idle)
                .duration(0.1f)
                .predicate(ctx -> ctx.isCurrentAnimationFinished() && !ctx.isMoving())
                .afterTrigger(ctx -> ctx.playLooping(ctx.idleAnimation()))
                .build();

        ZtiAnimationContext context = new ZtiAnimationContext(entity);
        context.playLooping(context.idleAnimation());
        this.stateMachine = new AnimationStateMachine<>(idle, context, System::nanoTime);
    }

    public void renderTick() {
        stateMachine.getContext().tick();
        stateMachine.tick();
    }

    public AnimationStateMachine<ZtiAnimationContext> getStateMachine() {
        return stateMachine;
    }
}
