package gregtech6.items.armor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The GT6 hazard full-set judgment seam — task p29-w5-t8-armor-24 spec ②/③, the ONLY
 * public seam of the armor card (the SPEC boundary: future hazard-damage-source cards
 * CONSUME this API, they do not modify it).
 *
 * <p>Upstream anchor: the {@code ArmorsGT.HAZMATS_*} ItemStackSet family
 * (CS.java:1712-1721 — GAS/BIO/CHEM/INSECTS/FROST/HEAT/RADIOACTIVE/LIGHTNING, eight
 * sets), filled by the Loader_Tools.java:98-112 mutual-joins (universal joins ALL EIGHT
 * — :106 is the LIGHTNING line, the ninth membership the card gloss's "7 sets" under-
 * counts; biochemgas joins GAS/BIO/CHEM — :108-112) and by each base suit's own
 * registration row (:68-91). The port translates the flat lists onto a static
 * (id-keyed) membership table derived from the {@link GT6ArmorMaterials#SUITS} walk —
 * zero registration lifecycle, the ids are stable before the registry ever opens, which
 * is what makes the seam offline-testable (the ArmorSetTest parity).
 *
 * <p>Query faces: the pure id core ({@link #isFullSet(Hazard, ResourceLocation,
 * ResourceLocation, ResourceLocation, ResourceLocation)}) and the worn-state wrapper
 * ({@link #isWearingFullSet(LivingEntity, Hazard)}). RED LINE (the card's declared
 * cut): this class carries ZERO damage-guard event wiring — Forge 1.20.1 has no
 * LivingFreezeEvent and vanilla {@code canFreeze} is an entity override, not an event,
 * so the frost/heat/radiation/gas CONSUMER face belongs to the future hazard
 * damage-source card (decisions.open_items_for_user ruling, verbatim).
 */
public final class GT6HazardSets {

	/** The eight upstream hazard classes (the HAZMATS_* set roster, CS.java:1712-1721). */
	public enum Hazard {
		GAS("gas"), BIO("bio"), CHEM("chem"), INSECTS("insects"),
		FROST("frost"), HEAT("heat"), RADIATION("radiation"), LIGHTNING("lightning");

		/** The lower-snake path word — the #gt6:hazmat/&lt;word&gt; tag tail (GT6ItemTags). */
		public final String path;

		Hazard(String aPath) {
			this.path = aPath;
		}
	}

	/** The gt6 namespace literal (the GT6Tools form). */
	private static final String MOD_ID = "gt6";

	/**
	 * The per-hazard membership table — DERIVED (never registered): one walk over the
	 * {@link GT6ArmorMaterials#SUITS} table reproduces the Loader_Tools.java:68-112 join
	 * semantics exactly (a suit's pieces join every hazard its row lists). Insertion
	 * order kept (LinkedHashSet) so the datagen tag band and the /gt6tags dump face
	 * stay deterministic.
	 */
	private static final Map<Hazard, Set<ResourceLocation>> MEMBERS;

	static {
		Map<Hazard, Set<ResourceLocation>> tMap = new EnumMap<>(Hazard.class);
		for (Hazard tHazard : Hazard.values()) {
			tMap.put(tHazard, new LinkedHashSet<>());
		}
		for (GT6ArmorMaterials.SuitRow tSuit : GT6ArmorMaterials.SUITS) {
			for (Hazard tHazard : tSuit.hazards()) {
				for (int i = 0; i < GT6ArmorMaterials.PIECE_WORDS.length; i++) {
					tMap.get(tHazard).add(gt6Id(tSuit.pieceId(i)));
				}
			}
		}
		Map<Hazard, Set<ResourceLocation>> tFrozen = new EnumMap<>(Hazard.class);
		for (Hazard tHazard : Hazard.values()) {
			tFrozen.put(tHazard, Collections.unmodifiableSet(tMap.get(tHazard)));
		}
		MEMBERS = Collections.unmodifiableMap(tFrozen);
	}

	/** The live membership of one hazard class — read-only view, the consumer face. */
	public static Set<ResourceLocation> members(Hazard aHazard) {
		return MEMBERS.get(aHazard);
	}

	/** The tag path of one hazard class — #gt6:hazmat/&lt;snake&gt; (the GT6ItemTags band). */
	public static String tagPath(Hazard aHazard) {
		return "hazmat/" + aHazard.path;
	}

	/**
	 * The pure full-set judgment — all four slots present AND each worn id a member of
	 * the hazard set. Null = empty slot (a partial set is never a full set).
	 */
	public static boolean isFullSet(Hazard aHazard, @Nullable ResourceLocation aHead,
			@Nullable ResourceLocation aChest, @Nullable ResourceLocation aLegs, @Nullable ResourceLocation aBoots) {
		if (aHead == null || aChest == null || aLegs == null || aBoots == null) return false;
		Set<ResourceLocation> tMembers = MEMBERS.get(aHazard);
		return tMembers.contains(aHead) && tMembers.contains(aChest)
				&& tMembers.contains(aLegs) && tMembers.contains(aBoots);
	}

	/** The worn-state face — the four equipment slots of a living entity (ArmorStand included). */
	public static boolean isWearingFullSet(LivingEntity aEntity, Hazard aHazard) {
		return isFullSet(aHazard,
				id(aEntity.getItemBySlot(EquipmentSlot.HEAD)),
				id(aEntity.getItemBySlot(EquipmentSlot.CHEST)),
				id(aEntity.getItemBySlot(EquipmentSlot.LEGS)),
				id(aEntity.getItemBySlot(EquipmentSlot.FEET)));
	}

	private static ResourceLocation id(ItemStack aStack) {
		return aStack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(aStack.getItem());
	}

	private static ResourceLocation gt6Id(String aPath) {
		return new ResourceLocation(MOD_ID, aPath);
	}

	private GT6HazardSets() {
	}
}
