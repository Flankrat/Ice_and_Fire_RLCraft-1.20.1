package com.github.alexthe666.iceandfire.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public final ForgeConfigSpec.BooleanValue customMainMenu;
    public final ForgeConfigSpec.BooleanValue useVanillaFont;
    public final ForgeConfigSpec.BooleanValue dragonAuto3rdPerson;
    public final ForgeConfigSpec.ConfigValue<String> fireDragonModel;
    public final ForgeConfigSpec.ConfigValue<String> iceDragonModel;
    public final ForgeConfigSpec.ConfigValue<String> lightningDragonModel;
    public final ForgeConfigSpec.ConfigValue<String> seaSerpentModel;
    public final ForgeConfigSpec.BooleanValue showMaleDragonSpots;

    public ClientConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("general");
        this.customMainMenu = buildBoolean(builder, "Custom main menu", "all", true, "Whether to display the dragon on the main menu or not");
        this.dragonAuto3rdPerson = buildBoolean(builder, "Auto 3rd person when riding dragon", "all", true, "True if riding dragons should make the player take a 3rd person view automatically.");
        this.useVanillaFont = buildBoolean(builder, "Use Vanilla Font", "all", false, "Whether to use the vanilla font in the bestiary or not");
        builder.pop();
        // Which model each mob is drawn with: classic (Ice and Fire's Tabula models) or community (Ice and Fire
        // Community Edition's GeckoLib remakes). Client only; can be changed while playing.
        builder.push("models");
        this.fireDragonModel = buildModel(builder, "Fire Dragon Model", "fire dragons");
        this.iceDragonModel = buildModel(builder, "Ice Dragon Model", "ice dragons");
        this.lightningDragonModel = buildModel(builder, "Lightning Dragon Model", "lightning dragons");
        this.seaSerpentModel = buildModel(builder, "Sea Serpent Model", "sea serpents");
        this.showMaleDragonSpots = buildBoolean(builder, "Show Male Dragon Spots", "all", false, "Whether male dragons get the darker spots on their wings and body that newer Ice and Fire versions draw over their texture (any model)");
        builder.pop();
    }

    private static ForgeConfigSpec.ConfigValue<String> buildModel(ForgeConfigSpec.Builder builder, String name, String mobs) {
        return builder.comment("Model for " + mobs + ": classic (Ice and Fire's models) or community (Ice and Fire Community Edition's GeckoLib models)")
            .translation(name).define(name, "classic", o -> o instanceof String s && (s.equalsIgnoreCase("classic") || s.equalsIgnoreCase("community")));
    }

    private static ForgeConfigSpec.BooleanValue buildBoolean(ForgeConfigSpec.Builder builder, String name, String catagory, boolean defaultValue, String comment){
        return builder.comment(comment).translation(name).define(name, defaultValue);
    }

    private static ForgeConfigSpec.IntValue buildInt(ForgeConfigSpec.Builder builder, String name, String catagory, int defaultValue, int min, int max, String comment){
        return builder.comment(comment).translation(name).defineInRange(name, defaultValue, min, max);
    }

    private static ForgeConfigSpec.DoubleValue buildDouble(ForgeConfigSpec.Builder builder, String name, String catagory, double defaultValue, double min, double max, String comment){
        return builder.comment(comment).translation(name).defineInRange(name, defaultValue, min, max);
    }
}
