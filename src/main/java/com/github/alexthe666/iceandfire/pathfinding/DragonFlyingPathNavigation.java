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
import net.minecraft.world.phys.Vec3;

/**
 * Kettle5000's dragon flight navigator: no pathfinding, it only picks the dragon's next air target
 * ({@code dragon.airTarget}) and DragonFlyingMoveControl flies straight to it.
 */
public class DragonFlyingPathNavigation extends FlyingPathNavigation {

    private final EntityDragonBase dragon;
    // what the current air target was picked for: chasing the attack target, and whether to dive at it
    private boolean pickedForChase;
    private boolean pickedForDive;
    // whether a chasing dragon should dive now (DragonUtils.shouldDive: melee pick and a clear line to the
    // target), checked every DIVE_CHECK_TICKS ticks so a line that flickers past a tree doesn't make it bob
    private boolean dive;
    private boolean wasMelee;
    // where the attack target was when the current chase point was picked
    private Vec3 pickedTargetPos = Vec3.ZERO;
    // a chase point is re-picked once the target has moved this far from where it was when it was picked
    private static final double TARGET_MOVED = 4.0D;
    private int diveCheckedAt;
    private static final int DIVE_CHECK_TICKS = 5;

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
		boolean melee = chasing && this.dragon.attackDecision;
		if (!melee) {
			this.dive = false;
		} else if (!this.wasMelee || this.dragon.tickCount - this.diveCheckedAt >= DIVE_CHECK_TICKS) {
			// checked at once when it switches to melee, then every few ticks
			this.dive = DragonUtils.shouldDive(this.dragon);
			this.diveCheckedAt = this.dragon.tickCount;
		}
		this.wasMelee = melee;
		if (this.dragon.airTarget == null) {
			// (the move control clears it after flying into something)
			this.dragon.airTarget = chasing ? DragonUtils.getBlockWithTarget(dragon, this.dive) : DragonUtils.getBlockInView(dragon);
			rememberPick(chasing);
		}
		BlockPos target = this.dragon.airTarget;
		BlockPos dragonpos = this.dragon.blockPosition();
		// As in the 1.12 version, a dragon goes for its attack target as soon as it has one, dives at once when it
		// switches to melee with a clear line, and pulls up again when the line is lost. This navigator otherwise
		// only picks a new air target when the dragon gets to the old one, so a wandering dragon first finished its
		// flight to its wander point (up at cruising height, often 10+ seconds away) before it turned on the
		// player that hit it.
		// A chase point is also stale once the target has moved on from it (a running player): the dragon only
		// picks a new point when it gets close to the old one, so it kept flying to where the player had been and
		// trailed behind them at well under its top speed.
		boolean staleForChase = chasing && (!pickedForChase || pickedForDive != this.dive
				|| this.dragon.getTarget().position().distanceToSqr(this.pickedTargetPos) > TARGET_MOVED * TARGET_MOVED);
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
					this.dragon.airTarget = chasing ? DragonUtils.getBlockWithTarget(dragon, this.dive) : DragonUtils.getBlockInView(dragon);
					target = this.dragon.airTarget;
					rememberPick(chasing);
					control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
				}
			}
		} else { //if dragon doesnt have a patrol area
			if (distance <= dragon.getRenderSize() + 2 || this.dragon.horizontalCollision || BlockPos.ZERO.equals(target) || staleForChase) {
				this.dragon.airTarget = chasing ? DragonUtils.getBlockWithTarget(dragon, this.dive) : DragonUtils.getBlockInView(dragon);
				target = this.dragon.airTarget;
				rememberPick(chasing);
				control.setWantedPosition(target.getX(), target.getY(), target.getZ(), control.getSpeedModifier());
			}
		}
	}

	/** Notes what the air target just picked was for (see the staleForChase check in tick()). */
	private void rememberPick(boolean forChase) {
		this.pickedForChase = forChase;
		this.pickedForDive = forChase && this.dive;
		if (forChase) {
			this.pickedTargetPos = this.dragon.getTarget().position();
		}
	}
}
