package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

public class RenderDragonFireCharge extends EntityRenderer<Projectile> {

    public boolean isFire;

    public RenderDragonFireCharge(EntityRendererProvider.Context context, boolean isFire) {
        super(context);
        this.isFire = isFire;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Projectile entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(@NotNull Projectile entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
    	Minecraft instance = Minecraft.getInstance();
        matrixStackIn.pushPose();
    	BlockRenderDispatcher blockrendererdispatcher = instance.getBlockRenderer();
    	Vec3 motion = entityIn.getDeltaMovement();
        float x = (float) motion.x;
        float z = (float) motion.z;


        float newyaw   = (float)(Math.PI - Math.atan2(z, x));
        float roll  = (entityIn.tickCount % 20) * 18f * Mth.DEG_TO_RAD;
        matrixStackIn.translate(0.0, 0.5, 0.0);
        matrixStackIn.mulPose(Axis.YP.rotation(newyaw));
        matrixStackIn.mulPose(Axis.ZP.rotation(roll));
        matrixStackIn.translate(-0.5, -0.5, -0.5);
        blockrendererdispatcher.renderSingleBlock(isFire ? Blocks.MAGMA_BLOCK.defaultBlockState() : IafBlockRegistry.DRAGON_ICE.get().defaultBlockState(), matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY);
        matrixStackIn.popPose();
    }

}
