package com.github.mcmodderanchor.simplebedrockmodel.v1.client.compat.epicfight;

import net.fabricmc.loader.api.FabricLoader;

public class EpicFightCompat {
    private static final String EPIC_FIGHT = "epicfight";
    private static boolean LOADED = false;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded(EPIC_FIGHT)) {
            LOADED = true;
            EpicFightRegister.register();
        }
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static class EpicFightRegister {
        private static void register() {
//            try {
//                HumanoidModelBaker.registerNewTransformer(new BedrockArmorTransformer());
//                MinecraftForge.EVENT_BUS.addListener(BedrockArmorTransformer::getBedrockArmorTexturePath);
//            } catch (Exception e) {
//                throw new RuntimeException("Failed to register Epic Fight compatibility", e);
//            }
        }
    }

}
