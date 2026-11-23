package com.github.alexthe666.iceandfire.entity.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Spawner logic for Ice and Fire's spawners: its own particles, plus the 1.12 version's required spawn count
 * ("RequiredSpawnCount" in NBT). While the count is above 0 the spawner shows red particles and can't be broken
 * in survival; each mob it spawns lowers the count (see ServerEvents.onSpawnerPositionCheck).
 */
public abstract class DreadSpawnerBaseLogic extends BaseSpawner {
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F);

    private short spawnDelay = 20;
    private double spin;
    private double oSpin;
    private int requiredSpawnCount = 0;

    @Override
    public void clientTick(@NotNull Level p_151320_, @NotNull BlockPos p_151321_) {
        if (!this.isNearPlayer(p_151320_, p_151321_)) {
            this.oSpin = this.spin;
        } else {
            double d0 = (double) p_151321_.getX() + p_151320_.random.nextDouble();
            double d1 = (double) p_151321_.getY() + p_151320_.random.nextDouble();
            double d2 = (double) p_151321_.getZ() + p_151320_.random.nextDouble();
            p_151320_.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            if (this.requiredSpawnCount > 0) {
                p_151320_.addParticle(RED_DUST, d0, d1, d2, 0.0D, 0.0D, 0.0D);
            } else {
                this.spawnIdleParticle(p_151320_, d0, d1, d2);
            }
            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            }

            this.oSpin = this.spin;
            this.spin = (this.spin + (double) (1000.0F / ((float) this.spawnDelay + 200.0F))) % 360.0D;
        }

    }

    /** The particle shown once the required spawn count is met (or when there is none). */
    protected abstract void spawnIdleParticle(Level level, double x, double y, double z);

    private boolean isNearPlayer(Level p_151344_, BlockPos p_151345_) {
        return p_151344_.hasNearbyAlivePlayer((double) p_151345_.getX() + 0.5D, (double) p_151345_.getY() + 0.5D, (double) p_151345_.getZ() + 0.5D, 20);
    }

    @Override
    public void load(@Nullable Level level, @NotNull BlockPos pos, @NotNull CompoundTag tag) {
        super.load(level, pos, tag);
        this.requiredSpawnCount = tag.getShort("RequiredSpawnCount");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        super.save(tag);
        tag.putShort("RequiredSpawnCount", (short) this.requiredSpawnCount);
        return tag;
    }

    public int getRequiredSpawnCount() {
        return this.requiredSpawnCount;
    }

    public void setRequiredSpawnCount(int count) {
        this.requiredSpawnCount = Math.max(0, count);
    }

    @Override
    public double getSpin() {
        return spin;
    }

    @Override
    public double getoSpin() {
        return oSpin;
    }
}
