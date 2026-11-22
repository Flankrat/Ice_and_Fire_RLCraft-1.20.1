package com.github.alexthe666.iceandfire.compat;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

import java.util.function.Consumer;

/**
 * The 1.12 version's glacier biome, placed through TerraBlender (only loaded when TerraBlender is). Biomes no longer
 * shape the terrain, so the glacier takes the place of snowy plains in its own region ("Glacier Spawn Weight" is that
 * region's weight) and gets the 1.12 ground: snow on top, packed ice under it, gravel under water.
 */
public final class TerraBlenderCompat {
    public static final ResourceKey<Biome> GLACIER = ResourceKey.create(Registries.BIOME, new ResourceLocation(IceAndFire.MODID, "glacier"));

    private TerraBlenderCompat() {
    }

    public static void init() {
        if (!IafConfig.spawnGlaciers) {
            return;
        }
        Regions.register(new GlacierRegion(new ResourceLocation(IceAndFire.MODID, "glacier"), IafConfig.glacierSpawnChance));
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, IceAndFire.MODID, glacierSurface());
    }

    private static SurfaceRules.RuleSource glacierSurface() {
        SurfaceRules.RuleSource snow = SurfaceRules.state(Blocks.SNOW_BLOCK.defaultBlockState());
        SurfaceRules.RuleSource packedIce = SurfaceRules.state(Blocks.PACKED_ICE.defaultBlockState());
        SurfaceRules.RuleSource gravel = SurfaceRules.state(Blocks.GRAVEL.defaultBlockState());
        return SurfaceRules.ifTrue(SurfaceRules.isBiome(GLACIER), SurfaceRules.sequence(
            // Above the water line: a snow top and a few blocks of packed ice
            SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, snow),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(0, true, 3, CaveSurface.FLOOR), packedIce))),
            // Under water: gravel, like the 1.12 version
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, gravel)));
    }

    private static class GlacierRegion extends Region {
        GlacierRegion(ResourceLocation name, int weight) {
            super(name, RegionType.OVERWORLD, weight);
        }

        @Override
        public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
            this.addModifiedVanillaOverworldBiomes(mapper, builder -> builder.replaceBiome(Biomes.SNOWY_PLAINS, GLACIER));
        }
    }
}
