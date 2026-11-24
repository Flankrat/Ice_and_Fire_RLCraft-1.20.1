package com.github.alexthe666.iceandfire.compat.spartan;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.oblivioussp.spartanweaponry.api.WeaponTraits;
import com.oblivioussp.spartanweaponry.api.trait.WeaponTrait;
import krelox.spartantoolkit.SpartanAddon;
import krelox.spartantoolkit.SpartanMaterial;
import krelox.spartantoolkit.WeaponMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Gilded dragon bone weapons: the gold dragon counterpart of Spartan Fire's flamed, iced and lightning dragon bone
 * weapons, one of every Spartan weapon type. Forged from Spartan Fire's dragon bone weapons with gold dragon blood in a
 * dragonforge that gold dragon breath powers. Only loaded through {@link SpartanCompat} when the Spartan mods are present.
 */
public class GildedSpartanWeapons extends SpartanAddon {
    public static final DeferredRegister<Item> ITEMS = SpartanAddon.itemRegister(IceAndFire.MODID);
    public static final DeferredRegister<WeaponTrait> TRAITS = SpartanAddon.traitRegister(IceAndFire.MODID);
    public static final WeaponMap WEAPONS = new WeaponMap();

    // the Arcane trait: knockback, arcane burst and bonus damage against the elemental dragons
    public static final RegistryObject<WeaponTrait> ARCANE = SpartanAddon.registerTrait(TRAITS, new ArcaneTrait());
    // Spartan Fire's own fire/ice dragon damage bonus traits, which its lightning dragon bone weapons also have
    private static final RegistryObject<WeaponTrait> ICE_DRAGON_DAMAGE_BONUS_I = spartanFireTrait("ice_dragon_damage_bonus_1");
    private static final RegistryObject<WeaponTrait> FIRE_DRAGON_DAMAGE_BONUS_I = spartanFireTrait("fire_dragon_damage_bonus_1");

    // same stats as the gilded dragon bone sword; Spartan scales damage and speed per weapon type
    public static final SpartanMaterial GILDED_DRAGON_BONE = new SpartanMaterial("gilded_dragon_bone", IceAndFire.MODID,
            IafItemRegistry.GOLD_DRAGONBONE_TOOL_MATERIAL, ItemTags.create(new ResourceLocation("forge", "bones/dragon")),
            ICE_DRAGON_DAMAGE_BONUS_I, FIRE_DRAGON_DAMAGE_BONUS_I, ARCANE);

    private static GildedSpartanWeapons instance;

    private GildedSpartanWeapons() {
    }

    static void init(IEventBus modBus) {
        instance = new GildedSpartanWeapons();
        // registers gilded_dragon_bone_<type> for every Spartan weapon type into ITEMS and WEAPONS
        instance.registerSpartanWeapons(ITEMS);
        ITEMS.register(modBus);
        TRAITS.register(modBus);
        modBus.addListener((FMLCommonSetupEvent event) ->
                WEAPONS.values().forEach(weapon -> SpartanCompat.GILDED_WEAPONS.add(weapon.get())));
    }

    // a trait Spartan Fire registers, looked up by name so Ice and Fire doesn't need Spartan Fire to build
    private static RegistryObject<WeaponTrait> spartanFireTrait(String name) {
        return RegistryObject.create(new ResourceLocation(SpartanCompat.SPARTAN_FIRE, name), WeaponTraits.REGISTRY_KEY, IceAndFire.MODID);
    }

    @Override
    public String modid() {
        return IceAndFire.MODID;
    }

    @Override
    public List<SpartanMaterial> getMaterials() {
        return List.of(GILDED_DRAGON_BONE);
    }

    @Override
    public WeaponMap getWeaponMap() {
        return WEAPONS;
    }
}
