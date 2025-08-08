package example.resource;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockModelPOJO;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;

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

public class BedrockModelResourceSet implements PreparableReloadListener {
    private final Dist dist;
    private final List<ResourceLocation> knownLocations;
    private final Lock lock = new ReentrantLock();
    private final Condition prepared = lock.newCondition();
    private volatile Map<ResourceLocation, BedrockModel> modelCache;

    public BedrockModelResourceSet(List<ResourceLocation> knownLocations, Dist dist) {
        this.knownLocations = knownLocations;
        this.dist = dist;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier pStage, ResourceManager manager, ProfilerFiller pPreparationsProfiler, ProfilerFiller pReloadProfiler, Executor pBackgroundExecutor, Executor pGameExecutor) {
        return CompletableFuture.runAsync(() -> {
            lock.lock();
            modelCache = null;
            lock.unlock();
            Map<ResourceLocation, BedrockModelPOJO> pojoMap = Maps.newHashMap();
            knownLocations.forEach(location -> {
                // 将 ID 转换成实际模型文件路径，默认是 <namespace>:models/bedrock/<path>.json
                ResourceLocation path = new ResourceLocation(location.getNamespace(), "models/bedrock/" + location.getPath() + ".json");
                manager.getResource(path).ifPresentOrElse(model -> {
                    SimpleBedrockModel.LOGGER.info("Loading bedrock model file: {}", path);
                    try (InputStream stream = model.open()) {
                        Gson gson = dist == Dist.CLIENT ? GsonUtil.CLIENT_GSON : GsonUtil.SERVER_NORMAL_GSON;
                        BedrockModelPOJO pojo = gson.fromJson(new InputStreamReader(stream), BedrockModelPOJO.class);
                        pojoMap.put(location, pojo);
                    } catch (IOException e) {
                        SimpleBedrockModel.LOGGER.error("Failed to load model file: {}", path, e);
                    }
                }, () -> SimpleBedrockModel.LOGGER.error("Not found model file: {}", path));
            });
            Map<ResourceLocation, BedrockModel> modelMap = Maps.newHashMap();
            for (Map.Entry<ResourceLocation, BedrockModelPOJO> entry : pojoMap.entrySet()) {
                BedrockModel bedrockModel = new BedrockModel(entry.getValue());
                modelMap.put(entry.getKey(), bedrockModel);
            }
            // 通知所有等待线程 modelCache 已准备
            lock.lock();
            try {
                this.modelCache = modelMap;
                prepared.signalAll();
            } finally {
                lock.unlock();
            }
        }, pBackgroundExecutor).thenCompose(pStage::wait);
    }

    public BedrockModel getModel(ResourceLocation resourceLocation) {
        // 模型实例是异步创建的，需要 await 等待
        lock.lock();
        try {
            while (modelCache == null) {
                prepared.await(); // 等待 modelCache 准备好
            }
            return modelCache.get(resourceLocation);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
