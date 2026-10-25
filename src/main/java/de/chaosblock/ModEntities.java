package de.chaosblock;

import de.chaosblock.ambient.VisitorEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static EntityType<VisitorEntity> VISITOR;

	private ModEntities() {
	}

	public static void register() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ChaosBlockMod.id("visitor"));
		VISITOR = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
			EntityType.Builder.<VisitorEntity>of(VisitorEntity::new, MobCategory.MONSTER)
				.sized(0.6F, 1.95F)
				.eyeHeight(1.62F)
				.clientTrackingRange(10)
				.build(key));
		FabricDefaultAttributeRegistry.register(VISITOR, VisitorEntity.createAttributes());
	}
}
