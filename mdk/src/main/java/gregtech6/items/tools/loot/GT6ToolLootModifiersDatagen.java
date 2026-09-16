package gregtech6.items.tools.loot;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

//? if forge {
import net.minecraftforge.common.data.GlobalLootModifierProvider;
//?} else {
/*import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
//21.1: same simple name, neoforged package (the DatapackBuiltinEntriesProvider
//GT6DataGenerators fork lesson); the ctor grows the registries future.
*///?}

import gregtech6.registry.GT6Tools;

/**
 * The drop-conversion loot JSON producer — task p29-w5-t1-dig-six spec ①: ONE datagen
 * JSON per converting tool, over the platform {@code GlobalLootModifierProvider} (the
 * base class writes the per-modifier JSONs AND the platform index; the forge leg emits
 * {@code data/forge/loot_modifiers/global_loot_modifiers.json}, the 21.1 runtime reads
 * {@code data/neoforge/loot_modifiers/...}).
 *
 * <p><b>The twin index</b>: the shared generated tree (ADR-P17-1) is written by the
 * canonical 1.20.1-forge producer, while BOTH runtimes consume that one tree — so the
 * forge-leg run ALSO saves the {@code neoforge}-namespaced index (the SAME entries
 * list, byte-form identical: the directory name {@code loot_modifiers} is the same on
 * both legs, only the index namespace differs — LootModifierManager folder constant
 * verified on both jars). {@link #MODIFIER_NAMES} is the single source both the
 * {@code add()} calls and the twin builder walk, so the two indices cannot drift. The
 * 21.1 node's own runData emits its native index into the node-local verification tree.
 */
public class GT6ToolLootModifiersDatagen extends GlobalLootModifierProvider {

	/**
	 * The single source of the modifier set (the {@code add} names AND the twin index
	 * entries). ORDER: the {@code start()} CALL ORDER — the 21.1 base run() emits its
	 * native index in call order (the p29-w5-t2 correction of the t1/t4 "alphabetical /
	 * HashMap iteration" readings: t1's calls happened to BE alphabetical and t4's list
	 * matched its call order, both masking the real face), so the canonical twin must
	 * match it for the datagen_tree_check byte comparison; the forge base index keeps
	 * its own HashMap order (the declared forge-gated canonical-only face). A
	 * renamed/reordered entry drifts the twin and the gate flags it — re-derive the
	 * order from the node output after ANY name change.
	 */
	static final List<String> MODIFIER_NAMES = List.of(
			"construction_ender_chest",
			"spade_harvest",
			"universal_spade_openable",
			"axe_tree_fell",
			"club_rock_crush",
			"sword_harvest",
			"branch_cutter_leaves",
			"sense_vegetal");

	/** The ctor face of the platform output (the base field is private — kept for the twin path). */
	private final PackOutput mOutput;

	//? if forge {
	public GT6ToolLootModifiersDatagen(PackOutput aOutput) {
		super(aOutput, "gt6");
		mOutput = aOutput;
	}
	//?} else {
	/*public GT6ToolLootModifiersDatagen(PackOutput aOutput, CompletableFuture<net.minecraft.core.HolderLookup.Provider> aRegistries) {
		super(aOutput, aRegistries, "gt6");
		mOutput = aOutput;
	}
	//21.1: the registries future joined the ctor (javap 21.1.249); add() grew the varargs
	//conditions — the zero-arg call below stays source-compatible.
	*///?}

	@Override
	protected void start() {
		// the per-tool identity rides the gt6:holds_tool condition; the mode picks the arm.
		add("construction_ender_chest", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.PICKAXE_CONSTRUCTION.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.ENDER_CHEST_SELF));
		add("spade_harvest", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.SPADE.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.HARVESTABLE_SPADE));
		add("universal_spade_openable", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.UNIVERSAL_SPADE.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.UNBOXINATOR_OPEN));
		// task p29-w5-t2-blade-six — the three blade-conversion rows (the same per-tool
		// holds_tool gate; the felling row rides the gt6_tree_fell serializer, the two
		// pure rows ride gt6_tool_convert with the new modes)
		add("axe_tree_fell", new GT6TreeFellModifier(conditions(GT6Tools.AXE.get())));
		add("club_rock_crush", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.CLUB.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.CLUB_ROCK_CRUSH));
		add("sword_harvest", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.SWORD.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.SWORD_HARVEST));
		// task p29-w5-t4-field-five — the field-tool rows (the same per-tool gate shape)
		add("branch_cutter_leaves", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.BRANCH_CUTTER.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.BRANCHCUTTER_LEAVES));
		add("sense_vegetal", new GT6ToolLootModifiers.GT6ToolConvertModifier(
				conditions(GT6Tools.SENSE.get()),
				GT6ToolLootModifiers.GT6ToolConvertModifier.Mode.SENSE_VEGETAL));
	}

	private static LootItemCondition[] conditions(net.minecraft.world.item.Item aTool) {
		return new LootItemCondition[] { GT6ToolHoldsCondition.holdsTool(aTool).build() };
	}

	/**
	 * The canonical-producer twin: after the platform run wrote the forge index, write the
	 * {@code neoforge}-namespaced twin from {@link #MODIFIER_NAMES}. Forge-leg only — the
	 * 21.1 base run() is final (javap 21.1.249) and already emits the native index.
	 */
	//? if forge {
	@Override
	public CompletableFuture<?> run(CachedOutput aCache) {
		CompletableFuture<?> tBase = super.run(aCache);
		JsonObject tTwin = new JsonObject();
		tTwin.addProperty("replace", false);
		JsonArray tEntries = new JsonArray();
		for (String tName : MODIFIER_NAMES) {
			tEntries.add("gt6:" + tName);
		}
		tTwin.add("entries", tEntries);
		java.nio.file.Path tTwinPath = mOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
				.resolve("neoforge").resolve("loot_modifiers").resolve("global_loot_modifiers.json");
		return CompletableFuture.allOf(tBase, DataProvider.saveStable(aCache, tTwin, tTwinPath));
	}
	//?}
}
