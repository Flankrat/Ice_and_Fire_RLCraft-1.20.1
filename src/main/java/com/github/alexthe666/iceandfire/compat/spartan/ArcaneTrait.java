package com.github.alexthe666.iceandfire.compat.spartan;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.api.ArcaneBurstUtils;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;
import com.oblivioussp.spartanweaponry.api.WeaponMaterial;
import com.oblivioussp.spartanweaponry.api.trait.WeaponTrait;
import krelox.spartantoolkit.BetterWeaponTrait;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The gilded dragon bone weapons' trait, the gold counterpart of Spartan Fire's Shocked: knocks the target back, sets
 * off an arcane burst (on cooldown, see {@link ArcaneBurstUtils}) and deals extra magic damage to fire, ice and
 * lightning dragons, like the gilded dragon bone sword.
 */
public class ArcaneTrait extends BetterWeaponTrait {
    /** Extra damage against fire, ice and lightning dragons (the elemental swords' 9.5 bonus). */
    public static final float DRAGON_BONUS = 9.5F;

    public ArcaneTrait() {
        super("arcane", IceAndFire.MODID, WeaponTrait.TraitQuality.POSITIVE);
        setUniversal();
    }

    @Override
    public String getDescription() {
        return "Strikes foes with an arcane burst that also hits nearby hostile mobs";
    }

    @Override
    public void onHitEntity(WeaponMaterial material, ItemStack stack, LivingEntity target, LivingEntity attacker, Entity projectile) {
        // knock the target away from the attacker, like the other elemental traits
        target.knockback(1.0D, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
        if (target.level().isClientSide) {
            return;
        }
        if (IafConfig.dragonWeaponGoldAbility) {
            // thrown weapons and shot bolts skip the cooldown, like gilded dragon bone arrows
            boolean projectileHit = projectile != null && projectile != attacker;
            ArcaneBurstUtils.createArcaneBurst(target.level(), target, attacker, !projectileHit);
        }
        if (target instanceof EntityFireDragon || target instanceof EntityIceDragon || target instanceof EntityLightningDragon) {
            target.invulnerableTime = 0;
            target.hurt(target.level().damageSources().indirectMagic(attacker, attacker), DRAGON_BONUS);
        }
    }
}
