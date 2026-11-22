package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.entity.EntitySnowVillager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** From the 1.12 version: the villager model with a texture per snow villager profession; the fisherman holds his spear. */
public class RenderSnowVillager extends MobRenderer<EntitySnowVillager, VillagerModel<EntitySnowVillager>> {
    private static final ResourceLocation[] TEXTURES = {
        new ResourceLocation("iceandfire:textures/models/snowvillager/fisherman.png"),
        new ResourceLocation("iceandfire:textures/models/snowvillager/craftsman.png"),
        new ResourceLocation("iceandfire:textures/models/snowvillager/shaman.png")
    };

    public RenderSnowVillager(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntitySnowVillager entity) {
        return TEXTURES[entity.getProfession()];
    }

    // Same size as a vanilla villager
    @Override
    protected void scale(@NotNull EntitySnowVillager entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
