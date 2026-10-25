package de.suitcraft;

import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;

public final class SuitItems {
	public static Item WEB_SHOOTER;

	private SuitItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SuitcraftMod.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void register() {
		for (Suit suit : Suit.values()) {
			for (ArmorType type : new ArmorType[]{ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}) {
				Item.Properties properties = new Item.Properties().humanoidArmor(suit.material, type).rarity(Rarity.UNCOMMON);
				if (suit == Suit.EMBER) {
					properties.fireResistant();
				}
				if (suit == Suit.STORM && type == ArmorType.CHESTPLATE) {
					// Die Sturmflügel-Weste gleitet wie Elytren.
					properties.component(DataComponents.GLIDER, Unit.INSTANCE);
				}
				suit.pieces.put(type, register(suit.id + "_" + type.getName(), Item::new, properties));
			}
		}

		WEB_SHOOTER = register("web_shooter", WebShooterItem::new, new Item.Properties().durability(96).rarity(Rarity.UNCOMMON));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
			for (Suit suit : Suit.values()) {
				suit.pieces.values().forEach(output::accept);
			}
			output.accept(WEB_SHOOTER);
		});
	}
}
