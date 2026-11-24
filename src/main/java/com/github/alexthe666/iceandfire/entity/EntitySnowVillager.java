package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.enums.EnumBestiaryPages;
import com.github.alexthe666.iceandfire.enums.EnumTroll;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * From the 1.12 version: the snow villager of snow villages, a fisherman, craftsman or shaman who trades for
 * sapphires instead of emeralds. Like a 1.12 villager it starts with its first tier of trades, and using its last
 * trade unlocks the next tier (three tiers in all).
 */
public class EntitySnowVillager extends AbstractVillager {
    public static final int FISHERMAN = 0;
    public static final int CRAFTSMAN = 1;
    public static final int SHAMAN = 2;
    private static final String[] PROFESSION_NAMES = {"fisherman", "craftsman", "shaman"};
    public static final int PROFESSION_COUNT = PROFESSION_NAMES.length;
    private static final int MAX_TIER = 3;
    private static final EntityDataAccessor<Integer> PROFESSION = SynchedEntityData.defineId(EntitySnowVillager.class, EntityDataSerializers.INT);
    private int tier = 0;
    // The job site block this villager works at (placed in its hut), or null until it finds one
    @Nullable
    private BlockPos jobSite;

    public EntitySnowVillager(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PROFESSION, FISHERMAN);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5D, 0.5D));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.5D));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35D));
        // During the day, go back to work at the job site now and then
        this.goalSelector.addGoal(6, new WorkAtJobSiteGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    public int getProfession() {
        return Math.floorMod(this.entityData.get(PROFESSION), PROFESSION_NAMES.length);
    }

    public void setProfession(int profession) {
        this.entityData.set(PROFESSION, Math.floorMod(profession, PROFESSION_NAMES.length));
        // The 1.12 fisherman is drawn holding a fishing spear
        this.setItemSlot(EquipmentSlot.MAINHAND, this.getProfession() == FISHERMAN ? new ItemStack(IafItemRegistry.FISHING_SPEAR.get()) : ItemStack.EMPTY);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public String getProfessionName() {
        return PROFESSION_NAMES[this.getProfession()];
    }

    /**
     * The job site block for a profession, the same blocks vanilla villagers use for similar jobs: a barrel for the
     * fisherman, a crafting table for the craftsman, a brewing stand for the shaman (who trades in blaze powder,
     * ghast tears and dragon blood).
     */
    public static BlockState getJobSiteBlock(int profession) {
        return switch (Math.floorMod(profession, PROFESSION_COUNT)) {
            case CRAFTSMAN -> Blocks.CRAFTING_TABLE.defaultBlockState();
            case SHAMAN -> Blocks.BREWING_STAND.defaultBlockState();
            default -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        };
    }

    // True if the block at pos is this villager's kind of job site
    private boolean isJobSiteBlock(BlockPos pos) {
        return this.level().getBlockState(pos).is(getJobSiteBlock(this.getProfession()).getBlock());
    }

    @Nullable
    public BlockPos getJobSite() {
        return this.jobSite;
    }

    public void setJobSite(@Nullable BlockPos jobSite) {
        this.jobSite = jobSite == null ? null : jobSite.immutable();
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        this.setProfession(this.getRandom().nextInt(PROFESSION_NAMES.length));
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SpawnEggItem) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (this.getOffers().isEmpty()) {
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            if (!this.level().isClientSide) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // First tier when the trades are first looked at
    @Override
    protected void updateTrades() {
        if (this.tier == 0) {
            this.unlockNextTier();
        }
    }

    // Using the last trade on the list unlocks the next tier, as in the 1.12 version
    @Override
    protected void rewardTradeXp(@NotNull MerchantOffer offer) {
        MerchantOffers offers = this.getOffers();
        if (!offers.isEmpty() && offers.get(offers.size() - 1) == offer && this.tier < MAX_TIER) {
            this.unlockNextTier();
        }
    }

    private void unlockNextTier() {
        this.tier++;
        MerchantOffers offers = this.getOffers();
        for (VillagerTrades.ItemListing listing : getTrades(this.getProfession(), this.tier)) {
            MerchantOffer offer = listing.getOffer(this, this.random);
            if (offer != null) {
                offers.add(offer);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Profession", this.getProfession());
        tag.putInt("Tier", this.tier);
        if (this.jobSite != null) {
            tag.put("JobSite", NbtUtils.writeBlockPos(this.jobSite));
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setProfession(tag.getInt("Profession"));
        this.tier = tag.getInt("Tier");
        this.jobSite = tag.contains("JobSite") ? NbtUtils.readBlockPos(tag.getCompound("JobSite")) : null;
    }

    @Override
    protected @NotNull Component getTypeName() {
        return Component.translatable("entity.iceandfire.snow_villager." + this.getProfessionName());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    // The 1.12 version's trades, by profession and tier
    private static List<VillagerTrades.ItemListing> getTrades(int profession, int tier) {
        List<VillagerTrades.ItemListing> trades = new ArrayList<>();
        switch (profession) {
            case FISHERMAN -> {
                if (tier == 1) {
                    trades.add(sapphireForItems(() -> Items.COD, 1, 10));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.FISHING_SPEAR.get()), 1, 5));
                } else if (tier == 2) {
                    trades.add(itemForSapphires(() -> new ItemStack(Items.FISHING_ROD), 1, 3));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.COOKED_COD), 1, 5));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.COOKED_COD), 1, 5));
                } else if (tier == 3) {
                    trades.add(itemForSapphires(() -> new ItemStack(Items.INK_SAC), 1, 2));
                    trades.add(itemForSapphires(() -> new ItemStack(Blocks.TRIPWIRE_HOOK), 1, 3));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.PUFFERFISH), 1, 4));
                }
            }
            case CRAFTSMAN -> {
                if (tier == 1) {
                    if (IafConfig.allowSnowForSapphireTrade) {
                        trades.add(sapphireForItems(() -> Blocks.SNOW_BLOCK.asItem(), 1, 32));
                    }
                    trades.add(itemForSapphires(() -> new ItemStack(Items.IRON_SHOVEL), 1, 4));
                } else if (tier == 2) {
                    trades.add(itemForSapphires(() -> new ItemStack(Blocks.PACKED_ICE), 1, 3));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.SILVER_SHOVEL.get()), 1, 5));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.LEATHER), 1, 10));
                } else if (tier == 3) {
                    trades.add(itemForSapphires(() -> new ItemStack(IafBlockRegistry.DRAGON_ICE.get()), 1, 4));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.DIAMOND_SHOVEL), 1, 9));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.LEATHER_BOOTS), 1, 3));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.LEATHER_HELMET), 1, 4));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.LEATHER_CHESTPLATE), 1, 6));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.LEATHER_LEGGINGS), 1, 6));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.DIAMOND_SHOVEL), 1, 7));
                    trades.add(itemForSapphires(() -> new ItemStack(EnumTroll.FROST.leather.get()), 1, 5));
                }
            }
            case SHAMAN -> {
                if (tier == 1) {
                    trades.add(sapphireForItems(() -> Items.BLAZE_POWDER, 2, 3));
                    trades.add(sapphireForItems(() -> Items.GHAST_TEAR, 1, 4));
                    trades.add(sapphireForItems(() -> IafItemRegistry.DRAGON_BONE.get(), 1, 8));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.MANUSCRIPT.get()), 1, 2));
                } else if (tier == 2) {
                    trades.add(sapphireForItems(() -> Items.BREWING_STAND, 1, 9));
                    trades.add(itemForSapphires(EntitySnowVillager::iceDragonBestiary, 1, 3));
                    trades.add(itemForSapphires(() -> new ItemStack(Items.ENDER_EYE), 2, 5));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.WITHERBONE.get()), 2, 5));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.WITHER_SHARD.get()), 2, 3));
                } else if (tier == 3) {
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.ICE_DRAGON_FLESH.get()), 1, 5));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.ICE_DRAGON_BLOOD.get()), 1, 12));
                    trades.add(itemForSapphires(() -> new ItemStack(IafItemRegistry.DRAGON_FLUTE.get()), 1, 5));
                }
            }
            default -> {
            }
        }
        return trades;
    }

    // A bestiary with the ice dragon pages, like the 1.12 shaman sells (the 1.12 villager page doesn't exist here)
    private static ItemStack iceDragonBestiary() {
        ItemStack stack = new ItemStack(IafItemRegistry.BESTIARY.get());
        stack.getOrCreateTag().putIntArray("Pages", new int[]{EnumBestiaryPages.INTRODUCTION.ordinal(), EnumBestiaryPages.ICEDRAGON.ordinal(), EnumBestiaryPages.ICEDRAGONEGG.ordinal(), EnumBestiaryPages.MATERIALS.ordinal()});
        return stack;
    }

    // 1.12 PriceInfo: a number from min to max
    private static int price(RandomSource random, int min, int max) {
        return min >= max ? min : min + random.nextInt(max - min + 1);
    }

    // The player gives min-max of the item for one sapphire
    private static VillagerTrades.ItemListing sapphireForItems(Supplier<Item> item, int min, int max) {
        return (trader, random) -> new MerchantOffer(new ItemStack(item.get(), price(random, min, max)), new ItemStack(IafItemRegistry.SAPPHIRE_GEM.get()), 7, 0, 0.05F);
    }

    // The player gives min-max sapphires for one of the item
    private static VillagerTrades.ItemListing itemForSapphires(Supplier<ItemStack> item, int min, int max) {
        return (trader, random) -> new MerchantOffer(new ItemStack(IafItemRegistry.SAPPHIRE_GEM.get(), price(random, min, max)), item.get(), 7, 0, 0.05F);
    }

    /**
     * During the day, now and then walk back to the job site and stand at it for a while. A villager without a job
     * site (spawn egg, or its hut's block was in a chunk built later) claims the nearest matching block close by,
     * and one whose block was broken forgets it.
     */
    static class WorkAtJobSiteGoal extends Goal {
        private static final int SEARCH_HORIZONTAL = 8;
        private static final int SEARCH_VERTICAL = 4;
        // Job sites farther than this (e.g. the villager was led away) are given up
        private static final double MAX_DISTANCE_SQ = 32 * 32;
        private final EntitySnowVillager villager;
        private int searchCooldown;
        private int workTicks;

        WorkAtJobSiteGoal(EntitySnowVillager villager) {
            this.villager = villager;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // Only in daylight, not while trading, and only now and then so it still wanders
            if (!this.villager.level().isDay() || this.villager.isTrading() || this.villager.getRandom().nextInt(60) != 0) {
                return false;
            }
            BlockPos site = this.villager.getJobSite();
            if (site != null && (!this.villager.isJobSiteBlock(site) || site.distSqr(this.villager.blockPosition()) > MAX_DISTANCE_SQ)) {
                this.villager.setJobSite(null);
                site = null;
            }
            if (site == null) {
                site = this.findJobSite();
                this.villager.setJobSite(site);
            }
            return site != null && site.distToCenterSqr(this.villager.position()) > 4.0D;
        }

        // The nearest block for this villager's profession; searched every 5th try (about every 15 seconds)
        @Nullable
        private BlockPos findJobSite() {
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return null;
            }
            this.searchCooldown = 5;
            Optional<BlockPos> found = BlockPos.findClosestMatch(this.villager.blockPosition(), SEARCH_HORIZONTAL, SEARCH_VERTICAL, this.villager::isJobSiteBlock);
            return found.orElse(null);
        }

        @Override
        public void start() {
            this.workTicks = 0;
            BlockPos site = this.villager.getJobSite();
            if (site != null) {
                this.villager.getNavigation().moveTo(site.getX() + 0.5D, site.getY(), site.getZ() + 0.5D, 0.35D);
            }
        }

        @Override
        public boolean canContinueToUse() {
            // Walk there, then stay working for about 10 seconds
            return this.villager.getJobSite() != null && this.villager.level().isDay() && !this.villager.isTrading()
                    && this.workTicks < 200;
        }

        @Override
        public void tick() {
            BlockPos site = this.villager.getJobSite();
            if (site == null) {
                return;
            }
            this.villager.getLookControl().setLookAt(site.getX() + 0.5D, site.getY() + 0.5D, site.getZ() + 0.5D);
            if (site.distToCenterSqr(this.villager.position()) <= 4.0D) {
                this.villager.getNavigation().stop();
                this.workTicks++;
            } else if (this.villager.getNavigation().isDone()) {
                // Couldn't get there (blocked); give up this time
                this.workTicks = 200;
            }
        }

        @Override
        public void stop() {
            this.villager.getNavigation().stop();
        }
    }
}
