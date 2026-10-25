package de.chaosblock.event;

import de.chaosblock.ChaosBlockMod;
import de.chaosblock.Fx;
import de.chaosblock.Places;
import de.chaosblock.Scheduler;
import de.chaosblock.curse.CurseManager;
import de.chaosblock.mixin.CreeperAccessor;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Alle Events, die beim Abbauen des Chaos-Blocks passieren können.
 */
public final class BlockEvents {
	public enum Category {
		GOOD, BAD, WEIRD, CURSE
	}

	public record ChaosEvent(String id, Category category, String title, String subtitle, BiConsumer<ServerPlayer, BlockPos> action) {
	}

	private static final List<ChaosEvent> ALL = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();
	/** Vom Fuchs-Dieb gestohlene Items, nach Fuchs-UUID. */
	private static final Map<UUID, List<ItemStack>> STOLEN = new HashMap<>();

	private BlockEvents() {
	}

	private static void add(String id, Category category, String title, String subtitle, BiConsumer<ServerPlayer, BlockPos> action) {
		ALL.add(new ChaosEvent(id, category, title, subtitle, action));
	}

	public static List<String> ids() {
		return ALL.stream().map(ChaosEvent::id).toList();
	}

	public static void trigger(ServerPlayer player, BlockPos pos) {
		Category category = WorldEventManager.isLuckyStreak() ? Category.GOOD : rollCategory();
		if (category == Category.CURSE) {
			if (CurseManager.applyRandom(player)) {
				return;
			}
			category = Category.BAD;
		}
		final Category chosen = category;
		List<ChaosEvent> pool = ALL.stream().filter(e -> e.category() == chosen).toList();
		run(pool.get(RANDOM.nextInt(pool.size())), player, pos);
	}

	public static boolean run(String id, ServerPlayer player, BlockPos pos) {
		for (ChaosEvent event : ALL) {
			if (event.id().equals(id)) {
				run(event, player, pos);
				return true;
			}
		}
		return false;
	}

	private static void run(ChaosEvent event, ServerPlayer player, BlockPos pos) {
		Fx.title(player, event.title(), event.subtitle());
		ChaosBlockMod.LOGGER.debug("Chaos-Event {} für {}", event.id(), player.getName().getString());
		try {
			event.action().accept(player, pos);
		} catch (Exception e) {
			ChaosBlockMod.LOGGER.error("Chaos-Event {} ist fehlgeschlagen", event.id(), e);
		}
	}

	private static Category rollCategory() {
		var config = ChaosBlockMod.config();
		int good = Math.max(0, config.chanceGood);
		int bad = Math.max(0, config.chanceBad);
		int weird = Math.max(0, config.chanceWeird);
		int curse = Math.max(0, config.chanceCurse);
		int total = good + bad + weird + curse;
		if (total <= 0) {
			return Category.WEIRD;
		}
		int roll = RANDOM.nextInt(total);
		if (roll < good) {
			return Category.GOOD;
		}
		if (roll < good + bad) {
			return Category.BAD;
		}
		if (roll < good + bad + weird) {
			return Category.WEIRD;
		}
		return Category.CURSE;
	}

	/** Wird aufgerufen, wenn irgendein Lebewesen stirbt (für den Fuchs-Dieb). */
	public static void onDeath(LivingEntity entity) {
		List<ItemStack> loot = STOLEN.remove(entity.getUUID());
		if (loot != null && entity.level() instanceof ServerLevel level) {
			for (ItemStack stack : loot) {
				Places.dropItem(level, entity.position(), stack);
			}
		}
	}

	// ------------------------------------------------------------------
	// Hilfen

	private static void effect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int seconds, int amplifier) {
		player.addEffect(new MobEffectInstance(effect, seconds * 20, amplifier));
	}

	private static void particles(ServerLevel level, net.minecraft.core.particles.SimpleParticleType type, Vec3 pos, int count, double spread) {
		level.sendParticles(type, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.05);
	}

	private static void launch(ServerPlayer player, Vec3 motion) {
		player.setDeltaMovement(motion);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	/** Ändert ein Attribut für eine Weile und setzt es danach zurück. */
	private static void tempModifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
		String name, double amount, AttributeModifier.Operation operation, int ticks) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		var id = ChaosBlockMod.id("event_" + name);
		instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		Scheduler.later(ticks, () -> {
			AttributeInstance current = player.getAttribute(attribute);
			if (current != null) {
				current.removeModifier(id);
			}
		});
	}

	private static boolean replaceable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getDestroySpeed(level, pos) >= 0 && level.getBlockEntity(pos) == null;
	}

	private static ItemStack firework(RandomSource random) {
		ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
		int[] palette = {0xFF0000, 0xFF8800, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0088FF, 0xAA00FF, 0xFF00AA};
		FireworkExplosion explosion = new FireworkExplosion(
			FireworkExplosion.Shape.values()[random.nextInt(FireworkExplosion.Shape.values().length)],
			IntList.of(palette[random.nextInt(palette.length)], palette[random.nextInt(palette.length)]),
			IntList.of(palette[random.nextInt(palette.length)]),
			random.nextBoolean(),
			random.nextBoolean()
		);
		stack.set(DataComponents.FIREWORKS, new Fireworks(1 + random.nextInt(2), List.of(explosion)));
		return stack;
	}

	// ------------------------------------------------------------------
	// Die Events

	static {
		// ===== GUT =====
		add("diamantregen", Category.GOOD, "💎 DIAMANTEN-REGEN!", "Schnell aufsammeln!", (p, pos) -> {
			Scheduler.repeat(5, 8, () -> {
				if (!Fx.online(p)) {
					return;
				}
				Vec3 at = p.position().add(RANDOM.nextDouble() * 4 - 2, 6, RANDOM.nextDouble() * 4 - 2);
				Places.dropItem(p.level(), at, new ItemStack(Items.DIAMOND));
				Fx.sound(p.level(), at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
			});
		});

		add("werkzeug", Category.GOOD, "⚒ Geschenk der Götter", "Ein verzaubertes Werkzeug!", (p, pos) -> {
			ItemStack[] options = {
				new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SWORD),
				new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.DIAMOND_SHOVEL), new ItemStack(Items.BOW)
			};
			ItemStack stack = options[RANDOM.nextInt(options.length)];
			stack = EnchantmentHelper.enchantItem(RANDOM, stack, 30, p.level().registryAccess(), Optional.empty());
			Places.give(p, stack);
			Fx.sound(p.level(), p.position(), SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
		});

		add("fliegen", Category.GOOD, "🕊 Flügel!", "30 Sekunden Fliegen", (p, pos) -> {
			p.getAbilities().mayfly = true;
			p.onUpdateAbilities();
			for (int s = 5; s >= 1; s--) {
				final int left = s;
				Scheduler.later((30 - s) * 20, () -> Fx.actionBar(p, "§eFlug endet in " + left + "…"));
			}
			Scheduler.later(30 * 20, () -> {
				if (!Fx.online(p) || p.isCreative() || p.isSpectator()) {
					return;
				}
				p.getAbilities().mayfly = false;
				p.getAbilities().flying = false;
				p.onUpdateAbilities();
				effect(p, MobEffects.SLOW_FALLING, 10, 0);
			});
		});

		add("wolf", Category.GOOD, "🐺 Ein treuer Freund", "Darf ich vorstellen: Glückspilz", (p, pos) -> {
			var wolf = Places.spawn(EntityTypes.WOLF, p.level(), Places.center(pos));
			if (wolf != null) {
				wolf.tame(p);
				wolf.setCustomName(Component.literal("Glückspilz"));
				particles(p.level(), ParticleTypes.HEART, wolf.position().add(0, 1, 0), 8, 0.4);
			}
		});

		add("heilung", Category.GOOD, "❤ Volle Kraft", "Geheilt und goldene Herzen", (p, pos) -> {
			p.setHealth(p.getMaxHealth());
			p.getFoodData().setFoodLevel(20);
			p.getFoodData().setSaturation(10.0F);
			effect(p, MobEffects.ABSORPTION, 120, 3);
			particles(p.level(), ParticleTypes.HEART, p.position().add(0, 1, 0), 12, 0.6);
		});

		add("trankmix", Category.GOOD, "🧪 Trank-Cocktail", "Schnell, stark und sprunggewaltig", (p, pos) -> {
			effect(p, MobEffects.SPEED, 120, 1);
			effect(p, MobEffects.STRENGTH, 120, 1);
			effect(p, MobEffects.JUMP_BOOST, 120, 1);
			effect(p, MobEffects.NIGHT_VISION, 120, 0);
		});

		add("schatz", Category.GOOD, "📦 Schatztruhe!", "Da ist was für dich", (p, pos) -> {
			ServerLevel level = p.level();
			level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
			if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
				ItemStack[] loot = {
					new ItemStack(Items.DIAMOND, 2 + RANDOM.nextInt(4)), new ItemStack(Items.GOLD_INGOT, 4 + RANDOM.nextInt(8)),
					new ItemStack(Items.IRON_INGOT, 8 + RANDOM.nextInt(12)), new ItemStack(Items.GOLDEN_APPLE, 1 + RANDOM.nextInt(2)),
					new ItemStack(Items.ENDER_PEARL, 2 + RANDOM.nextInt(4)), new ItemStack(Items.EXPERIENCE_BOTTLE, 8 + RANDOM.nextInt(8)),
					new ItemStack(Items.EMERALD, 3 + RANDOM.nextInt(6)), new ItemStack(Items.NAME_TAG)
				};
				for (ItemStack stack : loot) {
					if (RANDOM.nextFloat() < 0.7F) {
						chest.setItem(RANDOM.nextInt(chest.getContainerSize()), stack);
					}
				}
				chest.setItem(13, new ItemStack(Items.DIAMOND, 1));
			}
			particles(level, ParticleTypes.HAPPY_VILLAGER, Places.center(pos).add(0, 0.5, 0), 20, 0.5);
		});

		add("xp", Category.GOOD, "✨ XP-Explosion", "Level up!", (p, pos) -> {
			ServerLevel level = p.level();
			for (int i = 0; i < 12; i++) {
				ExperienceOrb.award(level, Places.center(pos).add(RANDOM.nextDouble() - 0.5, 0.5, RANDOM.nextDouble() - 0.5), 25);
			}
			Fx.sound(level, p.position(), SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F);
		});

		add("doppelt", Category.GOOD, "⛏ Doppelte Beute", "1 Minute lang droppt alles doppelt", (p, pos) -> {
			ChaosBlockMod.manager().grantDoubleDrops(p, 60 * 20);
			effect(p, MobEffects.HASTE, 60, 1);
		});

		add("golem", Category.GOOD, "🛡 Bodyguard", "Ein Eisengolem passt auf dich auf", (p, pos) -> {
			var golem = Places.spawn(EntityTypes.IRON_GOLEM, p.level(), Places.center(Places.around(p, 2, 4)));
			if (golem != null) {
				golem.setPlayerCreated(true);
				golem.setCustomName(Component.literal("Bodyguard"));
			}
		});

		// ===== SCHLECHT =====
		add("creeperring", Category.BAD, "💥 Creeper-Ring", "Lauf. JETZT.", (p, pos) -> {
			ServerLevel level = p.level();
			for (int i = 0; i < 8; i++) {
				double angle = Math.PI * 2 * i / 8;
				int x = (int) Math.floor(p.getX() + Math.cos(angle) * 6);
				int z = (int) Math.floor(p.getZ() + Math.sin(angle) * 6);
				BlockPos at = level.canSeeSky(p.blockPosition()) ? Places.surface(level, x, z) : new BlockPos(x, p.getBlockY(), z);
				var creeper = Places.spawn(EntityTypes.CREEPER, level, Places.center(at));
				if (creeper != null) {
					creeper.getEntityData().set(CreeperAccessor.chaosblock$poweredData(), true);
					creeper.setTarget(p);
				}
			}
			Fx.sound(level, p.position(), SoundEvents.CREEPER_PRIMED, 1.0F, 0.6F);
		});

		add("dieb", Category.BAD, "🦊 Haltet den Dieb!", "Er hat deine Sachen geklaut!", (p, pos) -> {
			Inventory inv = p.getInventory();
			List<Integer> filled = new ArrayList<>();
			for (int i = 0; i < 36; i++) {
				if (!inv.getItem(i).isEmpty()) {
					filled.add(i);
				}
			}
			List<ItemStack> loot = new ArrayList<>();
			for (int n = 0; n < 5 && !filled.isEmpty(); n++) {
				int slot = filled.remove(RANDOM.nextInt(filled.size()));
				loot.add(inv.removeItemNoUpdate(slot));
			}
			inv.setChanged();
			var fox = Places.spawn(EntityTypes.FOX, p.level(), Places.center(Places.around(p, 2, 3)));
			if (fox != null) {
				fox.setCustomName(Component.literal("Dieb"));
				fox.setPersistenceRequired();
				fox.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60 * 5, 2));
				fox.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 15, 0));
				fox.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BUNDLE));
				fox.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
				STOLEN.put(fox.getUUID(), loot);
			}
		});

		add("gewitter", Category.BAD, "⚡ Gewitter des Zorns", "Der Himmel ist wütend auf dich", (p, pos) -> {
			Scheduler.repeat(32, 6, () -> {
				if (!Fx.online(p)) {
					return;
				}
				LightningBolt bolt = Places.spawn(EntityTypes.LIGHTNING_BOLT, p.level(), p.position());
				if (bolt != null) {
					bolt.setCause(p);
				}
			});
		});

		add("bodenlos", Category.BAD, "🕳 Bodenlos", "Halt dich fest…", (p, pos) -> {
			Fx.sound(p.level(), p.position(), SoundEvents.GRAVEL_BREAK, 1.5F, 0.5F);
			Scheduler.later(20, () -> {
				if (!Fx.online(p)) {
					return;
				}
				ServerLevel level = p.level();
				BlockPos feet = p.blockPosition();
				int bottom = level.getMinY() + 5;
				for (int y = feet.getY() - 1; y > bottom; y--) {
					for (int dx = 0; dx <= 1; dx++) {
						for (int dz = 0; dz <= 1; dz++) {
							BlockPos at = new BlockPos(feet.getX() + dx, y, feet.getZ() + dz);
							if (replaceable(level, at)) {
								level.setBlock(at, y == bottom + 1 ? Blocks.LAVA.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
							}
						}
					}
				}
			});
		});

		add("warden", Category.BAD, "👂 Pssst…", "Etwas Großes hat dich gehört", (p, pos) -> {
			effect(p, MobEffects.DARKNESS, 60, 0);
			Scheduler.later(40, () -> {
				if (!Fx.online(p)) {
					return;
				}
				var warden = Places.spawn(EntityTypes.WARDEN, p.level(), Places.center(Places.around(p, 18, 22)));
				if (warden != null) {
					warden.increaseAngerAt(p, 100, true);
				}
			});
		});

		add("ruestung", Category.BAD, "🛡 Rostiger Fluch", "Deine Rüstung zerfällt", (p, pos) -> {
			for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ItemStack stack = p.getItemBySlot(slot);
				if (stack.isDamageableItem()) {
					int remaining = stack.getMaxDamage() - stack.getDamageValue();
					stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + remaining / 2));
				}
			}
			Fx.sound(p.level(), p.position(), SoundEvents.ITEM_BREAK.value(), 1.0F, 0.6F);
		});

		add("hunger", Category.BAD, "🍖 Hungersnot", "Dein Essen ist verdorben", (p, pos) -> {
			p.getFoodData().setFoodLevel(0);
			p.getFoodData().setSaturation(0.0F);
			Inventory inv = p.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack stack = inv.getItem(i);
				if (!stack.isEmpty() && stack.has(DataComponents.FOOD) && !stack.is(Items.ROTTEN_FLESH)) {
					inv.setItem(i, new ItemStack(Items.ROTTEN_FLESH, stack.getCount()));
				}
			}
			inv.setChanged();
		});

		add("begraben", Category.BAD, "⚰ Lebendig begraben", "Grab dich raus!", (p, pos) -> {
			Fx.sound(p.level(), p.position(), SoundEvents.GRAVEL_BREAK, 1.5F, 0.4F);
			Scheduler.later(20, () -> {
				if (!Fx.online(p)) {
					return;
				}
				ServerLevel level = p.level();
				BlockPos top = p.blockPosition();
				BlockPos pocket = top.below(10);
				if (pocket.getY() < level.getMinY() + 2) {
					return;
				}
				for (int dy = -1; dy <= 12; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos at = pocket.offset(dx, dy, dz);
							boolean air = dx == 0 && dz == 0 && (dy == 0 || dy == 1);
							if (replaceable(level, at)) {
								level.setBlock(at, air ? Blocks.AIR.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 2);
							}
						}
					}
				}
				p.teleportTo(pocket.getX() + 0.5, pocket.getY(), pocket.getZ() + 0.5);
			});
		});

		add("phantome", Category.BAD, "🦇 Phantom-Schwarm", "Die schlafen heute nicht", (p, pos) -> {
			for (int i = 0; i < 6; i++) {
				var phantom = Places.spawn(EntityTypes.PHANTOM, p.level(),
					p.position().add(RANDOM.nextDouble() * 10 - 5, 10 + RANDOM.nextDouble() * 4, RANDOM.nextDouble() * 10 - 5));
				if (phantom != null) {
					phantom.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 60 * 5, 0));
					phantom.setTarget(p);
				}
			}
		});

		add("werkzeugbruch", Category.BAD, "🔨 KNACK", "Das war's mit deinem Werkzeug", (p, pos) -> {
			ItemStack held = p.getMainHandItem();
			if (held.isEmpty()) {
				Fx.title(p, "🔨 KNACK", "…du hattest Glück, die Hand war leer");
				return;
			}
			boolean precious = held.is(Items.DIAMOND_PICKAXE) || held.is(Items.DIAMOND_SWORD) || held.is(Items.DIAMOND_AXE)
				|| held.is(Items.NETHERITE_PICKAXE) || held.is(Items.NETHERITE_SWORD) || held.is(Items.NETHERITE_AXE)
				|| held.is(Items.DIAMOND_SHOVEL) || held.is(Items.NETHERITE_SHOVEL);
			if (precious && RANDOM.nextFloat() < 0.25F) {
				Fx.title(p, "🔨 Knirsch…", "…hat gerade noch gehalten!");
				return;
			}
			p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			Fx.sound(p.level(), p.position(), SoundEvents.ITEM_BREAK.value(), 1.0F, 0.8F);
		});

		add("wither", Category.BAD, "☠ Wither-Hauch", "Dir wird ganz schwarz vor Augen", (p, pos) -> {
			effect(p, MobEffects.WITHER, 30, 1);
			Fx.sound(p.level(), p.position(), SoundEvents.WITHER_AMBIENT, 1.0F, 1.0F);
		});

		add("zurueck", Category.BAD, "🏠 Zurück auf Los", "Deine Sachen bleiben hier…", (p, pos) -> {
			p.getInventory().dropAll();
			ServerLevel targetLevel = p.level().getServer().overworld();
			BlockPos target = null;
			var respawn = p.getRespawnConfig();
			if (respawn != null) {
				ServerLevel bedLevel = p.level().getServer().getLevel(respawn.respawnData().globalPos().dimension());
				if (bedLevel != null) {
					targetLevel = bedLevel;
					target = respawn.respawnData().globalPos().pos().above();
				}
			}
			if (target == null) {
				BlockPos spawn = targetLevel.getRespawnData().globalPos().pos();
				target = Places.surface(targetLevel, spawn.getX(), spawn.getZ());
			}
			p.teleportTo(targetLevel, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
		});

		// ===== ABGESPACED =====
		add("rakete", Category.WEIRD, "🚀 Abflug!", "Hier, ein Wassereimer. Viel Glück.", (p, pos) -> {
			Places.give(p, new ItemStack(Items.WATER_BUCKET));
			launch(p, new Vec3(0, 3.4, 0));
			particles(p.level(), ParticleTypes.CLOUD, p.position(), 30, 0.5);
			Fx.sound(p.level(), p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5F, 0.6F);
		});

		add("fischregen", Category.WEIRD, "🐟 Es regnet Fische", "…warum auch immer", (p, pos) -> {
			Scheduler.repeat(3, 30, () -> {
				if (!Fx.online(p)) {
					return;
				}
				Vec3 at = p.position().add(RANDOM.nextDouble() * 12 - 6, 12 + RANDOM.nextDouble() * 4, RANDOM.nextDouble() * 12 - 6);
				switch (RANDOM.nextInt(3)) {
					case 0 -> Places.spawn(EntityTypes.COD, p.level(), at);
					case 1 -> Places.spawn(EntityTypes.SALMON, p.level(), at);
					default -> Places.spawn(EntityTypes.TROPICAL_FISH, p.level(), at);
				}
			});
		});

		add("huehner", Category.WEIRD, "🐔 Alles Hühner", "Puff!", (p, pos) -> {
			ServerLevel level = p.level();
			for (Animal animal : level.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(16))) {
				if (animal.getType() == EntityTypes.CHICKEN || animal.hasCustomName()) {
					continue;
				}
				Places.spawn(EntityTypes.CHICKEN, level, animal.position());
				particles(level, ParticleTypes.POOF, animal.position().add(0, 0.5, 0), 10, 0.3);
				animal.discard();
			}
			Fx.sound(level, p.position(), SoundEvents.CHICKEN_EGG, 1.5F, 1.0F);
		});

		add("regenbogen", Category.WEIRD, "🌈 Regenbogen!", "Die Schafe haben sich schick gemacht", (p, pos) -> {
			ServerLevel level = p.level();
			var sheep = level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, p.getBoundingBox().inflate(32));
			if (sheep.isEmpty()) {
				for (int i = 0; i < 4; i++) {
					Places.spawn(EntityTypes.SHEEP, level, Places.center(Places.around(p, 2, 5)));
				}
				sheep = level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, p.getBoundingBox().inflate(32));
			}
			for (var s : sheep) {
				s.setColor(DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)]);
				s.setCustomName(Component.literal("jeb_"));
			}
		});

		add("villager", Category.WEIRD, "😱 HUAAAH", "Gerd hat Angst. Und folgt dir.", (p, pos) -> {
			var villager = Places.spawn(EntityTypes.VILLAGER, p.level(), Places.center(Places.around(p, 2, 3)));
			if (villager == null) {
				return;
			}
			villager.setCustomName(Component.literal("Schreiender Gerd"));
			Scheduler.repeat(15, 60 * 20 / 15, () -> {
				if (villager.isRemoved() || !Fx.online(p) || villager.level() != p.level()) {
					return;
				}
				villager.getNavigation().moveTo(p, 0.7);
				Fx.sound(p.level(), villager.position(), RANDOM.nextBoolean() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_HURT, 1.5F,
					0.8F + RANDOM.nextFloat() * 0.6F);
			});
			Scheduler.later(60 * 20 + 5, () -> {
				if (!villager.isRemoved()) {
					particles((ServerLevel) villager.level(), ParticleTypes.POOF, villager.position(), 15, 0.4);
					villager.discard();
				}
			});
		});

		add("mini", Category.WEIRD, "🐜 Mini-Ich", "Eine Minute lang winzig", (p, pos) -> {
			tempModifier(p, Attributes.SCALE, "mini", -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 60 * 20);
		});

		add("riese", Category.WEIRD, "🗿 RIESE", "Eine Minute lang gigantisch", (p, pos) -> {
			tempModifier(p, Attributes.SCALE, "riese", 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 60 * 20);
			tempModifier(p, Attributes.STEP_HEIGHT, "riese_step", 1.0, AttributeModifier.Operation.ADD_VALUE, 60 * 20);
			tempModifier(p, Attributes.BLOCK_INTERACTION_RANGE, "riese_reach", 3.0, AttributeModifier.Operation.ADD_VALUE, 60 * 20);
		});

		add("schwerelos", Category.WEIRD, "🌌 Schwerkraft? Nie gehört.", "Ab nach oben!", (p, pos) -> {
			effect(p, MobEffects.LEVITATION, 8, 1);
			Scheduler.later(8 * 20, () -> {
				if (Fx.online(p)) {
					effect(p, MobEffects.SLOW_FALLING, 15, 0);
				}
			});
		});

		add("tagnacht", Category.WEIRD, "🌗 Zeitsprung", "Tag und Nacht haben getauscht", (p, pos) -> {
			ServerLevel overworld = p.level().getServer().overworld();
			overworld.dimensionType().defaultClock().ifPresent(clock -> overworld.getServer().clockManager()
				.moveToTimeMarker(clock, overworld.isBrightOutside() ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY));
		});

		add("party", Category.WEIRD, "🎉 PARTY!", "Alle tanzen!", (p, pos) -> {
			ServerLevel level = p.level();
			Scheduler.repeat(10, 20, () -> {
				if (!Fx.online(p)) {
					return;
				}
				Vec3 at = p.position().add(RANDOM.nextDouble() * 10 - 5, 0.5, RANDOM.nextDouble() * 10 - 5);
				level.addFreshEntity(new FireworkRocketEntity(level, at.x, at.y, at.z, firework(RANDOM)));
				float pitch = 0.5F + RANDOM.nextInt(12) / 8.0F;
				level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(),
					net.minecraft.sounds.SoundSource.RECORDS, 1.0F, pitch);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(12))) {
					if (mob.onGround()) {
						mob.setDeltaMovement(mob.getDeltaMovement().add(0, 0.45, 0));
					}
					mob.setYRot(mob.getYRot() + 90.0F);
					mob.setYHeadRot(mob.getYRot());
				}
				particles(level, ParticleTypes.NOTE, p.position().add(0, 2, 0), 3, 1.0);
			});
		});
	}

	/** Für Befehle/Tests: alle Events einer Kategorie. */
	public static List<ChaosEvent> byCategory(Category category) {
		return ALL.stream().filter(e -> e.category() == category).toList();
	}
}
