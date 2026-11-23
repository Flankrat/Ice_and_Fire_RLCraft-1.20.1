package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.client.model.ModelDreadQueen;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerGenericGlowing;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** From the 1.12 version: the Dread Queen, with glowing eyes and her sword and staff in hand. */
public class RenderDreadQueen extends MobRenderer<EntityDreadQueen, ModelDreadQueen> {
    public static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire:textures/models/dread/dread_queen.png");
    public static final ResourceLocation TEXTURE_EYES = new ResourceLocation("iceandfire:textures/models/dread/dread_queen_eyes.png");

    public RenderDreadQueen(EntityRendererProvider.Context context) {
        super(context, new ModelDreadQueen(0.0F), 0.6F);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(@NotNull EntityDreadQueen entity, PoseStack matrixStackIn, float partialTickTime) {
        matrixStackIn.scale(0.95F, 0.95F, 0.95F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDreadQueen entity) {
        return TEXTURE;
    }
}
