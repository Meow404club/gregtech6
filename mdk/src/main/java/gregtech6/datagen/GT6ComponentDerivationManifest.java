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

package gregtech6.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * The derivation manifest emission (task component-derivation-reload): the C-leg deriver's
 * datapack-domain gate file, {@code data/gt6/components/derivation.json} —
 * {@code force}/{@code suppress} recipe-id lists (GT6ComponentDeriver.Manifest). v1 ships the
 * MECHANISM with empty gates (the gt6-namespace default needs no entries); a datapack edit of
 * this file re-gates the derivation on the next /reload — that is the whole point of it being
 * a datapack file (the ADR red line 5: "derivation.json manifest=datapack 域天然可改零适配").
 * The rows are NOT a hand-written component table (red line 1): they gate per-RECIPE ids, and
 * the component data itself stays reload-derived from the runtime recipe graph.
 *
 * <p>Both legs run this provider and emit through the shared {@link GT6BiomeModifierConditions
 * #serializeCanonical}/{@link #saveCanonical} canonical byte shape, so the two trees land
 * byte-identical (the datagen_tree_check gate).
 */
public class GT6ComponentDerivationManifest implements DataProvider {

	private final PackOutput mOutput;

	public GT6ComponentDerivationManifest(PackOutput aOutput) {
		mOutput = aOutput;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput aCache) {
		JsonObject rManifest = new JsonObject();
		rManifest.add("force", new com.google.gson.JsonArray());
		rManifest.add("suppress", new com.google.gson.JsonArray());
		return GT6BiomeModifierConditions.saveCanonical(aCache, rManifest,
				mOutput.getOutputFolder().resolve("data/gt6/components/derivation.json"));
	}

	@Override
	public String getName() {
		return "GT6 Component Derivation Manifest (the C-leg reload gate file)";
	}
}
