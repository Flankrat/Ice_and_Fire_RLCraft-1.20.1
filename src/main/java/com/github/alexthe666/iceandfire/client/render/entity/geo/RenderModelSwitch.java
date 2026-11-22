package com.github.alexthe666.iceandfire.client.render.entity.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;
import java.util.function.ToDoubleFunction;

/**
 * Hands each frame to one of two renderers depending on a client option, so a mob's model can be switched without
 * restarting: the classic Ice and Fire renderer, or the Community Edition GeckoLib one.
 */
public class RenderModelSwitch<T extends Entity> extends EntityRenderer<T> {
    private final EntityRenderer<T> classic;
    private final EntityRenderer<T> community;
    private final BooleanSupplier useCommunity;
    // The dispatcher reads this renderer's own shadow size, so it's worked out here for both models
    private final ToDoubleFunction<T> shadowSize;

    public RenderModelSwitch(EntityRendererProvider.Context context, EntityRenderer<T> classic, EntityRenderer<T> community, BooleanSupplier useCommunity, ToDoubleFunction<T> shadowSize) {
        super(context);
        this.classic = classic;
        this.community = community;
        this.useCommunity = useCommunity;
        this.shadowSize = shadowSize;
    }

    private EntityRenderer<T> current() {
        return this.useCommunity.getAsBoolean() ? this.community : this.classic;
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = (float) this.shadowSize.applyAsDouble(entity);
        this.current().render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(@NotNull T entity, @NotNull Frustum camera, double camX, double camY, double camZ) {
        // The classic renderer's check also covers things drawn away from the body, like the lightning breath
        return this.classic.shouldRender(entity, camera, camX, camY, camZ);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T entity) {
        return this.current().getTextureLocation(entity);
    }
}
