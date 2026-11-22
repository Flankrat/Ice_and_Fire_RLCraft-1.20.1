package com.github.alexthe666.iceandfire.world.feature;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SpawnDragonSkeleton extends Feature<NoneFeatureConfiguration> {

    protected EntityType<? extends EntityDragonBase> dragonType;

    public SpawnDragonSkeleton(EntityType<? extends EntityDragonBase> dt, Codec<NoneFeatureConfiguration> configFactoryIn) {
        super(configFactoryIn);
        dragonType = dt;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // Dimension lists from the world gen config
        if (!IafWorldRegistry.isDimensionAllowed(context.level(), true)) {
            return false;
        }
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = context.origin();

        position = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, position.offset(8, 0, 8));

        if (IafConfig.generateDragonSkeletons) {
            if (rand.nextInt(IafConfig.generateDragonSkeletonChance + 1) == 0) {
                EntityDragonBase dragon = dragonType.create(worldIn.getLevel());
                dragon.setPos(position.getX() + 0.5F, position.getY() + 1, position.getZ() + 0.5F);
                int dragonage = 10 + rand.nextInt(100);
                // Generate Dragon Skeletons Max Stage: stage 5 starts at 100 days, so cap the age below the next stage
                dragonage = Math.min(dragonage, maxAgeForStage(IafConfig.generateDragonSkeletonMaximumStage));
                dragon.growDragon(dragonage);
                dragon.modelDeadProgress = 20;
                dragon.setModelDead(true);
                dragon.setDeathStage((dragonage / 5) / 2);
                dragon.setYRot(rand.nextInt(360));
                worldIn.addFreshEntity(dragon);
            }
        }

        return true;
    }

    // Highest dragon age (in days) that is still the given stage: stages start at 0, 25, 50, 75 and 100 days
    private static int maxAgeForStage(int stage) {
        return stage >= 5 ? Integer.MAX_VALUE : stage * 25 - 1;
    }
}
