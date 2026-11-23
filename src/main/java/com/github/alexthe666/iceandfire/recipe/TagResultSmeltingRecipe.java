package com.github.alexthe666.iceandfire.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * A furnace recipe whose result is the first item in an item tag, like the 1.12 version's "smelt into whichever
 * bronze ingot is registered". When the tag is empty the result is empty, so the furnace doesn't smelt it.
 */
public class TagResultSmeltingRecipe extends SmeltingRecipe {
    private final TagKey<Item> resultTag;
    private final int resultCount;

    public TagResultSmeltingRecipe(ResourceLocation id, String group, CookingBookCategory category, Ingredient ingredient, TagKey<Item> resultTag, int resultCount, float experience, int cookingTime) {
        super(id, group, category, ingredient, ItemStack.EMPTY, experience, cookingTime);
        this.resultTag = resultTag;
        this.resultCount = resultCount;
    }

    // First item in the tag, read each time so it follows tag reloads
    private ItemStack resolve() {
        return BuiltInRegistries.ITEM.getTag(resultTag)
            .flatMap(set -> set.stream().findFirst())
            .map(holder -> new ItemStack(holder.value(), resultCount))
            .orElse(ItemStack.EMPTY);
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        return resolve();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Container container, @NotNull RegistryAccess registryAccess) {
        return resolve();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return IafRecipeSerializers.SMELTING_TO_TAG.get();
    }

    public static class Serializer implements RecipeSerializer<TagResultSmeltingRecipe> {
        // Same as a smelting recipe, but "result" is {"tag": "...", "count": n}
        @Override
        public @NotNull TagResultSmeltingRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CookingBookCategory category = CookingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CookingBookCategory.MISC);
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            JsonObject result = GsonHelper.getAsJsonObject(json, "result");
            TagKey<Item> tag = TagKey.create(Registries.ITEM, new ResourceLocation(GsonHelper.getAsString(result, "tag")));
            int count = GsonHelper.getAsInt(result, "count", 1);
            float experience = GsonHelper.getAsFloat(json, "experience", 0.0F);
            int cookingTime = GsonHelper.getAsInt(json, "cookingtime", 200);
            return new TagResultSmeltingRecipe(id, group, category, ingredient, tag, count, experience, cookingTime);
        }

        @Override
        public TagResultSmeltingRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buf) {
            String group = buf.readUtf();
            CookingBookCategory category = buf.readEnum(CookingBookCategory.class);
            Ingredient ingredient = Ingredient.fromNetwork(buf);
            TagKey<Item> tag = TagKey.create(Registries.ITEM, buf.readResourceLocation());
            int count = buf.readVarInt();
            float experience = buf.readFloat();
            int cookingTime = buf.readVarInt();
            return new TagResultSmeltingRecipe(id, group, category, ingredient, tag, count, experience, cookingTime);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TagResultSmeltingRecipe recipe) {
            buf.writeUtf(recipe.getGroup());
            buf.writeEnum(recipe.category());
            recipe.getIngredients().get(0).toNetwork(buf);
            buf.writeResourceLocation(recipe.resultTag.location());
            buf.writeVarInt(recipe.resultCount);
            buf.writeFloat(recipe.getExperience());
            buf.writeVarInt(recipe.getCookingTime());
        }
    }
}
