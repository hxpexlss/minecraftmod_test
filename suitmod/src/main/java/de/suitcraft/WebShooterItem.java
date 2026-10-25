package de.suitcraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Schießt Netzkugeln, die Gegner in Spinnweben einwickeln. */
public class WebShooterItem extends Item {
	public WebShooterItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.8F, 1.6F);
		if (level instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileFromRotation(WebBallEntity::new, serverLevel, stack, player, 0.0F, 2.2F, 0.5F);
			stack.hurtAndBreak(1, player, hand);
		}
		player.getCooldowns().addCooldown(stack, 8);
		return InteractionResult.SUCCESS;
	}
}
