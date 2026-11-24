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
        // Which model set each mob is drawn with. Client only; can be changed while playing.
        builder.push("models");
        this.fireDragonModel = buildModel(builder, "Fire Dragon Model", "fire dragons (gold dragons follow this too)");
        this.iceDragonModel = buildModel(builder, "Ice Dragon Model", "ice dragons");
        this.lightningDragonModel = buildModel(builder, "Lightning Dragon Model", "lightning dragons (Shivaxi follows this too)");
        this.seaSerpentModel = buildModel(builder, "Sea Serpent Model", "sea serpents");
        this.showMaleDragonSpots = buildBoolean(builder, "Show Male Dragon Spots", "all", false, "Whether male dragons get the darker spots on their wings and body that newer Ice and Fire versions draw over their texture");
        builder.pop();
    }

    // Any name is accepted so a config written for a later model set doesn't get reset; unknown names use classic
    private static ForgeConfigSpec.ConfigValue<String> buildModel(ForgeConfigSpec.Builder builder, String name, String mobs) {
        return builder.comment("Model set for " + mobs + ". Only \"classic\" (Ice and Fire's own models) exists right now; other names fall back to classic")
            .translation(name).define(name, "classic", o -> o instanceof String);
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
