package com.github.alexthe666.iceandfire.compat.spartan;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the "Legendary Weapon" line under the name of the gilded Spartan weapons, as Spartan Fire does for its elemental ones. */
@Mod.EventBusSubscriber(modid = IceAndFire.MODID, value = Dist.CLIENT)
public final class GildedWeaponClientEvents {
    private GildedWeaponClientEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (SpartanCompat.GILDED_WEAPONS.contains(event.getItemStack().getItem()) && !event.getToolTip().isEmpty()) {
            // index 1 = right below the item name
            event.getToolTip().add(1, Component.translatable("item.iceandfire.legendary_weapon.desc").withStyle(ChatFormatting.GOLD));
        }
    }
}
