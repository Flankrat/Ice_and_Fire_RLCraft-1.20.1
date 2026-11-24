package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityFishingSpear;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * A weaker trident. Hold right-click and let go to throw it (loyalty, riptide, channeling and impaling work like on
 * a trident). Letting go while looking at water up to 5 blocks away spears a fish instead, as in the 1.12 version.
 */
public class ItemFishingSpear extends TridentItem {
    // Trident: 9 melee damage (8 + 1 base), thrown at speed 2.5
    private static final double MELEE_DAMAGE_BONUS = 5.0D;
    private static final float THROW_SPEED = 2.0F;
    // How far away the water can be for spear fishing, as in 1.12
    private static final double FISHING_REACH = 5.0D;
    private static final Item[] FISH = {Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH};

    public ItemFishingSpear() {
        super(new Item.Properties().durability(64));
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player) || this.getUseDuration(stack) - timeLeft < 10) {
            return;
        }
        // Aimed at water close by: spear a fish (1.12 behaviour) instead of throwing
        BlockPos water = findWaterInReach(level, player);
        if (water != null) {
            if (!level.isClientSide) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(user.getUsedItemHand()));
                Item fish = FISH[level.random.nextInt(FISH.length)];
                level.addFreshEntity(new ItemEntity(level, water.getX() + 0.5D, water.getY() + 1.5D, water.getZ() + 0.5D, new ItemStack(fish)));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.awardStat(Stats.ITEM_USED.get(this));
            return;
        }
        int riptide = EnchantmentHelper.getRiptide(stack);
        if (riptide > 0 && !player.isInWaterOrRain()) {
            return;
        }
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(user.getUsedItemHand()));
            if (riptide == 0) {
                // Thrown like a trident, a bit slower
                EntityFishingSpear spear = new EntityFishingSpear(level, player, stack);
                spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_SPEED, 1.0F);
                if (player.getAbilities().instabuild) {
                    spear.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.addFreshEntity(spear);
                level.playSound(null, spear, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.getInventory().removeItem(stack);
                }
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (riptide > 0) {
            // Riptide launches the player, same as a trident
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            float x = -Mth.sin(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            float y = -Mth.sin(pitch * Mth.DEG_TO_RAD);
            float z = Mth.cos(yaw * Mth.DEG_TO_RAD) * Mth.cos(pitch * Mth.DEG_TO_RAD);
            float length = Mth.sqrt(x * x + y * y + z * z);
            float strength = 3.0F * ((1.0F + riptide) / 4.0F);
            player.push(x * strength / length, y * strength / length, z * strength / length);
            player.startAutoSpinAttack(20);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0D, 1.2D, 0.0D));
            }
            SoundEvent sound = riptide >= 3 ? SoundEvents.TRIDENT_RIPTIDE_3 : riptide == 2 ? SoundEvents.TRIDENT_RIPTIDE_2 : SoundEvents.TRIDENT_RIPTIDE_1;
            level.playSound(null, player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    // The water block the player is looking at within FISHING_REACH, or null (only when fishing is turned on)
    private static BlockPos findWaterInReach(Level level, Player player) {
        if (!IafConfig.fishingSpearCatchesFish) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(FISHING_REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
            return hit.getBlockPos();
        }
        return null;
    }

    // Weaker than a trident in melee too: 6 damage instead of 9, same speed
    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", MELEE_DAMAGE_BONUS, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.9F, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }
}
