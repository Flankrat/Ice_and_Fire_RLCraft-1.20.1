package com.github.alexthe666.iceandfire.integration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Optional Lycanites Mobs bridge matching the old 1.12.2 behaviour without
 * taking a hard compile-time dependency on Lycanites.
 */
public final class LycanitesCompat {
    private static final ResourceLocation PARALYSIS = new ResourceLocation("lycanitesmobs", "paralysis");

    private LycanitesCompat() {
    }

    public static boolean isEnabled() {
        return ModList.get().isLoaded("lycanitesmobs");
    }

    public static void applyParalysis(Entity entity, int duration) {
        if (!(entity instanceof LivingEntity living) || duration <= 0 || !isEnabled()) {
            return;
        }

        MobEffect paralysis = ForgeRegistries.MOB_EFFECTS.getValue(PARALYSIS);
        if (paralysis != null && !living.hasEffect(paralysis)) {
            living.addEffect(new MobEffectInstance(paralysis, duration));
        }
    }
}
