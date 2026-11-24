package com.github.alexthe666.iceandfire.event;

import com.github.alexthe666.iceandfire.compat.FirstAidCompat;
import com.github.alexthe666.iceandfire.item.blooded.BloodedDefendEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "iceandfire", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BloodedArmorEventHandler {

    // Without First Aid: the armor strikes back at a mob that hits the wearer in melee (FirstAidCompat does it with First Aid)
    @SubscribeEvent
    public static void onWearerHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || FirstAidCompat.isLoaded())
            return;
        if (event.getEntity() instanceof Player wearer && BloodedDefendEffect.isMeleeHit(event.getSource())) {
            BloodedDefendEffect.onWearerHit(wearer, (LivingEntity) event.getSource().getEntity());
        }
    }
}
