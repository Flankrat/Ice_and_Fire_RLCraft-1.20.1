package com.github.alexthe666.iceandfire.entity.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The 1.12 version's plain Ice and Fire mob spawner: a dread spawner's logic with flame particles. */
public class TileEntityMonsterSpawner extends TileEntityDreadSpawner {
    public TileEntityMonsterSpawner(BlockPos pos, BlockState state) {
        super(pos, state, IafTileEntityRegistry.MONSTER_SPAWNER.get());
    }

    @Override
    protected void spawnIdleParticle(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
