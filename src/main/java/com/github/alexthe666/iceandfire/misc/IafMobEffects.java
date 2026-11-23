package com.github.alexthe666.iceandfire.misc;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

/** Status effects added by Ice and Fire. */
public class IafMobEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, IceAndFire.MODID);

    public static final ResourceKey<DamageType> ACID_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(IceAndFire.MODID, "acid"));

    /** Acid from hydra breath: 1 damage that ignores armor, every 25 ticks (faster at higher levels). Water washes it off. */
    public static final RegistryObject<MobEffect> ACID = EFFECTS.register("acid", () -> new MobEffect(MobEffectCategory.HARMFUL, 9227300) {
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            int interval = 25 >> amplifier;
            return interval <= 0 || duration % interval == 0;
        }

        @Override
        public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
            entity.hurt(acidDamage(entity), 1.0F);
        }
    });

    public static DamageSource acidDamage(LivingEntity entity) {
        return new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ACID_TYPE));
    }
}
