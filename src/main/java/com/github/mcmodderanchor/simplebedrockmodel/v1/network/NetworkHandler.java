package com.github.mcmodderanchor.simplebedrockmodel.v1.network;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.network.message.ServerMessageSwapItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;

public class NetworkHandler {
    private static final String VERSION = "0.1.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SimpleBedrockModel.modLoc("network"),
            () -> VERSION, it -> it.equals(VERSION), it -> it.equals(VERSION));


    public static void init() {
        CHANNEL.registerMessage(PacketId.S_SLICED_PACKET.value(), ServerMessageSwapItem.class,
                ServerMessageSwapItem::encode, ServerMessageSwapItem::decode,
                ServerMessageSwapItem::handle,
                Optional.of(PLAY_TO_CLIENT));
    }

    public static void sendToClientPlayer(Object message, Player player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), message);
    }

    /**
     * 发送给所有监听此实体的玩家
     */
    public static void sendToTrackingEntityAndSelf(Entity centerEntity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> centerEntity), message);
    }

    public static void sendToAllPlayers(Object message) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    public static void sendToTrackingEntity(Object message, final Entity centerEntity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> centerEntity), message);
    }

    public static void sendToDimension(Object message, final Entity centerEntity) {
        ResourceKey<Level> dimension = centerEntity.level().dimension();
        CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
    }

    public enum PacketId {
        S_SLICED_PACKET(100),
        ;

        private final int id;

        PacketId(int id) {
            this.id = id;
        }

        int value() {
            return id;
        }

    }
}
