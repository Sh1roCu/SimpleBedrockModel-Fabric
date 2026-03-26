package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDeserializer;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * 粒子定义资源加载器。
 * <p>
 * 扫描 {@code assets/<namespace>/particle_definitions/<name>.json} 路径，
 * 解析基岩版粒子效果定义并缓存。
 */
public class ParticleDefinitionLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
    private static final String DIRECTORY = "particle_definitions";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ParticleEffectDefinition.class, new ParticleEffectDeserializer())
            .create();

    private static ParticleDefinitionLoader INSTANCE;

    private final Map<ResourceLocation, ParticleEffectDefinition> cache = Maps.newHashMap();
    /** 按粒子效果的 identifier（JSON 中的 description.identifier）索引 */
    private final Map<ResourceLocation, ParticleEffectDefinition> identifierIndex = Maps.newHashMap();

    public static ParticleDefinitionLoader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ParticleDefinitionLoader();
        }
        return INSTANCE;
    }

    @Override
    @NotNull
    @ParametersAreNonnullByDefault
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> result = Maps.newHashMap();
        resourceManager.listResources(DIRECTORY, loc -> loc.getPath().endsWith(".json"))
                .forEach((location, resource) -> {
                    try (InputStream stream = resource.open()) {
                        JsonElement json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                        // 从路径中提取 ID：particle_definitions/xxx.json -> xxx
                        String path = location.getPath();
                        String name = path.substring(DIRECTORY.length() + 1, path.length() - 5); // 去掉 .json
                        ResourceLocation id = new ResourceLocation(location.getNamespace(), name);
                        result.put(id, json);
                    } catch (IOException e) {
                        SimpleBedrockModel.LOGGER.error("Failed to read particle definition: {}", location, e);
                    }
                });
        return result;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        cache.clear();
        identifierIndex.clear();
        prepared.forEach((id, json) -> {
            try {
                ParticleEffectDefinition definition = GSON.fromJson(json, ParticleEffectDefinition.class);
                if (definition != null) {
                    cache.put(id, definition);
                    identifierIndex.put(definition.getIdentifier(), definition);
                    SimpleBedrockModel.LOGGER.debug("Loaded particle definition: {}", id);
                }
            } catch (Exception e) {
                SimpleBedrockModel.LOGGER.error("Failed to parse particle definition: {}", id, e);
            }
        });
        SimpleBedrockModel.LOGGER.info("Loaded {} particle definitions", cache.size());
    }

    /**
     * 获取已加载的粒子效果定义（按文件路径 ID 查找）。
     */
    @Nullable
    public ParticleEffectDefinition getDefinition(ResourceLocation id) {
        // 先按文件路径查找
        ParticleEffectDefinition def = cache.get(id);
        if (def != null) return def;
        // 再按 identifier 查找
        return identifierIndex.get(id);
    }

    /**
     * 获取所有已加载的粒子效果定义。
     */
    public Map<ResourceLocation, ParticleEffectDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(cache);
    }
}
