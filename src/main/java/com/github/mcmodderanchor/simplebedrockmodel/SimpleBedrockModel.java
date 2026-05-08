package com.github.mcmodderanchor.simplebedrockmodel;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleBedrockModel {
    public static final String MOD_ID = "simplebedrockmodel";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static void setUp() {

    }

    public static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
