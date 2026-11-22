package com.github.alexthe666.iceandfire.event;

import com.github.alexthe666.iceandfire.compat.FirstAidCompat;
import com.github.alexthe666.iceandfire.item.blooded.BloodedDefendEffect;
import com.github.alexthe666.iceandfire.item.blooded.BloodedSetEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "iceandfire", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodedArmorEventHandler {

    @SubscribeEvent
    public static void onPlayerAttack(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide)
            return;

        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof Player player))
            return;

        // Attack mode: the wearer's hits carry the set's element
        if (!BloodedDefendEffect.isDefendMode()) {
            BloodedSetEffect.tryApplySetEffect(player, target, event.getAmount());
        }
    }

    // Defend mode without First Aid: the armor strikes back at a mob that hits the wearer in melee
    @SubscribeEvent
    public static void onWearerHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || FirstAidCompat.isLoaded() || !BloodedDefendEffect.isDefendMode())
            return;
        if (event.getEntity() instanceof Player wearer && BloodedDefendEffect.isMeleeHit(event.getSource())) {
            BloodedDefendEffect.onWearerHit(wearer, (LivingEntity) event.getSource().getEntity());
        }
    }
}
