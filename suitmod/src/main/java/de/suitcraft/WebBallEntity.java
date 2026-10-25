package de.suitcraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Geschoss des Netzwerfers. Hinterlässt kurzzeitig Spinnweben. */
public class WebBallEntity extends ThrowableItemProjectile {
	private static final int WEB_SECONDS = 8;

	public WebBallEntity(EntityType<? extends WebBallEntity> type, Level level) {
		super(type, level);
	}

	public WebBallEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(SuitEntities.WEB_BALL, owner, level, new ItemStack(Items.COBWEB));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.COBWEB;
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (this.level() instanceof ServerLevel level && hit.getEntity() instanceof LivingEntity target) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 5, 3));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 0));
			placeWeb(level, target.blockPosition());
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (this.level() instanceof ServerLevel level) {
			placeWeb(level, hit.getBlockPos().relative(hit.getDirection()));
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (!this.level().isClientSide()) {
			this.discard();
		}
	}

	/** Setzt ein Spinnennetz, das nach ein paar Sekunden wieder verschwindet. */
	private static void placeWeb(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
			return;
		}
		BlockPos at = pos.immutable();
		level.setBlockAndUpdate(at, Blocks.COBWEB.defaultBlockState());
		Tasks.later(20 * WEB_SECONDS, () -> {
			if (level.getBlockState(at).is(Blocks.COBWEB)) {
				level.removeBlock(at, false);
			}
		});
	}
}
