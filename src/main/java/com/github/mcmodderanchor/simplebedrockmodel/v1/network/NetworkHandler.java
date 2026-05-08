package com.github.mcmodderanchor.simplebedrockmodel.v1.network;

import com.github.mcmodderanchor.simplebedrockmodel.v1.network.message.ServerMessageSwapItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class NetworkHandler {
    public static void init() {
        registerS2CPackets();
    }

    private static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(ServerMessageSwapItem.TYPE, ServerMessageSwapItem.STREAM_CODEC);
    }

    @Environment(EnvType.CLIENT)
    public static void registerS2CReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageSwapItem.TYPE, ServerMessageSwapItem::handle);
    }

    public static void sendToClientPlayer(CustomPacketPayload message, ServerPlayer player) {
        ServerPlayNetworking.send(player, message);
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, CustomPacketPayload message) {
        sendToTrackingEntity(message, centerEntity);

        if (centerEntity instanceof ServerPlayer serverPlayer) {
            sendToClientPlayer(message, serverPlayer);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload message, MinecraftServer server) {
        for (ServerPlayer player : PlayerLookup.all(server)) {
            sendToClientPlayer(message, player);
        }
    }

    public static void sendToTrackingEntity(CustomPacketPayload message, final Entity centerEntity) {
        for (ServerPlayer serverPlayer : PlayerLookup.tracking(centerEntity)) {
            sendToClientPlayer(message, serverPlayer);
        }
    }

    public static void sendToDimension(CustomPacketPayload message, final Entity centerEntity) {
        if (centerEntity.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : PlayerLookup.world(serverLevel)) {
                sendToClientPlayer(message, player);
            }
        }
    }
}
