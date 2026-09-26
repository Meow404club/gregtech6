package gregtech6.reactor.neutron;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The coolant table pins (task debt-reactor-a-neutron-core): every row of
 * {@link ReactorCoolant} against the upstream constants and branch bodies —
 * CS.java:218-242 (the EU_PER coolant constants, STEAM_PER_WATER), Core2x2.java:129-131 (heat dividers),
 * :138-195 (hot-output identities), Nuclear.java:179-199 (emission modulation).
 * Expected values are hand-recomputed from those lines, not copied from the port.
 */
public class ReactorCoolantTest {

	@Test
	public void whitelistIsElevenRowsInCoreOrder() {
		// MultiTileEntityReactorCore.java:255-267 fill-whitelist order
		ReactorCoolant[] tExpected = {
			ReactorCoolant.IC2_COOLANT, ReactorCoolant.DISTILLED_WATER, ReactorCoolant.THORIUM_SALT,
			ReactorCoolant.MOLTEN_TIN, ReactorCoolant.MOLTEN_SODIUM, ReactorCoolant.SEMIHEAVY_WATER,
			ReactorCoolant.HEAVY_WATER, ReactorCoolant.TRITIATED_WATER, ReactorCoolant.MOLTEN_LICL,
			ReactorCoolant.HELIUM, ReactorCoolant.CARBON_DIOXIDE};
		assertEquals(11, ReactorCoolant.values().length);
		assertArrayEquals(tExpected, ReactorCoolant.values());
	}

	@Test
	public void conversionAndDividerColumnsMatchUpstream() {
		// (huPerLiter, heatDivider, hotOutputId, outputMultiplier) per row — CS.java:218-242,
		// Core2x2.java:129-131/138-195. ThSalt outputs plain LiCl (:190), water steam ×160 (:145).
		Object[][] tExpected = {
			{ReactorCoolant.IC2_COOLANT,       20, 1, "ic2_coolant_hot",       1},
			{ReactorCoolant.DISTILLED_WATER,   80, 1, "steam",               160},
			{ReactorCoolant.THORIUM_SALT, 2_560_000, 1, "licl",                 1},
			{ReactorCoolant.MOLTEN_TIN,        40, 3, "hot_molten_tin",        1},
			{ReactorCoolant.MOLTEN_SODIUM,     30, 6, "hot_molten_sodium",     1},
			{ReactorCoolant.SEMIHEAVY_WATER,   40, 1, "hot_semiheavy_water",   1},
			{ReactorCoolant.HEAVY_WATER,       50, 1, "hot_heavy_water",       1},
			{ReactorCoolant.TRITIATED_WATER,   60, 1, "hot_tritiated_water",   1},
			{ReactorCoolant.MOLTEN_LICL,       15, 1, "hot_molten_licl",       1},
			{ReactorCoolant.HELIUM,            30, 1, "hot_helium",            1},
			{ReactorCoolant.CARBON_DIOXIDE,    20, 1, "hot_carbon_dioxide",    1}};
		for (Object[] tRow : tExpected) {
			ReactorCoolant tCoolant = (ReactorCoolant) tRow[0];
			assertEquals(tRow[1], tCoolant.huPerLiter, tCoolant.name() + " huPerLiter");
			assertEquals(tRow[2], tCoolant.heatDivider, tCoolant.name() + " heatDivider");
			assertEquals(tRow[3], tCoolant.hotOutputId, tCoolant.name() + " hotOutputId");
			assertEquals(tRow[4], tCoolant.outputMultiplier, tCoolant.name() + " outputMultiplier");
		}
		// EU_PER_THORIUM_SALT = 2560000 (CS.java:236) is the one suspect balance value —
		// pinned as-is per task spec item 7 (single-source suspicion, faithful copy).
		assertEquals(2_560_000, ReactorCoolant.THORIUM_SALT.huPerLiter);
	}

	@Test
	public void moderationColumnMatchesNuclear208to214() {
		// Nuclear.java:208-211 — only distilled/semiheavy/heavy/tritiated water moderate fuel
		for (ReactorCoolant tCoolant : ReactorCoolant.values()) {
			boolean tExpected = tCoolant == ReactorCoolant.DISTILLED_WATER || tCoolant == ReactorCoolant.SEMIHEAVY_WATER
				|| tCoolant == ReactorCoolant.HEAVY_WATER || tCoolant == ReactorCoolant.TRITIATED_WATER;
			assertEquals(tExpected, tCoolant.moderatesFuel, tCoolant.name());
		}
	}

	@Test
	public void maxModeColumnMatchesNuclear240to252() {
		// Nuclear.java:241-248 — LiCl +div4, ThSalt ×4, D2O /8, T2O /16, everything else plain
		assertEquals(ReactorCoolant.MaxMode.PLUS_DIV4, ReactorCoolant.MOLTEN_LICL.maxMode);
		assertEquals(ReactorCoolant.MaxMode.TIMES4, ReactorCoolant.THORIUM_SALT.maxMode);
		assertEquals(ReactorCoolant.MaxMode.DIV8, ReactorCoolant.HEAVY_WATER.maxMode);
		assertEquals(ReactorCoolant.MaxMode.DIV16, ReactorCoolant.TRITIATED_WATER.maxMode);
		for (ReactorCoolant tCoolant : ReactorCoolant.values()) {
			if (tCoolant == ReactorCoolant.MOLTEN_LICL || tCoolant == ReactorCoolant.THORIUM_SALT
				|| tCoolant == ReactorCoolant.HEAVY_WATER || tCoolant == ReactorCoolant.TRITIATED_WATER) continue;
			assertEquals(ReactorCoolant.MaxMode.PLAIN, tCoolant.maxMode, tCoolant.name());
		}
		// Hand-computed applications on a 2048 base (U-235) and a 16384 base (Naquadria):
		// 2048+divup(2048,4)=2560, 2048*4=8192, divup(2048,8)=256, divup(2048,16)=128,
		// divup(16384,8)=2048, divup(16384,16)=1024
		assertEquals(2560, ReactorCoolant.MaxMode.PLUS_DIV4.apply(2048));
		assertEquals(8192, ReactorCoolant.MaxMode.TIMES4.apply(2048));
		assertEquals(256, ReactorCoolant.MaxMode.DIV8.apply(2048));
		assertEquals(128, ReactorCoolant.MaxMode.DIV16.apply(2048));
		assertEquals(2048, ReactorCoolant.MaxMode.DIV8.apply(16384));
		assertEquals(1024, ReactorCoolant.MaxMode.DIV16.apply(16384));
	}

	@Test
	public void modulateEmissionCoversAllElevenBranches() {
		// Nuclear.java:179-196 — one branch per coolant, base triple (32, 32, 4) of U-235:
		// IC2 ×4/×4/×2 -> (128,128,8); CO2 self×3 -> (96,32,4); He other-divup(other,2) -> (32,16,4);
		// LiCl (160,16,4); ThSalt (0,16,3); Sn/Na div-1 -> (32,32,3); rest plain (32,32,4)
		Object[][] tExpected = {
			{ReactorCoolant.IC2_COOLANT, 128, 128, 8},
			{ReactorCoolant.CARBON_DIOXIDE, 96, 32, 4},
			{ReactorCoolant.HELIUM, 32, 16, 4},
			{ReactorCoolant.MOLTEN_LICL, 160, 16, 4},
			{ReactorCoolant.THORIUM_SALT, 0, 16, 3},
			{ReactorCoolant.MOLTEN_TIN, 32, 32, 3},
			{ReactorCoolant.MOLTEN_SODIUM, 32, 32, 3},
			{ReactorCoolant.DISTILLED_WATER, 32, 32, 4},
			{ReactorCoolant.SEMIHEAVY_WATER, 32, 32, 4},
			{ReactorCoolant.HEAVY_WATER, 32, 32, 4},
			{ReactorCoolant.TRITIATED_WATER, 32, 32, 4}};
		for (Object[] tRow : tExpected) {
			ReactorCoolant.EmissionParams tParams = ((ReactorCoolant) tRow[0]).modulateEmission(32, 32, 4);
			assertEquals(tRow[1], tParams.self(), tRow[0] + " self");
			assertEquals(tRow[2], tParams.other(), tRow[0] + " other");
			assertEquals(tRow[3], tParams.div(), tRow[0] + " div");
		}
		// Odd-other halving: divup(33,2)=17, so He other = 33-17 = 16
		assertEquals(16, ReactorCoolant.HELIUM.modulateEmission(8, 33, 16).other());
		// Odd div subtraction cannot reach zero for real rows (3..32 base): 3-1=2
		assertEquals(2, ReactorCoolant.MOLTEN_TIN.modulateEmission(2, 2, 3).div());
	}
}
