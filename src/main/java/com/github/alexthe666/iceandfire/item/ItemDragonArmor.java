package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemDragonArmor extends Item {

    public DragonArmorType type;
    public int dragonSlot;
    public String name;
    private Pattern baseName = Pattern.compile("[a-z]+_[a-z]+");

    public ItemDragonArmor(DragonArmorType type, int dragonSlot) {
        super(new Item.Properties()/*.tab(IceAndFire.TAB_ITEMS)*/.stacksTo(1));
        this.type = type;
        this.dragonSlot = dragonSlot;
        if (type == DragonArmorType.FIRE || type == DragonArmorType.ICE || type == DragonArmorType.LIGHTNING)
            baseName = Pattern.compile("[a-z]+_[a-z]+_[a-z]+");
    }

    @Override
    public @NotNull String getDescriptionId() {
        String fullName = ForgeRegistries.ITEMS.getKey(this).getPath();
        Matcher matcher = baseName.matcher(fullName);
        name = matcher.find() ? matcher.group() : fullName;
        return "item.iceandfire." + name;
    }

    /**
     * Key for the look of this piece on a dragon. The armor layer caches the combined texture of all four worn
     * pieces under these keys, so two pieces that look different need different keys. Ice and Fire's pieces use
     * their type's number (the old armor ordinal); an addon piece with its own texture should return its item id.
     */
    public String getDragonArmorKey() {
        return String.valueOf(type.ordinal() + 1);
    }

    /**
     * Texture drawn on a dragon wearing this piece, for addons that add dragon armor of their own. Null (the default)
     * means Ice and Fire's own texture for the piece's type. The texture is laid over the dragon model of the wearer:
     * fire dragons use the fire model, ice dragons the ice model and every other dragon the lightning model.
     */
    @Nullable
    public ResourceLocation getDragonArmorTexture(EntityDragonBase dragon, EquipmentSlot slot) {
        return null;
    }

    /** How much this piece adds to the dragon's armor multiplier (Ice and Fire's values by type). */
    public double getDragonArmorModifier() {
        return switch (type.ordinal() + 1) {
            case 1 -> 2D;
            case 2, 4 -> 3D;
            case 3 -> 5D;
            case 5, 6, 8 -> 10D;
            case 7 -> 1.5D;
            default -> 0D;
        };
    }

    static String getNameForSlot(int slot) {
        return switch (slot) {
            case 0 -> "head";
            case 1 -> "neck";
            case 2 -> "body";
            case 3 -> "tail";
            default -> "";
        };
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        String words = switch (dragonSlot) {
            case 1 -> "dragon.armor_neck";
            case 2 -> "dragon.armor_body";
            case 3 -> "dragon.armor_tail";
            default -> "dragon.armor_head";
        };
        tooltip.add(Component.translatable(words).withStyle(ChatFormatting.GRAY));
    }

    /**
     * FIRE, ICE and LIGHTNING are the dragonsteel dragon armor types. Ice and Fire doesn't register those items any
     * more; their dragon textures (armor_*_5, _6 and _8) and values stay for addons that do.
     */
    public enum DragonArmorType {
        IRON,
        GOLD,
        DIAMOND,
        SILVER,
        FIRE,
        ICE,
        COPPER,
        LIGHTNING
    }

}
