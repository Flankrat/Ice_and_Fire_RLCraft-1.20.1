package com.github.alexthe666.iceandfire.datagen;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.world.structure.GorgonTempleStructure;
import com.github.alexthe666.iceandfire.world.structure.GraveyardStructure;
import com.github.alexthe666.iceandfire.world.structure.MausoleumStructure;
import com.github.alexthe666.iceandfire.world.structure.SnowVillageStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.HashMap;

public class IafStructures {

    public static final ResourceKey<Structure> GRAVEYARD = registerKey("graveyard");
    public static final ResourceKey<Structure> MAUSOLEUM = registerKey("mausoleum");
    public static final ResourceKey<Structure> GORGON_TEMPLE = registerKey("gorgon_temple");
    public static final ResourceKey<Structure> SNOW_VILLAGE = registerKey("snow_village");

    public static ResourceKey<Structure> registerKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(IceAndFire.MODID, name));
    }

    public static void bootstrap(BootstapContext<Structure> context) {
        context.register(GRAVEYARD, GraveyardStructure.buildStructureConfig(context));
        context.register(MAUSOLEUM, MausoleumStructure.buildStructureConfig(context));
        context.register(GORGON_TEMPLE, GorgonTempleStructure.buildStructureConfig(context));
        // From the 1.12 version: built from code (SnowVillagePieces), no terrain adjustment like the 1.12 village
        context.register(SNOW_VILLAGE, new SnowVillageStructure(new Structure.StructureSettings(
                context.lookup(Registries.BIOME).getOrThrow(IafBiomeTagGenerator.HAS_SNOW_VILLAGE), new HashMap<>(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE)));
    }
}
