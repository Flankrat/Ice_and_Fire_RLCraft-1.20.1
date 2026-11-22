package com.github.alexthe666.iceandfire.client.model.geo;

// Ported from Ice and Fire Community Edition (github.com/IAFEnvoy/IceAndFire-CE, LGPL-3.0), "geckolib-test" branch.
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Function;

/** Ice and Fire Community Edition's GeckoLib sea serpent model, with the body bending through its tail pieces. */
public class SeaSerpentGeoModel extends GeoModel<EntitySeaSerpent> {
    private final Function<EntitySeaSerpent, ResourceLocation> textures;

    public SeaSerpentGeoModel(Function<EntitySeaSerpent, ResourceLocation> textures) {
        this.textures = textures;
    }

    @Override
    public ResourceLocation getModelResource(EntitySeaSerpent serpent) {
        return new ResourceLocation("iceandfire", "geo/seaserpent.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntitySeaSerpent serpent) {
        return this.textures.apply(serpent);
    }

    @Override
    public ResourceLocation getAnimationResource(EntitySeaSerpent serpent) {
        return new ResourceLocation("iceandfire", "animations/seaserpent.animation.json");
    }

    @Override
    public void setCustomAnimations(EntitySeaSerpent serpent, long instanceId, AnimationState<EntitySeaSerpent> state) {
        float partialTick = state.getPartialTick();
        float bodyYaw = Mth.rotLerp(partialTick, serpent.yBodyRotO, serpent.yBodyRot);
        boolean jumping = serpent.isJumpingOutOfWater() || serpent.jumpProgress > 0.0F || serpent.jumpRot > 0.0F;
        float jumpPitch = 0.0F;
        if (jumping) {
            // GeckoLib's X axis is the reverse of the Tabula pose data: pitch into the jump, bend the tail against it
            float jumpRotation = Mth.lerp(partialTick, serpent.prevJumpRot, serpent.jumpRot);
            jumpPitch = (float) serpent.getDeltaMovement().y * 4.0F * jumpRotation;
            this.rotateBone("BodyUpper", Mth.DEG_TO_RAD * 22.5F * jumpPitch, 0.0F, 0.0F);
        } else {
            this.rotateBone("BodyUpper", -serpent.getXRot() * Mth.DEG_TO_RAD, 0.0F, 0.0F);
        }
        for (int index = 1; index <= 4; index++) {
            float yaw = (serpent.getPieceYaw(index, partialTick) - bodyYaw) * Mth.DEG_TO_RAD;
            float pitch = jumping ? -jumpPitch * Mth.DEG_TO_RAD : -serpent.getPiecePitch(index, partialTick) * Mth.DEG_TO_RAD;
            this.rotateBone("Tail" + index, pitch, yaw, 0.0F);
        }
        if (serpent.breathProgress > 0.0F) {
            float progress = Mth.clamp(serpent.breathProgress / 20.0F, 0.0F, 1.0F);
            this.rotateBone("Head", Mth.DEG_TO_RAD * -15.0F * progress, 0.0F, 0.0F);
            this.rotateBone("HeadFront", Mth.DEG_TO_RAD * -20.0F * progress, 0.0F, 0.0F);
            this.rotateBone("Jaw", Mth.DEG_TO_RAD * 60.0F * progress, 0.0F, 0.0F);
        }
    }

    private void rotateBone(String name, float x, float y, float z) {
        CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.updateRotation(x, y, z);
        }
    }
}
