package com.github.alexthe666.iceandfire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface IDreadBlock {

    /** A dread block that generated with a structure (not placed by a player), which players can't break. */
    static boolean isIndestructible(BlockState state) {
        return state.getBlock() instanceof IDreadBlock
            && state.hasProperty(BlockDreadBase.PLAYER_PLACED)
            && !state.getValue(BlockDreadBase.PLAYER_PLACED);
    }

    /**
     * Whether a position is inside a mausoleum, as in the 1.12 version: there is a generated dread block within
     * 20 blocks above it and another within 20 blocks below it.
     */
    static boolean isBlockInsideMausoleum(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos current = pos.mutable();
        boolean above = false;
        for (int i = 0; i < 20 && !above; i++) {
            above = isIndestructible(level.getBlockState(current.setWithOffset(pos, 0, i, 0)));
        }
        if (!above) {
            return false;
        }
        for (int i = 0; i < 20; i++) {
            if (isIndestructible(level.getBlockState(current.setWithOffset(pos, 0, -i, 0)))) {
                return true;
            }
        }
        return false;
    }
}
