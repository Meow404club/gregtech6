package gregtech6.items.armor;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

//? if forge {
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Holder;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * The six Hazmat suit materials — task p29-w5-t8-armor-24 spec ①, the vanilla
 * {@link ArmorMaterial} face of the upstream
 * {@code EnumHelper.addArmorMaterial("armor." + aUnlocalized, ...)} row
 * (gregapi/item/ItemArmorBase.java:82, once per registration row Loader_Tools.java:68-96).
 * The stat triple is the upstream :68 parameter shape, flat across all six suits:
 * defense {@code new int[] {1, 1, 1, 1}} = 1 per piece ({@link #DEFENSE_PER_PIECE}),
 * durability 128 ({@link #DURABILITY}), enchantability 8 ({@link #ENCHANTMENT_VALUE}),
 * toughness/knockback 0.
 *
 * <p>Leg split (the ONLY //? surface of the armor card): 1.20.1 — {@code ArmorMaterial}
 * is an INTERFACE (vanilla ArmorMaterial.java:6-22: getDurabilityForType /
 * getDefenseForType / getEquipSound / getRepairIngredient / getName), so the enum
 * implements it directly; 1.21.1 — it became a RECORD (vanilla-mc 1.21.1
 * ArmorMaterial.java:16-25: defense Map / enchantmentValue / equipSound Holder /
 * repairIngredient Supplier / layers / toughness / knockbackResistance, the durability
 * moved onto the item Properties), so each constant builds a direct-held record whose
 * {@code Layer(assetName)} resolves the SAME gt6:textures/models/armor/&lt;suit&gt;_layer_*
 * paths the forge leg serves through the item-level getArmorTexture override. The
 * leg-neutral literal face ({@link #defenseFor}, {@link #durabilityFor},
 * {@link #enchantValue}) exists so the offline ArmorSetTest pins the :68 numbers without
 * touching either leg's vanilla signature.
 *
 * <p>The {@link #SUITS} table is the card's single source: the registration walk
 * (GT6Tools ARMOR_ROWS), the membership derivation (GT6HazardSets), the datagen bands
 * (recipes/tags/models/lang) and the offline ArmorSetTest all read THIS table.
 */
//? if forge {
public enum GT6ArmorMaterials implements ArmorMaterial {
	//?} else {
	/*public enum GT6ArmorMaterials {
	*///?}

	INSECTS("hazmat_insect"),
	FROST("hazmat_frost"),
	HEAT("hazmat_heat"),
	RADIATION("hazmat_radiation"),
	BIOCHEMGAS("hazmat_biochemgas"),
	UNIVERSAL("hazmat_universal");

	/** Upstream durability per piece — the :68 row's {@code 128} literal. */
	public static final int DURABILITY = 128;
	/** Upstream defense per piece — the :68 row's {@code new int[] {1, 1, 1, 1}}. */
	public static final int DEFENSE_PER_PIECE = 1;
	/** Upstream enchantability — the :68 row's {@code 8} literal. */
	public static final int ENCHANTMENT_VALUE = 8;

	/** The slot order every walk shares — ArmorItem.Type declaration order (HELMET..BOOTS). */
	public static final ArmorItem.Type[] PIECE_TYPES = {
			ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS };
	/** The slot path words — the vanilla Type-name snakes, the id composition tail. */
	public static final String[] PIECE_WORDS = { "helmet", "chestplate", "leggings", "boots" };

	private final String textureName;

	GT6ArmorMaterials(String aTextureName) {
		this.textureName = aTextureName;
	}

	/** The suit's lower-snake texture word — item id prefix and lang/model walk tail. */
	public String textureName() {
		return this.textureName;
	}

	/** The leg-neutral literal face — the ArmorSetTest pins these, not the vanilla signatures. */
	public int defenseFor(ArmorItem.Type aType) {
		//? if forge {
		return getDefenseForType(aType);
		//?} else {
		/*return material().value().getDefense(aType);
		*///?}
	}

	/** The leg-neutral durability literal (the :68 {@code 128}). */
	public int durabilityFor(ArmorItem.Type aType) {
		//? if forge {
		return getDurabilityForType(aType);
		//?} else {
		/*return DURABILITY; // the 1.21 durability rides the item Properties, not the record
		*///?}
	}

	/** The leg-neutral enchantability literal (the :68 {@code 8}). */
	public int enchantValue() {
		//? if forge {
		return getEnchantmentValue();
		//?} else {
		/*return material().value().enchantmentValue();
		*///?}
	}

	//? if forge {
	// the suit's lower-snake texture word — the vanilla HumanoidArmorLayer.java:121
	// fallback root (NO javadoc inside a fork branch: the stonecutter /*-wrapping
	// cannot nest block comments)
	public String getName() {
		return this.textureName;
	}

	public int getDurabilityForType(ArmorItem.Type aType) {
		return DURABILITY;
	}

	public int getDefenseForType(ArmorItem.Type aType) {
		return DEFENSE_PER_PIECE;
	}

	public int getEnchantmentValue() {
		return ENCHANTMENT_VALUE;
	}

	public SoundEvent getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_GENERIC;
	}

	public Ingredient getRepairIngredient() {
		return Ingredient.EMPTY;
	}

	public float getToughness() {
		return 0.0F;
	}

	public float getKnockbackResistance() {
		return 0.0F;
	}
	//?} else {
	/*private Holder<ArmorMaterial> tHolder;

	// The 1.21 record face — defense Map + the Layer whose assetName resolves
	// gt6:textures/models/armor/&lt;suit&gt;_layer_&lt;1|2&gt;.png natively
	// (vanilla-mc 1.21.1 ArmorMaterial.Layer.resolveTexture). Holder.direct keeps the
	// materials off the 1.21 ARMOR_MATERIAL registry (the trim/codec face is unused here).
	public Holder<ArmorMaterial> material() {
		if (tHolder == null) {
			tHolder = Holder.direct(new ArmorMaterial(
					Map.of(ArmorItem.Type.HELMET, DEFENSE_PER_PIECE,
							ArmorItem.Type.CHESTPLATE, DEFENSE_PER_PIECE,
							ArmorItem.Type.LEGGINGS, DEFENSE_PER_PIECE,
							ArmorItem.Type.BOOTS, DEFENSE_PER_PIECE),
					ENCHANTMENT_VALUE,
					SoundEvents.ARMOR_EQUIP_GENERIC,
					() -> Ingredient.EMPTY,
					List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("gt6", textureName))),
					0.0F, 0.0F));
		}
		return tHolder;
	}
	*///?}

	/**
	 * One suit row: the material plus the hazard-class coverage — the Loader_Tools
	 * join semantics as DATA (base suits cover their own class, :68-91; biochemgas
	 * additionally covers GAS/BIO/CHEM and universal everything, the :98-112 loops).
	 */
	public record SuitRow(GT6ArmorMaterials suit, Set<GT6HazardSets.Hazard> hazards) {

		/** The suit's texture word ({@link GT6ArmorMaterials#textureName()}). */
		public String textureName() {
			return suit.textureName();
		}

		/** The registered item id path of one piece — suit word + "_" + slot word. */
		public String pieceId(int aSlot) {
			return textureName() + "_" + PIECE_WORDS[aSlot];
		}
	}

	/** The six suits, upstream registration order (Loader_Tools.java:68-96 row order). */
	public static final List<SuitRow> SUITS = List.of(
			new SuitRow(INSECTS, EnumSet.of(GT6HazardSets.Hazard.INSECTS)),
			new SuitRow(FROST, EnumSet.of(GT6HazardSets.Hazard.FROST)),
			new SuitRow(HEAT, EnumSet.of(GT6HazardSets.Hazard.HEAT)),
			new SuitRow(RADIATION, EnumSet.of(GT6HazardSets.Hazard.RADIATION)),
			new SuitRow(BIOCHEMGAS, EnumSet.of(GT6HazardSets.Hazard.GAS,
					GT6HazardSets.Hazard.BIO, GT6HazardSets.Hazard.CHEM)),
			new SuitRow(UNIVERSAL, EnumSet.allOf(GT6HazardSets.Hazard.class)));

	/** The SUITS row of one material constant — SUITS order is the enum ordinal order. */
	public static SuitRow rowOf(GT6ArmorMaterials aSuit) {
		return SUITS.get(aSuit.ordinal());
	}
}
