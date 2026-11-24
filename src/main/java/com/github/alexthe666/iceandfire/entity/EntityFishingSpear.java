package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A thrown fishing spear: a vanilla thrown trident (loyalty, channeling, impaling, pickup all work the same)
 * that hits for Fishing Spear Thrown Damage instead of the trident's 8.
 */
public class EntityFishingSpear extends ThrownTrident {

    public EntityFishingSpear(EntityType<? extends ThrownTrident> type, Level level) {
        super(type, level);
        this.tridentItem = new ItemStack(IafItemRegistry.FISHING_SPEAR.get());
    }

    public EntityFishingSpear(Level level, LivingEntity thrower, ItemStack stack) {
        this(IafEntityRegistry.FISHING_SPEAR.get(), level);
        this.setPos(thrower.getX(), thrower.getEyeY() - 0.1F, thrower.getZ());
        this.setOwner(thrower);
        // The thrown item keeps its enchantments and durability, and loyalty/glint are synced like a trident's
        this.tridentItem = stack.copy();
        this.entityData.set(ID_LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
        this.entityData.set(ID_FOIL, stack.hasFoil());
    }

    // Same as ThrownTrident.onHitEntity, only the base damage comes from the config
    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = (float) IafConfig.fishingSpearThrownDamage;
        if (target instanceof LivingEntity living) {
            damage += EnchantmentHelper.getDamageBonus(this.tridentItem, living.getMobType());
        }
        Entity owner = this.getOwner();
        DamageSource source = this.damageSources().trident(this, owner == null ? this : owner);
        this.dealtDamage = true;
        SoundEvent sound = SoundEvents.TRIDENT_HIT;
        if (target.hurt(source, damage)) {
            if (target.getType() == EntityType.ENDERMAN) {
                return;
            }
            if (target instanceof LivingEntity hit) {
                if (owner instanceof LivingEntity attacker) {
                    EnchantmentHelper.doPostHurtEffects(hit, attacker);
                    EnchantmentHelper.doPostDamageEffects(attacker, hit);
                }
                this.doPostHurtEffects(hit);
            }
        }
        // Bounce off the target like a trident
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        float volume = 1.0F;
        // Channeling calls lightning in a thunderstorm, like a trident
        if (this.level() instanceof net.minecraft.server.level.ServerLevel && this.level().isThundering() && EnchantmentHelper.hasChanneling(this.tridentItem)) {
            BlockPos pos = target.blockPosition();
            if (this.level().canSeeSky(pos)) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(this.level());
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(pos));
                    bolt.setCause(owner instanceof ServerPlayer player ? player : null);
                    this.level().addFreshEntity(bolt);
                    sound = SoundEvents.TRIDENT_THUNDER;
                    volume = 5.0F;
                }
            }
        }
        this.playSound(sound, volume, 1.0F);
    }

    // The spear itself, for the renderer
    public ItemStack getSpearItem() {
        return this.tridentItem;
    }
}
