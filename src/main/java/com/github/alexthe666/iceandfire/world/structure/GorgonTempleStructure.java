package com.github.alexthe666.iceandfire.world.structure;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.config.BiomeConfig;
import com.github.alexthe666.iceandfire.datagen.IafBiomeTagGenerator;
import com.github.alexthe666.iceandfire.datagen.IafStructurePieces;
import com.github.alexthe666.iceandfire.world.IafStructureTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.HashMap;
import java.util.Optional;

public class GorgonTempleStructure extends IafStructure {
    // Chunks per structure set cell (spacing 8 in worldgen/structure_set/gorgon_temple.json)
    private static final int CHUNKS_PER_CELL = 64;


    public static final Codec<GorgonTempleStructure> CODEC = RecordCodecBuilder.<GorgonTempleStructure>mapCodec(instance ->
            instance.group(GorgonTempleStructure.settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
                    Codec.intRange(0, 30).fieldOf("size").forGetter(structure -> structure.size),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
            ).apply(instance, GorgonTempleStructure::new)).codec();

    public GorgonTempleStructure(StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter) {
        super(config, startPool, startJigsawName, size, startHeight, projectStartToHeightmap, maxDistanceFromCenter);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext pContext) {
        if (!IafConfig.generateGorgonTemple)
            return Optional.empty();
        // The structure set offers one spot per 8x8 chunk cell; keep it with a chance of 64 / "Generate Gorgon Temple
        // Chance", so each beach chunk gets about 1.12's 1 in N roll (the ground check below does the rest)
        if (IafConfig.generateGorgonTempleChance > CHUNKS_PER_CELL && pContext.random().nextInt(IafConfig.generateGorgonTempleChance) >= CHUNKS_PER_CELL)
            return Optional.empty();
        ChunkPos pos = pContext.chunkPos();
        BlockPos blockpos = pos.getMiddleBlockPosition(1);

        Optional<Structure.GenerationStub> structurePiecesGenerator =
                JigsawPlacement.addPieces(
                        pContext, // Used for JigsawPlacement to get all the proper behaviors done.
                        this.startPool, // The starting pool to use to create the structure layout from
                        this.startJigsawName, // Can be used to only spawn from one Jigsaw block. But we don't need to worry about this.
                        this.size, // How deep a branch of pieces can go away from center piece. (5 means branches cannot be longer than 5 pieces from center piece)
                        blockpos, // Where to spawn the structure.
                        false, // "useExpansionHack" This is for legacy villages to generate properly. You should keep this false always.
                        this.projectStartToHeightmap, // Adds the terrain height's y value to the passed in blockpos's y value. (This uses WORLD_SURFACE_WG heightmap which stops at top water too)
                        // Here, blockpos's y value is 60 which means the structure spawn 60 blocks above terrain height.
                        // Set this to false for structure to be place only at the passed in blockpos's Y value instead.
                        // Definitely keep this false when placing structures in the nether as otherwise, heightmap placing will put the structure on the Bedrock roof.
                        this.maxDistanceFromCenter);

        // 1.12's rules, checked where the temple really sits: the stub's x/z is the centre of the 32x32 building
        return structurePiecesGenerator.filter(stub -> isGoodSpot(pContext, stub.position().getX(), stub.position().getZ()));
    }

    /**
     * The 1.12.2 placement rules (StructureGenerator in the 1.12.2 version, 1.7.1), on the bare terrain:
     * the biome at the spot itself must be a gorgon temple biome (not just one within 16 blocks), the top block
     * must be solid ground (not water), and the ground must be solid under all four corners of the 32x32 building
     * and 9 blocks under its middle, where the basement goes. Beaches are thin strips by the sea, so most beach
     * spots fail the corner check; that is what kept 1.12's temples rare.
     */
    private static boolean isGoodSpot(GenerationContext context, int x, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor level = context.heightAccessor();
        RandomState random = context.randomState();
        // First free block above the top block; water counts as a block here, like 1.12's height map
        int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);

        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(surface - 1),
                QuartPos.fromBlock(z), random.sampler());
        if (!BiomeConfig.test(BiomeConfig.gorgonTempleBiomes, biome))
            return false;

        // Top block at the middle must be ground (sea water fails), and the basement spot under it too
        NoiseColumn middle = generator.getBaseColumn(x, z, level, random);
        if (!isGround(middle.getBlock(surface - 1)) || !isGround(middle.getBlock(surface - 9)))
            return false;
        // All four corners of the building at the same level (half of 32 = 16 blocks out, minus the centre block)
        for (int dx : CORNERS)
            for (int dz : CORNERS)
                if (!isGround(generator.getBaseColumn(x + dx, z + dz, level, random).getBlock(surface - 1)))
                    return false;
        return true;
    }

    // Offsets from the building's centre to its corners
    private static final int[] CORNERS = {-16, 15};

    // Bare terrain is only stone, fluid and air, so "ground" = not air and not a fluid
    private static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return IafStructureTypes.GORGON_TEMPLE.get();
    }

    public static GorgonTempleStructure buildStructureConfig(BootstapContext<Structure> context) {
        HolderGetter<StructureTemplatePool> templatePoolHolderGetter = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> graveyardHolder = templatePoolHolderGetter.getOrThrow(IafStructurePieces.GORGON_TEMPLE_START);

        return new GorgonTempleStructure(
                new Structure.StructureSettings(
                        context.lookup(Registries.BIOME).getOrThrow(IafBiomeTagGenerator.HAS_GORGON_TEMPLE),
                        new HashMap<>(),
                        GenerationStep.Decoration.SURFACE_STRUCTURES,
                        TerrainAdjustment.BEARD_THIN
                ),
                graveyardHolder,
                Optional.empty(),
                2,
                ConstantHeight.ZERO,
                Optional.of(Heightmap.Types.WORLD_SURFACE_WG),
                16
        );
    }
}
