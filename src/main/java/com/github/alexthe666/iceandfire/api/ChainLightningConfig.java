package com.github.alexthe666.iceandfire.api;

/** Constants for the legacy chain-lightning weapon behaviour. */
public final class ChainLightningConfig {
    private ChainLightningConfig() {
    }

    public static final float[] CHAIN_DAMAGE_PER_HOP = {5.0F, 4.0F, 3.0F, 2.0F, 1.0F};
    public static final int CHAIN_RANGE = 8;
    public static final int CHAIN_COOLDOWN_TICKS = 10;
    public static final boolean CHAIN_BYPASSES_ARMOR = true;
}
