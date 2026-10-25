package de.chaosblock;

import de.chaosblock.item.OracleEyeItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static Item ORACLE_EYE;
	public static Item ETERNAL_EYE;
	public static Item WHITE_EYE;

	private ModItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ChaosBlockMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void register(ModConfig config) {
		ORACLE_EYE = register("oracle_eye", p -> new OracleEyeItem(p, false),
			new Item.Properties().durability(Math.max(1, config.oracleCharges)).rarity(Rarity.UNCOMMON));
		ETERNAL_EYE = register("eternal_eye", p -> new OracleEyeItem(p, true),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
		WHITE_EYE = register("white_eye", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ORACLE_EYE));
	}
}
