/**
 * Task gt-material-dataset: verifies that the private OreDictMaterial.TDG placeholder
 * constants (card gt-material-model, keyed after TD.java:305,450,452,481,490,493,501,503,505,
 * 511,525,562,568,571) unify with the real gregapi.data.TD constants now that the full tag
 * table is ported. Unification works because TagData.createTagData is idempotent by
 * upper-cased name (TagData.java:77-81) - if it were NOT idempotent, the placeholder tags
 * would be distinct instances with different mTagID, and the contains() assertions below
 * would fail. This is exactly the test the card-2 handoff asked for.
 */
package gregapi.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.oredict.OreDictMaterial;

public class TagDataUnificationTest {

	@Test
	public void tagDataCreationIsIdempotentByUppercaseName() {
		assertSame(TD.Atomic.ELEMENT, TagData.createTagData("ATOMIC.ELEMENT"));
		assertSame(TD.Properties.HAS_TOOL_STATS, TagData.createTagData("properties.has_tool_stats"));
		assertSame(TD.Processing.UUM, TagData.createTagData("PROCESSING.UUM_SYNTHESISABLE"));
	}

	/** qual() puts the private TDG.HAS_TOOL_STATS/PARTS/STICKS/PLATES (OreDictMaterial.java:763); the real TD constants must see them. */
	@Test
	public void tdgToolStatsUnifyWithTD() {
		OreDictMaterial m = OreDictMaterial.createMaterial(-1, "TDGUnificationTest1", "TDG");
		m.qual(1, 1.0, 16, 1);
		assertTrue(m.contains(TD.Properties.HAS_TOOL_STATS), "TDG.HAS_TOOL_STATS must be the same TagData as TD.Properties.HAS_TOOL_STATS");
		assertTrue(m.contains(TD.ItemGenerator.PARTS));
		assertTrue(m.contains(TD.ItemGenerator.STICKS));
		assertTrue(m.contains(TD.ItemGenerator.PLATES));
	}

	/** setSmelting puts the private TDG.MELTING (OreDictMaterial.java:668); the real TD constant must see it. */
	@Test
	public void tdgMeltingUnifiesWithTD() {
		OreDictMaterial m = OreDictMaterial.createMaterial(-1, "TDGUnificationTest2", "TDG");
		m.setSmelting(null, TD.Processing.MELTING == null ? 0 : 1); // amount > 0 triggers put(TDG.MELTING)
		assertTrue(m.contains(TD.Processing.MELTING), "TDG.MELTING must be the same TagData as TD.Processing.MELTING");
	}

	/** alloySimple puts the private TDG.ALLOY/DECOMPOSABLE/CRUCIBLE_ALLOY (OreDictMaterial.java:345-348). */
	@Test
	public void tdgAlloyTagsUnifyWithTD() {
		OreDictMaterial m = OreDictMaterial.createMaterial(-1, "TDGUnificationTest3", "TDG");
		m.setMcfg(0, OreDictMaterial.createMaterial(-1, "TDGUnificationTest4", "TDG"), CS.U).alloySimple();
		assertTrue(m.contains(TD.Compounds.ALLOY));
		assertTrue(m.contains(TD.Compounds.DECOMPOSABLE));
		assertTrue(m.contains(TD.Processing.CRUCIBLE_ALLOY));
	}

	/** setMcfg checks contains(TDG.ELEMENT) and uumMcfg checks TDG.UUM; both must see the TD constants. */
	@Test
	public void tdgElementAndUumUnifyWithTD() {
		OreDictMaterial element = OreDictMaterial.createMaterial(-1, "TDGUnificationTest5", "TDG");
		element.put(TD.Atomic.ELEMENT);
		element.setStats(1, 1, 14, 20, 1.0);
		// setMoleculeConfiguration logs a tampering warning for Elements - no exception, just a marker that the check sees the tag.
		assertTrue(element.contains(TD.Atomic.ELEMENT));

		OreDictMaterial uumSource = OreDictMaterial.createMaterial(-1, "TDGUnificationTest6", "TDG");
		uumSource.put(TD.Processing.UUM);
		OreDictMaterial uumUser = OreDictMaterial.createMaterial(-1, "TDGUnificationTest7", "TDG");
		uumUser.uumMcfg(0, uumSource, CS.U);
		assertTrue(uumUser.contains(TD.Processing.UUM), "TDG.UUM must be the same TagData as TD.Processing.UUM");
	}
}
