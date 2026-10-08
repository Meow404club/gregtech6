/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
//? if forge {
// (1.20.1: no RecipeHolder — the id rides Recipe.getId() directly.)
//?} else {
/*import net.minecraft.world.item.crafting.RecipeHolder;
 *///?}

import gregapi.oredict.OreDictItemData;
import gregtech6.components.OM;

/**
 * The C-leg reload derivation engine (ADR docs/adr/2026-10-06-components-subsystem.md §1
 * leg C — "the ONLY volume strategy"): the Modern replay of the upstream CR.shaped REV face.
 * Upstream attached component data to a recipe's output at REGISTRATION time
 * (CR.java:405-410: the per-shape-cell input datas {@code OM.data_(:386)} /
 * {@code getAutomaticItemData(:395)}, aggregated by the OreDictItemData aggregation
 * constructor and written through {@code OM.data(aResult, …) → addItemData_}); the machine
 * registrations rode it wholesale (CR.DEF_REV_NCC, MultiTileEntityRegistry.java:208). The
 * port's recipes are datapack JSON, so the same derivation runs over the LIVE vanilla
 * RecipeManager graph at the dual-gate timing the GT6VanillaRecipeFilter proved
 * (GT6VanillaRecipeFilter.java:77-99): ServerStartedEvent (the boot load) and
 * OnDatapackSyncEvent (every /reload and join — the event sees the POST-APPLY manager,
 * MinecraftServer.reloadResources swaps the fresh manager in BEFORE the sync read).
 * {@link net.minecraftforge.event.AddReloadListenerEvent} stays rejected there too (the
 * listener-registration point BEFORE the reload applies — writes would be overwritten).
 *
 * <p><b>The REV opt-in carrier</b> (ADR §1 leg C): upstream REV was a per-call bit
 * (CR.java:135 "Reverses the Output of the Recipe for smelting and pulverising", :134; the
 * vanilla-domain callers were explicit DEF_REV rows too, Loader_Recipes_Vanilla.java:413-416);
 * CR.DEF (:161) = BUF|NO_REM carries NO REV, and shapeless defaulted the same
 * (CR.java:454 — DEF, never REV). The port carrier is the NAMESPACE default plus the
 * derivation manifest: a {@code gt6:} shaped recipe derives (the GT6-authored universe), a
 * {@code minecraft:} (or other-namespace) recipe derives only when the manifest FORCES its id,
 * and any id the manifest SUPPRESSES never derives. shapeless v1 does not derive (the upstream
 * shapeless default had no REV). The census of the over-derivation this buys (gt6 namespace
 * default vs the upstream call sites that lacked REV) is DECLARED and pinned in
 * GT6ComponentDeriverTest.
 *
 * <p><b>The write face</b> is the central component face, upstream verbatim:
 * {@link OM#addItemData_} — write only into the ABSENCE of data (upstream OM.java:113-115),
 * so explicit declarations always beat derivation, exactly as upstream's registration-order
 * add-only race did; the central face's stack-size amortization (:653-657) divides by the
 * output stack count and the recyclable notification gate (:666-670) fires on every real
 * write. The engine passes the NATURAL-count result stack (upstream :409 passed
 * {@code aResult} with its own count).
 *
 * <p><b>/reload idempotency</b> (ADR red line 3: "derived 集清除重建", the p32 lesson): the
 * engine tracks the derived set it actually wrote (the (item, damage) key → data) and
 * RECONCILES instead of blind clear+rebuild — keys whose derivation vanished or CHANGED are
 * retired through {@link OM#removeItemData} (the removal face this task added to the central
 * face: map entry + the identity-held recyclable registrations) and rewritten; unchanged keys
 * are skipped WITHOUT re-writing, so no duplicate recyclable registration is ever fired for
 * static content (the container has no equals — a blind rebuild would log N copies per
 * reload). Keys the add-only gate refused (explicit declarations) are never tracked, so
 * remove can never retire another author's data. Observable contract: two identical reloads
 * leave the OM map and the recyclable registration log byte-identical.
 *
 * <p><b>Declared deviations</b> (all pinned in the test):
 * <ul>
 * <li>Input cells resolve through {@link OM#anydata_} (all arms). Upstream :386 read
 *     {@code OM.data_} — the map arms only — but upstream's map was REGISTRATION-FED: every
 *     GT prefix item was in it (OreDictManager.onOreRegistration2 :391-465). The port's
 *     isomorph of that fill is the read-time self-description arm (the central-face class
 *     doc), which lives behind the override gate — hence the override read. The damageable
 *     proportionality arm (:702-708) rides along behind the same gate; it is DORMANT for
 *     derivation (the flag has no port writer yet, no recipe-input item declares
 *     mUseVanillaDamage).</li>
 * <li>A tag cell resolves to its FIRST resolvable member's data (the modern form of upstream
 *     :395 {@code getAutomaticItemData(oredictName)} — the oredict name IS the tag id, its
 *     members the former registration stream; ADR §1 leg C). Live, the members come from
 *     {@link Ingredient#getItems()} (the tag manager answers); offline (tests) the tag
 *     manager never boots and every tag cell reads empty, so {@link #sCellMembers} carries
 *     the injected membership stub (the OM.sStackTags / GT6RecipeTagFallbackTest.java:53
 *     seam discipline — the production binding answers empty and the seam is a no-op).</li>
 * <li>Tag-family-less GT prefixes resolve through the provider arm above; non-material
 *     inputs (tools, chests — no oredict data upstream either) resolve null and contribute
 *     nothing, upstream verbatim (the hopper row's hammer/wrench cells).</li>
 * <li>The {@code gt6:material_tool} serializer recipes and other custom serializers are not
 *     {@link ShapedRecipe} instances and never derive (12,959 rows in the shipped tree;
 *     upstream tool recipes were CR.shaped rows whose REV aggregation ran through the same
 *     face — DECLARED skip, the serializer face owns them).</li>
 * </ul>
 *
 * <p>KJS face: the whole input domain here is the DATAPACK domain (the vanilla RecipeManager
 * JSON graph + the datapack manifest {@code data/gt6/components/derivation.json}) — datapack
 * edits are天然 reload-rederived, which IS the KJS recipe-editing payoff (edit JSON, /reload,
 * the component index follows); no binding seam is owed beyond that. The registration face
 * (central write API) and the RM runtime graph stay deferred to the kjs-binding card (ADR
 * red line 5).
 */
public final class GT6ComponentDeriver {

	private GT6ComponentDeriver() {}

	/** The manifest file the derivation gates read (the datapack-domain face, datagen-produced
	 * by GT6ComponentDerivationManifest; absent file = empty gates). */
	/*package*/ static final String MANIFEST_PATH = "components/derivation.json";

	/**
	 * The offline tag-cell seam: production-bound to EMPTY — a live server's
	 * {@link Ingredient#getItems()} already enumerates tag members (the tag manager answers),
	 * so the production face never consults this; the offline JVM's tag manager never boots
	 * and every tag cell reads empty, so the tests inject the members (the
	 * GT6RecipeTagFallbackTest.java:53 seam discipline).
	 */
	public static Function<Ingredient, Collection<ItemStack>> sCellMembers = aCell -> List.of();

	/** The derived set this engine authored: the (item, damage) key → what it wrote — the
	 * upstream ItemStackContainer :103 key shape (the central face stores under the same
	 * pair), NOT the ItemStack itself: forge-patched ItemStack equality compares capability
	 * dispatchers, which are not content-stable across offline-fabricated copies (two
	 * equal-content result stacks answered equals=false in the idempotency pin), and a
	 * false-unequal key would retire+rewrite a LIVE derivation every /reload. The reconcile
	 * bookkeeping (the class doc); test-visible for the state hygiene. */
	static Map<Key, Derived> sDerived = new LinkedHashMap<>();

	/** The derivation key: (item, damage), the OM.StackKey shape (central-face private, so a
	 * local record here — same fields, same upstream :103 citation). */
	record Key(Item item, int damage) {}

	/** One derived row: the natural-count stack the write went through (the central face's
	 * amortization divides by ITS count, upstream :653-657) and the data instance authored. */
	record Derived(ItemStack writeStack, OreDictItemData data) {}

	/**
	 * One derivation pass over the live graph: collect what the current recipes derive,
	 * then reconcile the previous derived set against it. Order-stable (the RecipeManager's
	 * own iteration order, stable across reloads of an unchanged pack); a duplicate output
	 * keeps the FIRST derivation (the add-only face, upstream parity).
	 */
	public static void apply(RecipeManager aManager, Manifest aManifest, net.minecraft.core.RegistryAccess aAccess) {
		Map<Key, Derived> tFresh = new LinkedHashMap<>();
		//? if forge {
		for (Recipe<?> tRecipe : aManager.getRecipes()) collect(tRecipe.getId(), tRecipe, aManifest, aAccess, tFresh);
		//?} else {
		/*for (RecipeHolder<?> tHolder : aManager.getRecipes()) collect(tHolder.id(), tHolder.value(), aManifest, aAccess, tFresh);
		 *///?}
		reconcile(tFresh);
	}

	/** The upstream CR.java:368-410 walk, one recipe. */
	private static void collect(ResourceLocation aId, Recipe<?> aRecipe, Manifest aManifest, net.minecraft.core.RegistryAccess aAccess, Map<Key, Derived> aFresh) {
		if (!(aRecipe instanceof ShapedRecipe tShaped)) return; // shapeless v1: no derivation (CR.java:454 — the DEF default carries no REV)
		if (!aManifest.shouldDerive(aId)) return;
		ItemStack tResult = tShaped.getResultItem(aAccess);
		if (tResult == null || tResult.isEmpty()) return;
		// the per-cell datas (upstream :406-408: tData[shape cell] = tItemDataMap.get(chr))
		List<Ingredient> tCells = tShaped.getIngredients();
		OreDictItemData[] tData = new OreDictItemData[tCells.size()];
		int x = -1;
		for (Ingredient tCell : tCells) tData[++x] = resolveCell(tCell);
		if (!containsSomething(tData)) return; // upstream :409 gate (UT.Code.containsSomething :1327-1330)
		aFresh.putIfAbsent(new Key(tResult.getItem(), tResult.getDamageValue()), new Derived(tResult, new OreDictItemData(tData))); // first wins
	}

	/**
	 * One cell → its component data: the first resolvable member. Item cells carry their
	 * stack directly; tag cells enumerate their members live ({@link Ingredient#getItems()},
	 * offline via {@link #sCellMembers}). The member's data consult is {@link OM#anydata_}
	 * — the self-description arm stands in for the 1.7.10 registration-filled map (the class
	 * doc, deviation ①).
	 */
	static OreDictItemData resolveCell(Ingredient aCell) {
		// both legs' vanilla Ingredient.getItems() returns ItemStack[] (1.20.1 Ingredient.java / 21.1 same shape)
		Collection<ItemStack> tMembers = java.util.Arrays.asList(aCell.getItems());
		if (tMembers.isEmpty()) tMembers = sCellMembers.apply(aCell);
		for (ItemStack tStack : tMembers) {
			OreDictItemData tData = OM.anydata_(tStack);
			if (tData != null) return tData;
		}
		return null;
	}

	/** Upstream UT.Code.containsSomething (UT.java:1327-1330): any non-null element. */
	private static boolean containsSomething(OreDictItemData[] aData) {
		for (OreDictItemData tCell : aData) if (tCell != null) return true;
		return false;
	}

	/**
	 * The ADR red-line-3 "/reload 重入幂等" duty, in its reconcile form: retire every derived
	 * key the current graph no longer produces or produces DIFFERENTLY (through
	 * {@link OM#removeItemData} — its own authorship is proven by this very map), then write
	 * the new/changed rows through the add-only face. Unchanged rows touch nothing — no
	 * re-write, no duplicate recyclable registration (the class doc).
	 */
	private static void reconcile(Map<Key, Derived> aFresh) {
		for (var tIterator = sDerived.entrySet().iterator(); tIterator.hasNext();) {
			var tPrevious = tIterator.next();
			Derived tCurrent = aFresh.get(tPrevious.getKey());
			if (tCurrent == null || differs(tCurrent.data(), tPrevious.getValue().data())) {
				OM.removeItemData(tPrevious.getValue().writeStack());
				tIterator.remove();
			}
		}
		for (var tEntry : aFresh.entrySet()) {
			if (sDerived.containsKey(tEntry.getKey())) continue; // unchanged, already live — zero mutations
			Derived tDerived = tEntry.getValue();
			if (OM.addItemData_(tDerived.writeStack(), tDerived.data())) sDerived.put(tEntry.getKey(), tDerived);
			// refused = an explicit declaration lives there (upstream add-only parity); NOT
			// tracked, so reconcile can never retire another author's data.
		}
	}

	/** The structural content compare (OreDictItemData has no equals — toString is the
	 * oredict name only): prefix, main material identity+amount, byproducts pairwise. */
	static boolean differs(OreDictItemData aNew, OreDictItemData aOld) {
		if (aNew.mPrefix != aOld.mPrefix) return true;
		if (aNew.mMaterial == null ? aOld.mMaterial != null : aOld.mMaterial == null
				|| aNew.mMaterial.mMaterial != aOld.mMaterial.mMaterial
				|| aNew.mMaterial.mAmount != aOld.mMaterial.mAmount) return true;
		if (aNew.mByProducts == null ? aOld.mByProducts != null : aOld.mByProducts == null
				|| aNew.mByProducts.length != aOld.mByProducts.length) return true;
		if (aNew.mByProducts != null) for (int i = 0; i < aNew.mByProducts.length; i++) {
			if (aNew.mByProducts[i].mMaterial != aOld.mByProducts[i].mMaterial
					|| aNew.mByProducts[i].mAmount != aOld.mByProducts[i].mAmount) return true;
		}
		return false;
	}

	// --------------------------------------------------------------------------
	// the derivation manifest (the datapack-domain gate file)
	// --------------------------------------------------------------------------

	/**
	 * The recipe-id gates: {@code suppress} wins over everything, then the gt6 namespace
	 * default derives, then a manifest {@code force} id derives, everything else declines.
	 * Datapack-domain (ADR red line 5): the file is plain JSON any pack can ship or override.
	 */
	public record Manifest(Set<ResourceLocation> force, Set<ResourceLocation> suppress) {

		public static final Manifest EMPTY = new Manifest(Set.of(), Set.of());

		public boolean shouldDerive(ResourceLocation aId) {
			if (suppress.contains(aId)) return false;
			return "gt6".equals(aId.getNamespace()) || force.contains(aId);
		}

		/** The production load: gt6:components/derivation.json off the live resource manager
		 * (post-apply at both gates). A missing file = EMPTY; a broken file logs and degrades
		 * to EMPTY — the manifest is an opt-in knob, not a load-bearing input. */
		public static Manifest load(ResourceManager aManager) {
			try {
				//? if forge {
				var tResource = aManager.getResource(new ResourceLocation("gt6", MANIFEST_PATH));
				//?} else {
				/*var tResource = aManager.getResource(ResourceLocation.fromNamespaceAndPath("gt6", MANIFEST_PATH));
				 *///?}
				if (tResource.isEmpty()) return EMPTY;
				try (var tStream = tResource.get().open()) {
					JsonObject tJson = JsonParser.parseReader(new java.io.InputStreamReader(tStream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
					return new Manifest(parseIds(tJson.get("force")), parseIds(tJson.get("suppress")));
				}
			} catch (Throwable tError) {
				org.slf4j.LoggerFactory.getLogger("gt6").warn("GT6ComponentDeriver: unreadable derivation manifest, deriving ungated", tError);
				return EMPTY;
			}
		}

		private static Set<ResourceLocation> parseIds(JsonElement aElement) throws java.io.IOException {
			if (!(aElement instanceof JsonArray tArray)) return Set.of();
			var rIds = new java.util.LinkedHashSet<ResourceLocation>(tArray.size());
			for (JsonElement tEntry : tArray) {
				//? if forge {
				rIds.add(new ResourceLocation(tEntry.getAsString()));
				//?} else {
				/*rIds.add(ResourceLocation.parse(tEntry.getAsString())); // the full "gt6:foo" id string parses itself
				 *///?}
			}
			return rIds;
		}
	}

	// --------------------------------------------------------------------------
	// the dual-gate subscriber (the GT6VanillaRecipeFilter dialect, verbatim timing)
	// --------------------------------------------------------------------------

	/**
	 * The timing hooks, byte-for-byte the proven filter dialect: ServerStarted covers the
	 * boot load, OnDatapackSync covers every /reload and join (the post-apply manager; the
	 * ordering proof lives on {@link GT6VanillaRecipeFilter}'s class doc). Both handlers hit
	 * the SERVER-side manager and carry the server's registries for the result decode.
	 */
	//? if forge {
	@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Subscriber {
		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent aEvent) {
			derive(aEvent.getServer());
		}

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent aEvent) {
			derive(aEvent.getPlayerList().getServer());
		}
	}
	//?} else {
	/*@net.neoforged.fml.common.EventBusSubscriber(modid = "gt6") // the game bus, routed by event type (the RegistrationFreezer dialect)
	public static final class Subscriber {
		@net.neoforged.bus.api.SubscribeEvent
		public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent aEvent) {
			derive(aEvent.getServer());
		}

		@net.neoforged.bus.api.SubscribeEvent
		public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent aEvent) {
			derive(aEvent.getPlayerList().getServer());
		}
	}
	*///?}

	private static void derive(net.minecraft.server.MinecraftServer aServer) {
		apply(aServer.getRecipeManager(), Manifest.load(aServer.getResourceManager()), aServer.registryAccess());
	}
}
