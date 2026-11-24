package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.util.MyrmexHive;
import com.github.alexthe666.iceandfire.item.ItemMyrmexStaff;
import com.github.alexthe666.iceandfire.world.MyrmexWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MessageGetMyrmexHive {

    public CompoundTag hive;

    public MessageGetMyrmexHive(CompoundTag hive) {
        this.hive = hive;
    }

    public MessageGetMyrmexHive() {
    }

    public static MessageGetMyrmexHive read(FriendlyByteBuf buf) {
        return new MessageGetMyrmexHive(buf.readNbt());
    }

    public static void write(MessageGetMyrmexHive message, FriendlyByteBuf buf) {
        buf.writeNbt(message.hive);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(final MessageGetMyrmexHive message, final Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    // Server -> client: the hive shown in the Myrmex Staff screens
                    MyrmexHive serverHive = MyrmexHive.fromNBT(message.hive);
                    CompoundTag tag = new CompoundTag();
                    serverHive.writeVillageDataToNBT(tag);
                    serverHive.readVillageDataFromNBT(tag);
                    IceAndFire.PROXY.setReferencedHive(serverHive);
                    return;
                }

                // Client -> server: the staff screens send the edited hive back when they close
                Player player = context.getSender();
                if (player == null || !message.hive.hasUUID("HiveUUID")) {
                    return;
                }
                UUID hiveId = message.hive.getUUID("HiveUUID");
                MyrmexHive realHive = MyrmexWorldData.get(player.level()).getHiveFromUUID(hiveId);
                // Only someone holding a staff linked to this hive may edit it (that's how the screens open)
                if (realHive == null || !isHoldingStaffFor(player, hiveId)) {
                    return;
                }
                // The screens edit rooms and settings, never who owns the hive, who lives in it or players' reputation: keep the server's values
                CompoundTag edited = message.hive.copy();
                CompoundTag current = realHive.toNBT();
                for (String key : new String[]{"HasOwner", "OwnerUUID", "HiveMembers", "PopSize", "Players"}) {
                    edited.remove(key);
                    if (current.contains(key)) {
                        edited.put(key, current.get(key));
                    }
                }
                realHive.readVillageDataFromNBT(edited);
            });

            context.setPacketHandled(true);
        }

        private static boolean isHoldingStaffFor(Player player, UUID hiveId) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof ItemMyrmexStaff && stack.getTag() != null && stack.getTag().hasUUID("HiveUUID")
                        && hiveId.equals(stack.getTag().getUUID("HiveUUID"))) {
                    return true;
                }
            }
            return false;
        }
    }
}