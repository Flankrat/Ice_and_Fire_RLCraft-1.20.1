package com.github.alexthe666.iceandfire.compat.spartan;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Entry point of the optional Spartan Weaponry compat (gilded dragon bone weapons). This class never touches a Spartan
 * class itself, so it is safe to load without Spartan installed; {@link GildedSpartanWeapons} is only loaded when
 * Spartan Weaponry, Spartan Toolkit and Spartan Fire are all present.
 */
public final class SpartanCompat {
    public static final String SPARTAN_WEAPONRY = "spartanweaponry";
    public static final String SPARTAN_TOOLKIT = "spartantoolkit";
    public static final String SPARTAN_FIRE = "spartanfire_rlc";

    /** The gilded weapons once registered (empty without Spartan); used for their glint and tooltip line. */
    public static final Set<Item> GILDED_WEAPONS = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean enabled;

    private SpartanCompat() {
    }

    /** True when all three Spartan mods are loaded, so the gilded weapons exist. */
    public static boolean isEnabled() {
        return enabled;
    }

    /** Called from the mod constructor: registers the gilded weapons when the Spartan mods are installed. */
    public static void init(IEventBus modBus) {
        ModList mods = ModList.get();
        enabled = mods.isLoaded(SPARTAN_WEAPONRY) && mods.isLoaded(SPARTAN_TOOLKIT) && mods.isLoaded(SPARTAN_FIRE);
        if (enabled) {
            GildedSpartanWeapons.init(modBus);
        }
    }
}
