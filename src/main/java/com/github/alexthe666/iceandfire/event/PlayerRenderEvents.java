package com.github.alexthe666.iceandfire.event;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public class PlayerRenderEvents {
    public ResourceLocation redTex = new ResourceLocation("iceandfire", "textures/models/misc/cape_fire.png");
    public ResourceLocation redElytraTex = new ResourceLocation("iceandfire", "textures/models/misc/elytra_fire.png");
    public ResourceLocation blueTex = new ResourceLocation("iceandfire", "textures/models/misc/cape_ice.png");
    public ResourceLocation blueElytraTex = new ResourceLocation("iceandfire", "textures/models/misc/elytra_ice.png");
    public ResourceLocation betaTex = new ResourceLocation("iceandfire", "textures/models/misc/cape_beta.png");
    public ResourceLocation betaElytraTex = new ResourceLocation("iceandfire", "textures/models/misc/elytra_beta.png");
    // 64x32 cape sheet from 1.12.2, also used for the elytra like it was there
    public ResourceLocation lightningTex = new ResourceLocation("iceandfire", "textures/models/misc/cape_lightning.png");

    public UUID[] redcapes = new UUID[]{
            /* zeklo */UUID.fromString("59efccaf-902d-45da-928a-5a549b9fd5e0"),
            /* Alexthe666 */UUID.fromString("71363abe-fd03-49c9-940d-aae8b8209b7c")
    };
    public UUID[] bluecapes = new UUID[]{
            /* Raptorfarian */UUID.fromString("0ed918c8-d612-4360-b711-cd415671356f"),
            /*Zyranna*/        UUID.fromString("5d43896a-06a0-49fb-95c5-38485c63667f")};
    public UUID[] betatesters = new UUID[]{
    };
    // Same lightning cape list as the 1.12.2 RLCraft Ice and Fire (RenderModCapes)
    public UUID[] lightningcapes = new UUID[]{
            /* Cdstk */UUID.fromString("a62d6653-112e-4e81-bffb-7d778bfb7cb0"),
            /* fonnymunkey */UUID.fromString("648ab92b-e045-43c8-af1e-85a069d7bf42"),
            /* ShadicBypass (Flankrat) */UUID.fromString("e1dd391f-0be1-4ade-8aeb-d651bb144958"),
            /* Xvareon */UUID.fromString("41f279ac-ddbd-489f-a189-ef2a52a45d13")
    };

    // PlayerInfo.textureLocations (SRG name), the map vanilla reads cape/elytra textures from
    private static Field textureLocationsField;
    private static boolean textureLocationsChecked;
    // OptiFine's AbstractClientPlayer.setLocationOfCape(ResourceLocation), null when OptiFine isn't loaded
    private static Method optifineCapeSetter;
    private static boolean optifineCapeSetterChecked;

    @SubscribeEvent
    public void playerRender(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            UUID uuid = player.getUUID();
            // Later checks win, same order as before (beta, then red, blue, lightning)
            if (hasBetaCape(uuid)) {
                setCape(player, betaTex, betaElytraTex);
            }
            if (hasRedCape(uuid)) {
                setCape(player, redTex, redElytraTex);
            }
            if (hasBlueCape(uuid)) {
                setCape(player, blueTex, blueElytraTex);
            }
            if (hasLightningCape(uuid)) {
                setCape(player, lightningTex, lightningTex);
            }
        }
        if (event.getEntity().getUUID().equals(ServerEvents.ALEX_UUID)) {
            event.getPoseStack().pushPose();
            float f2 = ((float) event.getEntity().tickCount - 1 + event.getPartialTick());
            float f3 = Mth.sin(f2 / 10.0F) * 0.1F + 0.1F;
            event.getPoseStack().translate((float) 0, event.getEntity().getBbHeight() * 1.25F, (float) 0);
            float f4 = (f2 / 20.0F) * (180F / (float) Math.PI);
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(f4));
            event.getPoseStack().pushPose();
            Minecraft.getInstance().getItemRenderer().renderStatic(Minecraft.getInstance().player, new ItemStack(IafItemRegistry.WEEZER_BLUE_ALBUM.get()), ItemDisplayContext.GROUND, false, event.getPoseStack(), event.getMultiBufferSource(), event.getEntity().level(), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
            event.getPoseStack().popPose();
            event.getPoseStack().popPose();

        }
    }

    // Puts the cape and elytra into the player's vanilla texture map, plus OptiFine's own cape slot when OptiFine is loaded
    // (OptiFine checks its downloaded cape before the vanilla map, so an OptiFine cape would otherwise cover this one).
    // Runs every frame because the vanilla skin download can finish later and put the Mojang cape back.
    @SuppressWarnings("unchecked")
    private void setCape(AbstractClientPlayer player, ResourceLocation capeTex, ResourceLocation elytraTex) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player.getUUID());
        Field field = getTextureLocationsField();
        if (info != null && field != null) {
            try {
                Map<MinecraftProfileTexture.Type, ResourceLocation> textureMap = (Map<MinecraftProfileTexture.Type, ResourceLocation>) field.get(info);
                textureMap.put(MinecraftProfileTexture.Type.CAPE, capeTex);
                textureMap.put(MinecraftProfileTexture.Type.ELYTRA, elytraTex);
            } catch (IllegalAccessException e) {
                // Stop trying after a failure instead of logging it every frame
                textureLocationsField = null;
                IceAndFire.LOGGER.error("Could not set Ice and Fire cape texture", e);
            }
        }
        Method setter = getOptifineCapeSetter();
        if (setter != null) {
            try {
                setter.invoke(player, capeTex);
            } catch (IllegalAccessException | InvocationTargetException e) {
                optifineCapeSetter = null;
                IceAndFire.LOGGER.error("Could not set Ice and Fire cape on OptiFine", e);
            }
        }
    }

    // Finds PlayerInfo.textureLocations once; ObfuscationReflectionHelper maps the SRG name in dev and production
    private static Field getTextureLocationsField() {
        if (!textureLocationsChecked) {
            textureLocationsChecked = true;
            try {
                textureLocationsField = ObfuscationReflectionHelper.findField(PlayerInfo.class, "f_105299_");
            } catch (ObfuscationReflectionHelper.UnableToFindFieldException e) {
                IceAndFire.LOGGER.error("Could not find PlayerInfo.textureLocations, Ice and Fire capes are disabled", e);
            }
        }
        return textureLocationsField;
    }

    // Looks up OptiFine's cape setter once; stays null without OptiFine
    private static Method getOptifineCapeSetter() {
        if (!optifineCapeSetterChecked) {
            optifineCapeSetterChecked = true;
            try {
                optifineCapeSetter = AbstractClientPlayer.class.getMethod("setLocationOfCape", ResourceLocation.class);
            } catch (NoSuchMethodException ignored) {
                // OptiFine isn't installed
            }
        }
        return optifineCapeSetter;
    }

    private boolean hasLightningCape(UUID uniqueID) {
        for (UUID uuid1 : lightningcapes) {
            if (uniqueID.equals(uuid1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRedCape(UUID uniqueID) {
        for (UUID uuid1 : redcapes) {
            if (uniqueID.equals(uuid1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBlueCape(UUID uniqueID) {
        for (UUID uuid1 : bluecapes) {
            if (uniqueID.equals(uuid1)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBetaCape(UUID uniqueID) {
        for (UUID uuid1 : betatesters) {
            if (uniqueID.equals(uuid1)) {
                return true;
            }
        }
        return false;
    }
}
