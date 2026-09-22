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
//? if kjs {
package gregtech6.integration.kjs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.RecipeMap;

/**
 * The bindings census pins (task p34-kjs-bindings acceptance ②): the exact binding
 * key set, the class-filter prefixes, the kubejs.plugins.txt resource registration
 * and the row-write seam's RECIPE_MAPS coverage census (the "schema count ==
 * RECIPE_MAPS count" pin restated for the bindings face, ruling 2026-09-22 —
 * state key tasks.p34-kjs-bindings.rewrite).
 *
 * <p>Optional-dependency semantics: every test ASSUMES the KubeJS plugin class is
 * loadable and SKIPS otherwise (acceptance ② "skip 而非 fail") — the module is
 * compile-optional, so a classpath without KubeJS must degrade to skips, never
 * failures. (The chisel {@code //? if kjs} wrap is the compile-time half of the
 * same semantics: with the kjs const false this file compiles to nothing.)
 */
class GT6KJSBindingsCensusTest {

	/** The phase-gate generation discipline: every test starts and leaves a clean registry. */
	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset();
	}

	@AfterEach
	void dropGeneration() {
		GT6RecipeMaps.reset();
	}

	private static void assumeKubeJSOnClasspath() {
		// The optional-dependency guard: KubeJS absent from the test classpath = the
		// plugin-class face is inert — that pin SKIPS instead of failing (acceptance ②).
		try {
			Class.forName("dev.latvian.mods.kubejs.KubeJSPlugin");
		} catch (ClassNotFoundException tAbsent) {
			assumeTrue(false, "KubeJS not on the test classpath — optional-dependency skip");
		}
	}

	@Test
	void bindingKeyCensus() {
		// No kubejs assume here by design: the census drives the kubejs-free GT6KJS
		// core, so it RUNS on every classpath (stricter than skipping).
		Map<String, Object> tBindings = GT6KJS.bindingClasses();
		Set<String> tExpected = Set.of(
				"GT6Recipes", "GT6RecipeMaps", "RecipeMap",
				"MT", "GTMaterialItems", "GTMaterialBlocks",
				"GT6Bumbles", "GT6Kitchen", "GT6Distillation", "GT6Anvils", "GT6Sensors",
				"GTMachines");
		assertEquals(tExpected, tBindings.keySet(), "the binding key census — additions are conscious script-surface growth");
		// The RM trio bound by identity (Class literals, no clinit side effects).
		assertEquals(GT6RecipeMaps.class, tBindings.get("GT6RecipeMaps"));
		assertEquals(RecipeMap.class, tBindings.get("RecipeMap"));
		// The facade is the one INSTANCE binding (its row-builder methods are instance-fluent).
		assertTrue(tBindings.get("GT6Recipes") instanceof GT6Recipes, "GT6Recipes binds as a live facade instance");
	}

	@Test
	void classFilterAllowsThePortTrees() {
		Set<String> tPrefixes = new HashSet<>(java.util.Arrays.asList(GT6KJS.classFilterPrefixes()));
		assertTrue(tPrefixes.contains("gregtech6"), "scripts may import the port tree");
		assertTrue(tPrefixes.contains("gregapi"), "scripts may import gregapi (MT/OM/IL faces)");
	}

	@Test
	void pluginsTxtRegistersThePluginClass() throws Exception {
		// The resource-face pin runs unconditionally (the file is always on the main
		// resources); the loadability half honors the optional-dependency skip.
		try (java.io.InputStream tStream = getClass().getResourceAsStream("/kubejs.plugins.txt")) {
			assumeTrue(tStream != null, "kubejs.plugins.txt on the resource root");
			String tContent = new String(tStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			String tFqcn = tContent.lines()
					.map(s -> s.split("#", 2)[0].trim()) // the KubeJSPlugins.loadFromFile line grammar
					.filter(s -> !s.isBlank())
					.findFirst().orElse("");
			assertEquals("gregtech6.integration.kjs.GT6KubeJSPlugin", tFqcn, "the resource-root registration");
			assumeKubeJSOnClasspath(); // loadable only when the compile-optional dep is present
			Class.forName(tFqcn); // the registered FQCN must be the loadable plugin class
		}
	}

	@Test
	void rowWriteSeamCoversTheWholeRecipeMapRegistry() {
		// The census pin of the rewritten spec: the seam keys EVERY map generically —
		// recipeMapTargets() is exactly the live RECIPE_MAPS key set (no per-map
		// handwriting, nothing missed). A fixture map guarantees the pin is non-vacuous
		// even in a bare-JVM generation where no production pour has run.
		RecipeMap tFixture = new RecipeMap(new java.util.HashSet<>(), "gt.recipe.kjs.census.fixture", "KJS Census", null,
				0, 1, "gt6:textures/gui/machines/fixture", 1, 1, 0, 0, 0, 0, 0, 1);
		assertEquals(1, RecipeMap.RECIPE_MAPS.size(), "fixture-only generation");
		assertEquals(new HashSet<>(RecipeMap.RECIPE_MAPS.keySet()), new HashSet<>(GT6KJS.recipeMapTargets()),
				"the row-write seam covers exactly the registry");
		assertTrue(GT6KJS.recipeMapTargets().contains("gt.recipe.kjs.census.fixture"));
		assertEquals(tFixture, GT6KJS.map("gt.recipe.kjs.census.fixture"));
	}
}
//?}
