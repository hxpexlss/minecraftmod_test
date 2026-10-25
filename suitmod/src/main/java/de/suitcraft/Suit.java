package de.suitcraft;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Die vier Anzüge. Die Fähigkeiten gibt es nur mit allen vier Teilen.
 */
public enum Suit {
	WEBRUNNER("netzlaeufer", "Netzläufer", 18, 1.0F, SoundEvents.ARMOR_EQUIP_LEATHER, List.of(
		"Wände hochklettern",
		"[Taste] Netz schießen und hinschwingen",
		"Höher springen, weniger Fallschaden",
		"Gefahrensinn: spürt Monster hinter dir")),
	EMBER("glutpanzer", "Glutpanzer", 25, 2.0F, SoundEvents.ARMOR_EQUIP_NETHERITE, List.of(
		"Immun gegen Feuer und Lava",
		"Läufst du über Lava, wird sie zu Magma",
		"[Taste] Flammenstoß nach vorne",
		"Wer dich schlägt, fängt Feuer")),
	SHADOW("schattenmantel", "Schattenmantel", 16, 0.0F, SoundEvents.ARMOR_EQUIP_LEATHER, List.of(
		"Schleichen = unsichtbar, Monster verlieren dich",
		"[Taste] Teleport dorthin, wo du hinschaust",
		"Nachtsicht",
		"Doppelsprung")),
	STORM("sturmfluegel", "Sturmflügel", 20, 0.0F, SoundEvents.ARMOR_EQUIP_ELYTRA, List.of(
		"Gleiten wie mit Elytren",
		"Sprung in der Luft: Aufwind und Abheben",
		"[Taste] Windstoß, im Flug: Schub nach vorne",
		"Weniger Fallschaden"));

	public final String id;
	public final String displayName;
	public final ArmorMaterial material;
	public final List<String> abilities;
	public final Map<ArmorType, Item> pieces = new EnumMap<>(ArmorType.class);

	Suit(String id, String displayName, int durability, float toughness, Holder<SoundEvent> sound, List<String> abilities) {
		this.id = id;
		this.displayName = displayName;
		this.abilities = abilities;
		this.material = new ArmorMaterial(
			durability,
			Map.of(ArmorType.BOOTS, 2, ArmorType.LEGGINGS, 5, ArmorType.CHESTPLATE, 6, ArmorType.HELMET, 2, ArmorType.BODY, 5),
			15,
			sound,
			toughness,
			0.0F,
			TagKey.create(Registries.ITEM, SuitcraftMod.id("repairs_" + id)),
			ResourceKey.create(EquipmentAssets.ROOT_ID, SuitcraftMod.id(id))
		);
	}

	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** Zu welchem Anzug gehört dieses Teil? null = gar keinem. */
	public static Suit of(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		for (Suit suit : values()) {
			if (suit.pieces.containsValue(stack.getItem())) {
				return suit;
			}
		}
		return null;
	}

	/** Der Anzug, von dem der Spieler alle vier Teile trägt, sonst null. */
	public static Suit worn(Player player) {
		Suit suit = of(player.getItemBySlot(EquipmentSlot.HEAD));
		if (suit == null) {
			return null;
		}
		for (EquipmentSlot slot : SLOTS) {
			if (of(player.getItemBySlot(slot)) != suit) {
				return null;
			}
		}
		return suit;
	}

	public static boolean wears(Player player, Suit suit) {
		return worn(player) == suit;
	}
}
