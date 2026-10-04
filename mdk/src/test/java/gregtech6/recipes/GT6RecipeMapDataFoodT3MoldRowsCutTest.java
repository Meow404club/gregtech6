package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * The food T3 tail ledger (task food-t3-mold-rows): the five upstream {@code RM.Press}
 * statements of {@code Loader_Recipes_Food.java:146-152} (the foodDough listener rows
 * :146-:149 and the foodSugarDough listener row :152) are POURED-ELSEWHERE — the
 * {@link GT6RecipesBake} runtime band transcribed them (GT6RecipesBake.java:274-291,
 * task food-bake-recipes: the four dough rows plus the :152 statement expanded over the
 * three {@code foodSugarDough} members = 7 rows, GT6RecipesBakeTest pins the count and
 * the verbatim legs) — so the press.json datapack face stays ROW-FREE. The per-row
 * ledger, all five CUT from the JSON face:
 * <ul>
 * <li>:146 — dough x1 + Shape_Foodmold_Bun → Food_Bun_Raw, 16t — CUT</li>
 * <li>:147 — dough x2 + Shape_Foodmold_Bread → Food_Bread_Raw, 32t — CUT</li>
 * <li>:148 — dough x3 + Shape_Foodmold_Baguette → Food_Baguette_Raw, 48t — CUT</li>
 * <li>:149 — dough x4 + Shape_Foodmold_Toast → Food_Toast_Raw, 64t — CUT</li>
 * <li>:152 — sugar-dough x4 + Shape_Foodmold_Cylinder → Food_CakeBottom_Raw, 64t — CUT
 *     (three members, all in the java band)</li>
 * </ul>
 * The shared reason is twofold: the JSON v1 row schema carries no per-row notConsumed
 * face (GT6RecipeMapJsonLoader.java:142-143) while the upstream rows keep the mold as a
 * not-consumed shaping tool (the {@code Recipe.sNotConsumable} fifth arm,
 * {@code GT6RecipesBake.isFoodShapingTool}) — a JSON row would EAT the mold on every
 * press; and the loader's three-owner co-existence has no content-level dedup
 * (GT6RecipeMapJsonLoader.java:84-90), so a JSON twin of the java rows would
 * double-register. The press.json header carries the ownership declaration — this test
 * retires the expired B1-era pool snapshot (the pool-account lesson: a POOLED clause is
 * a snapshot of its writing day, re-audit it against the merge history before pouring).
 */
public class GT6RecipeMapDataFoodT3MoldRowsCutTest {

	/** Reads the shipped press.json text (the test classpath = the main resources). */
	private static String shippedPressJson() throws Exception {
		try (InputStream tStream = GT6RecipeMapDataFoodT3MoldRowsCutTest.class.getResourceAsStream("/data/gt6/recipe_maps/press.json")) {
			assertNotNull(tStream, "the shipped press.json rides the test classpath");
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * The CUT pin: press.json carries zero food-chain rows — no {@code Shape_Foodmold}
	 * leg and no {@code gt6:food_*} item id anywhere in the row stock (the lamp/TNT
	 * stock of the B1 pour stays; a food row here is a duplicate of the java band).
	 */
	@Test
	public void thePressJsonFaceStaysFreeOfTheFoodMoldRows() throws Exception {
		JsonElement tRoot = JsonParser.parseString(shippedPressJson());
		assertTrue(tRoot.isJsonObject(), "the object-shaped file (the vanilla datapack convention)");
		int tRows = 0;
		for (JsonElement tRow : tRoot.getAsJsonObject().getAsJsonArray("recipes")) {
			tRows++;
			String tRowText = tRow.toString();
			assertFalse(tRowText.contains("shape_foodmold"), "no food-mold input leg on row " + (tRows - 1));
			assertFalse(tRowText.contains("gt6:food_"), "no gt6:food_* item leg on row " + (tRows - 1));
		}
		assertTrue(tRows > 0, "the press face still carries its lamp/TNT stock");
	}

	/**
	 * The header declaration: the POOLED snapshot names its owner (the stale "the
	 * food-mold event rows" pool clause of the B1 era is retired — the pool account
	 * expired when food-bake-recipes merged).
	 */
	@Test
	public void theHeaderDeclaresTheJavaBandOwner() throws Exception {
		JsonElement tComment = JsonParser.parseString(shippedPressJson()).getAsJsonObject().get("comment");
		assertNotNull(tComment, "the header comment key survives");
		String tText = tComment.getAsString();
		assertTrue(tText.contains("GT6RecipesBake.java:274-291"), "the ownership pointer into the runtime band");
		assertTrue(tText.contains("POURED-ELSEWHERE"), "the ledger verdict, not a pending POOL");
		assertFalse(tText.contains("the food-mold event rows (Loader_Recipes_Food.java:146-152) and the remaining"), "the stale B1-era pool clause is retired");
	}
}
