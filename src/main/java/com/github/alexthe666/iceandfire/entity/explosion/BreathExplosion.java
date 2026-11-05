package com.github.alexthe666.iceandfire.entity.explosion;

import java.util.List;
import java.util.Set;

import com.github.alexthe666.iceandfire.compat.ModCompatList;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import team.creative.enhancedvisuals.EnhancedVisuals;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;

public class BreathExplosion extends Explosion {
	protected final EntityDragonBase dragon;
	protected final Level level;
	private final float size;
	public final ObjectArrayList<BlockPos> blocks = new ObjectArrayList<>();
	public final ObjectArrayList<Entity> entities = new ObjectArrayList<>();

	public BreathExplosion(Level world, EntityDragonBase dragon, double x, double y, double z, float size) {
		super(world, dragon, null, null, x, y, z, size, true, BlockInteraction.KEEP);
		this.dragon = dragon;
		this.level = world;
		this.size = size;
	}

	// this method often define block positions and damages entities, this time we
	// will only return a list of both and let the damage be done in
	// IafDragonDestructionManager class
	@Override
	public void explode() {
		BlockPos center = BlockPos.containing(this.getPosition());
		level.gameEvent(dragon, GameEvent.EXPLODE, center);
		if (this.dragon instanceof EntityFireDragon) {
			level.playSound(null, center, IafSoundRegistry.FIREDRAGON_BREATH, SoundSource.BLOCKS, 1.0F + this.size * 0.2f, 1.0F);
		} else if (this.dragon instanceof EntityIceDragon) {
			level.playSound(null, center, IafSoundRegistry.ICEDRAGON_BREATH, SoundSource.BLOCKS, 1.0F + this.size * 0.2f, 1.0F);
		} else {
			level.playSound(null, center, IafSoundRegistry.LIGHTNINGDRAGON_BREATH_CRACKLE, SoundSource.BLOCKS, 1.0F + this.size * 0.2f, 1.0F);
		}
		Set<BlockPos> set = Sets.newHashSet();
		final int radius = (int)this.size;
		float hard = 0;
		for (int x = 0; x <= radius * 2; x++) {
			for (int y = 0; y <= radius * 2; y++) {
				for (int z = 0; z <= radius * 2; z++) {
					int dx = Math.abs(x - radius);
					int dy = Math.abs(y - radius);
					int dz = Math.abs(z - radius);
					int d = dx * dx + dy * dy + dz * dz;
					int radius2 = radius * radius;
					if (d <= radius2) {
						BlockPos newpos = center.offset(x - radius, y - radius, z - radius);
						BlockState state = level.getBlockState(newpos);
						if (state.isAir())
							continue; // skip air
						hard = state.getDestroySpeed(level, newpos);
						if (hard <= 0 || hard > this.size)
							continue;
						if (d <= 1) {
							set.add(newpos);
						} else if (level.random.nextFloat() * radius2 > (float) d) {
							set.add(newpos);
						}
					}
				}
			}
		}

		this.blocks.addAll(set);
		this.entities.addAll(level.getEntities(this.dragon,
				new AABB((double) center.getX() - this.size, (double) center.getY() - this.size,
						(double) center.getZ() - this.size, (double) center.getX() + this.size,
						(double) center.getY() + this.size, (double) center.getZ() + this.size)));
		ForgeEventFactory.onExplosionDetonate(this.level, this, this.entities, this.size*2f);
		if (ModCompatList.isEVon) {
			EnhancedVisuals.EVENTS.explosion(this, (List<Entity>) this.entities);
		}
	}
	
	@Override
	public void finalizeExplosion(boolean p_46076_) {
		return;
	}
	
}
