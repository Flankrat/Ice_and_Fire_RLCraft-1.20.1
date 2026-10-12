package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemAlchemySword;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BloodedSetEffect {

    private BloodedSetEffect() {
    }

    public static void tryApplySetEffect(Player player, LivingEntity target, float damage) {
        if (player.level().isClientSide)
            return;

        BloodedDragonType.DragonElement element = getFullSetElement(player);
        if (element == null)
            return;

        if (isHoldingMatchingWeapon(player, element))
            return;

        switch (element) {
            case FIRE -> applyFireEffect(player, target);
            case ICE -> applyIceEffect(player, target);
            case LIGHTNING -> applyLightningEffect(player, target, damage);
        }
    }

    private static BloodedDragonType.DragonElement getFullSetElement(Player player) {
        BloodedDragonType.DragonElement element = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ItemBloodedArmor blooded))
                return null;
            BloodedDragonType.DragonElement pieceElement = blooded.getDragonType().getElement();
            if (element == null) {
                element = pieceElement;
            } else if (element != pieceElement) {
                return null;
            }
        }
        return element;
    }

    private static boolean isHoldingMatchingWeapon(Player player, BloodedDragonType.DragonElement element) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty())
            return false;

        if (mainHand.getItem() instanceof ItemAlchemySword) {
            return switch (element) {
                case FIRE -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_FIRE.get());
                case ICE -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_ICE.get());
                case LIGHTNING -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_LIGHTNING.get());
            };
        }

        return false;
    }

    private static void applyFireEffect(Player player, LivingEntity target) {
        if (!IafConfig.dragonWeaponFireAbility)
            return;
        target.setSecondsOnFire(5);
        target.knockback(1.0F, player.getX() - target.getX(), player.getZ() - target.getZ());
    }

    private static void applyIceEffect(Player player, LivingEntity target) {
        if (!IafConfig.dragonWeaponIceAbility)
            return;
        EntityDataProvider.getCapability(target).ifPresent(data -> data.frozenData.setFrozen(target, 200));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
        target.knockback(1.0F, player.getX() - target.getX(), player.getZ() - target.getZ());
    }

    private static void applyLightningEffect(Player player, LivingEntity target, float damage) {
        if (!IafConfig.dragonWeaponLightningAbility)
            return;
        ChainLightningUtils.createChainLightning(player.level(), target, player, damage);
        target.knockback(1.0F, player.getX() - target.getX(), player.getZ() - target.getZ());
    }
}
