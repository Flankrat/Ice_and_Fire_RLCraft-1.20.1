package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.entity.EntityHippocampus;
import com.github.alexthe666.iceandfire.entity.EntityHippogryph;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageHippogryphArmor {

    public int dragonId;
    public int slot_index;
    public int armor_type;

    public MessageHippogryphArmor(int dragonId, int slot_index, int armor_type) {
        this.dragonId = dragonId;
        this.slot_index = slot_index;
        this.armor_type = armor_type;
    }

    public MessageHippogryphArmor() {
    }

    public static MessageHippogryphArmor read(FriendlyByteBuf buf) {
        return new MessageHippogryphArmor(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void write(MessageHippogryphArmor message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeInt(message.slot_index);
        buf.writeInt(message.armor_type);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(final MessageHippogryphArmor message, final Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                Player player = context.getSender();

                if (player != null) {
                    // The client only tells the server that the mount's inventory changed. Never trust the slot values it
                    // sends (anyone could send "saddled" for any mount): re-read the saddle, chest and armor from the
                    // server's own copy of the inventory instead.
                    Entity entity = player.level().getEntity(message.dragonId);
                    if (entity instanceof EntityHippogryph hippogryph) {
                        hippogryph.refreshInventory();
                    } else if (entity instanceof EntityHippocampus hippo) {
                        hippo.refreshInventory();
                    }
                }
            });

            context.setPacketHandled(true);
        }
    }
}