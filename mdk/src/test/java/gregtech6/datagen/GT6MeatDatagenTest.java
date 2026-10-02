/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * The meat smelting datagen pins (task food-meat-recipes) — the SECOND layer of the
 * DECLARED bug-transcription note (the first lives on the GT6MeatDatagen class doc):
 *
 * <p><b>DECLARED — MultiItemFood.java:574</b> is a character-for-character duplicate of
 * the dogmeat smelting line :560, sitting after the MULE registration pair (:572-:573);
 * the argument should have been {@code Food_Mule_Raw → Food_Mule_Cooked}. The port
 * transcribes the line VERBATIM (dogmeat_raw → dogmeat_cooked, 照灌禁修正), so mule meat
 * keeps its upstream-native ABSENCE of a smelting path. Correcting it = a content change
 * requiring an explicit upstream-fix/content decision card — this test FAILS if anyone
 * "fixes" the row or adds the mule row without such a card.
 */
public class GT6MeatDatagenTest {

	/** One committed generated-tree JSON as an object (the GT6CupDatagenTest classpath face). */
	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6MeatDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** The :574 verbatim transcription — dogmeat_raw → dogmeat_cooked, the standard 200t/0xp face. */
	@Test
	void theDeclaredDogmeatLineTranscribesVerbatim() throws Exception {
		JsonObject tSmelt = generatedJson("data/gt6/recipes/smelt_food_dogmeat.json");
		assertEquals("minecraft:smelting", tSmelt.get("type").getAsString(), "the smelting type");
		assertEquals("gt6:food_dogmeat_raw", tSmelt.getAsJsonObject("ingredient").get("item").getAsString(),
				"the :574 input — Food_DogMeat_Raw VERBATIM (the :560 duplicate, do NOT fix to mule)");
		assertEquals("gt6:food_dogmeat_cooked", tSmelt.get("result").getAsString(),
				"the :574 output — Food_DogMeat_Cooked (1.20.1 cooking result = the bare id string)");
		assertEquals(200, tSmelt.get("cookingtime").getAsInt(), "the standard 200t face");
		assertEquals(0.0F, tSmelt.get("experience").getAsFloat(), "the standard 0xp face");
	}

	/** The absence face: the generated tree carries NO mule smelting JSON (the upstream-native state). */
	@Test
	void noMuleSmeltingRowExists() throws Exception {
		Path tRecipes = null;
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			Path tCandidate = p.resolve("mdk/src/generated/resources/data/gt6/recipes");
			if (Files.isDirectory(tCandidate)) {tRecipes = tCandidate; break;}
		}
		assertNotNull(tRecipes, "the generated recipes dir not found upward");
		try (Stream<Path> tWalk = Files.list(tRecipes)) {
			long tMule = tWalk.map(tPath -> tPath.getFileName().toString())
					.filter(tName -> tName.contains("mule") && tName.contains("smelt")).count();
			assertEquals(0, tMule, "no mule smelting row — MultiItemFood.java:574 gave mule meat NO smelting path; "
					+ "adding one requires an explicit upstream-fix/content decision card");
		}
	}
}
