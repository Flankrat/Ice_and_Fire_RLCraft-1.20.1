package com.github.alexthe666.iceandfire.client.model.geo;

// Ported from Ice and Fire Community Edition (github.com/IAFEnvoy/IceAndFire-CE, LGPL-3.0), "geckolib-test" branch.
import com.github.alexthe666.iceandfire.entity.DragonType;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;

/** Ice and Fire Community Edition's GeckoLib dragon model. Textures come from the classic renderer, so they match. */
public class DragonGeoModel extends GeoModel<EntityDragonBase> {
    private final Function<EntityDragonBase, ResourceLocation> textures;

    public DragonGeoModel(Function<EntityDragonBase, ResourceLocation> textures) {
        this.textures = textures;
    }

    @Override
    public ResourceLocation getModelResource(EntityDragonBase dragon) {
        return new ResourceLocation("iceandfire", "geo/" + modelName(dragon) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntityDragonBase dragon) {
        return this.textures.apply(dragon);
    }

    @Override
    public ResourceLocation getAnimationResource(EntityDragonBase dragon) {
        return new ResourceLocation("iceandfire", "animations/" + modelName(dragon) + ".animation.json");
    }

    // Blends the sleeping pose in and out with the dragon's sleep progress instead of snapping to it
    @Override
    public void setCustomAnimations(EntityDragonBase dragon, long instanceId, AnimationState<EntityDragonBase> state) {
        float sleep = Mth.clamp(Mth.lerp(state.getPartialTick(), dragon.prevAnimationProgresses[1], dragon.sleepProgress) / 20.0F, 0.0F, 1.0F);
        if (!dragon.isSleeping() && sleep <= 0.0F || sleep >= 1.0F) {
            return;
        }
        for (CoreGeoBone bone : this.getAnimationProcessor().getRegisteredBones()) {
            BoneSnapshot initial = bone.getInitialSnapshot();
            bone.setRotX(Mth.lerp(sleep, initial.getRotX(), bone.getRotX()));
            bone.setRotY(Mth.lerp(sleep, initial.getRotY(), bone.getRotY()));
            bone.setRotZ(Mth.lerp(sleep, initial.getRotZ(), bone.getRotZ()));
            bone.setPosX(Mth.lerp(sleep, initial.getOffsetX(), bone.getPosX()));
            bone.setPosY(Mth.lerp(sleep, initial.getOffsetY(), bone.getPosY()));
            bone.setPosZ(Mth.lerp(sleep, initial.getOffsetZ(), bone.getPosZ()));
            bone.setScaleX(Mth.lerp(sleep, initial.getScaleX(), bone.getScaleX()));
            bone.setScaleY(Mth.lerp(sleep, initial.getScaleY(), bone.getScaleY()));
            bone.setScaleZ(Mth.lerp(sleep, initial.getScaleZ(), bone.getScaleZ()));
        }
    }

    public static String modelName(EntityDragonBase dragon) {
        // Gold dragons are built on the fire dragon model
        return dragon.dragonType == DragonType.FIRE || dragon.dragonType == DragonType.GOLD ? "firedragon" : dragon.dragonType == DragonType.ICE ? "icedragon" : "lightningdragon";
    }
}
