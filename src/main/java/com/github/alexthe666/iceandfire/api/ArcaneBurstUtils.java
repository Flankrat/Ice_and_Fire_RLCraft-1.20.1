package com.github.alexthe666.iceandfire.api;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.util.IDeadMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Arcane burst, the gold dragon weapons' ability (the gold counterpart of {@link ChainLightningUtils}): a burst of gold
 * dragon magic at the target. The target and the hostile mobs within Arcane Burst Radius of it take Arcane Burst Damage
 * as magic (which ignores armor) and are knocked away from the attacker. Players, the attacker's allies and the pets of
 * players are never hit, except the target itself. Puts the held weapon on Arcane Burst Cooldown, like chain lightning;
 * arrows skip that. Other mods' gold weapons call this too.
 */
public final class ArcaneBurstUtils {

    private ArcaneBurstUtils() {
    }

    public static void createArcaneBurst(Level level, LivingEntity target, Entity attacker) {
        createArcaneBurst(level, target, attacker, true);
    }

    /**
     * @param applyPlayerCooldown false for projectiles: the bow isn't put on cooldown, like the lightning arrow
     */
    public static void createArcaneBurst(Level level, LivingEntity target, Entity attacker, boolean applyPlayerCooldown) {
        if (!(level instanceof ServerLevel serverLevel) || !target.isAttackable()) {
            return;
        }
        if (applyPlayerCooldown && attacker instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
                return;
            }
            player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), IafConfig.arcaneBurstCooldown);
        }
        float damage = (float) IafConfig.arcaneBurstDamage;
        DamageSource source = attacker instanceof LivingEntity living
                ? level.damageSources().indirectMagic(attacker, living) : level.damageSources().magic();
        double radius = IafConfig.arcaneBurstRadius;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius),
                entity -> entity == target || canBurst(entity, attacker))) {
            if (entity == target) {
                // the weapon's own hit just made the target invulnerable for a moment; the burst is a separate hit
                entity.invulnerableTime = 0;
            }
            entity.hurt(source, damage);
            entity.knockback(0.6F, attacker.getX() - entity.getX(), attacker.getZ() - entity.getZ());
        }
        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.5D), target.getZ(), 40,
                radius * 0.5D, 0.5D, radius * 0.5D, 0.2D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY(0.5D), target.getZ(), 30,
                radius * 0.5D, 0.5D, radius * 0.5D, 0.5D);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    // Hostile mobs and mobs that are after the attacker; never players, the attacker, its allies or players' pets
    private static boolean canBurst(LivingEntity entity, Entity attacker) {
        if (entity == attacker || !entity.isAlive() || !entity.isAttackable() || entity instanceof Player || entity.isAlliedTo(attacker)) {
            return false;
        }
        if (entity instanceof IDeadMob dead && dead.isMobDead()) {
            return false;
        }
        if (entity instanceof OwnableEntity ownable) {
            UUID owner = ownable.getOwnerUUID();
            if (owner != null && !(entity instanceof Mob mob && (mob.getTarget() == attacker || mob.getLastHurtByMob() == attacker))) {
                return false;
            }
        }
        return entity instanceof Enemy || (entity instanceof Mob mob && (mob.getTarget() == attacker || mob.getLastHurtByMob() == attacker));
    }
}
