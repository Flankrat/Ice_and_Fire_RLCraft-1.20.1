package com.github.alexthe666.iceandfire.mixins;

import com.github.alexthe666.iceandfire.compat.spartan.SpartanCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the gilded Spartan weapons the enchantment glint, like Spartan Fire's flamed, iced and lightning weapons
 * (their item classes belong to Spartan Weaponry, so the glint can't come from an override).
 */
@Mixin(Item.class)
public class GildedWeaponFoilMixin {
    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void iceandfire$gildedFoil(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // empty set without Spartan installed, so this is a no-op then
        if (SpartanCompat.GILDED_WEAPONS.contains((Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
