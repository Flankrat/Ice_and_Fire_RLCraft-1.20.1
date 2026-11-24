package com.github.alexthe666.iceandfire.world.structure;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntitySnowVillager;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The 1.12 version's snow village, rebuilt on the current structure piece API: a packed ice well in the middle,
 * frozen paths out from it, and along the paths small snow huts (each with a snow villager) and lamp posts.
 * The layouts, sizes and odds are the 1.12 version's. Vanilla's old village code it was copied from is gone, so the
 * piece-picking logic lives here too.
 */
public class SnowVillagePieces {

    // Largest distance (blocks) a piece may be from the well, and how many pieces deep the village may branch
    private static final int MAX_DISTANCE = 112;
    private static final int MAX_HOUSE_DEPTH = 50;

    /** Builds a whole village into the accessor, starting with the well at x, z (y is fixed up when placed). */
    public static void generate(StructurePieceAccessor accessor, RandomSource random, int x, int z, int size) {
        Start start = new Start(random, x, z, getPieceWeights(random, size), size);
        accessor.addPiece(start);
        start.addChildren(start, accessor, random);
        // Grow roads first, then houses, picking at random, like the 1.12 version
        while (!start.pendingRoads.isEmpty() || !start.pendingHouses.isEmpty()) {
            List<StructurePiece> list = start.pendingRoads.isEmpty() ? start.pendingHouses : start.pendingRoads;
            list.remove(random.nextInt(list.size())).addChildren(start, accessor, random);
        }
    }

    private static List<PieceWeight> getPieceWeights(RandomSource random, int size) {
        List<PieceWeight> list = Lists.newArrayList();
        list.add(new PieceWeight(75, Mth.nextInt(random, 5 + size, 6 + size)));
        list.add(new PieceWeight(65, Mth.nextInt(random, 3 + size, 5 + size)));
        list.removeIf(weight -> weight.limit == 0);
        return list;
    }

    private static int totalWeight(List<PieceWeight> weights) {
        boolean canSpawn = false;
        int total = 0;
        for (PieceWeight weight : weights) {
            if (weight.limit > 0 && weight.spawned < weight.limit) {
                canSpawn = true;
            }
            total += weight.weight;
        }
        return canSpawn ? total : -1;
    }

    // The 1.12 version picks a hut or a lamp post at random here, whatever weight was rolled
    @Nullable
    private static Village createPiece(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
        return random.nextBoolean() ? WoodHut.createPiece(start, accessor, random, x, y, z, facing, depth) : TorchNew.createPiece(start, accessor, random, x, y, z, facing, depth);
    }

    @Nullable
    private static Village pickPiece(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
        int total = totalWeight(start.weights);
        if (total <= 0) {
            return null;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            int roll = random.nextInt(total);
            for (PieceWeight weight : start.weights) {
                roll -= weight.weight;
                if (roll < 0) {
                    if (!weight.canSpawnMore() || weight == start.lastWeight && start.weights.size() > 1) {
                        break;
                    }
                    Village piece = createPiece(start, accessor, random, x, y, z, facing, depth);
                    if (piece != null) {
                        weight.spawned++;
                        start.lastWeight = weight;
                        if (!weight.canSpawnMore()) {
                            start.weights.remove(weight);
                        }
                        return piece;
                    }
                }
            }
        }
        BoundingBox box = Torch.findPieceBox(accessor, x, y, z, facing);
        return box != null ? new Torch(depth, box, facing, start.zombie) : null;
    }

    @Nullable
    private static StructurePiece addHouse(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
        if (depth > MAX_HOUSE_DEPTH || Math.abs(x - start.getBoundingBox().minX()) > MAX_DISTANCE || Math.abs(z - start.getBoundingBox().minZ()) > MAX_DISTANCE) {
            return null;
        }
        StructurePiece piece = pickPiece(start, accessor, random, x, y, z, facing, depth + 1);
        if (piece != null) {
            accessor.addPiece(piece);
            start.pendingHouses.add(piece);
        }
        return piece;
    }

    @Nullable
    private static StructurePiece addRoad(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
        if (depth > 3 + start.size || Math.abs(x - start.getBoundingBox().minX()) > MAX_DISTANCE || Math.abs(z - start.getBoundingBox().minZ()) > MAX_DISTANCE) {
            return null;
        }
        BoundingBox box = Path.findPieceBox(accessor, random, x, y, z, facing);
        if (box == null || box.minY() <= 10) {
            return null;
        }
        StructurePiece piece = new Path(depth, random, box, facing, start.zombie);
        accessor.addPiece(piece);
        start.pendingRoads.add(piece);
        return piece;
    }

    static class PieceWeight {
        final int weight;
        final int limit;
        int spawned;

        PieceWeight(int weight, int limit) {
            this.weight = weight;
            this.limit = limit;
        }

        boolean canSpawnMore() {
            return this.limit == 0 || this.spawned < this.limit;
        }
    }

    /** Shared code for every village piece: ground levelling, villagers, doors and torches. */
    public abstract static class Village extends StructurePiece {
        // -1 until the piece is placed; then the ground height it was moved to
        protected int averageGroundLvl = -1;
        // One village in 50 is abandoned: zombie villagers, no doors or torches
        protected boolean zombie;
        private int villagersSpawned;

        protected Village(StructurePieceType type, int depth, BoundingBox box, Direction facing, boolean zombie) {
            super(type, depth, box);
            this.setOrientation(facing);
            this.zombie = zombie;
        }

        protected Village(StructurePieceType type, CompoundTag tag) {
            super(type, tag);
            this.averageGroundLvl = tag.getInt("HPos");
            this.villagersSpawned = tag.getInt("VCount");
            this.zombie = tag.getBoolean("Zombie");
        }

        @Override
        protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
            tag.putInt("HPos", this.averageGroundLvl);
            tag.putInt("VCount", this.villagersSpawned);
            tag.putBoolean("Zombie", this.zombie);
        }

        protected static boolean canGoDeeper(BoundingBox box) {
            return box != null && box.minY() > 10;
        }

        // Next piece off the left side of this one (1.12's getNextComponentNN)
        @Nullable
        protected StructurePiece addHouseLeft(Start start, StructurePieceAccessor accessor, RandomSource random, int offsetY, int offset) {
            Direction facing = this.getOrientation();
            if (facing == null) {
                return null;
            }
            return switch (facing) {
                case WEST, EAST -> addHouse(start, accessor, random, this.boundingBox.minX() + offset, this.boundingBox.minY() + offsetY, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
                default -> addHouse(start, accessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + offsetY, this.boundingBox.minZ() + offset, Direction.WEST, this.getGenDepth());
            };
        }

        // Next piece off the right side of this one (1.12's getNextComponentPP)
        @Nullable
        protected StructurePiece addHouseRight(Start start, StructurePieceAccessor accessor, RandomSource random, int offsetY, int offset) {
            Direction facing = this.getOrientation();
            if (facing == null) {
                return null;
            }
            return switch (facing) {
                case WEST, EAST -> addHouse(start, accessor, random, this.boundingBox.minX() + offset, this.boundingBox.minY() + offsetY, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
                default -> addHouse(start, accessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + offsetY, this.boundingBox.minZ() + offset, Direction.EAST, this.getGenDepth());
            };
        }

        /**
         * Moves the piece onto the ground the first time it's placed, from the average surface height under the part
         * of it in this chunk. Returns false if none of it is in this chunk yet.
         */
        protected boolean levelToGround(WorldGenLevel level, ChunkGenerator generator, BoundingBox chunkBox, int height, int extra) {
            if (this.averageGroundLvl < 0) {
                int total = 0;
                int count = 0;
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); ++z) {
                    for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); ++x) {
                        pos.set(x, 64, z);
                        if (chunkBox.isInside(pos)) {
                            total += Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), generator.getSeaLevel());
                            ++count;
                        }
                    }
                }
                if (count == 0) {
                    return false;
                }
                this.averageGroundLvl = total / count;
                this.boundingBox.move(0, this.averageGroundLvl - this.boundingBox.maxY() + height + extra, 0);
            }
            return true;
        }

        // Clears blocks from this spot upwards until it reaches air (1.12's clearCurrentPositionBlocksUpwards)
        protected void clearUpwards(WorldGenLevel level, int x, int y, int z, BoundingBox box) {
            BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
            if (box.isInside(pos)) {
                while (!level.isEmptyBlock(pos) && pos.getY() < level.getMaxBuildHeight() - 1) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    pos.move(Direction.UP);
                }
            }
        }

        // Puts a wall torch here facing this way, unless the village is abandoned
        protected void placeTorch(WorldGenLevel level, Direction facing, int x, int y, int z, BoundingBox box) {
            if (!this.zombie) {
                this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing), x, y, z, box);
            }
        }

        // A spruce door, unless the village is abandoned
        protected void placeDoor(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
            if (!this.zombie) {
                BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH);
                this.placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), x, y, z, box);
                this.placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, y + 1, z, box);
            }
        }

        // Snow villagers (or zombie villagers in an abandoned village) standing in a row from x, y, z.
        // Snow villagers get the given profession and job site; zombie villagers ignore them.
        protected void spawnVillagers(WorldGenLevel level, BoundingBox box, int x, int y, int z, int count, int profession, @Nullable BlockPos jobSite) {
            for (int i = this.villagersSpawned; i < count; ++i) {
                BlockPos pos = this.getWorldPos(x + i, y, z);
                if (!box.isInside(pos)) {
                    break;
                }
                ++this.villagersSpawned;
                Mob mob = this.zombie ? EntityType.ZOMBIE_VILLAGER.create(level.getLevel()) : IafEntityRegistry.SNOW_VILLAGER.get().create(level.getLevel());
                if (mob == null) {
                    continue;
                }
                mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
                if (mob instanceof EntitySnowVillager villager) {
                    villager.setProfession(profession);
                    villager.setJobSite(jobSite);
                }
                mob.setPersistenceRequired();
                level.addFreshEntityWithPassengers(mob);
            }
        }
    }

    /** The well in the middle: a packed ice pillar topped with a pool of water and four torches. */
    public static class Well extends Village {
        public Well(StructurePieceType type, int depth, RandomSource random, int x, int z, boolean zombie) {
            super(type, depth, new BoundingBox(x, 64, z, x + 5, 78, z + 5), Direction.Plane.HORIZONTAL.getRandomDirection(random), zombie);
        }

        public Well(CompoundTag tag) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_WELL.get(), tag);
        }

        public Well(StructurePieceSerializationContext context, CompoundTag tag) {
            this(tag);
        }

        @Override
        public void addChildren(@NotNull StructurePiece start, @NotNull StructurePieceAccessor accessor, @NotNull RandomSource random) {
            Start village = (Start) start;
            addRoad(village, accessor, random, this.boundingBox.minX() - 1, this.boundingBox.maxY() - 4, this.boundingBox.minZ() + 1, Direction.WEST, this.getGenDepth());
            addRoad(village, accessor, random, this.boundingBox.maxX() + 1, this.boundingBox.maxY() - 4, this.boundingBox.minZ() + 1, Direction.EAST, this.getGenDepth());
            addRoad(village, accessor, random, this.boundingBox.minX() + 1, this.boundingBox.maxY() - 4, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
            addRoad(village, accessor, random, this.boundingBox.minX() + 1, this.boundingBox.maxY() - 4, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
        }

        @Override
        public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager manager, @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
            // The 1.12 version sinks the well so only its top shows: maxY ends 3 above the ground
            if (!this.levelToGround(level, generator, box, 0, 3)) {
                return;
            }
            BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
            BlockState water = Blocks.WATER.defaultBlockState();
            BlockState torch = Blocks.TORCH.defaultBlockState();
            this.generateBox(level, box, 1, 0, 1, 4, 12, 4, ice, water, false);
            this.placeBlock(level, water, 2, 12, 2, box);
            this.placeBlock(level, water, 3, 12, 2, box);
            this.placeBlock(level, water, 2, 12, 3, box);
            this.placeBlock(level, water, 3, 12, 3, box);
            this.placeBlock(level, torch, 1, 12, 1, box);
            this.placeBlock(level, torch, 4, 12, 1, box);
            this.placeBlock(level, torch, 1, 12, 4, box);
            this.placeBlock(level, torch, 4, 12, 4, box);
        }
    }

    /** The starting piece: the well, plus the state used while the village is being laid out. */
    public static class Start extends Well {
        final List<PieceWeight> weights;
        final int size;
        final List<StructurePiece> pendingHouses = Lists.newArrayList();
        final List<StructurePiece> pendingRoads = Lists.newArrayList();
        PieceWeight lastWeight;

        public Start(RandomSource random, int x, int z, List<PieceWeight> weights, int size) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_WELL.get(), 0, random, x, z, random.nextInt(50) == 0);
            this.weights = weights;
            this.size = size;
        }
    }

    /** A frozen path. It follows the ground: grass becomes frozen path, water and ice become spruce planks. */
    public static class Path extends Village {
        private final int length;

        public Path(int depth, RandomSource random, BoundingBox box, Direction facing, boolean zombie) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_PATH.get(), depth, box, facing, zombie);
            this.length = Math.max(box.getXSpan(), box.getZSpan());
        }

        public Path(StructurePieceSerializationContext context, CompoundTag tag) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_PATH.get(), tag);
            this.length = tag.getInt("Length");
        }

        @Override
        protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("Length", this.length);
        }

        @Nullable
        static BoundingBox findPieceBox(StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing) {
            for (int length = 7 * Mth.nextInt(random, 3, 5); length >= 7; length -= 7) {
                BoundingBox box = BoundingBox.orientBox(x, y, z, 0, 0, 0, 3, 3, length, facing);
                if (accessor.findCollisionPiece(box) == null) {
                    return box;
                }
            }
            return null;
        }

        @Override
        public void addChildren(@NotNull StructurePiece start, @NotNull StructurePieceAccessor accessor, @NotNull RandomSource random) {
            Start village = (Start) start;
            boolean placed = false;
            for (int i = random.nextInt(5); i < this.length - 8; i += 2 + random.nextInt(5)) {
                StructurePiece piece = this.addHouseLeft(village, accessor, random, 0, i);
                if (piece != null) {
                    i += Math.max(piece.getBoundingBox().getXSpan(), piece.getBoundingBox().getZSpan());
                    placed = true;
                }
            }
            for (int j = random.nextInt(5); j < this.length - 8; j += 2 + random.nextInt(5)) {
                StructurePiece piece = this.addHouseRight(village, accessor, random, 0, j);
                if (piece != null) {
                    j += Math.max(piece.getBoundingBox().getXSpan(), piece.getBoundingBox().getZSpan());
                    placed = true;
                }
            }
            Direction facing = this.getOrientation();
            if (placed && random.nextInt(3) > 0 && facing != null) {
                switch (facing) {
                    case SOUTH -> addRoad(village, accessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 2, Direction.WEST, this.getGenDepth());
                    case WEST -> addRoad(village, accessor, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
                    case EAST -> addRoad(village, accessor, random, this.boundingBox.maxX() - 2, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth());
                    default -> addRoad(village, accessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, this.getGenDepth());
                }
            }
            if (placed && random.nextInt(3) > 0 && facing != null) {
                switch (facing) {
                    case SOUTH -> addRoad(village, accessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 2, Direction.EAST, this.getGenDepth());
                    case WEST -> addRoad(village, accessor, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
                    case EAST -> addRoad(village, accessor, random, this.boundingBox.maxX() - 2, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth());
                    default -> addRoad(village, accessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, this.getGenDepth());
                }
            }
        }

        @Override
        public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager manager, @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
            BlockState path = IafBlockRegistry.FROZEN_DIRT_PATH.get().defaultBlockState();
            BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
            BlockState gravel = Blocks.GRAVEL.defaultBlockState();
            BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
            int seaLevel = generator.getSeaLevel();
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); ++x) {
                for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); ++z) {
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 64, z);
                    if (!box.isInside(pos)) {
                        continue;
                    }
                    pos.setY(Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, seaLevel - 1));
                    while (pos.getY() >= seaLevel - 1) {
                        BlockState state = level.getBlockState(pos);
                        BlockState above = level.getBlockState(pos.above());
                        // Snow layers count as air here: in snowy biomes all the grass has them, which kept the
                        // 1.12 version's paths from showing up
                        if (state.is(Blocks.GRASS_BLOCK) && (above.isAir() || above.is(Blocks.SNOW))) {
                            if (above.is(Blocks.SNOW)) {
                                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);
                            }
                            level.setBlock(pos, path, 2);
                            break;
                        }
                        if (!state.getFluidState().isEmpty() || state.is(Blocks.ICE)) {
                            level.setBlock(pos, planks, 2);
                            break;
                        }
                        if (state.is(Blocks.SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)) {
                            level.setBlock(pos, gravel, 2);
                            level.setBlock(pos.below(), cobble, 2);
                            break;
                        }
                        pos.move(Direction.DOWN);
                    }
                }
            }
        }
    }

    /** The lamp post the village falls back on when nothing else fits: an oak fence, white wool and torches. */
    public static class Torch extends Village {
        public Torch(int depth, BoundingBox box, Direction facing, boolean zombie) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_TORCH.get(), depth, box, facing, zombie);
        }

        public Torch(StructurePieceSerializationContext context, CompoundTag tag) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_TORCH.get(), tag);
        }

        @Nullable
        static BoundingBox findPieceBox(StructurePieceAccessor accessor, int x, int y, int z, Direction facing) {
            BoundingBox box = BoundingBox.orientBox(x, y, z, 0, 0, 0, 3, 4, 2, facing);
            return accessor.findCollisionPiece(box) != null ? null : box;
        }

        @Override
        public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager manager, @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
            if (!this.levelToGround(level, generator, box, 4, -1)) {
                return;
            }
            BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
            this.generateBox(level, box, 0, 0, 0, 2, 3, 1, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(level, fence, 1, 0, 0, box);
            this.placeBlock(level, fence, 1, 1, 0, box);
            this.placeBlock(level, fence, 1, 2, 0, box);
            this.placeBlock(level, Blocks.WHITE_WOOL.defaultBlockState(), 1, 3, 0, box);
            this.placeTorch(level, Direction.EAST, 2, 3, 0, box);
            this.placeTorch(level, Direction.NORTH, 1, 3, 1, box);
            this.placeTorch(level, Direction.WEST, 0, 3, 0, box);
            this.placeTorch(level, Direction.SOUTH, 1, 3, -1, box);
        }
    }

    /** The village's own lamp post: a spruce fence, black wool and torches. */
    public static class TorchNew extends Village {
        public TorchNew(int depth, BoundingBox box, Direction facing, boolean zombie) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_LAMP.get(), depth, box, facing, zombie);
        }

        public TorchNew(StructurePieceSerializationContext context, CompoundTag tag) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_LAMP.get(), tag);
        }

        @Nullable
        static TorchNew createPiece(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
            BoundingBox box = BoundingBox.orientBox(x, y, z, 0, 0, 0, 4, 6, 5, facing);
            return canGoDeeper(box) && accessor.findCollisionPiece(box) == null ? new TorchNew(depth, box, facing, start.zombie) : null;
        }

        @Override
        public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager manager, @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
            if (!this.levelToGround(level, generator, box, 0, 5)) {
                return;
            }
            BlockState fence = Blocks.SPRUCE_FENCE.defaultBlockState();
            this.placeBlock(level, fence, 1, 0, 0, box);
            this.placeBlock(level, fence, 1, 1, 0, box);
            this.placeBlock(level, fence, 1, 2, 0, box);
            this.placeBlock(level, Blocks.BLACK_WOOL.defaultBlockState(), 1, 3, 0, box);
            this.placeTorch(level, Direction.EAST, 2, 3, 0, box);
            this.placeTorch(level, Direction.NORTH, 1, 3, 1, box);
            this.placeTorch(level, Direction.WEST, 0, 3, 0, box);
            this.placeTorch(level, Direction.SOUTH, 1, 3, -1, box);
        }
    }

    /** A small snow hut: frozen cobblestone floor, snow walls, packed ice corners and roof, one snow villager. */
    public static class WoodHut extends Village {
        private final boolean isTallHouse;
        private final int tablePosition;
        // The profession of this hut's villager, picked with the hut so its job site block matches;
        // -1 for huts saved before job sites existed (picked when the hut is built)
        private int profession;

        public WoodHut(int depth, RandomSource random, BoundingBox box, Direction facing, boolean zombie) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_HUT.get(), depth, box, facing, zombie);
            this.isTallHouse = random.nextBoolean();
            this.tablePosition = random.nextInt(3);
            this.profession = random.nextInt(EntitySnowVillager.PROFESSION_COUNT);
        }

        public WoodHut(StructurePieceSerializationContext context, CompoundTag tag) {
            super(IafStructurePieceTypes.SNOW_VILLAGE_HUT.get(), tag);
            this.tablePosition = tag.getInt("T");
            this.isTallHouse = tag.getBoolean("C");
            this.profession = tag.contains("P") ? tag.getInt("P") : -1;
        }

        @Override
        protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, @NotNull CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("T", this.tablePosition);
            tag.putBoolean("C", this.isTallHouse);
            tag.putInt("P", this.profession);
        }

        @Nullable
        static WoodHut createPiece(Start start, StructurePieceAccessor accessor, RandomSource random, int x, int y, int z, Direction facing, int depth) {
            BoundingBox box = BoundingBox.orientBox(x, y, z, 0, 0, 0, 4, 6, 5, facing);
            return canGoDeeper(box) && accessor.findCollisionPiece(box) == null ? new WoodHut(depth, random, box, facing, start.zombie) : null;
        }

        @Override
        public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager manager, @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box, @NotNull ChunkPos chunkPos, @NotNull BlockPos pivot) {
            if (!this.levelToGround(level, generator, box, 6, -1)) {
                return;
            }
            BlockState floor = IafBlockRegistry.FROZEN_COBBLESTONE.get().defaultBlockState();
            BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
            BlockState stairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
            BlockState fence = Blocks.OAK_FENCE.defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            this.generateBox(level, box, 1, 1, 1, 3, 5, 4, air, air, false);
            this.generateBox(level, box, 0, 0, 0, 3, 0, 4, floor, floor, false);
            if (this.isTallHouse) {
                this.generateBox(level, box, 1, 4, 1, 2, 4, 3, ice, ice, false);
            } else {
                this.generateBox(level, box, 1, 5, 1, 2, 5, 3, ice, ice, false);
            }
            this.placeBlock(level, ice, 1, 4, 0, box);
            this.placeBlock(level, ice, 2, 4, 0, box);
            this.placeBlock(level, ice, 1, 4, 4, box);
            this.placeBlock(level, ice, 2, 4, 4, box);
            this.placeBlock(level, ice, 0, 4, 1, box);
            this.placeBlock(level, ice, 0, 4, 2, box);
            this.placeBlock(level, ice, 0, 4, 3, box);
            this.placeBlock(level, ice, 3, 4, 1, box);
            this.placeBlock(level, ice, 3, 4, 2, box);
            this.placeBlock(level, ice, 3, 4, 3, box);
            this.generateBox(level, box, 0, 1, 0, 0, 3, 0, ice, ice, false);
            this.generateBox(level, box, 3, 1, 0, 3, 3, 0, ice, ice, false);
            this.generateBox(level, box, 0, 1, 4, 0, 3, 4, ice, ice, false);
            this.generateBox(level, box, 3, 1, 4, 3, 3, 4, ice, ice, false);
            this.generateBox(level, box, 0, 1, 1, 0, 3, 3, snow, snow, false);
            this.generateBox(level, box, 3, 1, 1, 3, 3, 3, snow, snow, false);
            this.generateBox(level, box, 1, 1, 0, 2, 3, 0, snow, snow, false);
            this.generateBox(level, box, 1, 1, 4, 2, 3, 4, snow, snow, false);
            this.placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState(), 0, 2, 2, box);
            this.placeBlock(level, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState(), 3, 2, 2, box);
            if (this.tablePosition > 0) {
                this.placeBlock(level, fence, this.tablePosition, 1, 3, box);
                this.placeBlock(level, Blocks.OAK_PRESSURE_PLATE.defaultBlockState(), this.tablePosition, 2, 3, box);
            }
            this.placeBlock(level, air, 1, 1, 0, box);
            this.placeBlock(level, air, 1, 2, 0, box);
            this.placeDoor(level, box, 1, 1, 0);
            // A step down to the path in front of the door
            if (this.getBlock(level, 1, 0, -1, box).isAir() && !this.getBlock(level, 1, -1, -1, box).isAir()) {
                this.placeBlock(level, stairs, 1, 0, -1, box);
                if (this.getBlock(level, 1, -1, -1, box).is(IafBlockRegistry.FROZEN_DIRT_PATH.get())) {
                    this.placeBlock(level, Blocks.GRASS_BLOCK.defaultBlockState(), 1, -1, -1, box);
                }
            }
            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 4; ++j) {
                    this.clearUpwards(level, j, 6, i, box);
                    this.fillColumnDown(level, floor, j, -1, i, box);
                }
            }
            if (this.profession < 0) {
                this.profession = random.nextInt(EntitySnowVillager.PROFESSION_COUNT);
            }
            // The villager's job site block, in the corner just inside the door (clear of the table and the
            // villager's spot). Abandoned huts keep theirs too, like vanilla abandoned villages.
            this.placeBlock(level, EntitySnowVillager.getJobSiteBlock(this.profession), 2, 1, 1, box);
            BlockPos jobSite = this.getWorldPos(2, 1, 1);
            this.spawnVillagers(level, box, 1, 1, 2, 1, this.profession, box.isInside(jobSite) ? jobSite : null);
        }
    }
}
