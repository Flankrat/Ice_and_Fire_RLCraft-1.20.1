package com.github.alexthe666.iceandfire.entity.tile;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.enums.EnumParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TileEntityDreadSpawner extends SpawnerBlockEntity {
    // The spawner with a required spawn count whose server tick is running (server thread only)
    @Nullable
    private static TileEntityDreadSpawner tickingSpawner;
    private final BlockEntityType<?> type;
    private final DreadSpawnerBaseLogic spawner = new DreadSpawnerBaseLogic() {
        @Override
        public void broadcastEvent(Level p_155767_, @NotNull BlockPos p_155768_, int p_155769_) {
            // The block event has to name the block that's actually there, or the client never gets it
            p_155767_.blockEvent(p_155768_, p_155767_.getBlockState(p_155768_).getBlock(), p_155769_, 0);
        }

        @Override
        public void setNextSpawnData(@Nullable Level p_155771_, @NotNull BlockPos p_155772_, @NotNull SpawnData p_155773_) {
            super.setNextSpawnData(p_155771_, p_155772_, p_155773_);
            if (p_155771_ != null) {
                BlockState blockstate = p_155771_.getBlockState(p_155772_);
                p_155771_.sendBlockUpdated(p_155772_, blockstate, blockstate, 4);
            }

        }

        @Override
        protected void spawnIdleParticle(Level level, double x, double y, double z) {
            TileEntityDreadSpawner.this.spawnIdleParticle(level, x, y, z);
        }

        @Override
        @javax.annotation.Nullable
        public net.minecraft.world.level.block.entity.BlockEntity getSpawnerBlockEntity() {
            return TileEntityDreadSpawner.this;
        }
    };

    public TileEntityDreadSpawner(BlockPos pos, BlockState state) {
        this(pos, state, IafTileEntityRegistry.DREAD_SPAWNER.get());
    }

    protected TileEntityDreadSpawner(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state);
        this.type = type;
    }

    protected void spawnIdleParticle(Level level, double x, double y, double z) {
        IceAndFire.PROXY.spawnParticle(EnumParticles.Dread_Torch, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Nullable
    public static TileEntityDreadSpawner getTickingSpawner() {
        return tickingSpawner;
    }

    public int getRequiredSpawnCount() {
        return this.spawner.getRequiredSpawnCount();
    }

    public void setRequiredSpawnCount(int count) {
        this.spawner.setRequiredSpawnCount(count);
        this.setChanged();
    }

    /** Called for each mob this spawner spawns; once the count reaches 0 the client is told so it drops the red particles. */
    public void onRequiredSpawn() {
        int count = this.getRequiredSpawnCount();
        if (count <= 0 || this.level == null) {
            return;
        }
        this.setRequiredSpawnCount(count - 1);
        if (count == 1) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // Spawn eggs call this; the vanilla version sets SpawnerBlockEntity's own (unused) spawner instead of this one
    @Override
    public void setEntityId(@NotNull EntityType<?> entityType, @NotNull RandomSource random) {
        this.spawner.setEntityId(entityType, this.level, random, this.worldPosition);
    }

    @Override
    public void load(@NotNull CompoundTag p_155760_) {
        super.load(p_155760_);
        this.spawner.load(this.level, this.worldPosition, p_155760_);
    }

    // Block entities write their data in saveAdditional on 1.20 (this used to be a save(CompoundTag) method that
    // nothing called, so disk saves only had vanilla's unused spawner: RequiredSpawnCount and spawn egg changes
    // were lost whenever the chunk reloaded)
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag); // vanilla writes its own unused spawner here...
        tag.remove("SpawnData"); // ...so drop its SpawnData in case this spawner has none
        this.spawner.save(tag); // and write this spawner's state over it
    }

    public static void clientTick(Level p_155755_, BlockPos p_155756_, BlockState p_155757_, TileEntityDreadSpawner p_155758_) {
        p_155758_.spawner.clientTick(p_155755_, p_155756_);
    }

    public static void serverTick(Level p_155762_, BlockPos p_155763_, BlockState p_155764_, TileEntityDreadSpawner p_155765_) {
        // Vanilla spawns the mobs inside this call, so mobs joining the world meanwhile are this spawner's (see
        // ServerEvents.onSpawnerMobJoin). Forge's spawner events can't be used for this: PositionCheck passes no
        // spawner and FinalizeSpawn skips spawners with custom NBT.
        tickingSpawner = p_155765_.getRequiredSpawnCount() > 0 ? p_155765_ : null;
        try {
            p_155765_.spawner.serverTick((ServerLevel) p_155762_, p_155763_);
        } finally {
            tickingSpawner = null;
        }
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag compoundtag = this.saveWithoutMetadata();
        compoundtag.remove("SpawnPotentials");
        return compoundtag;
    }

    @Override
    public boolean triggerEvent(int p_59797_, int p_59798_) {
        return this.spawner.onEventTriggered(this.level, p_59797_) || super.triggerEvent(p_59797_, p_59798_);
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }

    @Override
    public @NotNull BaseSpawner getSpawner() {
        return this.spawner;
    }

    @Override
    public @NotNull BlockEntityType<?> getType() {
        return this.type != null ? this.type : super.getType();
    }

}