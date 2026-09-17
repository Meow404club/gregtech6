package gregtech6.items.tools;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.itemdata.GT6ItemData;
import gregtech6.itemdata.GT6ToolStats;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The material-ladder math shared by the dig-family tools (task p31-dig-ladder) —
 * ONE parameterized face over the identity seam instead of seven copies of the
 * crowbar's per-class statics. Every upstream formula (MultiItemTool.java, the
 * research.p31-metatool-ladder pins) reads the stack's {@link GT6ToolStats#KEY}
 * identity, with the VERBATIM upstream steel fallback reproducing the single-tier
 * numbers exactly (the identity-less legacy arm — {@code getPrimaryMaterial(stack,
 * MT.Steel)} is how the upstream tools read :60/:65, so "no identity" upstream IS
 * Steel):
 * <ul>
 * <li><b>durability</b> — {@code j = mToolDurability * 100 * multiplier} (:182) at
 *     the pinned 100 units = 1 point ratio; Steel 512 → the family 512, the gem
 *     pick's ×0.25 → the flat 128 (the pre-ladder constants reproduce bit-exact).
 *     The durability multiplier is the ONLY form constant that reaches the fallback
 *     path, so it rides the {@link #statsOf(ItemStack, float)} signature alone;</li>
 * <li><b>speed</b> — {@code getSpeedMultiplier() * mToolSpeed} (:483): the form
 *     multiplier × the material speed (Steel 6.0 = the pre-ladder anchors);</li>
 * <li><b>mining level</b> — {@code getBaseQuality() + mToolQuality} (:482/:495) —
 *     the drop authorization and the speed gate; a quality-starved tool returns
 *     ZERO speed on the too-hard surface (:482), never the vanilla slow-mine;</li>
 * <li><b>attack</b> — {@code getBaseDamage() + mToolQuality} (:392) is NOT wired:
 *     the attribute modifiers are item constants (registry-frozen per instance),
 *     the per-material attack face is a declared cut (the W5 attack rows pin the
 *     steel anchor; revives if the port ever moves to stack-dependent attributes);</li>
 * <li><b>name</b> — the composed "Pickaxe (Bronze)" form (the P7 machine-ladder
 *     convention, over the existing {@code gt6.material.*} lang family; upstream
 *     shows the bare meta name, the paren suffix is the declared port convention).
 *     Identity-less stacks keep the bare form (the upstream {@code mMat1 == MT.NULL}
 *     tooltip arm shows no material either).</li>
 * </ul>
 *
 * <p>All faces are static pure functions so the offline tests pin them without
 * constructing a mod Item (the mod-Item wall, DigSixTest premise). The wear roll
 * {@code nextInt(max(10, quality*20))} (:437) applies to ELECTRIC tools only (the
 * {@code tElectric == null ||} short-circuit) — hand tools pay every event, so the
 * port's folded 1-point-per-event mapping is upstream-exact and the roll is
 * deliberately not ported.
 */
public final class GT6ToolLadder {

	/** The upstream :182 unit budget per vanilla point (the crowbar seam constant). */
	public static final long UNITS_PER_POINT = 100;

	/**
	 * The ladder-form face: the item's own durability multiplier, the polymorphic read
	 * the RCON material arm (and any future identity-writer) uses to stamp the form's
	 * budget. The whole dig family implements it; the blade/machine ladders inherit the
	 * same face.
	 */
	public interface LadderTool {

		float durabilityMultiplier();
	}

	private GT6ToolLadder() {
	}

	/**
	 * The effective stats of a stack: its {@link GT6ToolStats#KEY} identity, or the
	 * synthesized steel stats at this form's durability multiplier (the legacy arm —
	 * bit-exact the pre-ladder constants: Steel 512 ×0.25 = the gem pick's 128).
	 */
	public static GT6ToolStats statsOf(ItemStack aStack, float aDurabilityMultiplier) {
		return GT6ItemData.find(aStack, GT6ToolStats.KEY)
				.orElseGet(() -> GT6ToolStats.of(MT.Steel, null, aDurabilityMultiplier));
	}

	/** The primary material of a stack (the identity, or the upstream Steel fallback). */
	public static OreDictMaterial materialOf(ItemStack aStack) {
		return GT6ItemData.find(aStack, GT6ToolStats.KEY)
				.map(GT6ToolStats::primaryMaterial).orElse(MT.Steel);
	}

	/**
	 * The snake-name lookup the recipe rows resolve through: the JSON carries the snake
	 * form (the id/tag convention), the material table keys on {@code mNameInternal} —
	 * and a naive camel-reconstruction is NOT invertible (acronym materials like PVC).
	 * The exact inverse is the same {@code GTMaterialItems.snakeCase} the datagen writes
	 * with, applied over the whole registry once (lazily; the index is a 2200-entry map).
	 */
	public static OreDictMaterial materialBySnake(String aSnake) {
		Map<String, OreDictMaterial> tIndex = sSnakeIndex;
		if (tIndex == null) {
			tIndex = new HashMap<>();
			for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
				if (tMaterial == null || tMaterial.mID < 0) continue;
				tIndex.putIfAbsent(gregtech6.registry.GTMaterialItems.snakeCase(tMaterial.mNameInternal), tMaterial);
			}
			sSnakeIndex = tIndex;
		}
		OreDictMaterial tMaterial = tIndex.get(aSnake);
		if (tMaterial == null) tMaterial = OreDictMaterial.get(aSnake);
		return tMaterial;
	}

	private static volatile Map<String, OreDictMaterial> sSnakeIndex;

	/**
	 * The identity writer (the upstream {@code getToolWithStats} face, MultiItemTool
	 * .java:180-192): stamps the {@link GT6ToolStats#KEY} payload for the material at
	 * the form's multiplier — the recipe assembler, the RCON material arm and the
	 * future identity writers all route through this one face.
	 */
	public static ItemStack stampIdentity(ItemStack aStack, OreDictMaterial aPrimary, float aDurabilityMultiplier) {
		GT6ItemData.set(aStack, GT6ToolStats.KEY, GT6ToolStats.of(aPrimary, null, aDurabilityMultiplier));
		return aStack;
	}

	/** The per-material durability in vanilla points (j / 100, min 1 — the crowbar seam). */
	public static int durabilityPoints(GT6ToolStats aStats) {
		return (int) Math.max(1, aStats.maxDamage() / UNITS_PER_POINT);
	}

	/** The dig speed: the form's speed multiplier × the material's mToolSpeed (:483). */
	public static float speed(float aFormSpeedMultiplier, OreDictMaterial aPrimary) {
		return aFormSpeedMultiplier * aPrimary.mToolSpeed;
	}

	/** The mining level: the form's base quality + the material's mToolQuality (:482/:495). */
	public static int miningLevel(int aBaseQuality, OreDictMaterial aPrimary) {
		return aBaseQuality + aPrimary.mToolQuality;
	}

	/**
	 * The vanilla block requirement level (the DiggerItem tier gates): 3 = needs
	 * diamond, 2 = needs iron, 1 = needs stone, 0 = any (the 1.7.10
	 * {@code bind4(getHarvestLevel)} counterpart).
	 */
	public static int requiredLevel(BlockState aState) {
		if (aState.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
		if (aState.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
		if (aState.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
		return 0;
	}

	/**
	 * The level gate (upstream :482): true when the stack's material quality cannot
	 * harvest the block — the tool returns ZERO speed there, never the vanilla
	 * slow-mine. Base quality is 0 for the whole dig family (the upstream
	 * getBaseQuality rows, ToolStats.java:67 default).
	 */
	public static boolean qualityGate(ItemStack aStack, BlockState aState) {
		return requiredLevel(aState) > miningLevel(0, materialOf(aStack));
	}

	/**
	 * The tint (upstream getRGBa :146-149 verbatim): tint index 0 = the head layer,
	 * the material {@code mRGBaSolid} packed ARGB with the steel fallback; every other
	 * index = the {@code -1} no-tint sentinel (the crowbar {@code tintARGB} family
	 * form). The one shared face serves the whole dig family (the fallback multiplier
	 * only ever reaches the durability budget, never the colour).
	 */
	public static int tintARGB(ItemStack aStack, int aTintIndex) {
		if (aTintIndex != 0) return -1;
		OreDictMaterial tMaterial = materialOf(aStack);
		return 0xFF000000 | (tMaterial.mRGBaSolid[0] << 16) | (tMaterial.mRGBaSolid[1] << 8) | tMaterial.mRGBaSolid[2];
	}

	/**
	 * The composed display name — "Pickaxe (Bronze)" over the {@code gt6.material.*}
	 * lang family (no per-material lang rows to mint; the fill rides the existing
	 * {@code MaterialPrefixItem.materialFill} seam). A stack with NO identity keeps
	 * the bare translatable (the legacy arm — the upstream {@code mMat1 == MT.NULL}
	 * surfaces carry no material word either).
	 */
	public static Component displayName(ItemStack aStack, String aDescriptionId) {
		net.minecraft.network.chat.MutableComponent tName = Component.translatable(aDescriptionId);
		if (!GT6ItemData.find(aStack, GT6ToolStats.KEY).isPresent()) return tName;
		return tName.append(" (").append(gregtech6.item.MaterialPrefixItem.materialFill(materialOf(aStack))).append(")");
	}
}
