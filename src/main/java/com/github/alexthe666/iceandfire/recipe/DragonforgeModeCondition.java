package com.github.alexthe666.iceandfire.recipe;

import com.github.alexthe666.iceandfire.IafConfig;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Recipe condition {"type": "iceandfire:dragonforge_mode", "mode": "global" | "individual"}: the recipe only loads when
 * Dragonforge Mode matches, so each mode only has the crafting recipes for the forge blocks it uses. Forge checks it
 * when recipes are loaded (before KubeJS sees them), so a recipe that fails it does not exist at all.
 * The setting is in the mod's common config, which is loaded before any datapack.
 */
public record DragonforgeModeCondition(String mode) implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation("iceandfire", "dragonforge_mode");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return mode.equalsIgnoreCase(IafConfig.dragonforgeMode);
    }

    public static class Serializer implements IConditionSerializer<DragonforgeModeCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, DragonforgeModeCondition value) {
            json.addProperty("mode", value.mode());
        }

        @Override
        public DragonforgeModeCondition read(JsonObject json) {
            return new DragonforgeModeCondition(GsonHelper.getAsString(json, "mode"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
