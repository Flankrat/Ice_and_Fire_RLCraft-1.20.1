package com.github.alexthe666.iceandfire.entity.tile;

import com.github.alexthe666.iceandfire.entity.EntityGhost;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.NotNull;

public class TileEntityGhostChest extends ChestBlockEntity {

    // True once this chest has rolled for its ghost, so each chest only ever spawns one (saved to NBT)
    private boolean spawnChecked = false;
    // Percent chance (0-100) that the one roll spawns a ghost; structures can lower it through NBT
    private int spawnChance = 100;

    public TileEntityGhostChest(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.GHOST_CHEST.get(), pos, state);
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        // Only read the keys when present, so chests from older worlds keep the defaults (one ghost, 100%)
        if (nbt.contains("SpawnChecked", Tag.TAG_ANY_NUMERIC)) {
            this.spawnChecked = nbt.getBoolean("SpawnChecked");
        }
        if (nbt.contains("SpawnChance", Tag.TAG_ANY_NUMERIC)) {
            this.spawnChance = nbt.getInt("SpawnChance");
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putBoolean("SpawnChecked", this.spawnChecked);
        nbt.putInt("SpawnChance", this.spawnChance);
    }

    @Override
    public void startOpen(@NotNull Player player) {
        super.startOpen(player);
        // Spectators don't really open the chest, so they don't use up its ghost
        if (!player.isSpectator()) {
            this.checkSpawn(player);
        }
    }

    // Rolls this chest's one ghost (called when it's opened, and by BlockGhostChest when a player breaks it, like 1.12)
    public void checkSpawn(@NotNull Player player) {
        // Server only, and only the first time ever
        if (this.level instanceof ServerLevel serverLevel && !this.spawnChecked) {
            // A double chest calls startOpen on both halves, so they share one roll: if the other half
            // already used it (it was opened first this time, or rolled back in an older world), just copy that
            TileEntityGhostChest other = this.getOtherHalf();
            if (other == null || !other.spawnChecked) {
                if (serverLevel.getDifficulty() != Difficulty.PEACEFUL && serverLevel.random.nextInt(100) < this.spawnChance) {
                    this.spawnGhost(serverLevel, player);
                }
            }
            // The roll is used up whether or not a ghost came out, on both halves
            this.markSpawnChecked();
            if (other != null) {
                other.markSpawnChecked();
            }
        }
    }

    // Sets the one-ghost flag; setChanged makes the chunk save it
    private void markSpawnChecked() {
        this.spawnChecked = true;
        this.setChanged();
    }

    // Returns the ghost chest this one is joined to as a double chest, or null for a single chest
    private TileEntityGhostChest getOtherHalf() {
        BlockState state = this.getBlockState();
        if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        BlockPos otherPos = this.worldPosition.relative(ChestBlock.getConnectedDirection(state));
        return this.level.getBlockEntity(otherPos) instanceof TileEntityGhostChest otherChest ? otherChest : null;
    }

    // Spawns the chest's ghost, scaring and targeting the player who opened it
    private void spawnGhost(ServerLevel serverLevel, Player player) {
        EntityGhost ghost = IafEntityRegistry.GHOST.get().create(serverLevel);
        if (ghost == null) {
            return;
        }
        ghost.absMoveTo(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F,
            serverLevel.random.nextFloat() * 360F, 0);
        ghost.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(this.worldPosition), MobSpawnType.SPAWNER, null, null);
        if (!player.isCreative()) {
            ghost.setTarget(player);
        }
        ghost.setPersistenceRequired();
        ghost.setAnimation(EntityGhost.ANIMATION_SCARE);
        ghost.restrictTo(this.worldPosition, 4);
        ghost.setFromChest(true);
        serverLevel.addFreshEntity(ghost);
    }

    @Override
    protected void signalOpenCount(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, int p_155336_, int p_155337_) {
        super.signalOpenCount(level, pos, state, p_155336_, p_155337_);
        level.updateNeighborsAt(pos.below(), state.getBlock());
    }
}
