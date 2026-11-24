package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Blooded armor's set effect, as in the 1.12 version (the armor only defends; it adds nothing to the wearer's own
 * hits): when a mob hits the wearer in melee, a blooded armor piece strikes back at the attacker with its element. With First Aid the piece on the hit body part fires;
 * without it, a random piece fires, and only when all four pieces are blooded (any elements). The pieces share
 * Blooded Dragon Armor Set Effect Cooldown.
 */
public final class BloodedDefendEffect {
    private BloodedDefendEffect() {
    }

    /** Melee hits only: not arrows, fire, explosions or magic, and the attacker is the direct source. */
    public static boolean isMeleeHit(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
            && !source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) && !source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
            && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
    }

    /** Without First Aid: a random piece, if every armor slot holds a blooded piece. */
    public static void onWearerHit(Player wearer, LivingEntity attacker) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            if (!(wearer.getItemBySlot(slot).getItem() instanceof ItemBloodedArmor)) {
                return;
            }
        }
        onPieceHit(wearer, attacker, slots[wearer.getRandom().nextInt(slots.length)]);
    }

    /** The blooded piece in this slot (if any) strikes back at the attacker. */
    public static void onPieceHit(Player wearer, LivingEntity attacker, EquipmentSlot slot) {
        ItemStack stack = wearer.getItemBySlot(slot);
        if (!(stack.getItem() instanceof ItemBloodedArmor armor) || isOnCooldown(wearer)) {
            return;
        }
        switch (armor.getDragonType().getElement()) {
            case FIRE -> {
                // Extra damage against the opposite dragon type
                if (attacker instanceof EntityIceDragon) {
                    attacker.hurt(wearer.damageSources().inFire(), 13.5F);
                }
                attacker.setSecondsOnFire(5);
            }
            case ICE -> {
                if (attacker instanceof EntityFireDragon) {
                    attacker.hurt(wearer.damageSources().freeze(), 13.5F);
                }
                EntityDataProvider.getCapability(attacker).ifPresent(data -> data.frozenData.setFrozen(attacker, 200));
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                attacker.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
            }
            case LIGHTNING -> {
                if (attacker instanceof EntityFireDragon || attacker instanceof EntityIceDragon) {
                    attacker.hurt(wearer.damageSources().lightningBolt(), 4.0F);
                }
                ChainLightningUtils.createChainLightning(wearer.level(), attacker, wearer, 0, false);
            }
            // gold dragon magic at the attacker only
            case GOLD -> BloodedArcaneStrike.strike(wearer, attacker);
        }
        attacker.knockback(1.0F, wearer.getX() - attacker.getX(), wearer.getZ() - attacker.getZ());
    }

    // Starts the shared cooldown if it isn't running; the cooldown is shown on the worn chestplate
    private static boolean isOnCooldown(Player wearer) {
        int cooldown = IafConfig.bloodedDragonArmorSetEffectCooldown;
        if (cooldown <= 0) {
            return false;
        }
        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) {
            return false;
        }
        if (wearer.getCooldowns().isOnCooldown(chest.getItem())) {
            return true;
        }
        wearer.getCooldowns().addCooldown(chest.getItem(), cooldown);
        return false;
    }
}
