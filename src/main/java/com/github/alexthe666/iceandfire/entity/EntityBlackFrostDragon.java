package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.entity.ai.*;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.entity.util.IDreadMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * From the 1.12 version: the Black Frost, an undead ice dragon that serves the Dread Queen. It looks for a queen
 * nearby to carry, attacks whatever she attacks, is on the dread team, never sleeps or despawns, and drops nothing
 * (no scales, eggs, heart, blood or skull).
 */
public class EntityBlackFrostDragon extends EntityIceDragon implements IDreadMob {
    protected static final EntityDataAccessor<Optional<UUID>> COMMANDER_UNIQUE_ID = SynchedEntityData.defineId(EntityBlackFrostDragon.class, EntityDataSerializers.OPTIONAL_UUID);
    // Set while DreadAIDragonFindQueen runs, so the dragon stays on the ground where the queen can reach it
    private boolean seekingQueen = false;

    public EntityBlackFrostDragon(EntityType<?> t, Level worldIn) {
        super(t, worldIn);
        this.maximumArmor = 70D;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COMMANDER_UNIQUE_ID, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DreadAIDragonFindQueen(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new DragonAIAttackMelee(this, 1.5D, false));
        this.goalSelector.addGoal(5, new DragonAIWander(this, 1.0D));
        this.goalSelector.addGoal(6, new DragonAIWatchClosest(this, LivingEntity.class, 6.0F));
        this.goalSelector.addGoal(6, new DragonAILookIdle(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new DragonAIHurtByTarget(this));
        this.targetSelector.addGoal(4, new DreadAITargetNonDread(this, LivingEntity.class, false, entity -> entity != null && DragonUtils.canHostilesTarget(entity)));
        this.targetSelector.addGoal(5, new DragonAITargetItems<>(this, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Fight whatever the queen riding it fights
        EntityDreadQueen queen = this.getRidingQueen();
        if (!this.level().isClientSide && queen != null && queen.getTarget() != null && queen.getTarget() != this.getTarget()) {
            this.setTarget(queen.getTarget());
        }
    }

    @Nullable
    public EntityDreadQueen getRidingQueen() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof EntityDreadQueen queen) {
                return queen;
            }
        }
        return null;
    }

    // The queen rides in the saddle. Without this the dragon treats any passenger that isn't its owner as prey in its
    // mouth, shakes her and drops her. She isn't made the controlling passenger: vanilla turns off a mob's own movement
    // goals while another mob controls it, and the queen can't steer a dragon, so the dragon would just stand still.
    @Override
    public void positionRider(@NotNull Entity passenger, @NotNull MoveFunction callback) {
        if (passenger instanceof EntityDreadQueen && this.isAlive() && !this.isModelDead()) {
            Vec3 riderPos = this.getRiderPosition();
            callback.accept(passenger, riderPos.x, riderPos.y + passenger.getBbHeight(), riderPos.z);
            return;
        }
        super.positionRider(passenger, callback);
    }

    public void setSeekingQueen(boolean seekingQueen) {
        this.seekingQueen = seekingQueen;
    }

    // Carrying the queen doesn't keep it on the ground, unlike other passengers; going to pick her up does
    @Override
    public boolean isAllowedToTriggerFlight() {
        return !this.seekingQueen && this.hasFlightClearance() && !this.isOrderedToSit() && !this.isBaby() && !this.isSleeping() && this.canMove() && this.onGround();
    }

    @Override
    public boolean shouldRiderSit() {
        return super.shouldRiderSit() || this.getRidingQueen() != null;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (this.getCommanderId() != null) {
            compound.putUUID("CommanderUUID", this.getCommanderId());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("CommanderUUID")) {
            this.setCommanderId(compound.getUUID("CommanderUUID"));
        }
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entityIn) {
        return entityIn instanceof IDreadMob || super.isAlliedTo(entityIn);
    }

    @Nullable
    public UUID getCommanderId() {
        return this.entityData.get(COMMANDER_UNIQUE_ID).orElse(null);
    }

    public void setCommanderId(@Nullable UUID uuid) {
        this.entityData.set(COMMANDER_UNIQUE_ID, Optional.ofNullable(uuid));
    }

    @Override
    public Entity getCommander() {
        UUID uuid = this.getCommanderId();
        if (uuid == null) {
            return null;
        }
        Player player = this.level().getPlayerByUUID(uuid);
        if (player != null) {
            return player;
        }
        if (!this.level().isClientSide && this.level().getServer() != null) {
            Entity entity = this.level().getServer().getLevel(this.level().dimension()).getEntity(uuid);
            if (entity instanceof LivingEntity) {
                return entity;
            }
        }
        return null;
    }

    @Override
    public @NotNull MobType getMobType() {
        return MobType.UNDEAD;
    }

    // Always a grown female, awake, ready to fight
    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor worldIn, @NotNull DifficultyInstance difficultyIn, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setGender(false);
        this.setVariant(1);
        this.setInSittingPose(false);
        this.setAgingDisabled(false);
        this.setAgeInDays(125);
        this.updateAttributes();
        this.setAgingDisabled(true);
        this.heal((float) this.maximumHealth);
        this.attackDecision = true;
        this.setHunger(50);
        return spawnDataIn;
    }

    @Override
    public boolean isTimeToWake() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int getFlightChancePerTick() {
        return 15;
    }

    @Override
    public String getVariantName(int variant) {
        return "white_";
    }

    @Override
    public boolean isFood(@Nullable ItemStack stack) {
        return false;
    }

    @Override
    public int getArmorOrdinal(ItemStack stack) {
        return 0;
    }

    @Override
    public ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    // Harvesting its blood gives the bottle back, as in the 1.12 version
    @Override
    public Item getBloodItem() {
        return Items.GLASS_BOTTLE;
    }

    @Override
    public ItemLike getHeartItem() {
        return Items.AIR;
    }

    @Override
    public Item getFleshItem() {
        return Items.AIR;
    }

    @Override
    public Item getVariantScale(int variant) {
        return Items.AIR;
    }

    @Override
    public Item getVariantEgg(int variant) {
        return Items.AIR;
    }

    @Override
    public ResourceLocation getDeadLootTable() {
        return BuiltInLootTables.EMPTY;
    }
}
