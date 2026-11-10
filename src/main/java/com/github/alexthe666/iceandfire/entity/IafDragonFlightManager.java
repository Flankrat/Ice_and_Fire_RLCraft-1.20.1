package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.util.IFlyingMount;
import com.github.alexthe666.iceandfire.util.IAFMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * MoveControl
 * No longer using flyaround method
 * this handle the dragon moving to its air target block position
 */
public class IafDragonFlightManager {
	
	public static float approach(float number, float max, float min) {
        min = Math.abs(min);
        return number < max ? Mth.clamp(number + min, number, max) : Mth.clamp(number - min, max, number);
    }

    public static float approachDegrees(float number, float max, float min) {
        float add = Mth.wrapDegrees(max - number);
        return approach(number, number + add, min);
    }

    public static float degreesDifferenceAbs(float f1, float f2) {
        return Math.abs(Mth.wrapDegrees(f2 - f1));
    }

    protected static class GroundMoveHelper extends MoveControl {
        public GroundMoveHelper(Mob LivingEntityIn) {
            super(LivingEntityIn);
        }

        public float distance(float rotateAngleFrom, float rotateAngleTo) {
            return (float) IAFMath.atan2_accurate(Mth.sin(rotateAngleTo - rotateAngleFrom), Mth.cos(rotateAngleTo - rotateAngleFrom));
        }

        @Override
        public void tick() {
            if (this.operation == Operation.STRAFE) {
                float f = (float) this.mob.getAttribute(Attributes.MOVEMENT_SPEED).getValue();
                float f1 = (float) this.speedModifier * f;
                float f2 = this.strafeForwards;
                float f3 = this.strafeRight;
                float f4 = Mth.sqrt(f2 * f2 + f3 * f3);

                if (f4 < 1.0F) {
                    f4 = 1.0F;
                }

                f4 = f1 / f4;
                f2 = f2 * f4;
                f3 = f3 * f4;
                float f5 = Mth.sin(this.mob.getYRot() * 0.017453292F);
                float f6 = Mth.cos(this.mob.getYRot() * 0.017453292F);
                float f7 = f2 * f6 - f3 * f5;
                float f8 = f3 * f6 + f2 * f5;
                PathNavigation pathnavigate = this.mob.getNavigation();
                if (pathnavigate != null) {
                    NodeEvaluator nodeprocessor = pathnavigate.getNodeEvaluator();
                    if (nodeprocessor != null && nodeprocessor.getBlockPathType(this.mob.level(), Mth.floor(this.mob.getX() + (double) f7), Mth.floor(this.mob.getY()), Mth.floor(this.mob.getZ() + (double) f8)) != BlockPathTypes.WALKABLE) {
                        this.strafeForwards = 1.0F;
                        this.strafeRight = 0.0F;
                        f1 = f;
                    }
                }
                this.mob.setSpeed(f1);
                this.mob.setZza(this.strafeForwards);
                this.mob.setXxa(this.strafeRight);
                this.operation = Operation.WAIT;
            } else if (this.operation == Operation.MOVE_TO) {
                this.operation = Operation.WAIT;
                EntityDragonBase dragonBase = (EntityDragonBase) mob;
                double d0 = this.getWantedX() - this.mob.getX();
                double d1 = this.getWantedZ() - this.mob.getZ();
                double d2 = this.getWantedY() - this.mob.getY();
                double d3 = d0 * d0 + d2 * d2 + d1 * d1;

                if (d3 < 2.500000277905201E-7D) {
                    this.mob.setZza(0.0F);
                    return;
                }
                float targetDegree = (float) (Mth.atan2(d1, d0) * (180D / Math.PI)) - 90.0F;
                float changeRange = 70F;
                if (Math.ceil(dragonBase.getBbWidth()) > 2F) {
                    float ageMod = 1F - Math.min(dragonBase.getAgeInDays(), 125) / 125F;
                    changeRange = 5 + ageMod * 10;
                }
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetDegree, changeRange));
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (d2 > (double) this.mob.maxUpStep() && d0 * d0 + d1 * d1 < (double) Math.max(1.0F, this.mob.getBbWidth() / 2)) {
                    this.mob.getJumpControl().jump();
                    this.operation = Operation.JUMPING;
                }
            } else if (this.operation == Operation.JUMPING) {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));

                if (this.mob.onGround()) {
                    this.operation = Operation.WAIT;
                }
            } else {
                this.mob.setZza(0.0F);
            }
        }

    }
    
    //Adding this back from the original mod. its needed to control movement when flying.
    
    protected static class DragonFlyingMoveControl extends MoveControl {

        private final EntityDragonBase dragon;

        public DragonFlyingMoveControl(EntityDragonBase dragonBase) {
            super(dragonBase);
            this.dragon = dragonBase;
        }

        
        /** How fast an AI dragon turns while flying (degrees per tick), as in the original mod. */
        private static final float TURN_SPEED = 4.0F;

        @Override
        public void tick() {
            if (dragon.horizontalCollision) {
                this.speedModifier = 0.1D;
                dragon.airTarget = null;
                return;
            }
            if (dragon.airTarget == null) {
                return;
            }
            final Vec3 target = Vec3.atCenterOf(dragon.airTarget);
            double distX = target.x - dragon.getX();
            final double distY = target.y - dragon.getY();
            double distZ = target.z - dragon.getZ();

            // Original mod: a steep climb or dive shortens the horizontal part of the move.
            // Clamped so a target almost straight above/below can't flip the heading around.
            double planeDist = Math.sqrt(distX * distX + distZ * distZ);
            if (planeDist > 1.0E-3D) {
                double yDistMod = Mth.clamp(1.0D - Math.abs(distY * 0.7D) / planeDist, 0.1D, 1.0D);
                distX *= yDistMod;
                distZ *= yDistMod;
                planeDist *= yDistMod;
            }
            final double dist = Math.sqrt(distX * distX + distZ * distZ + distY * distY);
            if (dist <= 1.0D) {
                return;
            }

            // Turn toward the target at a fixed rate. Speed builds up while the dragon is lined up
            // and drops while it is still turning, so it flies in arcs instead of sliding sideways.
            final float oldYaw = dragon.getYRot();
            if (planeDist > 1.0E-3D) {
                float targetYaw = (float) (Mth.atan2(distZ, distX) * Mth.RAD_TO_DEG) - 90.0F;
                dragon.setYRot(IafDragonFlightManager.approachDegrees(oldYaw, targetYaw, TURN_SPEED));
            }
            dragon.yBodyRot = dragon.getYRot();
            if (IafDragonFlightManager.degreesDifferenceAbs(oldYaw, dragon.getYRot()) < 3.0F) {
                speedModifier = IafDragonFlightManager.approach((float) speedModifier, 1.8F, 0.005F * (1.8F / (float) Math.max(speedModifier, 0.1D)));
            } else {
                speedModifier = IafDragonFlightManager.approach((float) speedModifier, 0.2F, 0.025F);
            }

            final float pitch = (float) (-(Mth.atan2(-distY, planeDist) * Mth.RAD_TO_DEG));
            double speed = speedModifier * dragon.getFlightSpeedModifier();
            speed *= Math.min(1.0D, dist / 50.0D + 0.3D); // slow down close to the target

            // Thrust goes along the way the dragon is FACING, not straight at the target.
            final float headingRad = (dragon.getYRot() + 90.0F) * Mth.DEG_TO_RAD;
            final double x = speed * Mth.cos(headingRad) * Math.abs(distX / dist);
            final double y = speed * Mth.sin(pitch * Mth.DEG_TO_RAD) * Math.abs(distY / dist);
            final double z = speed * Mth.sin(headingRad) * Math.abs(distZ / dist);
            final double motionCap = 0.2D;
            Vec3 delta = dragon.getDeltaMovement().add(
                    Mth.clamp(x * 0.2D, -motionCap, motionCap),
                    Mth.clamp(y * 0.2D, -motionCap, motionCap),
                    Mth.clamp(z * 0.2D, -motionCap, motionCap));

            // Keep the port's tuned top speed per growth stage.
            final double maxSpeed = 0.15D + (dragon.getDragonStage() * 0.05D);
            final double length = delta.length();
            if (length > maxSpeed) {
                delta = delta.scale(maxSpeed / length);
            }
            dragon.setDeltaMovement(delta);
        }
    }

    protected static class PlayerFlightMoveHelper<T extends Mob & IFlyingMount> extends MoveControl {

        private final T dragon;

        public PlayerFlightMoveHelper(T dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (dragon instanceof EntityDragonBase theDragon && theDragon.getControllingPassenger() != null) {
                // New ride system doesn't need move controller
                // The flight move control is disabled here, the walking move controller will stay Operation.WAIT so nothing will happen too
                return;
            }

            double flySpeed = speedModifier * speedMod() * 3;
            Vec3 dragonVec = dragon.position();
            Vec3 moveVec = new Vec3(wantedX, wantedY, wantedZ);
            Vec3 normalized = moveVec.subtract(dragonVec).normalize();
            double dist = dragonVec.distanceTo(moveVec);
            dragon.setDeltaMovement(normalized.x * flySpeed, normalized.y * flySpeed, normalized.z * flySpeed);
            if (dist > 2.5E-7) {
                float yaw = (float) Math.toDegrees(Math.PI * 2 - Math.atan2(normalized.x, normalized.y));
                dragon.setYRot(rotlerp(dragon.getYRot(), yaw, 5));
                dragon.setSpeed((float) (speedModifier));
            }
            dragon.move(MoverType.SELF, dragon.getDeltaMovement());
        }

        public double speedMod() {
            return (dragon instanceof EntityAmphithere ? 0.6D : 1.25D) * IafConfig.dragonFlightSpeedMod * dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
        }
    }
}
