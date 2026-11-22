package com.github.alexthe666.iceandfire.pathfinding;

import javax.annotation.Nullable;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Kettle5000's dragon flight navigator: no pathfinding, it only picks the dragon's next air target
 * ({@code dragon.airTarget}) and DragonFlyingMoveControl flies straight to it.
 */
public class DragonFlyingPathNavigation extends FlyingPathNavigation {

    private final EntityDragonBase dragon;
    // what the current air target was picked for: chasing the attack target, and with which attack (melee/ranged)
    private boolean pickedForChase;
    private boolean pickedForMelee;

    public DragonFlyingPathNavigation(EntityDragonBase dragon, Level level) {
        super(dragon, level);
        this.dragon = dragon;
    }

    @Override
    public boolean moveTo(@Nullable Path p_26537_, double p_26538_) {
    	return true;
    }

	@Override
	public void tick() {
		MoveControl control = this.dragon.getMoveControl();
		boolean chasing = this.dragon.getTarget() != null && this.dragon.getTarget().isAlive();
		if (this.dragon.airTarget == null) {
			// (the move control clears it after flying into something)
			this.dragon.airTarget = chasing ? DragonUtils.getBlockWithTarget(dragon) : DragonUtils.getBlockInView(dragon);
			rememberPick(chasing);
		}
		BlockPos target = this.dragon.airTarget;
		BlockPos dragonpos = this.dragon.blockPosition();
		// As in the 1.12 version, a dragon goes for its attack target as soon as it has one, and dives at once
		// when it switches to melee. This navigator otherwise only picks a new air target when the dragon gets
		// to the old one, so a wandering dragon first finished its flight to its wander point (up at cruising
		// height, often 10+ seconds away) before it turned on the player that hit it.
		boolean staleForChase = chasing && (!pickedForChase || pickedForMelee != this.dragon.attackDecision);
		int distance = target.distManhattan(dragonpos);
		//if home then restrict the dragon to go away regardless of targeting, technically this is not needed if DragonsUtils.getBlockInView already chooses a position inside its home
		if (this.dragon.hasHomePosition && this.dragon.homePos != null) {
			if (this.dragon.homePos.getPosition().distManhattan(dragonpos) >= IafConfig.dragonWanderFromHomeDistance) {  // manhattan is very useful for those approximations
				this.dragon.airTarget = DragonUtils.getBlockInView(dragon);
				target = this.dragon.airTarget;
				rememberPick(false);
				control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier()); // i wonder if this is necessary
			} else {
				if (distance <= dragon.getRenderSize() + 2 || staleForChase) { // dragon is close to the target so we choose a new one, no idea what threshold is good enough
					this.dragon.airTarget = this.dragon.getTarget() != null ? DragonUtils.getBlockWithTarget(dragon): DragonUtils.getBlockInView(dragon);
					target = this.dragon.airTarget;
					rememberPick(chasing);
					control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
				}
			}
		} else { //if dragon doesnt have a patrol area
			if (distance <= dragon.getRenderSize() + 2 || this.dragon.horizontalCollision || BlockPos.ZERO.equals(target) || staleForChase) {
				this.dragon.airTarget = this.dragon.getTarget() != null ? DragonUtils.getBlockWithTarget(dragon): DragonUtils.getBlockInView(dragon);
				target = this.dragon.airTarget;
				rememberPick(chasing);
				control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
			}
		}
	}

	/** Notes what the air target just picked was for (see the staleForChase check in tick()). */
	private void rememberPick(boolean forChase) {
		this.pickedForChase = forChase;
		this.pickedForMelee = this.dragon.attackDecision;
	}
}
