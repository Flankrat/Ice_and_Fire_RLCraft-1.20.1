package com.github.alexthe666.iceandfire.entity.props;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;

/**
 * Data-backed ice dragon freeze state.  This deliberately does not use a
 * MobEffect; it mirrors the old entity-effect/capability implementation.
 */
public class FrozenData {
    public int frozenTicks;
    public boolean isFrozen;

    private boolean triggerClientUpdate;

    public void tickFrozen(final LivingEntity entity) {
        if (!isFrozen) {
            return;
        }

        if (!entity.level().isClientSide()) {
            if (entity instanceof EntityIceDragon || entity.isDeadOrDying() || entity.isInLava()) {
                clearFrozen(entity);
                return;
            }

            // Old behaviour: fire breaks a normal freeze and is consumed doing so.
            if (entity.isOnFire()) {
                entity.clearFire();
                clearFrozen(entity);
                return;
            }

            if (frozenTicks > 0) {
                frozenTicks--;
            }
            if (frozenTicks <= 0) {
                clearFrozen(entity);
                return;
            }
        } else {
            // Keep the client-side render timer moving between sync packets.
            if (frozenTicks > 0) {
                frozenTicks--;
            } else {
                isFrozen = false;
                return;
            }
        }

        if (!(entity instanceof Player player && player.isCreative())) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25F, 1.0F, 0.25F));
            if (!(entity instanceof EnderDragon) && !entity.onGround()) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, -0.1D, 0.0D));
            }
        }
    }

    public void setFrozen(final LivingEntity target, int duration) {
        if (target instanceof EntityIceDragon || duration <= 0) {
            return;
        }

        if (!isFrozen) {
            target.playSound(SoundEvents.GLASS_PLACE, 1.0F, 1.0F);
        }

        frozenTicks = duration;
        isFrozen = true;
        triggerClientUpdate = true;
    }

    private void clearFrozen(final LivingEntity entity) {
        if (!isFrozen) {
            return;
        }

        for (int i = 0; i < 15; i++) {
            entity.level().addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, IafBlockRegistry.DRAGON_ICE.get().defaultBlockState()),
                    entity.getX() + ((entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth()),
                    entity.getY() + (entity.getRandom().nextDouble() * entity.getBbHeight()),
                    entity.getZ() + ((entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth()),
                    0.0D, 0.0D, 0.0D);
        }

        entity.playSound(SoundEvents.GLASS_BREAK, 3.0F, 1.0F);
        isFrozen = false;
        frozenTicks = 0;
        triggerClientUpdate = true;
    }

    public void serialize(final CompoundTag tag) {
        CompoundTag frozenData = new CompoundTag();
        frozenData.putInt("frozenTicks", frozenTicks);
        frozenData.putBoolean("isFrozen", isFrozen);
        tag.put("frozenData", frozenData);
    }

    public void deserialize(final CompoundTag tag) {
        CompoundTag frozenData = tag.getCompound("frozenData");
        frozenTicks = frozenData.getInt("frozenTicks");
        isFrozen = frozenData.getBoolean("isFrozen");
    }

    public boolean doesClientNeedUpdate() {
        if (triggerClientUpdate) {
            triggerClientUpdate = false;
            return true;
        }
        return false;
    }
}
