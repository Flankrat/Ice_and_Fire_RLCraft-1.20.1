package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageDragonSyncFire {

    public int dragonId;
    public double posX;
    public double posY;
    public double posZ;
    public int syncType;

    public MessageDragonSyncFire(int dragonId, double posX, double posY, double posZ, int syncType) {
        this.dragonId = dragonId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.syncType = syncType;
    }

    public MessageDragonSyncFire() {
    }

    public static MessageDragonSyncFire read(FriendlyByteBuf buf) {
        return new MessageDragonSyncFire(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void write(MessageDragonSyncFire message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeDouble(message.posX);
        buf.writeDouble(message.posY);
        buf.writeDouble(message.posZ);
        buf.writeInt(message.syncType);
    }

    // Farther than any breath or fire charge a ridden dragon aims (rayTraceRider reaches 10 blocks per stage)
    private static final double MAX_TARGET_DISTANCE_SQ = 128 * 128;

    /** A client only sends this while its player rides and owns the dragon and makes it breathe (syncType 0 = breath,
     *  5 = fire charge). Anything else, or a target that isn't a real nearby position, is ignored. */
    private static boolean isAllowedFromRider(Player player, EntityDragonBase dragon, MessageDragonSyncFire message) {
        if (dragon.getControllingPassenger() != player || !dragon.isOwnedBy(player)) {
            return false;
        }
        if (message.syncType != 0 && message.syncType != 5) {
            return false;
        }
        return Double.isFinite(message.posX) && Double.isFinite(message.posY) && Double.isFinite(message.posZ)
                && dragon.distanceToSqr(message.posX, message.posY, message.posZ) <= MAX_TARGET_DISTANCE_SQ;
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(final MessageDragonSyncFire message, final Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                Player player = context.getSender();

                if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    player = IceAndFire.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    Entity entity = player.level().getEntity(message.dragonId);

                    if (entity instanceof EntityDragonBase dragon) {
                        boolean fromClient = context.getDirection().getReceptionSide() == LogicalSide.SERVER;
                        if (!fromClient || isAllowedFromRider(player, dragon, message)) {
                            dragon.stimulateFire(message.posX, message.posY, message.posZ, message.syncType);
                        }
                    }
                }
            });

            context.setPacketHandled(true);
        }
    }

}
