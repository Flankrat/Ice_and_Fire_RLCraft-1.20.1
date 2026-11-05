package com.github.alexthe666.iceandfire.entity.ai;

import java.util.EnumSet;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.pathfinding.DragonFlyingPathNavigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

public class DragonAIHurtByTarget extends TargetGoal
{
    private final EntityDragonBase dragon;
    private int timestamp;
    
    public DragonAIHurtByTarget(EntityDragonBase dragon){
        super(dragon, true, false);
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

	@Override
	public void start() {
		LivingEntity attacker = this.mob.getLastHurtByMob();

		if (attacker == null || attacker == this.dragon) {
			return;
		}

		double dx = attacker.getX() - dragon.getX();
		double dz = attacker.getZ() - dragon.getZ();

		double horizontalDistanceSqr = dx * dx + dz * dz;

		// Within 64 blocks -> normal targeting
		double reach = this.mob.getAttributeValue(Attributes.FOLLOW_RANGE);
		if (horizontalDistanceSqr <= reach * reach) {
			this.dragon.setTarget(attacker);
			this.targetMob = this.dragon.getTarget();
			this.timestamp = this.dragon.getLastHurtByMobTimestamp();
			this.unseenMemoryTicks = 300;
			super.start();
			return;
		}

		// Far away -> investigate chance
		if (dragon.getRandom().nextFloat() <= (dragon.getHealth() > 100 ? 0.4F : 0.5F)) {
			RandomSource random = dragon.getRandom();

			double distance = random.nextDouble() * 20D;
			double angle = random.nextDouble() * Math.PI * 2D;

			double offsetX = Math.cos(angle) * distance;
			double offsetZ = Math.sin(angle) * distance;

			double targetX = attacker.getX() + offsetX;
			double targetZ = attacker.getZ() + offsetZ;
			MoveControl control = dragon.getMoveControl();
			if (this.dragon.getNavigation() instanceof DragonFlyingPathNavigation) {
				this.dragon.airTarget = BlockPos.containing(targetX, this.dragon.getY(), targetZ);
			} else {
				this.dragon.getNavigation().moveTo(targetX, attacker.getY(), targetZ, 1);
			}
			control.setWantedPosition(targetX, control.getWantedY(), targetZ, control.getSpeedModifier());
		}

		this.timestamp = this.mob.getLastHurtByMobTimestamp();
	}

	@Override
	public boolean canUse() {
		int i = this.mob.getLastHurtByMobTimestamp();
		LivingEntity livingentity = this.mob.getLastHurtByMob();
		if (i != this.timestamp && livingentity != null && livingentity != this.dragon) {
			return livingentity != this.dragon.getOwner();
		} else {
			return false;
		}
	}
}