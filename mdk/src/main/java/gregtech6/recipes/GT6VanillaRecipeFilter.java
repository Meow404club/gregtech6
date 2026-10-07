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

//? if forge {
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.world.item.crafting.Recipe;
//?} else {
/*import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

import com.google.common.collect.ImmutableSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Runtime removal of the six vanilla recipes upstream deletes at load time
 * (the 1.7.10 {@code CR.delate}/{@code CR.remove} face), ported as a
 * RecipeManager filter — plan (a) of the pool-drain removal ruling: upstream
 * semantics is "registered-then-removed", so the port lets vanilla load and
 * strips the six outputs from the live recipe graph.
 *
 * <p><b>The removal face</b> (whitelisted recipe ids, exact-match only — never a
 * predicate scan): {@code minecraft:cake} (upstream MultiItemFood.java:637
 * {@code CR.delate}, next to the :636 GT6 replacement cake recipe that this
 * removal makes room for), {@code minecraft:cookie} /
 * {@code minecraft:golden_apple} / {@code minecraft:golden_carrot} (upstream
 * Loader_Recipes_Vanilla.java:470-473 {@code CR.delate}) and
 * {@code minecraft:water_bucket} / {@code minecraft:milk_bucket} (upstream
 * Loader_Recipes_Food.java:651-652 {@code CR.remove}).
 *
 * <p><b>Declared id facts</b> (vanilla jars, both legs): recipe data ships
 * {@code cake/cookie/golden_apple/golden_carrot} (1.20.1
 * {@code data/minecraft/recipes/}, 1.21.1 {@code data/minecraft/recipe/}) and
 * ships NO recipe for {@code water_bucket}, {@code milk_bucket} or
 * {@code enchanted_golden_apple}. Consequences, per the card guard: the two
 * bucket rows sit in the table as pins of the upstream removal face but never
 * match (an exact-match no-op — upstream's output-based {@code CR.remove}
 * matched nothing vanilla either); the upstream enchanted-golden-apple row
 * (Loader_Recipes_Vanilla.java:472, meta 1) has no modern counterpart to
 * remove and is deliberately absent from the table.
 *
 * <p><b>The seam</b> (verified, not guessed): vanilla RecipeManager exposes a
 * live-view read and a public rebuild — 1.20.1 RecipeManager.java
 * {@code getRecipes()} :131-133 / {@code replaceRecipes(Iterable<Recipe<?>>)}
 * :147-162 (a duplicate id throws IllegalStateException :156-158 — this filter
 * only REMOVES, so it can never trip), and 21.1 (javap neoforge-21.1.249)
 * {@code getRecipes()} → {@code Collection<RecipeHolder<?>>} /
 * {@code replaceRecipes(Iterable<RecipeHolder<?>>)} with the id on the holder
 * ({@code RecipeHolder.id()} — 1.20.1's {@code Recipe.getId()} Recipe.java:54
 * is gone there, the ScrewdriverTest :412 precedent). Filtering is idempotent:
 * a second pass finds nothing to remove and rebuilds the same graph.
 *
 * <p><b>The timing hooks</b> (both legs verified, package-forked only):
 * <ul>
 * <li>{@code ServerStartedEvent} — the startup gate, fired once after the
 * datapack load finished and the server is about to tick (the
 * {@link GT6RecipeMaps} RegistrationFreezer precedent, GT6RecipeMaps.java:296-311;
 * {@code getServer()} from ServerLifecycleEvent, both legs).</li>
 * <li>{@code OnDatapackSyncEvent} — the /reload gate: "Fires when a player
 * joins the server or when the reload command is ran, before tags and crafting
 * recipes are sent to the client" (forge net.minecraftforge.event.OnDatapackSyncEvent.java:15-17;
 * neoforge-21.1.249-sources net/neoforged/neoforge/event/OnDatapackSyncEvent.java:15-17,
 * identical wording). Ordering proof, vanilla 1.20.1: MinecraftServer.reloadResources
 * :1296 swaps in the fresh RecipeManager {@code this.resources = $$1x} :1324
 * BEFORE calling {@code this.getPlayerList().reloadResources()} :1329, and
 * PlayerList.reloadResources :890-902 builds the sync packet from
 * {@code server.getRecipeManager().getRecipes()} :897 / sends :900 — the event
 * (patched in ahead of that read) therefore sees the post-apply manager on both
 * the reload and the join paths, and filtering inside it mutates exactly what
 * the server will use and the clients will receive.</li>
 * </ul>
 * Rejected candidates: {@code AddReloadListenerEvent} (the listener-registration
 * point BEFORE the reload applies — mutating there would be overwritten; forge-1.20.1
 * AddReloadListenerEvent.java:35-39) and {@code RecipesUpdatedEvent} (client-side
 * only, forge-1.20.1 RecipesUpdatedEvent.java:17-19 "only on the logical client").
 *
 * <p>KJS face: this card touches ONLY the vanilla-domain RecipeManager graph;
 * the GT6 RecipeMap runtime stays untouched and the KJS binding face is deferred
 * to the kjs-binding card (a KJS user-side removal of the same ids is orthogonal
 * — both directions are idempotent exact-id removals).
 */
public final class GT6VanillaRecipeFilter {

	/**
	 * The exact recipe-id removal set. The golden_apple pin covers the NORMAL
	 * apple only — the upstream enchanted row (Loader_Recipes_Vanilla.java:472)
	 * has no modern recipe (declared skip, see the class doc).
	 */
	public static final ImmutableSet<ResourceLocation> REMOVAL_IDS = ImmutableSet.of(
			//? if forge {
			ResourceLocation.fromNamespaceAndPath("minecraft", "cake"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "milk_bucket"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "cookie"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "golden_apple"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "golden_carrot")
		//?} else {
		/*ResourceLocation.fromNamespaceAndPath("minecraft", "cake"),
		ResourceLocation.fromNamespaceAndPath("minecraft", "water_bucket"),
		ResourceLocation.fromNamespaceAndPath("minecraft", "milk_bucket"),
		ResourceLocation.fromNamespaceAndPath("minecraft", "cookie"),
		ResourceLocation.fromNamespaceAndPath("minecraft", "golden_apple"),
		ResourceLocation.fromNamespaceAndPath("minecraft", "golden_carrot")
		*///?}
	);

	private GT6VanillaRecipeFilter() {}

	/** The whitelist membership test — the only gate, nothing outside {@link #REMOVAL_IDS} is ever touched. */
	public static boolean shouldRemove(ResourceLocation aId) {
		return REMOVAL_IDS.contains(aId);
	}

	/**
	 * Strips the whitelisted ids from the live RecipeManager graph: read the
	 * live view, drop the whitelisted members, rebuild. Only-removes (so the
	 * duplicate-id guard in replaceRecipes can never fire) and idempotent.
	 */
	//? if forge {
	public static void apply(RecipeManager aManager) {
		List<Recipe<?>> tKeep = aManager.getRecipes().stream()
				.filter(aRecipe -> !shouldRemove(aRecipe.getId()))
				.collect(Collectors.toList());
		aManager.replaceRecipes(tKeep);
	}
	//?} else {
	/*public static void apply(RecipeManager aManager) {
		List<RecipeHolder<?>> tKeep = aManager.getRecipes().stream()
				.filter(aHolder -> !shouldRemove(aHolder.id()))
				.collect(Collectors.toList());
		aManager.replaceRecipes(tKeep);
	}
	*///?}

	/**
	 * The dual-leg event subscriber (the {@link GT6RecipeMaps} RegistrationFreezer
	 * dialect): ServerStarted covers the boot load, OnDatapackSync covers every
	 * /reload (and re-filters on each join — idempotent, so the redundancy is free).
	 * Both handlers hit the SERVER-side manager (MinecraftServer.getRecipeManager()).
	 */
	//? if forge {
	@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "gt6", bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
	public static final class Subscriber {
		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent aEvent) {
			apply(aEvent.getServer().getRecipeManager());
		}

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void onDatapackSync(net.minecraftforge.event.OnDatapackSyncEvent aEvent) {
			apply(aEvent.getPlayerList().getServer().getRecipeManager());
		}
	}
	//?} else {
	/*@net.neoforged.fml.common.EventBusSubscriber(modid = "gt6") // the game bus, routed by event type (the RegistrationFreezer dialect)
	public static final class Subscriber {
		@net.neoforged.bus.api.SubscribeEvent
		public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent aEvent) {
			apply(aEvent.getServer().getRecipeManager());
		}

		@net.neoforged.bus.api.SubscribeEvent
		public static void onDatapackSync(net.neoforged.neoforge.event.OnDatapackSyncEvent aEvent) {
			apply(aEvent.getPlayerList().getServer().getRecipeManager());
		}
	}
	*///?}
}
