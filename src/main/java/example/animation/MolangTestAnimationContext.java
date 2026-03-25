package example.animation;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.DynamicQueryBinding;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.MolangContext;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang.MolangEngineHelper;
import com.github.mcmodderanchor.simplebedrockmodel.v1.event.RegisterBedrockAnimationReloadListenerEvent;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.runner.AnimationContext;
import com.maydaymemory.mae.control.runner.AnimationRunner;
import com.maydaymemory.mae.control.runner.LoopingState;
import example.resource.KnownResources;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.unnamed.mocha.MochaEngine;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Molang 动画测试用上下文。
 * 使用 AnimationRunner 驱动含 Molang 表达式的动画，更接近实际使用场景。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MolangTestAnimationContext {
    private static final MochaEngine<?> SHARED_ENGINE;
    private static final DynamicQueryBinding QUERY_BINDING;
    private static final MolangContext<Object> SHARED_CONTEXT;

    private static BedrockAnimation molangTestAnimation;
    @Nullable
    private static AnimationRunner runner;

    static {
        SHARED_ENGINE = MolangEngineHelper.createEngine();
        QUERY_BINDING = new DynamicQueryBinding();
        SHARED_CONTEXT = new MolangContext<>();

        // 注册 query.anim_time —— runner 求值时会读取当前进度
        QUERY_BINDING.registerProperty("anim_time", () -> {
            if (runner != null) {
                return (double) runner.getAnimationContext().getProgressInSecond();
            }
            return 0.0;
        });

        MolangEngineHelper.registerQueryBinding(SHARED_ENGINE, QUERY_BINDING);
        MolangEngineHelper.bindContext(SHARED_ENGINE, SHARED_CONTEXT);
    }

    public static MochaEngine<?> getSharedEngine() {
        return SHARED_ENGINE;
    }

    @SubscribeEvent
    public static void onAnimationReloadListenerRegister(RegisterBedrockAnimationReloadListenerEvent event) {
        event.register(map -> {
            List<BedrockAnimation> animations = map.get(KnownResources.MOLANG_TEST);
            if (animations != null && !animations.isEmpty()) {
                molangTestAnimation = animations.stream()
                        .filter(a -> a.getName().equals("molang_test"))
                        .findFirst()
                        .orElse(null);
                // 动画加载后创建 runner
                if (molangTestAnimation != null) {
                    AnimationContext ctx = new AnimationContext(molangTestAnimation.getSpecifiedEndTimeS());
                    ctx.setState(new LoopingState(System::nanoTime));
                    runner = new AnimationRunner(molangTestAnimation, ctx);
                }
            }
        });
    }

    /**
     * 每帧调用，驱动 AnimationRunner 更新进度。
     */
    public static void tick() {
        if (runner != null) {
            runner.tick();
        }
    }

    /**
     * 获取当前动画 Pose。
     */
    @Nullable
    public static Pose evaluatePose() {
        if (runner != null) {
            return runner.evaluate();
        }
        return null;
    }

    @Nullable
    public static AnimationRunner getRunner() {
        return runner;
    }
}
