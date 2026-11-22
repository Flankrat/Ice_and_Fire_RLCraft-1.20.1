package com.github.alexthe666.iceandfire.compat;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.item.blooded.BloodedDefendEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * First Aid support for blooded armor's defend mode, without a compile dependency: First Aid's damage event says
 * which body parts lost health, so the armor piece on that part strikes back (as in the 1.12 version).
 * Hooked through reflection only when First Aid is loaded.
 */
public final class FirstAidCompat {
    private static boolean loaded = false;

    private FirstAidCompat() {
    }

    public static boolean isLoaded() {
        return loaded;
    }

    @SuppressWarnings("unchecked")
    public static void init() {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent");
            Method before = eventClass.getMethod("getBeforeDamage");
            Method after = eventClass.getMethod("getAfterDamage");
            Method source = eventClass.getMethod("getSource");
            Method undistributed = eventClass.getMethod("getUndistributedDamage");
            Method player = eventClass.getMethod("getEntity");
            Class<?> model = Class.forName("ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel");
            Field health = Class.forName("ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart").getField("currentHealth");
            // Body parts, grouped by the armor slot that covers them
            String[][] parts = {{"HEAD"}, {"BODY", "LEFT_ARM", "RIGHT_ARM"}, {"LEFT_LEG", "RIGHT_LEG"}, {"LEFT_FOOT", "RIGHT_FOOT"}};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            Field[][] partFields = new Field[parts.length][];
            for (int i = 0; i < parts.length; i++) {
                partFields[i] = new Field[parts[i].length];
                for (int j = 0; j < parts[i].length; j++) {
                    partFields[i][j] = model.getField(parts[i][j]);
                }
            }
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, eventClass, event -> {
                try {
                    if (!BloodedDefendEffect.isDefendMode()) {
                        return;
                    }
                    DamageSource damageSource = (DamageSource) source.invoke(event);
                    Player wearer = (Player) player.invoke(event);
                    if (wearer.level().isClientSide || !BloodedDefendEffect.isMeleeHit(damageSource)) {
                        return;
                    }
                    LivingEntity attacker = (LivingEntity) damageSource.getEntity();
                    Object beforeModel = before.invoke(event);
                    Object afterModel = after.invoke(event);
                    for (int i = 0; i < partFields.length; i++) {
                        for (Field part : partFields[i]) {
                            if (health.getFloat(part.get(afterModel)) < health.getFloat(part.get(beforeModel))) {
                                BloodedDefendEffect.onPieceHit(wearer, attacker, slots[i]);
                                return;
                            }
                        }
                    }
                    // Damage that didn't land on a part: a random piece, like without First Aid
                    if ((Float) undistributed.invoke(event) > 0) {
                        BloodedDefendEffect.onWearerHit(wearer, attacker);
                    }
                } catch (ReflectiveOperationException | ClassCastException e) {
                    IceAndFire.LOGGER.debug("Ice and Fire couldn't read a First Aid damage event", e);
                }
            });
            loaded = true;
        } catch (ReflectiveOperationException e) {
            IceAndFire.LOGGER.warn("First Aid is loaded but its damage event wasn't found; blooded armor will use a random piece", e);
        }
    }
}
