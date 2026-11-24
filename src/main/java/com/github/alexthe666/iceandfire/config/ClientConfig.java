package com.github.alexthe666.iceandfire.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public final ForgeConfigSpec.BooleanValue customMainMenu;
    public final ForgeConfigSpec.BooleanValue useVanillaFont;
    public final ForgeConfigSpec.BooleanValue dragonAuto3rdPerson;
    public final ForgeConfigSpec.ConfigValue<String> fireDragonTexture;
    public final ForgeConfigSpec.ConfigValue<String> iceDragonTexture;
    public final ForgeConfigSpec.ConfigValue<String> lightningDragonTexture;
    public final ForgeConfigSpec.BooleanValue showMaleDragonSpots;

    public ClientConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("general");
        this.customMainMenu = buildBoolean(builder, "Custom main menu", "all", true, "Whether to display the dragon on the main menu or not");
        this.dragonAuto3rdPerson = buildBoolean(builder, "Auto 3rd person when riding dragon", "all", true, "True if riding dragons should make the player take a 3rd person view automatically.");
        this.useVanillaFont = buildBoolean(builder, "Use Vanilla Font", "all", false, "Whether to use the vanilla font in the bestiary or not");
        builder.pop();
        // Which texture set each dragon type is drawn with. Client only; can be changed while playing.
        builder.push("textures");
        this.fireDragonTexture = buildTexture(builder, "Fire Dragon Texture", "fire dragons (gold dragons follow this too)");
        this.iceDragonTexture = buildTexture(builder, "Ice Dragon Texture", "ice dragons");
        this.lightningDragonTexture = buildTexture(builder, "Lightning Dragon Texture", "lightning dragons");
        this.showMaleDragonSpots = buildBoolean(builder, "Show Male Dragon Spots", "all", false, "Whether male dragons get the darker spots on their wings and body that newer Ice and Fire versions draw over their texture");
        builder.pop();
    }

    // Any name is accepted so a config written for a later texture set doesn't get reset; unknown names use classic
    private static ForgeConfigSpec.ConfigValue<String> buildTexture(ForgeConfigSpec.Builder builder, String name, String mobs) {
        return builder.comment("Texture set for " + mobs + ": \"classic\" (the normal dragon textures) or \"skeleton\" (always the bone look"
                + " from the Spooky Season Dragons Halloween event, eyes still glow). Other names fall back to classic")
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
