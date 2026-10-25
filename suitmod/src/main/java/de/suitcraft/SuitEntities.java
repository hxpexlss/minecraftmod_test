package de.suitcraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class SuitEntities {
	public static EntityType<WebBallEntity> WEB_BALL;

	private SuitEntities() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SuitcraftMod.id("web_ball"));
		WEB_BALL = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.<WebBallEntity>of(WebBallEntity::new, MobCategory.MISC)
				.noLootTable()
				.sized(0.25F, 0.25F)
				.clientTrackingRange(4)
				.updateInterval(10)
				.build(key));
	}
}
