package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The blooded gold armor's strike-back: gold dragon magic hits the mob that struck the wearer. Magic ignores armor;
 * fire, ice and lightning dragons take extra. BloodedDefendEffect knocks the attacker back afterwards, as for every set.
 */
public final class BloodedArcaneStrike {

    public static final float STRIKE_DAMAGE = 4.0F;
    // Same total (13.5) as the fire and ice sets' strike against the opposite dragon
    public static final float ELEMENTAL_DRAGON_BONUS = 9.5F;

    private BloodedArcaneStrike() {
    }

    /** The strike's damage against this attacker. */
    public static float damageAgainst(LivingEntity attacker) {
        boolean elementalDragon = attacker instanceof EntityFireDragon || attacker instanceof EntityIceDragon || attacker instanceof EntityLightningDragon;
        return STRIKE_DAMAGE + (elementalDragon ? ELEMENTAL_DRAGON_BONUS : 0.0F);
    }

    /** Hits only the attacker, with magic damage from the wearer. */
    public static void strike(Player wearer, LivingEntity attacker) {
        attacker.hurt(wearer.damageSources().indirectMagic(wearer, wearer), damageAgainst(attacker));
        if (wearer.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.5D), attacker.getZ(), 20,
                    attacker.getBbWidth() * 0.5D, attacker.getBbHeight() * 0.3D, attacker.getBbWidth() * 0.5D, 0.2D);
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }
}
