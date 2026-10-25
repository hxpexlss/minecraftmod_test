package de.chaosblock;

import de.chaosblock.event.BlockEvents;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lost alle paar Minuten heimlich den Chaos-Block aus und löst beim Abbauen Events aus.
 */
public class ChaosBlockManager {
	/** Blöcke, die man im normalen Spiel oft abbaut. */
	public static final List<Block> CANDIDATES = List.of(
		Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.SPRUCE_LOG, Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG,
		Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG,
		Blocks.STONE, Blocks.COBBLESTONE, Blocks.DEEPSLATE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.TUFF,
		Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.GRAVEL, Blocks.SANDSTONE, Blocks.CLAY,
		Blocks.COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.COPPER_ORE,
		Blocks.OAK_LEAVES, Blocks.BIRCH_LEAVES, Blocks.SPRUCE_LEAVES,
		Blocks.SHORT_GRASS, Blocks.NETHERRACK, Blocks.SNOW_BLOCK
	);

	private final MinecraftServer server;
	private final ChaosData data;
	private final ModConfig config;
	private final RandomSource random = RandomSource.create();
	private final Map<UUID, Integer> lastEventTick = new HashMap<>();
	private final Map<UUID, Integer> doubleDropsUntil = new HashMap<>();
	private Block current = Blocks.STONE;

	public ChaosBlockManager(MinecraftServer server, ChaosData data, ModConfig config) {
		this.server = server;
		this.data = data;
		this.config = config;
	}

	public void init() {
		Identifier id = Identifier.tryParse(this.data.currentBlock);
		Block saved = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		if (saved == null || !CANDIDATES.contains(saved)) {
			this.rotate();
		} else {
			this.current = saved;
			if (this.data.ticksUntilRotation <= 0) {
				this.data.ticksUntilRotation = this.rotationTicks();
			}
		}
	}

	private int rotationTicks() {
		return Math.max(1, this.config.rotationMinutes) * 60 * 20;
	}

	public void tick() {
		if (--this.data.ticksUntilRotation <= 0) {
			this.rotate();
		}
	}

	public void rotate() {
		Block next;
		do {
			next = CANDIDATES.get(this.random.nextInt(CANDIDATES.size()));
		} while (next == this.current && CANDIDATES.size() > 1);
		this.current = next;
		this.data.currentBlock = BuiltInRegistries.BLOCK.getKey(next).toString();
		this.data.ticksUntilRotation = this.rotationTicks();
		ChaosBlockMod.LOGGER.debug("Neuer Chaos-Block: {}", this.data.currentBlock);
	}

	public Block current() {
		return this.current;
	}

	public String currentBlockId() {
		return this.data.currentBlock;
	}

	public void setCurrent(Block block) {
		this.current = block;
		this.data.currentBlock = BuiltInRegistries.BLOCK.getKey(block).toString();
		this.data.ticksUntilRotation = this.rotationTicks();
	}

	public void grantDoubleDrops(ServerPlayer player, int ticks) {
		this.doubleDropsUntil.put(player.getUUID(), this.server.getTickCount() + ticks);
	}

	public void onBlockBroken(ServerPlayer player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		int now = this.server.getTickCount();

		Integer doubleUntil = this.doubleDropsUntil.get(player.getUUID());
		if (doubleUntil != null && doubleUntil > now && !player.isCreative()) {
			Block.dropResources(state, player.level(), pos, blockEntity, player, player.getMainHandItem());
		}

		if (!state.is(this.current)) {
			return;
		}

		Integer last = this.lastEventTick.get(player.getUUID());
		if (last != null && now - last < this.config.eventCooldownSeconds * 20) {
			return;
		}
		this.lastEventTick.put(player.getUUID(), now);
		BlockEvents.trigger(player, pos);
	}
}
