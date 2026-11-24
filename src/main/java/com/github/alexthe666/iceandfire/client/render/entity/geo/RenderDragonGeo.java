package com.github.alexthe666.iceandfire.client.render.entity.geo;

// Ported from Ice and Fire Community Edition (github.com/IAFEnvoy/IceAndFire-CE, LGPL-3.0), "geckolib-test" branch.
import com.github.alexthe666.iceandfire.client.ClientProxy;
import com.github.alexthe666.iceandfire.client.model.geo.DragonGeoModel;
import com.github.alexthe666.iceandfire.client.render.entity.RenderDragonBase;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerDragonArmor;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.enums.EnumDragonTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Draws a dragon with Ice and Fire Community Edition's GeckoLib model. The texture (with or without the male spots),
 * eyes and armor are the classic renderer's, since the Community Edition model uses the same texture layout.
 */
public class RenderDragonGeo extends GeoEntityRenderer<EntityDragonBase> {
    private final RenderDragonBase classic;

    public RenderDragonGeo(EntityRendererProvider.Context context, RenderDragonBase classic) {
        super(context, new DragonGeoModel(classic::getTextureLocation));
        this.classic = classic;
        this.shadowRadius = 0.0025F;
        this.addRenderLayer(new OverlayLayer(this));
        this.addRenderLayer(new AttachmentLayer(this));
    }

    // Same pitch and size scaling as the classic renderer
    @Override
    public void preRender(PoseStack poseStack, EntityDragonBase dragon, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.shadowRadius = dragon.getRenderSize() / 3;
        if (!isReRender) {
            float pitch = dragon.prevDragonPitch + (dragon.getDragonPitch() - dragon.prevDragonPitch) * partialTick;
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.scale(this.shadowRadius, this.shadowRadius, this.shadowRadius);
        }
        super.preRender(poseStack, dragon, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDragonBase dragon) {
        return this.classic.getTextureLocation(dragon);
    }

    /** Eyes and armor, drawn over the same geometry. */
    private static class OverlayLayer extends GeoRenderLayer<EntityDragonBase> {
        OverlayLayer(GeoRenderer<EntityDragonBase> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, EntityDragonBase dragon, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (dragon.shouldRenderEyes()) {
                ResourceLocation eyes = dragon instanceof com.github.alexthe666.iceandfire.entity.EntityGoldDragon
                    ? com.github.alexthe666.iceandfire.enums.GoldDragonTextures.getEyeTexture(dragon)
                    : EnumDragonTextures.getEyeTextureFromDragon(dragon);
                RenderType eyesType = RenderType.eyes(eyes);
                this.getRenderer().reRender(model, poseStack, bufferSource, dragon, eyesType, bufferSource.getBuffer(eyesType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            ResourceLocation armor = LayerDragonArmor.getArmorTexture(dragon);
            if (armor != null) {
                RenderType armorType = RenderType.entityCutoutNoCull(armor);
                this.getRenderer().reRender(model, poseStack, bufferSource, dragon, armorType, bufferSource.getBuffer(armorType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    /** Riders and the banner, carried on the animated upper body. */
    private static class AttachmentLayer extends GeoRenderLayer<EntityDragonBase> {
        AttachmentLayer(GeoRenderer<EntityDragonBase> renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, EntityDragonBase dragon, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (!bone.getName().equals("BodyUpper")) {
                return;
            }
            float dragonScale = dragon.getRenderSize() / 3F;
            ItemStack banner = dragon.getItemInHand(InteractionHand.OFF_HAND);
            if (banner.getItem() instanceof BannerItem) {
                poseStack.pushPose();
                poseStack.translate(0, -0.2F, 0.4F);
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                poseStack.scale(1F / dragonScale, 1F / dragonScale, 1F / dragonScale);
                Minecraft.getInstance().getItemRenderer().renderStatic(banner, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, dragon.level(), 0);
                poseStack.popPose();
            }
            for (Entity passenger : dragon.getPassengers()) {
                boolean prey = dragon.getControllingPassenger() == null || dragon.getControllingPassenger().getId() != passenger.getId();
                poseStack.pushPose();
                poseStack.translate(0, prey ? -0.12F : -0.01F, prey ? -0.55F : -0.04F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                float riderYaw = passenger.yRotO + (passenger.getYRot() - passenger.yRotO) * partialTick;
                poseStack.mulPose(Axis.YP.rotationDegrees(riderYaw + 180.0F));
                poseStack.scale(1F / dragonScale, 1F / dragonScale, 1F / dragonScale);
                // Riders are hidden from the normal pass (ClientEvents) and drawn here instead, as with the classic model
                ClientProxy.currentDragonRiders.remove(passenger.getUUID());
                Minecraft.getInstance().getEntityRenderDispatcher().render(passenger, 0, 0, 0, 0.0F, partialTick, poseStack, bufferSource, packedLight);
                ClientProxy.currentDragonRiders.add(passenger.getUUID());
                poseStack.popPose();
            }
        }
    }
}
