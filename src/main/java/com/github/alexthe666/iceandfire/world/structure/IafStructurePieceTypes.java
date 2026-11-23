package com.github.alexthe666.iceandfire.world.structure;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Piece types for structures built from code rather than templates, so their pieces can be saved and loaded. */
public class IafStructurePieceTypes {
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, IceAndFire.MODID);

    public static final RegistryObject<StructurePieceType> SNOW_VILLAGE_WELL = PIECE_TYPES.register("snow_village_well", () -> SnowVillagePieces.Well::new);
    public static final RegistryObject<StructurePieceType> SNOW_VILLAGE_PATH = PIECE_TYPES.register("snow_village_path", () -> SnowVillagePieces.Path::new);
    public static final RegistryObject<StructurePieceType> SNOW_VILLAGE_TORCH = PIECE_TYPES.register("snow_village_torch", () -> SnowVillagePieces.Torch::new);
    public static final RegistryObject<StructurePieceType> SNOW_VILLAGE_LAMP = PIECE_TYPES.register("snow_village_lamp", () -> SnowVillagePieces.TorchNew::new);
    public static final RegistryObject<StructurePieceType> SNOW_VILLAGE_HUT = PIECE_TYPES.register("snow_village_hut", () -> SnowVillagePieces.WoodHut::new);
}
