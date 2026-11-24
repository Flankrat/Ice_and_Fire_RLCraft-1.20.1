package com.github.alexthe666.iceandfire.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Hands each frame to the renderer of the model set picked in the client config, so a mob's model can be switched
 * without restarting. "classic" (Ice and Fire's own models) is always there; other model sets are added with
 * {@link #with}. A name that has no model set falls back to classic.
 */
public class RenderModelSwitch<T extends Entity> extends EntityRenderer<T> {
    public static final String CLASSIC = "classic";

    private final EntityRenderer<T> classic;
    // Extra model sets by their config name (none yet)
    private final Map<String, EntityRenderer<T>> modelSets = new HashMap<>();
    private final Supplier<String> choice;
    // The dispatcher reads this renderer's own shadow size, so it's worked out here for every model set
    private final ToDoubleFunction<T> shadowSize;

    public RenderModelSwitch(EntityRendererProvider.Context context, EntityRenderer<T> classic, Supplier<String> choice, ToDoubleFunction<T> shadowSize) {
        super(context);
        this.classic = classic;
        this.choice = choice;
        this.shadowSize = shadowSize;
    }

    // Adds a model set that the config can pick by this name
    public RenderModelSwitch<T> with(String name, EntityRenderer<T> renderer) {
        this.modelSets.put(name.toLowerCase(), renderer);
        return this;
    }

    private EntityRenderer<T> current() {
        String name = this.choice.get();
        return name == null ? this.classic : this.modelSets.getOrDefault(name.toLowerCase(), this.classic);
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
