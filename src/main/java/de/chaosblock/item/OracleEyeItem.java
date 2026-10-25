package de.chaosblock.item;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.ambient.AmbientManager;
import de.chaosblock.network.ModPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Zeigt bei Rechtsklick den aktuell geheimen Chaos-Block an.
 */
public class OracleEyeItem extends Item {
	private final boolean eternal;

	public OracleEyeItem(Properties properties, boolean eternal) {
		super(properties);
		this.eternal = eternal;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			String blockId = ChaosBlockMod.manager().currentBlockId();
			boolean watched = AmbientManager.oracleIsWatched(serverPlayer);
			ModPayloads.Oracle payload = watched
				? new ModPayloads.Oracle("", "ER SIEHT DICH", 60)
				: new ModPayloads.Oracle(blockId, "", 100);
			ServerPlayNetworking.send(serverPlayer, payload);

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				watched ? SoundEvents.ELDER_GUARDIAN_CURSE : SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, watched ? 0.6F : 1.3F);

			if (this.eternal) {
				player.getCooldowns().addCooldown(stack, 20 * 20);
			} else {
				stack.hurtAndBreak(1, player, hand);
				player.getCooldowns().addCooldown(stack, 20);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return this.eternal;
	}
}
