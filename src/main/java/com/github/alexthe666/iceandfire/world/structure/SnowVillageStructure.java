package com.github.alexthe666.iceandfire.world.structure;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.world.IafStructureTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraftforge.common.Tags;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * From the 1.12 version: snow villages, in cold snowy biomes (the structure's biome tag holds the snowy biomes; the
 * cold check is done here). The 1.12 version rolled 1 in "Generate Snow Village Chance" per snowy chunk. Here the
 * structure set offers one spot per 12x12 chunk cell, and each spot is kept with a chance of 144 / that setting, so
 * villages stay about as common per chunk.
 */
public class SnowVillageStructure extends Structure {
    public static final Codec<SnowVillageStructure> CODEC = simpleCodec(SnowVillageStructure::new);
    // Chunks per structure set cell (spacing 12 in worldgen/structure_set/snow_village.json)
    private static final int CHUNKS_PER_CELL = 144;

    public SnowVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(@NotNull GenerationContext context) {
        if (!IafConfig.generateSnowVillages || !isDimensionAllowed(context)) {
            return Optional.empty();
        }
        if (IafConfig.generateSnowVillageChance > CHUNKS_PER_CELL && context.random().nextInt(IafConfig.generateSnowVillageChance) >= CHUNKS_PER_CELL) {
            return Optional.empty();
        }
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMinBlockX() + 2;
        int z = chunkPos.getMinBlockZ() + 2;
        int y = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler());
        if (!biome.is(Tags.Biomes.IS_COLD)) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(x, y, z), builder -> generatePieces(builder, context, x, z)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context, int x, int z) {
        SnowVillagePieces.generate(builder, context.random(), x, z, 0);
        // Like the 1.12 version, a village is only kept if it has more than two pieces besides paths
        int houses = 0;
        for (StructurePiece piece : builder.build().pieces()) {
            if (!(piece instanceof SnowVillagePieces.Path)) {
                houses++;
            }
        }
        if (houses <= 2) {
            builder.clear();
        }
    }

    // GenerationContext doesn't say which dimension it's for, so find the level that uses this chunk generator
    private static boolean isDimensionAllowed(GenerationContext context) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return true;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getChunkSource().getGenerator() == context.chunkGenerator()) {
                ResourceLocation id = level.dimension().location();
                return IafConfig.snowVillageDimensionWhitelist == IafConfig.snowVillageDimensions.contains(id);
            }
        }
        return true;
    }

    @Override
    public @NotNull StructureType<?> type() {
        return IafStructureTypes.SNOW_VILLAGE.get();
    }
}
