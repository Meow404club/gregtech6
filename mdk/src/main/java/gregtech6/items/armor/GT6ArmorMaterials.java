package gregtech6.items.armor;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The six Hazmat suit materials — task p29-w5-t8-armor-24 spec ①, the vanilla
 * {@link ArmorMaterial} interface implementation of the upstream
 * {@code EnumHelper.addArmorMaterial("armor." + aUnlocalized, ...)} row
 * (gregapi/item/ItemArmorBase.java:82, once per registration row Loader_Tools.java:68-96).
 * The stat triple is the upstream :68 parameter shape, flat across all six suits:
 * defense {@code new int[] {1, 1, 1, 1}} = 1 per piece ({@link #DEFENSE_PER_PIECE}),
 * durability 128 ({@link #DURABILITY}), enchantability 8 ({@link #ENCHANTMENT_VALUE}),
 * toughness/knockback 0.
 *
 * <p>{@link #getName()} returns the suit's lower-snake texture word
 * ({@code hazmat_insect}...) — the vanilla {@code HumanoidArmorLayer.java:121} fallback
 * composition root. The WORN texture actually resolves through the item-level
 * {@code getArmorTexture} override ({@link GT6ArmorItem}, the ForgeHooksClient
 * :264 seam — the port of the upstream ItemArmorBase.java:86/:136 mArmorTexture face),
 * so the name stays a texture-word identity, never a namespace-carrying path.
 *
 * <p>The {@link #SUITS} table is the card's single source: the registration walk
 * (GT6Tools ARMOR_ROWS), the membership derivation (GT6HazardSets), the datagen bands
 * (recipes/tags/models/lang) and the offline ArmorSetTest all read THIS table.
 */
public enum GT6ArmorMaterials implements ArmorMaterial {

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

	@Override
	public String getName() {
		return this.textureName;
	}

	@Override
	public int getDurabilityForType(ArmorItem.Type aType) {
		return DURABILITY;
	}

	@Override
	public int getDefenseForType(ArmorItem.Type aType) {
		return DEFENSE_PER_PIECE;
	}

	@Override
	public int getEnchantmentValue() {
		return ENCHANTMENT_VALUE;
	}

	@Override
	public SoundEvent getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_GENERIC;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.EMPTY;
	}

	@Override
	public float getToughness() {
		return 0.0F;
	}

	@Override
	public float getKnockbackResistance() {
		return 0.0F;
	}

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
