package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerDragonEyes;
import com.github.alexthe666.iceandfire.client.render.entity.layer.LayerGenericGlowing;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * From the 1.12 version: the Black Frost uses the ice dragon model with its own texture and glowing eyes, in place
 * of the variant texture and eye layer of normal ice dragons.
 */
public class RenderBlackFrostDragon extends RenderDragonBase {
    public static final ResourceLocation TEXTURE = new ResourceLocation("iceandfire:textures/models/dread/black_frost.png");
    public static final ResourceLocation TEXTURE_EYES = new ResourceLocation("iceandfire:textures/models/dread/black_frost_eyes.png");

    public RenderBlackFrostDragon(EntityRendererProvider.Context context, AdvancedEntityModel<EntityDragonBase> model) {
        super(context, model, 1);
        this.layers.removeIf(layer -> layer instanceof LayerDragonEyes);
        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDragonBase entity) {
        return TEXTURE;
    }
}
