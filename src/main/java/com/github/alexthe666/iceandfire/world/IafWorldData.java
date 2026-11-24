package com.github.alexthe666.iceandfire.world;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.world.gen.TypedFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class IafWorldData extends SavedData {
    public enum FeatureType {
        SURFACE,
        UNDERGROUND,
        OCEAN
    }

    private static final String IDENTIFIER = IceAndFire.MODID + "_general";
    // Per dimension (each dimension has its own saved data). This used to be static: one list for every dimension,
    // which also carried over into the next world opened in single player.
    // Worldgen threads add to it while autosave reads it, so every access holds the lock on this object.
    private final EnumMap<FeatureType, ArrayList<Map.Entry<String, BlockPos>>> lastGenerated = new EnumMap<>(FeatureType.class);

    public IafWorldData() {
        for (FeatureType type : FeatureType.values()) {
            lastGenerated.put(type, new ArrayList<>());
        }
    }

    public IafWorldData(final CompoundTag tag) {
        this();
        this.load(tag);
    }

    public static IafWorldData get(final Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(world.dimension());
            DimensionDataStorage storage = overworld.getDataStorage();
            // Worldgen threads call this at the same time as each other (the storage keeps a plain HashMap)
            synchronized (storage) {
                return storage.computeIfAbsent(IafWorldData::new, IafWorldData::new, IDENTIFIER);
            }
        }

        return null;
    }

    public boolean check(final TypedFeature feature, final BlockPos position, final String id) {
        return check(feature.getFeatureType(), position, id);
    }

    /**
     * Whether a dangerous structure can generate here: it has to be far enough from every structure recorded for this
     * feature type. Only positions that are allowed get recorded (each feature id keeps its latest position).
     */
    public synchronized boolean check(final FeatureType type, final BlockPos position, final String id) {
        ArrayList<Map.Entry<String, BlockPos>> entries = lastGenerated.get(type);
        double limitSq = (double) IafConfig.dangerousWorldGenSeparationLimit * IafConfig.dangerousWorldGenSeparationLimit;

        // Every entry has to be far enough (this was overwritten each time round the loop, so only the last one counted)
        for (Map.Entry<String, BlockPos> entry : entries) {
            if (position.distSqr(entry.getValue()) <= limitSq) {
                return false;
            }
        }

        entries.removeIf(entry -> entry.getKey().equals(id));

        if (entries.size() > 5_000) {
            IceAndFire.LOGGER.debug("Too many BlockPos entries for feature type {} tracked, removing oldest ones", type);
            entries.subList(0, 1_000).clear();
            entries.trimToSize();
        }

        entries.add(Map.entry(id, position));
        this.setDirty();

        return true;
    }

    public synchronized IafWorldData load(final CompoundTag tag) {
        FeatureType[] types = FeatureType.values();

        for (FeatureType type : types) {
            ListTag list = tag.getList(type.toString(), ListTag.TAG_COMPOUND);

            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String id = entry.getString("id");
                BlockPos position = NbtUtils.readBlockPos(entry.getCompound("position"));
                lastGenerated.get(type).add(Map.entry(id, position));
            }
        }

        return this;
    }

    @Override
    public synchronized @NotNull CompoundTag save(@NotNull final CompoundTag tag) {
        for (var e : lastGenerated.entrySet()) {
            ListTag listTag = new ListTag();

            for (Map.Entry<String, BlockPos> entry : e.getValue()) {
                CompoundTag subTag = new CompoundTag();
                subTag.putString("id", entry.getKey());
                subTag.put("position", NbtUtils.writeBlockPos(entry.getValue()));
                listTag.add(subTag);
            }

            tag.put(e.getKey().toString(), listTag);
        }

        return tag;
    }
}
