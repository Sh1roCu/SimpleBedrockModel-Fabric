package com.github.mcmodderanchor.simplebedrockmodel;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.config.WorldMeshConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleBedrockModel {
    public static final String MOD_ID = "simplebedrockmodel";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void setUp() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, WorldMeshConfig.SPEC);
    }

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
