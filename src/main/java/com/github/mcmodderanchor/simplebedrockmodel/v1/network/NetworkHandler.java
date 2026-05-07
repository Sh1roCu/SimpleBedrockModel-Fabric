package com.github.mcmodderanchor.simplebedrockmodel.v1.network;

import com.github.mcmodderanchor.simplebedrockmodel.v1.network.message.ServerMessageSwapItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class NetworkHandler {

    @Environment(EnvType.CLIENT)
    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageSwapItem.TYPE, ServerMessageSwapItem::handle);
    }

    public static void sendToClientPlayer(FabricPacket message, ServerPlayer player) {
        ServerPlayNetworking.send(player, message);
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, FabricPacket message) {
        sendToTrackingEntity(message, centerEntity);

        if (centerEntity instanceof ServerPlayer serverPlayer) {
            sendToClientPlayer(message, serverPlayer);
        }
    }

    public static void sendToAllPlayers(FabricPacket message, MinecraftServer server) {
        for (ServerPlayer player : PlayerLookup.all(server)) {
            sendToClientPlayer(message, player);
        }
    }

    public static void sendToTrackingEntity(FabricPacket message, final Entity centerEntity) {
        for (ServerPlayer serverPlayer : PlayerLookup.tracking(centerEntity)) {
            sendToClientPlayer(message, serverPlayer);
        }
    }

    public static void sendToDimension(FabricPacket message, final Entity centerEntity) {
        if (centerEntity.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : PlayerLookup.world(serverLevel)) {
                sendToClientPlayer(message, player);
            }
        }
    }
}
