package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageSetMyrmexHiveNull {

    public MessageSetMyrmexHiveNull() {
    }

    public static MessageSetMyrmexHiveNull read(FriendlyByteBuf buf) {
        return new MessageSetMyrmexHiveNull();
    }

    public static void write(MessageSetMyrmexHiveNull message, FriendlyByteBuf buf) {
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSetMyrmexHiveNull message, Supplier<NetworkEvent.Context> context) {
            context.get().setPacketHandled(true);
            // Server -> client only (the sender is null on the client, so the old sender check meant this never ran)
            if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                IceAndFire.PROXY.setReferencedHive(null);
            }
        }
    }
}