package com.github.alexthe666.iceandfire.client.render.entity.geo;

// Ported from Ice and Fire Community Edition (github.com/IAFEnvoy/IceAndFire-CE, LGPL-3.0), "geckolib-test" branch.
import com.github.alexthe666.iceandfire.client.model.geo.SeaSerpentGeoModel;
import com.github.alexthe666.iceandfire.client.render.entity.RenderSeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Draws a sea serpent with Ice and Fire Community Edition's GeckoLib model, using the classic textures. */
public class RenderSeaSerpentGeo extends GeoEntityRenderer<EntitySeaSerpent> {
    private static final ResourceLocation ANCIENT = new ResourceLocation("iceandfire:textures/models/seaserpent/ancient_overlay.png");
    private static final ResourceLocation ANCIENT_BLINK = new ResourceLocation("iceandfire:textures/models/seaserpent/ancient_overlay_blink.png");
    // Puts the model's root at the centre of the scaled multipart hitbox
    private static final float MODEL_VERTICAL_OFFSET = -0.50F;
    private final RenderSeaSerpent classic;

    public RenderSeaSerpentGeo(EntityRendererProvider.Context context, RenderSeaSerpent classic) {
        super(context, new SeaSerpentGeoModel(classic::getTextureLocation));
        this.classic = classic;
        this.shadowRadius = 1.6F;
        this.addRenderLayer(new AncientLayer(this));
    }

    @Override
    public void preRender(PoseStack poseStack, EntitySeaSerpent serpent, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.shadowRadius = serpent.getSeaSerpentScale();
        if (!isReRender) {
            poseStack.scale(this.shadowRadius, this.shadowRadius, this.shadowRadius);
            poseStack.translate(0.0F, MODEL_VERTICAL_OFFSET, 0.0F);
        }
        super.preRender(poseStack, serpent, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntitySeaSerpent serpent) {
        return this.classic.getTextureLocation(serpent);
    }

    /** The glowing overlay of ancient sea serpents. */
    private static class AncientLayer extends GeoRenderLayer<EntitySeaSerpent> {
        AncientLayer(GeoRenderer<EntitySeaSerpent> renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack poseStack, EntitySeaSerpent serpent, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            if (!serpent.isAncient()) {
                return;
            }
            RenderType overlay = RenderType.entityNoOutline(serpent.isBlinking() ? ANCIENT_BLINK : ANCIENT);
            this.getRenderer().reRender(model, poseStack, bufferSource, serpent, overlay, bufferSource.getBuffer(overlay), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
