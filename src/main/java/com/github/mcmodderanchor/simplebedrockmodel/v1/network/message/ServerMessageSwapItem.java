package com.github.mcmodderanchor.simplebedrockmodel.v1.network.message;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.event.SwapItemWithOffHand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

public class ServerMessageSwapItem implements FabricPacket {

    public static final PacketType<ServerMessageSwapItem> TYPE = PacketType.create(SimpleBedrockModel.modLoc("swap_item"), ServerMessageSwapItem::decode);

    public static ServerMessageSwapItem decode(FriendlyByteBuf buf) {
        return new ServerMessageSwapItem();
    }

    @Environment(EnvType.CLIENT)
    public static void handle(ServerMessageSwapItem packet, LocalPlayer player, PacketSender responseSender) {
        SwapItemWithOffHand.EVENT.invoker().post(new SwapItemWithOffHand());
    }

    @Override
    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
