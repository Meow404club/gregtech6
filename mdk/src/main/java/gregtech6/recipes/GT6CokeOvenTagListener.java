package gregtech6.recipes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.tags.ITag;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;

/**
 * The FORGE-bus listener that rebuilds the Coke Oven log-recipe subset whenever the tag
 * data loads (the 2026-08-30 coordinator amendment: the upstream OreDict:205 / Woods:165
 * dynamic registration surface maps onto the 1.20.1 tag system, #minecraft:logs keeps the
 * mod-adaptation face a static table cannot).
 *
 * <p><b>Hook</b> (forge-1.20.1 TagsUpdatedEvent.java:14-67, read-verified): the event fires
 * on the MAIN bus from the Forge TagManager patch after (re)loading static data;
 * {@code shouldUpdateStaticData()} (:47) is true only for real data (re)loads — the
 * single-player client-packet replay path ({@code getUpdateCause() == CLIENT_PACKET_RECEIVED}
 * :38) fires it too, and without the gate the subset would rebuild twice per world join.
 *
 * <p><b>Tag content</b>: read through {@code ForgeRegistries.ITEMS.getTag(ItemTags.LOGS)}
 * (ITagManager javadoc: "should be preferred to any Holder-related methods"). The ITag
 * instance is stable across rebinds ("safe to store for long periods", ITag.java javadoc)
 * and is bound BEFORE the event fires — the event is the documented refresh hook.
 *
 * <p><b>Idempotence</b>: the tag-derived recipes are a tracked subset — rebuild = remove
 * the previous subset from {@code COKE_OVEN.mRecipeList} then add the fresh expansion
 * (identity-based HashSet removal; {@link Recipe} carries no equals/hashCode, so the
 * removed instances are exactly the previously added ones). Repeated tag updates never
 * stack. The P4 Recipe/RecipeMap shells stay untouched (the coordinator ruling #4) —
 * the subset bookkeeping lives entirely here.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GT6CokeOvenTagListener {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The currently registered tag-derived subset (identity-tracked for the replace). */
	private static List<Recipe> sLogRecipes = List.of();

	@SubscribeEvent
	public static void onTagsUpdated(TagsUpdatedEvent aEvent) {
		if (!aEvent.shouldUpdateStaticData()) return; // :47 — skip the client-packet replay path
		rebuild();
	}

	/** The rebuild: tag content → expansion → subset replace. Also the offline test entry. */
	public static void rebuild() {
		RecipeMap tMap = GT6RecipeMaps.COKE_OVEN;
		if (tMap == null) return; // no map generation — nothing to pour into (a broken lifecycle)

		// the tag content: the log universe (vanilla + any mod that appends to #minecraft:logs)
		List<Item> tLogs = new ArrayList<>();
		net.minecraftforge.registries.tags.ITagManager<Item> tTagManager = ForgeRegistries.ITEMS.tags(); // IForgeRegistry.java:78
		if (tTagManager != null) {
			ITag<Item> tTag = tTagManager.getTag(ItemTags.LOGS);
			if (tTag.isBound()) for (Item tItem : tTag) tLogs.add(tItem); // ITag<V> iterates the VALUES (ITag.java:20)
		}

		// the expansion inputs: the charcoal gem (MT.Charcoal, OP.gem) + gt6:creosote
		Item tCharcoal = resolve(new GTMaterialItems.PrefixMaterial(OP.gem, MT.Charcoal));
		Fluid tCreosote = ForgeRegistries.FLUIDS.getValue(GTFluids.CREOSOTE.getId());

		replaceLogRecipes(GT6CokeOvenLogExpansion.expand(tLogs, tCharcoal, tCreosote));
		LOGGER.info("GT6 Coke Oven log recipes rebuilt from {}: {} recipes", ItemTags.LOGS.location(), sLogRecipes.size());
	}

	/**
	 * The subset replace: remove the previous tag-derived instances, register the fresh
	 * expansion. Identity-based (Recipe has identity semantics in the HashSet), so repeated
	 * rebuilds are stable — the idempotence the offline test pins.
	 */
	static void replaceLogRecipes(List<Recipe> aNewRecipes) {
		RecipeMap tMap = GT6RecipeMaps.COKE_OVEN;
		if (tMap == null) return;
		tMap.mRecipeList.removeAll(sLogRecipes);
		sLogRecipes = aNewRecipes;
		tMap.mRecipeList.addAll(sLogRecipes);
	}

	/** The current subset size (the audit/acceptance read). */
	public static int logRecipeCount() {
		return sLogRecipes.size();
	}

	@Nullable
	private static Item resolve(GTMaterialItems.PrefixMaterial aPair) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPair.prefix(), aPair.material());
		return tHandle == null ? null : tHandle.get();
	}
}
