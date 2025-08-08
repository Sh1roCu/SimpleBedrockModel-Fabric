package example.resource;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BedrockAnimationResourceSet implements PreparableReloadListener {
    private final Dist dist;
    private final List<ResourceLocation> knownLocations;
    private final Lock lock = new ReentrantLock();
    private final Condition prepared = lock.newCondition();
    private volatile Map<ResourceLocation, Map<String, BedrockAnimation>> animationCache;

    public BedrockAnimationResourceSet(List<ResourceLocation> knownLocations, Dist dist) {
        this.knownLocations = knownLocations;
        this.dist = dist;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier pStage, ResourceManager manager, ProfilerFiller pPreparationsProfiler, ProfilerFiller pReloadProfiler, Executor pBackgroundExecutor, Executor pGameExecutor) {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            animationCache = null;
            lock.unlock();
            Map<ResourceLocation, BedrockAnimationFile> pojoMap = Maps.newHashMap();
            knownLocations.forEach(animationLocation -> {
                // 将 ID 转换成实际动画文件路径，默认是 <namespace>:animations/<path>.json
                ResourceLocation path = new ResourceLocation(animationLocation.getNamespace(), "animations/" + animationLocation.getPath() + ".json");
                manager.getResource(path).ifPresentOrElse(resource -> {
                    SimpleBedrockModel.LOGGER.info("Loading bedrock animation file: {}", path);
                    try (InputStream stream = resource.open()) {
                        Gson gson = dist == Dist.CLIENT ? GsonUtil.CLIENT_GSON : GsonUtil.SERVER_NORMAL_GSON;
                        BedrockAnimationFile pojo = gson.fromJson(new InputStreamReader(stream), BedrockAnimationFile.class);
                        pojoMap.put(animationLocation, pojo);
                    } catch (IOException e) {
                        SimpleBedrockModel.LOGGER.error("Failed to load animation file: {}", path, e);
                    }
                }, () -> SimpleBedrockModel.LOGGER.error("Not found animation file: {}", path));
            });
            Map<ResourceLocation, Map<String, BedrockAnimation>> animationCache = Maps.newHashMap();
            for (Map.Entry<ResourceLocation, BedrockAnimationFile> entry : pojoMap.entrySet()) {
                BedrockModel model = BedrockModelRegister.INSTANCE.getModel(entry.getKey());
                if (model == null) {
                    continue;
                }
                List<BedrockAnimation> animations = BedrockAnimation.createAnimation(entry.getValue(), model);
                Map<String, BedrockAnimation> animationMap = Maps.newHashMap();
                for (BedrockAnimation animation : animations) {
                    animationMap.put(animation.getName(), animation);
                }
                animationCache.put(entry.getKey(), animationMap);
            }
            // 通知所有等待线程 animationCache 已准备
            lock.lock();
            try {
                this.animationCache = animationCache;
                prepared.signalAll();
            } finally {
                lock.unlock();
            }
        }, pBackgroundExecutor).thenCompose(pStage::wait);
    }

    @Nullable
    public Map<String, BedrockAnimation> getAnimations(ResourceLocation resourceLocation) {
        // 由于动画是异步创建的，需要 await 等待
        lock.lock();
        try {
            while (animationCache == null) {
                prepared.await(); // 等待 modelCache 准备好
            }
            return animationCache.get(resourceLocation);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
