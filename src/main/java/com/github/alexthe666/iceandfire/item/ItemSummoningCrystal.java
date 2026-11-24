package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.world.DragonPosWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSummoningCrystal extends Item {
    // Summons waiting for their dragon's chunk to load, by dragon UUID (server only). Each summon is its own object:
    // they used to be fields on the item, which every player shares, so two players summoning at once mixed up
    // their dragons, targets and crystals.
    public static final Map<UUID, Summon> DELAYED_SUMMONS = new ConcurrentHashMap<>();

    public ItemSummoningCrystal() {
        super(new Item.Properties()/*.tab(IceAndFire.TAB_ITEMS)*/.stacksTo(1));
    }

    private static CompoundTag getDragonTag(ItemStack stack) {
        if (stack.getItem() instanceof ItemSummoningCrystal && stack.getTag() != null) {
            for (String tagInfo : stack.getTag().getAllKeys()) {
                if (tagInfo.contains("Dragon")) {
                    return stack.getTag().getCompound(tagInfo);
                }
            }
        }
        return null;
    }

    public static boolean hasDragon(ItemStack stack) {
        return getDragonTag(stack) != null;
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, @NotNull Level world, @NotNull Player player) {
        itemStack.setTag(new CompoundTag());
    }

    public ItemStack onItemUseFinish(Level worldIn, LivingEntity LivingEntity) {
        return new ItemStack(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {

        boolean flag = false;
        String desc = "entity.iceandfire.fire_dragon";
        if (stack.getItem() == IafItemRegistry.SUMMONING_CRYSTAL_ICE.get()) {
            desc = "entity.iceandfire.ice_dragon";
        }
        if (stack.getItem() == IafItemRegistry.SUMMONING_CRYSTAL_LIGHTNING.get()) {
            desc = "entity.iceandfire.lightning_dragon";
        }
        if (stack.getTag() != null) {
            for (String tagInfo : stack.getTag().getAllKeys()) {
                if (tagInfo.contains("Dragon")) {
                    CompoundTag dragonTag = stack.getTag().getCompound(tagInfo);
                    String dragonName = I18n.get(desc);
                    if (!dragonTag.getString("CustomName").isEmpty()) {
                        dragonName = dragonTag.getString("CustomName");
                    }
                    tooltip.add(Component.translatable("item.iceandfire.summoning_crystal.bound", dragonName).withStyle(ChatFormatting.GRAY));
                    flag = true;
                }
            }
        }
        if (!flag) {
            tooltip.add(Component.translatable("item.iceandfire.summoning_crystal.desc_0").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.iceandfire.summoning_crystal.desc_1").withStyle(ChatFormatting.GRAY));

        }

    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.PASS;
        }

        ServerLevel serverWorld = (ServerLevel) context.getLevel();
        if (serverWorld.dimension() != Level.OVERWORLD) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() != this) {
            return InteractionResult.PASS;
        }

        CompoundTag dragonTag = getDragonTag(stack);
        if (dragonTag == null) {
            return InteractionResult.PASS;
        }

        Summon summon = new Summon(dragonTag.getUUID("DragonUUID"), player, hand, stack, serverWorld,
                context.getClickedPos().relative(context.getClickedFace()), player.getYRot());

        IceAndFire.LOGGER.info("Trying to summon dragon {} {}", summon.dragonUuid, dragonTag.getString("CustomName"));
        Entity entity = serverWorld.getEntity(summon.dragonUuid);
        // If the dragon is already loaded, summon it immediately
        if (entity != null) {
            summon.summonEntity(entity);
            return InteractionResult.PASS;
        }

        // early exit if we are not allowed to load chunks to summon
        if (!IafConfig.chunkLoadSummonCrystal) {
            IceAndFire.LOGGER.info("Dragon entity {} not loaded, and chunk loading is disabled", summon.dragonUuid);
            summon.displayClientError();
            return InteractionResult.PASS;
        }

        DragonPosWorldData data = DragonPosWorldData.get(serverWorld);
        if (data == null) {
            IceAndFire.LOGGER.warn("Unable to load DragonPosWorldData for world {}", serverWorld);
            summon.displayClientError();
            return InteractionResult.PASS;
        }

        summon.dragonOriginPosition = data.getDragonPos(summon.dragonUuid);

        if (summon.dragonOriginPosition == null) {
            IceAndFire.LOGGER.warn("Summoning dragon origin position unknown for dragon {}", summon.dragonUuid);
            summon.displayClientError();
            return InteractionResult.PASS;
        }

        ChunkPos pos = new ChunkPos(summon.dragonOriginPosition);
        IceAndFire.LOGGER.info("Dragon entity not loaded, loading chunk {}", pos);

        // try to load the chunk, and mark the entity to be summoned as soon as it gets added to the world
        if (ForgeChunkManager.forceChunk(serverWorld, IceAndFire.MODID, player, pos.x, pos.z, true, false)) {
            summon.summoningTime = serverWorld.getGameTime();
            Summon replaced = DELAYED_SUMMONS.put(summon.dragonUuid, summon);
            if (replaced != null) {
                replaced.releaseChunk(); // an earlier summon of the same dragon still waiting: this one replaces it
            }
        } else {
            IceAndFire.LOGGER.warn("Failed to force load chunk with Dragon {}", summon.dragonUuid);
        }

        return InteractionResult.PASS;
    }

    /** One player's summon of one dragon. */
    public static final class Summon {
        private final UUID dragonUuid;
        private final Player summoningPlayer;
        private final InteractionHand summoningHand;
        private final ItemStack stack;
        private final ServerLevel serverWorld;
        private final BlockPos dragonTargetPosition;
        private final float dragonTargetYaw;
        private BlockPos dragonOriginPosition;
        private long summoningTime = 0;

        private Summon(UUID dragonUuid, Player summoningPlayer, InteractionHand summoningHand, ItemStack stack,
                       ServerLevel serverWorld, BlockPos dragonTargetPosition, float dragonTargetYaw) {
            this.dragonUuid = dragonUuid;
            this.summoningPlayer = summoningPlayer;
            this.summoningHand = summoningHand;
            this.stack = stack;
            this.serverWorld = serverWorld;
            this.dragonTargetPosition = dragonTargetPosition;
            this.dragonTargetYaw = dragonTargetYaw;
        }

        private void summonEntity(Entity entity) {
            entity.moveTo(dragonTargetPosition.getX() + 0.5D, dragonTargetPosition.getY() + 0.5D, dragonTargetPosition.getZ() + 0.5D, dragonTargetYaw, 0);
            if (entity instanceof EntityDragonBase) {
                ((EntityDragonBase) entity).setCrystalBound(false);
            }
            if (IafConfig.chunkLoadSummonCrystal) {
                DragonPosWorldData data = DragonPosWorldData.get(serverWorld);
                if (data != null) {
                    data.removeDragon(entity.getUUID());
                }
            }
            this.summoningPlayer.playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
            this.summoningPlayer.playSound(SoundEvents.GLASS_BREAK, 1, 1);
            this.summoningPlayer.swing(this.summoningHand);
            this.summoningPlayer.displayClientMessage(Component.translatable("message.iceandfire.dragonTeleport"), true);
            stack.setTag(new CompoundTag());
        }

        /** Called when the dragon is added to the (server) world after its chunk was force-loaded for this summon. */
        public void delayedSummon() {
            DELAYED_SUMMONS.remove(this.dragonUuid, this);
            ChunkPos pos = new ChunkPos(this.dragonOriginPosition);

            // make delayed summons expire if for some reason the chunk loading, or entity loading takes too long
            // wouldn't want the dragon to summon minutes later at the original position
            final long SUMMON_DELAY_TOLERANCE = 2_000;
            if (this.serverWorld.getGameTime() - SUMMON_DELAY_TOLERANCE > this.summoningTime) {
                IceAndFire.LOGGER.info("Dragon summon timed out for dragon {}; unloading chunk {}", dragonUuid, pos);
            } else {
                Entity entity = serverWorld.getEntity(this.dragonUuid);
                if (entity != null) {
                    if (this.summoningPlayer.isAlive()) {
                        summonEntity(entity);
                        IceAndFire.LOGGER.info("Summoned dragon {} and unloading chunk {}", dragonUuid, pos);
                    } else {
                        IceAndFire.LOGGER.info("Player died since summoning {}; unloading chunk {}", dragonUuid, pos);
                    }
                }
            }

            releaseChunk();
        }

        private void releaseChunk() {
            ChunkPos pos = new ChunkPos(this.dragonOriginPosition);
            ForgeChunkManager.forceChunk(this.serverWorld, IceAndFire.MODID, this.summoningPlayer, pos.x, pos.z, false, false);
        }

        private void displayClientError() {
            summoningPlayer.displayClientMessage(Component.translatable("message.iceandfire.noDragonTeleport"), true);
        }
    }
}
