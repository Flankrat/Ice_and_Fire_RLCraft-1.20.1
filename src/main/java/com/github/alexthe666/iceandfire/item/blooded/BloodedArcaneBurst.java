package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The blooded gold set's effect: a burst of gold dragon magic around a point. Every hostile mob in range takes
 * magic damage (magic ignores armor), fire, ice and lightning dragons take extra, and all of them are knocked
 * away from the wearer. Players, the wearer's allies and its tamed pets are never hit.
 */
public final class BloodedArcaneBurst {

    public static final float BURST_DAMAGE = 4.0F;
    // Same total (13.5) as the fire and ice sets' hit against the opposite dragon in defend mode
    public static final float ELEMENTAL_DRAGON_BONUS = 9.5F;
    public static final double RADIUS = 3.0D;

    // True while a burst is hurting mobs, so those hits don't set off another burst
    private static boolean bursting;

    private BloodedArcaneBurst() {
    }

    public static boolean isBursting() {
        return bursting;
    }

    /** The burst's damage against one mob. */
    public static float damageAgainst(LivingEntity entity) {
        return BURST_DAMAGE + (isElementalDragon(entity) ? ELEMENTAL_DRAGON_BONUS : 0.0F);
    }

    /**
     * Bursts around {@code center}: {@code center} itself and every hostile mob within {@link #RADIUS} take
     * magic damage from the wearer, except {@code alreadyHit} (the target of the attack-mode hit, which gets its
     * burst damage added to that hit instead, because it is still invulnerable from it). All of them are knocked back.
     */
    public static void burst(Player wearer, LivingEntity center, @Nullable LivingEntity alreadyHit) {
        if (!(wearer.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = wearer.damageSources().indirectMagic(wearer, wearer);
        bursting = true;
        try {
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(RADIUS),
                    entity -> entity == center || isBurstTarget(wearer, entity))) {
                if (entity != alreadyHit) {
                    entity.hurt(source, damageAgainst(entity));
                }
                entity.knockback(1.0F, wearer.getX() - entity.getX(), wearer.getZ() - entity.getZ());
            }
        } finally {
            bursting = false;
        }
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.getX(), center.getY(0.5D), center.getZ(), 40,
                RADIUS * 0.5D, 0.5D, RADIUS * 0.5D, 0.2D);
        level.playSound(null, center.getX(), center.getY(), center.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static boolean isElementalDragon(LivingEntity entity) {
        return entity instanceof EntityFireDragon || entity instanceof EntityIceDragon || entity instanceof EntityLightningDragon;
    }

    // Hostile mobs, dragons and mobs that are after the wearer; never players, allies or the wearer's pets
    private static boolean isBurstTarget(Player wearer, LivingEntity entity) {
        if (entity == wearer || !entity.isAlive() || entity instanceof Player || wearer.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null && owner.equals(wearer.getUUID())) {
                return false;
            }
        }
        return entity instanceof Enemy || entity instanceof EntityDragonBase
                || (entity instanceof Mob mob && mob.getTarget() == wearer);
    }
}
