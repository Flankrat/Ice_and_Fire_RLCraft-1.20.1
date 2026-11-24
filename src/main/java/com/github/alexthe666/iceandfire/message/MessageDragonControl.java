package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.event.ServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageDragonControl {

    public int dragonId;
    public byte controlState;
    public int armor_type;
    private double posX;
    private double posY;
    private double posZ;

    public MessageDragonControl(int dragonId, byte controlState, double posX, double posY, double posZ) {
        this.dragonId = dragonId;
        this.controlState = controlState;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public MessageDragonControl() {
    }

    public static MessageDragonControl read(FriendlyByteBuf buf) {
        return new MessageDragonControl(buf.readInt(), buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void write(MessageDragonControl message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeByte(message.controlState);
        buf.writeDouble(message.posX);
        buf.writeDouble(message.posY);
        buf.writeDouble(message.posZ);
    }

    private double getPosX() {
        return posX;
    }

    private double getPosY() {
        return posY;
    }

    private double getPosZ() {
        return posZ;
    }

    // How far one control packet may move a ridden mount (a mount moves a few blocks per tick at most)
    private static final double MAX_STEP_SQ = 10 * 10;

    /** Moves the ridden mount to the client's position, unless that's a jump no mount can make in one tick. */
    private static void setRiddenPos(Entity mount, MessageDragonControl message) {
        double x = message.getPosX(), y = message.getPosY(), z = message.getPosZ();
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z) && mount.distanceToSqr(x, y, z) <= MAX_STEP_SQ) {
            mount.setPos(x, y, z);
        }
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(final MessageDragonControl message, final Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() -> {
                Player player = context.getSender();

                if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    player = IceAndFire.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    Entity entity = player.level().getEntity(message.dragonId);

                    if (ServerEvents.isRidingOrBeingRiddenBy(entity, player)) {
                        /*
                            For some of these entities the `setPos` is handled in `Entity#move`
                            Doing it here would cause server-side movement checks to fail (resulting in "moved wrongly" messages)
                        */
                        if (entity instanceof EntityDragonBase dragon) {
                            if (dragon.isOwnedBy(player)) {
                                dragon.setControlState(message.controlState);
                            }
                        } else if (entity instanceof EntityHippogryph hippogryph) {
                            if (hippogryph.isOwnedBy(player)) {
                                hippogryph.setControlState(message.controlState);
                            }
                        } else if (entity instanceof EntityHippocampus hippo) {
                            if (hippo.isOwnedBy(player)) {
                                hippo.setControlState(message.controlState);
                                setRiddenPos(hippo, message);
                            }
                        } else if (entity instanceof EntityDeathWorm deathWorm) {
                            if (deathWorm.isOwnedBy(player)) {
                                deathWorm.setControlState(message.controlState);
                                setRiddenPos(deathWorm, message);
                            }
                        } else if (entity instanceof EntityAmphithere amphithere) {
                            if (amphithere.isOwnedBy(player)) {
                                amphithere.setControlState(message.controlState);
                                // TODO :: Is this handled by Entity#move due to recent changes?
                                setRiddenPos(amphithere, message);
                            }
                        }
                    }
                }
            });

            context.setPacketHandled(true);
        }
    }
}