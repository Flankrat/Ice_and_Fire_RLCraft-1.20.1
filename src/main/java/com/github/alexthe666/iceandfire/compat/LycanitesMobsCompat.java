package com.github.alexthe666.iceandfire.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Optional Lycanites Mobs integration used by lightning dragons.
 *
 * <p>This class deliberately performs a runtime registry lookup instead of linking against
 * Lycanites classes. If Lycanites Mobs is not installed (or does not register its paralysis
 * effect), nothing is applied and Ice and Fire keeps working without that mod.</p>
 */
public final class LycanitesMobsCompat {
    private static final ResourceLocation PARALYSIS = new ResourceLocation("lycanitesmobs", "paralysis");
    private static final int PARALYSIS_DURATION = 10;

    private LycanitesMobsCompat() {
    }

    public static void applyParalysis(LivingEntity target) {
        if (target == null || target.level().isClientSide) {
            return;
        }

        MobEffect paralysis = ForgeRegistries.MOB_EFFECTS.getValue(PARALYSIS);
        if (paralysis != null) {
            target.addEffect(new MobEffectInstance(paralysis, PARALYSIS_DURATION, 0));
        }
    }
}
