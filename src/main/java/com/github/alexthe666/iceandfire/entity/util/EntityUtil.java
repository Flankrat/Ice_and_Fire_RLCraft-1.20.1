package com.github.alexthe666.iceandfire.entity.util;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EntityUtil {
    // Vanilla's attack/interact reach (ServerGamePacketListenerImpl: distance from the eyes to the target's box),
    // plus 2 blocks for multipart parts whose client position lags the server's
    private static final double MULTIPART_REACH_SQ = 8 * 8;

    /**
     * Whether the player can reach the parent entity or one of its multipart parts: the server-side check for the
     * multipart packets a client sends when it hits or uses a part (clients can't be trusted to only send those
     * within reach).
     */
    public static boolean isInReachOfMultipart(@NotNull final Player player, @NotNull final Entity parent) {
        Vec3 eyes = player.getEyePosition();
        if (parent.getBoundingBox().distanceToSqr(eyes) <= MULTIPART_REACH_SQ) {
            return true;
        }
        AABB around = new AABB(eyes, eyes).inflate(8);
        for (EntityMutlipartPart part : player.level().getEntitiesOfClass(EntityMutlipartPart.class, around)) {
            if (part.getParent() == parent && part.getBoundingBox().distanceToSqr(eyes) <= MULTIPART_REACH_SQ) {
                return true;
            }
        }
        return false;
    }

    public static void updatePart(@Nullable final EntityMutlipartPart part, @NotNull final LivingEntity parent) {
        if (part == null || !(parent.level() instanceof ServerLevel serverLevel) || parent.isRemoved()) {
            return;
        }

        if (!part.shouldContinuePersisting()) {
            UUID uuid = part.getUUID();
            Entity existing = serverLevel.getEntity(uuid);

            // Update UUID if a different entity with the same UUID exists already
            if (existing != null && existing != part) {
                while (serverLevel.getEntity(uuid) != null) {
                    uuid = Mth.createInsecureUUID(parent.getRandom());
                }

                IceAndFire.LOGGER.debug("Updated the UUID of [{}] due to a clash with [{}]", part, existing);
            }

            part.setUUID(uuid);
            serverLevel.addFreshEntity(part);
        }

        part.setParent(parent);
    }
}
