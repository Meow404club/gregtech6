package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The foreign-material attribution table (issue #46, mdh-2): which {@link OreDictMaterial}s
 * belong to a 1.7.10 foreign mod, and whether the driver may ever hide them.
 *
 * <p><b>Provenance.</b> Rebuilt row-by-row from the upstream attribution block
 * {@code tmp/gt6-1.7.10 MT.java:2062-2721} (segmented {@code get_source} re-read — the
 * sym_query line-skip trap, lesson id1218/research id1243; every row carries its upstream
 * {@code MT.java:<line>} anchor comment). The port's own carried attribution strings
 * ({@code MT.java:3099+} {@code setOriginalMod(MD.X.mID)} section, 599 rows) cross-check the
 * transcription domain-by-domain in {@code GT6ForeignMaterialAtlasTest}.
 *
 * <p><b>The three-value contract (ADR-MDH2/MDH5):</b>
 * <ul>
 * <li>{@link AttributionKind#PRIMARY} — foreign-exclusive content, the ONLY class mdh-3 may
 * ever clear, and the only class {@link #domainOf} answers for (so the visibility gate can
 * hide it when the owning domain is absent);</li>
 * <li>{@link AttributionKind#COMMON_SECONDARY} — upstream {@code COMMON_ORE} flag rows (the
 * {@code Cu.put(MD.EtFu, COMMON_ORE)} shape, MT.java:2080) plus conservative placements for
 * shared materials the port's own face depends on (marked {@code 待复核} below) — never hide,
 * never clear;</li>
 * <li>{@link AttributionKind#GT6_SELF} — upstream says foreign, the port's live registration
 * face reverses it (the theum quartet precedent: Thermal-named but GT6-created fluid rows,
 * GTFluids.java:2795-2798, lesson id1218) — never hide, never clear.</li>
 * </ul>
 *
 * <p><b>Batches.</b> Batch 1 = the eight largest foreign domains by exact re-read count
 * (164 rows): Metallurgy 34, ExtraPlanets 27, harvestcraft 19, hbm 18, IC2 17, Botania 17,
 * EnderIO 16, ThermalExpansion 16. Batch 2 (mdh-atlas-batch2, transcribed below) = the
 * remaining 56 domains, 276 rows, 144 PRIMARY / 132 COMMON_SECONDARY / 0 GT6_SELF, in
 * upstream line order: EtFu 7 (:2080-2086), Salt 1 (:2110), GrC 1 (:2113), NePl 3
 * (:2116-2118), NeLi 6 (:2121-2126), EnLi 3 (:2129-2131), GT5U 4 (:2195-2198, modid
 * "gregtech" = our own modid — never seedable), IHL 9 (:2226-2234), BC 2 (:2237-2238), FR
 * 15 (:2241-2255), FRMB 3 (:2258-2260), BINNIE 2 (:2263-2264), TFC 13 (:2267-2279), TF 14
 * (:2282-2295), ERE 4 (:2298-2301), RC 8 (:2304-2311), IE 2 (:2314-2315), AE 5
 * (:2336-2340), PnC 1 (:2343), SC2 2 (:2346-2347), TiC 6 (:2350-2355), AA 1 (:2358), MFR 3
 * (:2379-2381), BR 5 (:2384-2388), ReC 2 (:2411-2412), RoC 15 unique (:2415-2431, Prismane
 * and Lonsdaleite are upstream duplicate rows :2416/:2423 and :2417/:2424), Mek 5
 * (:2434-2438), TC 9 (:2441-2449), TCTE 1 (:2452), ALF 5 (:2474-2478), CANDY 4
 * (:2481-2484), GC_ADV_ROCKETRY 2 (:2487-2488), HEE 2 (:2491-2492), MaCu 6 (:2495-2500),
 * ABYSSAL 4 (:2503-2506), Fossil 1 (:2509), DE 2 (:2512-2513), AV 3 (:2516-2518), PE 2
 * (:2521-2522), TROPIC 4 (:2525-2528), BoP 4 (:2531-2534), FM 5 (:2537-2541), ARS 8
 * (:2544-2551), GC 10 (:2554-2563), GC_GALAXYSPACE 8 (:2595-2602), MO 4 (:2605-2608), RT 2
 * (:2611-2612), ExU 2 (:2615-2616), BTL 13 (:2619-2631), AETHER 7 (:2634-2640), RP 13 of
 * 14 (:2643-2656 — :2657 EnergiumCyan carries no {@code put()} and is not an attribution
 * row), PR 1 (:2660), BP 1 (:2663), FZ 5 (:2666-2670), PFAA 5 (:2673-2677), UB 1 (:2680).
 *
 * <p><b>Batch-2 deferral (a verified upstream row NOT transcribed, recorded in the state
 * ledger {@code tasks.mdh-atlas-batch2}):</b> {@code NikolineAlloy} (RP :2655) — a plain
 * {@code put(MD.RP)} (PRIMARY shape, RP-exclusive, {@code visDefault(Nikolite)}) that owns
 * the un-annotated GT6-created chemical row {@code nikolinealloy_molten}
 * (GTFluids.java:2277), and mdh-3's fluid ratchet requires every PRIMARY material-fluid row
 * to carry its domain annotation. This card may not touch GTFluids, and demoting a
 * foreign-exclusive alloy to COMMON_SECONDARY would wrongly shield it from the clear-out
 * forever — so the row waits for the clear-out card, which lands the annotation and the
 * atlas row together. Its NePl neighbour {@code AncientDebris} (:2118) looked like a second
 * deferral on the same grounds, but upstream flags it {@code COMMON_ORE} — the shared-ore
 * shape that maps to COMMON_SECONDARY exactly like its sibling {@code Netherite} (:2116) —
 * so it is transcribed below, never-hidden, and its un-annotated {@code ancientdebris_molten}
 * row (GTFluids.java:2239) needs no annotation because COMMON_SECONDARY never clears.
 * Kind-only deferrals do not exist: batch-2 GT6_SELF count is 0 (the review account found
 * no theum-shaped material — every GT6 fluid-bearing candidate — honey/lubricant/wax
 * family/seedoil/biomass/ethanol/plastic/indigo and the food-oil suite — is a real foreign
 * mod's content that GT6 transcribed and rides in its own fluid/recipe faces, hence
 * COMMON_SECONDARY, not SELF; the theum quartet stays the only reversal).
 *
 * <p><b>Batch-2 COMMON_SECONDARY evidence classes</b> (row comments carry the specifics):
 * upstream {@code COMMON_ORE} flag; the SPEC secondary pairs (Cu→EtFu, W→RP, Hg→TC, Si→AE,
 * Ge→Mek, Pb→FZ, Bi→TFC, Ti→MaCu, Steel→RC); the mdh-3 fluid-ratchet collisions above;
 * GT6's own stone-suite/concrete faces (GTStoneBlocks StoneSpec rows, GT6RecipesMixer
 * CFoam groups, GT6RecipesShCL); GT6 worldgen/surface faces (GTOreWorldgen, GT6OreBlocks,
 * GT6SurfaceBlocks); port machine-face case-maps (GTItemPipes, GT6Hoppers, GTMachines,
 * GTWireSpecs); GT6 own chains (GT6RecipesCokeOven creosote/coke, GTFluids own oil).
 * Still outside the MD-constant shape (documented, untranscribable as data rows): the
 * domain-less {@code COMMON_ORE} rows (:2201 Superconductor "rocketscience", :2204 Os
 * "gravisuite", :2719-2721 Force/Forcicium/Forcillium) and the 15 {@code unused()} HBM
 * rows (:4056-4070, mID &lt; 0 = no registered items, nothing to hide or clear).
 */
public final class GT6ForeignMaterialAtlas {

	/** The three attribution classes (ADR-MDH2): only {@link #PRIMARY} is ever clearable. */
	public enum AttributionKind {
		/** Foreign-exclusive content — the only class the driver may hide/clear (mdh-3). */
		PRIMARY,
		/** Shared via the upstream COMMON_ORE flag, or placed conservatively (待复核) — never hidden. */
		COMMON_SECONDARY,
		/** Upstream says foreign, the port's live registration face reverses it — never hidden. */
		GT6_SELF
	}

	/**
	 * One attribution row. The material resolves lazily: the MT fields are null until
	 * {@code MT.init()} ran (the offline bare-JVM trap, r10-debt-material-tint lesson), so
	 * rows must never dereference their supplier at class-load time.
	 */
	public record Row(Supplier<OreDictMaterial> material, String domain, AttributionKind kind) {
	}

	/** The batch-1 table, rows in upstream block order; each line comment is the upstream anchor. */
	private static final List<Row> ROWS = List.of(
			// ---- MD.HaC "harvestcraft" (upstream MT.java:2089-2107) ----
			new Row(() -> MT.NaCl, MT.MD.HaC.mID, AttributionKind.COMMON_SECONDARY), // :2089 COMMON_ORE — salt is shared cooking chemistry
			new Row(() -> MT.WaxPlant, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2090
			new Row(() -> MT.Barley, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2091
			new Row(() -> MT.Rye, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2092
			new Row(() -> MT.Rice, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2093
			new Row(() -> MT.Oat, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2094
			new Row(() -> MT.Corn, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2095
			new Row(() -> MT.Tofu, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2096
			new Row(() -> MT.Chocolate, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2097
			new Row(() -> MT.Cinnamon, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2098
			new Row(() -> MT.Nutmeg, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2099
			new Row(() -> MT.Peanut, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2100
			new Row(() -> MT.Pistachio, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2101
			new Row(() -> MT.Almond, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2102
			new Row(() -> MT.Vanilla, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2103
			new Row(() -> MT.PepperBlack, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2104
			new Row(() -> MT.Curry, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2105
			new Row(() -> MT.ButterSalted, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2106
			new Row(() -> MT.OliveOil, MT.MD.HaC.mID, AttributionKind.PRIMARY), // :2107
			// ---- MD.IC2 "IC2" (upstream MT.java:2207-2223) ----
			new Row(() -> MT.Sn, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2207 COMMON_ORE — shared element
			new Row(() -> MT.Bronze, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2208 — GT6 core alloy, port's own face depends on it 待复核
			new Row(() -> MT.TECH.RefinedIron, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2209 — GT machine face 待复核 (port hosts it in TECH, re-registered onto WroughtIron)
			new Row(() -> MT.Ir, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2210 COMMON_ORE — shared element
			new Row(() -> MT.U_238, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2211 — GT6 own nuclear chain uses it 待复核
			new Row(() -> MT.U_235, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2212 — GT6 own nuclear chain 待复核
			new Row(() -> MT.Pu, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2213 — GT6 own nuclear chain 待复核
			new Row(() -> MT.DistWater, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2214 — port condenser/distillery recipe legs (b2c-cut DistW) 待复核
			new Row(() -> MT.SiO2, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2215 — quartz/glass chemistry core 待复核
			new Row(() -> MT.EnergiumRed, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2216
			new Row(() -> MT.ConstructionFoam, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2217
			new Row(() -> MT.UUMatter, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2218
			new Row(() -> MT.HydratedCoal, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2219
			new Row(() -> MT.Coffee, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2220
			new Row(() -> MT.Rubber, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2221 — GT6 own rubber tree line 待复核
			new Row(() -> MT.WoodRubber, MT.MD.IC2.mID, AttributionKind.COMMON_SECONDARY), // :2222 — GT6 own rubber wood 待复核
			new Row(() -> MT.Advanced, MT.MD.IC2.mID, AttributionKind.PRIMARY), // :2223
			// ---- MD.TE "ThermalExpansion" (upstream MT.java:2318-2333) ----
			new Row(() -> MT.Ni, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2318 COMMON_ORE — shared element
			new Row(() -> MT.Pt, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2319 COMMON_ORE — shared element
			new Row(() -> MT.Invar, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2320 — shared multi-mod alloy, conservative placement 待复核
			new Row(() -> MT.Electrum, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2321 — shared multi-mod alloy, conservative placement 待复核
			new Row(() -> MT.Enderium, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2322
			new Row(() -> MT.Signalum, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2323
			new Row(() -> MT.Lumium, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2324
			new Row(() -> MT.RareEarth, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2325 COMMON_ORE
			new Row(() -> MT.Niter, MT.MD.TE.mID, AttributionKind.COMMON_SECONDARY), // :2326 COMMON_ORE
			new Row(() -> MT.Basalz, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2327
			new Row(() -> MT.Blitz, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2328
			new Row(() -> MT.Blizz, MT.MD.TE.mID, AttributionKind.PRIMARY), // :2329
			new Row(() -> MT.Petrotheum, MT.MD.TE.mID, AttributionKind.GT6_SELF), // :2330 — reversed: GT6-created fluid row GTFluids.java:2797 (lesson id1218)
			new Row(() -> MT.Aerotheum, MT.MD.TE.mID, AttributionKind.GT6_SELF), // :2331 — reversed: GTFluids.java:2798
			new Row(() -> MT.Pyrotheum, MT.MD.TE.mID, AttributionKind.GT6_SELF), // :2332 — reversed: GTFluids.java:2795
			new Row(() -> MT.Cryotheum, MT.MD.TE.mID, AttributionKind.GT6_SELF), // :2333 — reversed: GTFluids.java:2796
			// ---- MD.EIO "EnderIO" (upstream MT.java:2361-2376) ----
			new Row(() -> MT.RedstoneAlloy, MT.MD.EIO.mID, AttributionKind.COMMON_SECONDARY), // :2361 — port forceItemGeneration depends on it (GTMaterialItems.java:124) 待复核
			new Row(() -> MT.EnderiumBase, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2362
			new Row(() -> MT.PulsatingIron, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2363
			new Row(() -> MT.ConductiveIron, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2364
			new Row(() -> MT.EnergeticAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2365
			new Row(() -> MT.VibrantAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2366
			new Row(() -> MT.ElectricalSteel, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2367
			new Row(() -> MT.Soularium, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2368
			new Row(() -> MT.CrudeSteel, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2369
			new Row(() -> MT.CrystallineAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2370
			new Row(() -> MT.CrystallinePinkSlime, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2371
			new Row(() -> MT.EndSteel, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2372
			new Row(() -> MT.EnergeticSilver, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2373
			new Row(() -> MT.MelodicAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2374
			new Row(() -> MT.StellarAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2375
			new Row(() -> MT.VividAlloy, MT.MD.EIO.mID, AttributionKind.PRIMARY), // :2376
			// ---- MD.HBM "hbm" (upstream MT.java:2391-2408) ----
			new Row(() -> MT.Pu_238, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2391
			new Row(() -> MT.Pu_240, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2392
			new Row(() -> MT.Mingrade, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2393
			new Row(() -> MT.PhosphorusRed, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2394
			new Row(() -> MT.PhosphorusWhite, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2395
			new Row(() -> MT.Alexandrite, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2396
			new Row(() -> MT.Asbestos, MT.MD.HBM.mID, AttributionKind.COMMON_SECONDARY), // :2397 COMMON_ORE
			new Row(() -> MT.OREMATS.Columbite, MT.MD.HBM.mID, AttributionKind.COMMON_SECONDARY), // :2398 COMMON_ORE
			new Row(() -> MT.OREMATS.Tantalite, MT.MD.HBM.mID, AttributionKind.COMMON_SECONDARY), // :2399 COMMON_ORE
			new Row(() -> MT.OREMATS.Coltan, MT.MD.HBM.mID, AttributionKind.COMMON_SECONDARY), // :2400 COMMON_ORE
			new Row(() -> MT.Ta, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2401 — upstream: "don't COMMON_ORE this!"
			new Row(() -> MT.Nb, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2402 — upstream: "don't COMMON_ORE this!"
			new Row(() -> MT.Nd, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2403
			new Row(() -> MT.DeshAlloy, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2404
			new Row(() -> MT.PVC, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2405
			new Row(() -> MT.Teflon, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2406
			new Row(() -> MT.Bakelite, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2407
			new Row(() -> MT.Polycarbonate, MT.MD.HBM.mID, AttributionKind.PRIMARY), // :2408
			// ---- MD.BOTA "Botania" (upstream MT.java:2455-2471) ----
			new Row(() -> MT.Livingwood, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2455
			new Row(() -> MT.STONES.Livingrock, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2456
			new Row(() -> MT.Dreamwood, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2457
			new Row(() -> MT.Shimmerwood, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2458
			new Row(() -> MT.SunnyQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2459
			new Row(() -> MT.LavenderQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2460
			new Row(() -> MT.RedQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2461
			new Row(() -> MT.BlazeQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2462
			new Row(() -> MT.SmokeyQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2463
			new Row(() -> MT.ManaQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2464
			new Row(() -> MT.ElvenQuartz, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2465
			new Row(() -> MT.Manasteel, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2466
			new Row(() -> MT.ManaDiamond, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2467
			new Row(() -> MT.ElvenElementium, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2468
			new Row(() -> MT.ElvenDragonstone, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2469
			new Row(() -> MT.Terrasteel, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2470
			new Row(() -> MT.GaiaSpirit, MT.MD.BOTA.mID, AttributionKind.PRIMARY), // :2471
			// ---- MD.GC_EXTRAPLANETS "ExtraPlanets" (upstream MT.java:2566-2592) ----
			new Row(() -> MT.DiamondBlue, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2566
			new Row(() -> MT.DiamondGreen, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2567
			new Row(() -> MT.DiamondPurple, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2568
			new Row(() -> MT.DiamondRed, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2569
			new Row(() -> MT.DiamondYellow, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2570
			new Row(() -> MT.STONES.PhobosRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2571
			new Row(() -> MT.STONES.DeimosRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2572
			new Row(() -> MT.STONES.MercuryRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2573
			new Row(() -> MT.STONES.VenusRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2574
			new Row(() -> MT.STONES.CeresRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2575
			new Row(() -> MT.STONES.JupiterRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2576
			new Row(() -> MT.STONES.IoRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2577
			new Row(() -> MT.STONES.EuropaRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2578
			new Row(() -> MT.STONES.GanymedeRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2579
			new Row(() -> MT.STONES.CallistoRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2580
			new Row(() -> MT.STONES.SaturnRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2581
			new Row(() -> MT.STONES.RheaRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2582
			new Row(() -> MT.STONES.TitanRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2583
			new Row(() -> MT.STONES.OberonRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2584
			new Row(() -> MT.STONES.IapetusRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2585
			new Row(() -> MT.STONES.UranusRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2586
			new Row(() -> MT.STONES.TitaniaRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2587
			new Row(() -> MT.STONES.NeptuneRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2588
			new Row(() -> MT.STONES.TritonRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2589
			new Row(() -> MT.STONES.PlutoRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2590
			new Row(() -> MT.STONES.ErisRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2591
			new Row(() -> MT.STONES.Kepler22bRock, MT.MD.GC_EXTRAPLANETS.mID, AttributionKind.PRIMARY), // :2592
			// ---- MD.MET "Metallurgy" (upstream MT.java:2683-2716) ----
			new Row(() -> MT.Angmallen, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2683
			new Row(() -> MT.Hepatizon, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2684
			new Row(() -> MT.DamascusSteel, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2685
			new Row(() -> MT.Aredrite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2686 Fantasy
			new Row(() -> MT.Atl, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2687 Fantasy
			new Row(() -> MT.Tartarite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2688 Fantasy
			new Row(() -> MT.Adamantine, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2689 Fantasy
			new Row(() -> MT.AstralSilver, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2690 Fantasy
			new Row(() -> MT.Mithril, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2691 Fantasy
			new Row(() -> MT.Infuscolium, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2692 Fantasy
			new Row(() -> MT.Rubracium, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2693 Fantasy
			new Row(() -> MT.Oureclase, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2694 Fantasy
			new Row(() -> MT.Orichalcum, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2695 Fantasy
			new Row(() -> MT.Carmot, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2696 Fantasy
			new Row(() -> MT.Prometheum, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2697 Fantasy
			new Row(() -> MT.DeepIron, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2698 Fantasy
			new Row(() -> MT.Haderoth, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2699 Fantasy
			new Row(() -> MT.Celenegil, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2700 Fantasy
			new Row(() -> MT.Meutoite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2701 Ender
			new Row(() -> MT.Eximite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2702 Ender
			new Row(() -> MT.Desichalkos, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2703 Ender
			new Row(() -> MT.Midasium, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2704 Nether
			new Row(() -> MT.Alduorite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2705 Nether
			new Row(() -> MT.Lemurite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2706 Nether
			new Row(() -> MT.Ceruclase, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2707 Nether
			new Row(() -> MT.Kalendrite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2708 Nether
			new Row(() -> MT.Sanguinite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2709 Nether
			new Row(() -> MT.Vyroxeres, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2710 Nether
			new Row(() -> MT.Ignatius, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2711 Nether
			new Row(() -> MT.Vulcanite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2712 Nether
			new Row(() -> MT.ShadowIron, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2713 Nether
			new Row(() -> MT.ShadowSteel, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2714 Nether
			new Row(() -> MT.Inolashite, MT.MD.MET.mID, AttributionKind.PRIMARY), // :2715 Nether
			new Row(() -> MT.Amordrine, MT.MD.MET.mID, AttributionKind.PRIMARY)); // :2716

	/**
	 * The batch-2 table (mdh-atlas-batch2), rows in upstream block order; same anchor-comment
	 * convention as batch 1. Append-only relative to batch 1: this list never edits a batch-1
	 * line, and the batch-1 test pins stay verbatim on the {@link #ROWS} prefix.
	 */
	private static final List<Row> BATCH2_ROWS = List.of(
			// ---- MD.EtFu "etfuturum" (upstream MT.java:2080-2086) ----
			new Row(() -> MT.Cu, MT.MD.EtFu.mID, AttributionKind.COMMON_SECONDARY), // :2080 COMMON_ORE — SPEC secondary pair (shared element)
			new Row(() -> MT.STONES.Deepslate, MT.MD.EtFu.mID, AttributionKind.COMMON_SECONDARY), // :2081 — GT6 own concrete face GT6RecipesMixer:166 black group
			new Row(() -> MT.STONES.Granite, MT.MD.EtFu.mID, AttributionKind.COMMON_SECONDARY), // :2082 — GT6 stone-suite/concrete face (3 port files)
			new Row(() -> MT.STONES.Diorite, MT.MD.EtFu.mID, AttributionKind.COMMON_SECONDARY), // :2083 — GT6RecipesMixer:164 CFoam white group
			new Row(() -> MT.STONES.Andesite, MT.MD.EtFu.mID, AttributionKind.COMMON_SECONDARY), // :2084 — GT6RecipesMixer:164 CFoam white group
			new Row(() -> MT.PrismarineLight, MT.MD.EtFu.mID, AttributionKind.PRIMARY), // :2085
			new Row(() -> MT.PrismarineDark, MT.MD.EtFu.mID, AttributionKind.PRIMARY), // :2086
			// ---- MD.Salt "SaltMod" (upstream MT.java:2110) ----
			new Row(() -> MT.NaHCO3, MT.MD.Salt.mID, AttributionKind.PRIMARY), // :2110
			// ---- MD.GrC "Growthcraft" (upstream MT.java:2113) ----
			new Row(() -> MT.Butter, MT.MD.GrC.mID, AttributionKind.PRIMARY), // :2113
			// ---- MD.NePl "netheriteplus" (upstream MT.java:2116-2118) ----
			new Row(() -> MT.Netherite, MT.MD.NePl.mID, AttributionKind.COMMON_SECONDARY), // :2116 COMMON_ORE
			new Row(() -> MT.NetherizedDiamond, MT.MD.NePl.mID, AttributionKind.PRIMARY), // :2117
			new Row(() -> MT.AncientDebris, MT.MD.NePl.mID, AttributionKind.COMMON_SECONDARY), // :2118 COMMON_ORE — GT6-created row ancientdebris_molten GTFluids:2239 never needs annotation (CS never clears)
			// ---- MD.NeLi "netherlicious" (upstream MT.java:2121-2126) ----
			new Row(() -> MT.Efrine, MT.MD.NeLi.mID, AttributionKind.COMMON_SECONDARY), // :2121 COMMON_ORE
			new Row(() -> MT.VoidCrystal, MT.MD.NeLi.mID, AttributionKind.COMMON_SECONDARY), // :2122 COMMON_ORE
			new Row(() -> MT.Gloomstone, MT.MD.NeLi.mID, AttributionKind.COMMON_SECONDARY), // :2123 COMMON_ORE
			new Row(() -> MT.OatAbyssal, MT.MD.NeLi.mID, AttributionKind.PRIMARY), // :2124
			new Row(() -> MT.STONES.Basalt, MT.MD.NeLi.mID, AttributionKind.COMMON_SECONDARY), // :2125 — GT6 stone suite GTStoneBlocks:94 + GT6RecipesMixer:166
			new Row(() -> MT.STONES.Blackstone, MT.MD.NeLi.mID, AttributionKind.COMMON_SECONDARY), // :2126 — GT6 stone-suite face (4 port files)
			// ---- MD.EnLi "enderlicious" (upstream MT.java:2129-2131) ----
			new Row(() -> MT.Sugilite, MT.MD.EnLi.mID, AttributionKind.COMMON_SECONDARY), // :2129 COMMON_ORE
			new Row(() -> MT.EndSandWhite, MT.MD.EnLi.mID, AttributionKind.PRIMARY), // :2130
			new Row(() -> MT.EndSandBlack, MT.MD.EnLi.mID, AttributionKind.PRIMARY), // :2131
			// ---- MD.GT5U "gregtech" (upstream MT.java:2195-2198) — our own modid: PRIMARY answers
			// "gregtech" (port setOriginalMod MT.java:3093-3096), always loaded, never seedable —
			// the batch-1-only seed walk below keeps it out of SEEDED_DOMAINS.
			new Row(() -> MT.HSSG, MT.MD.GT5U.mID, AttributionKind.PRIMARY), // :2195
			new Row(() -> MT.HSSE, MT.MD.GT5U.mID, AttributionKind.PRIMARY), // :2196
			new Row(() -> MT.HSSS, MT.MD.GT5U.mID, AttributionKind.PRIMARY), // :2197
			new Row(() -> MT.PlatinumGroupSludge, MT.MD.GT5U.mID, AttributionKind.PRIMARY), // :2198
			// ---- MD.IHL "ihl" (upstream MT.java:2226-2234) ----
			new Row(() -> MT.SiC, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2226
			new Row(() -> MT.H2Ca2B2Si2O10, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2227
			new Row(() -> MT.H3BO3, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2228
			new Row(() -> MT.Li2O, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2229
			new Row(() -> MT.NaOH, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2230
			new Row(() -> MT.NaHSO4, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2231
			new Row(() -> MT.H2O2, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2232
			new Row(() -> MT.Li2Fe2O4, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2233
			new Row(() -> MT.Porcelain, MT.MD.IHL.mID, AttributionKind.PRIMARY), // :2234
			// ---- MD.BC "BuildCraft|Core" (upstream MT.java:2237-2238) ----
			new Row(() -> MT.Oil, MT.MD.BC.mID, AttributionKind.COMMON_SECONDARY), // :2237 — GT6 own oil registration GTFluids:358-388 + GT6RecipesCokeOven oil-shale rows
			new Row(() -> MT.Fuel, MT.MD.BC.mID, AttributionKind.COMMON_SECONDARY), // :2238 — GT6 own fuel chain (GTFluids engine family :479)
			// ---- MD.FR "Forestry" (upstream MT.java:2241-2255) ----
			new Row(() -> MT.I, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2241
			new Row(() -> MT.Ash, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2242
			new Row(() -> MT.Peat, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2243
			new Row(() -> MT.PeatBituminous, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2244
			new Row(() -> MT.Apatite, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2245
			new Row(() -> MT.PhosphorusBlue, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2246
			new Row(() -> MT.Biomass, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2247 — GT6-created chemical row "biomass" GTFluids:2786 un-annotated (mdh-3 ratchet); GT6 own bio chain
			new Row(() -> MT.BioFuel, MT.MD.FR.mID, AttributionKind.PRIMARY), // :2248
			new Row(() -> MT.Ethanol, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2249 — GT6 own ethanol face GTFluids:482/:574, never-hide conservative
			new Row(() -> MT.SeedOil, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2250 — GT6 own food-oil family GTFluids:1113, never-hide conservative
			new Row(() -> MT.Honey, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2251 — GT6-created chemical row "honey" GTFluids:2579 (ratchet); GT6 bee chain, "for_honey" is the alias row :912
			new Row(() -> MT.Honeydew, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2252 — GT6-created chemical row GTFluids:2580 (ratchet)
			new Row(() -> MT.Wax, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2253 — GT6-created chemical row wax_molten GTFluids:2809 (ratchet); GT6 candle/bee chain
			new Row(() -> MT.WaxBee, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2254 — waxbee_molten GTFluids:2810 (ratchet); GT6 own bee system
			new Row(() -> MT.WaxRefractory, MT.MD.FR.mID, AttributionKind.COMMON_SECONDARY), // :2255 — waxrefractory_molten GTFluids:2813 (ratchet)
			// ---- MD.FRMB "MagicBees" (upstream MT.java:2258-2260) ----
			new Row(() -> MT.WaxMagic, MT.MD.FRMB.mID, AttributionKind.COMMON_SECONDARY), // :2258 — waxmagic_molten GTFluids:2814 (ratchet); conservative 待复核 (annotation rides the clear-out card)
			new Row(() -> MT.WaxAmnesic, MT.MD.FRMB.mID, AttributionKind.COMMON_SECONDARY), // :2259 — waxamnesic_molten GTFluids:2815 (ratchet) 待复核
			new Row(() -> MT.WaxSoulful, MT.MD.FRMB.mID, AttributionKind.COMMON_SECONDARY), // :2260 — waxsoulful_molten GTFluids:2816 (ratchet) 待复核
			// ---- MD.BINNIE "BinnieCore" (upstream MT.java:2263-2264) ----
			new Row(() -> MT.Bark, MT.MD.BINNIE.mID, AttributionKind.PRIMARY), // :2263
			new Row(() -> MT.Hazelnut, MT.MD.BINNIE.mID, AttributionKind.PRIMARY), // :2264
			// ---- MD.TFC "terrafirmacraft" (upstream MT.java:2267-2279) ----
			new Row(() -> MT.Bi, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2267 COMMON_ORE — SPEC secondary pair
			new Row(() -> MT.Jasper, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2268 — GT6 worldgen face (GT6OreBlocks/GTOreWorldgen)
			new Row(() -> MT.WroughtIron, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2269 — port re-registration host (batch-1 RefinedIron note, atlas :104 precedent)
			new Row(() -> MT.RoseGold, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2270 — port machine-face case-map GTItemPipes.java:97
			new Row(() -> MT.SterlingSilver, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2271 — GTItemPipes.java:96 case-map
			new Row(() -> MT.BlackBronze, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2272 — GTItemPipes.java:99 case-map
			new Row(() -> MT.BismuthBronze, MT.MD.TFC.mID, AttributionKind.COMMON_SECONDARY), // :2273 — GT6Hoppers.java:129 + GTMachines.java:2629 case-maps
			new Row(() -> MT.BlackSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2274
			new Row(() -> MT.RedSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2275
			new Row(() -> MT.BlueSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2276
			new Row(() -> MT.MeteoricBlackSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2277
			new Row(() -> MT.MeteoricBlueSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2278
			new Row(() -> MT.MeteoricRedSteel, MT.MD.TFC.mID, AttributionKind.PRIMARY), // :2279
			// ---- MD.TF "TwilightForest" (upstream MT.java:2282-2295) ----
			new Row(() -> MT.STONES.Mazestone, MT.MD.TF.mID, AttributionKind.COMMON_SECONDARY), // :2282 — GT6 own recipe faces GT6RecipesMixer/GT6RecipesShCL
			new Row(() -> MT.STONES.Castlerock, MT.MD.TF.mID, AttributionKind.COMMON_SECONDARY), // :2283 — GT6RecipesMixer:165 CFoam white group
			new Row(() -> MT.STONES.Deadrock, MT.MD.TF.mID, AttributionKind.COMMON_SECONDARY), // :2284 — GT6RecipesMixer/GT6RecipesShCL faces
			new Row(() -> MT.LiveRoot, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2285
			new Row(() -> MT.IronWood, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2286
			new Row(() -> MT.Steeleaf, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2287
			new Row(() -> MT.Knightmetal, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2288
			new Row(() -> MT.FierySteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2289
			new Row(() -> MT.Fireleaf, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2290
			new Row(() -> MT.MeteoflameSteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2291
			new Row(() -> MT.MeteoflameBlackSteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2292
			new Row(() -> MT.MeteoflameBlueSteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2293
			new Row(() -> MT.MeteoflameRedSteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2294
			new Row(() -> MT.FlamascusSteel, MT.MD.TF.mID, AttributionKind.PRIMARY), // :2295
			// ---- MD.ERE "erebus" (upstream MT.java:2298-2301) ----
			new Row(() -> MT.STONES.Umber, MT.MD.ERE.mID, AttributionKind.COMMON_SECONDARY), // :2298 — GT6 stone-suite face
			new Row(() -> MT.STONES.Gneiss, MT.MD.ERE.mID, AttributionKind.COMMON_SECONDARY), // :2299 — GT6 stone-suite face
			new Row(() -> MT.PetrifiedWood, MT.MD.ERE.mID, AttributionKind.PRIMARY), // :2300
			new Row(() -> MT.Jade, MT.MD.ERE.mID, AttributionKind.PRIMARY), // :2301
			// ---- MD.RC "Railcraft" (upstream MT.java:2304-2311) ----
			new Row(() -> MT.K, MT.MD.RC.mID, AttributionKind.PRIMARY), // :2304
			new Row(() -> MT.S, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2305 COMMON_ORE
			new Row(() -> MT.KNO3, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2306 COMMON_ORE
			new Row(() -> MT.Firestone, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2307 COMMON_ORE
			new Row(() -> MT.Creosote, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2308 — GT6 own coke-oven chain GT6RecipesCokeOven.java:62
			new Row(() -> MT.TinAlloy, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2309 — port face (8 files incl. GTItemPipes case-maps) 待复核
			new Row(() -> MT.Steel, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2310 — SPEC secondary pair; GT6 core (23 port files)
			new Row(() -> MT.CoalCoke, MT.MD.RC.mID, AttributionKind.COMMON_SECONDARY), // :2311 — GT6 own coke-oven output GT6RecipesCokeOven.java:138-140
			// ---- MD.IE "ImmersiveEngineering" (upstream MT.java:2314-2315) ----
			new Row(() -> MT.Constantan, MT.MD.IE.mID, AttributionKind.COMMON_SECONDARY), // :2314 — shared multi-mod alloy (batch-1 Invar/Electrum sibling), 5 port files 待复核
			new Row(() -> MT.WoodTreated, MT.MD.IE.mID, AttributionKind.COMMON_SECONDARY), // :2315 — GT6 own treated-wood face (10 port files) 待复核
			// ---- MD.AE "appliedenergistics2" (upstream MT.java:2336-2340) ----
			new Row(() -> MT.STONES.SkyStone, MT.MD.AE.mID, AttributionKind.COMMON_SECONDARY), // :2336 — GT6 own loot face GT6LootInjectionDatagen:325
			new Row(() -> MT.Si, MT.MD.AE.mID, AttributionKind.COMMON_SECONDARY), // :2337 — SPEC secondary pair (upstream: "don't COMMON_ORE this!")
			new Row(() -> MT.CertusQuartz, MT.MD.AE.mID, AttributionKind.COMMON_SECONDARY), // :2338 COMMON_ORE
			new Row(() -> MT.ChargedCertusQuartz, MT.MD.AE.mID, AttributionKind.COMMON_SECONDARY), // :2339 COMMON_ORE
			new Row(() -> MT.Fluix, MT.MD.AE.mID, AttributionKind.COMMON_SECONDARY), // :2340 COMMON_ORE
			// ---- MD.PnC "PneumaticCraft" (upstream MT.java:2343) ----
			new Row(() -> MT.IronCompressed, MT.MD.PnC.mID, AttributionKind.PRIMARY), // :2343
			// ---- MD.SC2 "steamcraft2" (upstream MT.java:2346-2347) ----
			new Row(() -> MT.IronCast, MT.MD.SC2.mID, AttributionKind.PRIMARY), // :2346
			new Row(() -> MT.WhaleOil, MT.MD.SC2.mID, AttributionKind.COMMON_SECONDARY), // :2347 — GT6 own food-oil family GTFluids:1121, never-hide conservative
			// ---- MD.TiC "TConstruct" (upstream MT.java:2350-2355) ----
			new Row(() -> MT.Al, MT.MD.TiC.mID, AttributionKind.PRIMARY), // :2350 — upstream: "don't COMMON_ORE this!" (batch-1 Ta/Nb precedent)
			new Row(() -> MT.Co, MT.MD.TiC.mID, AttributionKind.COMMON_SECONDARY), // :2351 COMMON_ORE
			new Row(() -> MT.Ardite, MT.MD.TiC.mID, AttributionKind.COMMON_SECONDARY), // :2352 COMMON_ORE + GT6Hoppers/GTMachines case-maps
			new Row(() -> MT.Alumite, MT.MD.TiC.mID, AttributionKind.PRIMARY), // :2353
			new Row(() -> MT.Manyullyn, MT.MD.TiC.mID, AttributionKind.COMMON_SECONDARY), // :2354 — GT6Hoppers/GTItemPipes/GTMachines case-maps
			new Row(() -> MT.AluminiumBrass, MT.MD.TiC.mID, AttributionKind.COMMON_SECONDARY), // :2355 — GTItemPipes case-map
			// ---- MD.AA "ActuallyAdditions" (upstream MT.java:2358) ----
			new Row(() -> MT.BlackQuartz, MT.MD.AA.mID, AttributionKind.PRIMARY), // :2358
			// ---- MD.MFR "MineFactoryReloaded" (upstream MT.java:2379-2381) ----
			new Row(() -> MT.MeatRaw, MT.MD.MFR.mID, AttributionKind.PRIMARY), // :2379
			new Row(() -> MT.MeatCooked, MT.MD.MFR.mID, AttributionKind.PRIMARY), // :2380
			new Row(() -> MT.Plastic, MT.MD.MFR.mID, AttributionKind.COMMON_SECONDARY), // :2381 — GT6-created chemical row "plastic" GTFluids:2801 (ratchet); GT6 own plastic chain
			// ---- MD.BR "BigReactors" (upstream MT.java:2384-2388) ----
			new Row(() -> MT.Yellorium, MT.MD.BR.mID, AttributionKind.COMMON_SECONDARY), // :2384 COMMON_ORE
			new Row(() -> MT.Blutonium, MT.MD.BR.mID, AttributionKind.COMMON_SECONDARY), // :2385 COMMON_ORE
			new Row(() -> MT.Cyanite, MT.MD.BR.mID, AttributionKind.COMMON_SECONDARY), // :2386 COMMON_ORE
			new Row(() -> MT.Ludicrite, MT.MD.BR.mID, AttributionKind.COMMON_SECONDARY), // :2387 COMMON_ORE
			new Row(() -> MT.Yellorite, MT.MD.BR.mID, AttributionKind.COMMON_SECONDARY), // :2388 COMMON_ORE
			// ---- MD.ReC "ReactorCraft" (upstream MT.java:2411-2412) ----
			new Row(() -> MT.In, MT.MD.ReC.mID, AttributionKind.PRIMARY), // :2411
			new Row(() -> MT.TungstenCarbide, MT.MD.ReC.mID, AttributionKind.PRIMARY), // :2412
			// ---- MD.RoC "RotaryCraft" (upstream MT.java:2415-2431; :2416/:2423 and :2417/:2424 are
			// upstream duplicate statements of Prismane/Lonsdaleite — one row each here) ----
			new Row(() -> MT.Anthracite, MT.MD.RoC.mID, AttributionKind.COMMON_SECONDARY), // :2415 COMMON_ORE
			new Row(() -> MT.Prismane, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2416 (dup :2423)
			new Row(() -> MT.Lonsdaleite, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2417 (dup :2424)
			new Row(() -> MT.Lubricant, MT.MD.RoC.mID, AttributionKind.COMMON_SECONDARY), // :2418 — GT6 own lubricant GTFluids:2535/:2543 (Loader_Fluids.java:617), "rc lubricant" is the alias face
			new Row(() -> MT.F, MT.MD.RoC.mID, AttributionKind.COMMON_SECONDARY), // :2419 — upstream: "don't COMMON_ORE this!"; internal name "fluorine" rides GT6's own chemical row GTFluids:2211 (mdh-3 ratchet), never-hide conservative
			new Row(() -> MT.CaF2, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2420
			new Row(() -> MT.AgI, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2421
			new Row(() -> MT.InductiveAlloy, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2422
			new Row(() -> MT.Cd_In_Ag_Alloy, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2425
			new Row(() -> MT.HSLA, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2426
			new Row(() -> MT.SpringSteel, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2427
			new Row(() -> MT.AluminiumAlloy, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2428
			new Row(() -> MT.TungstenAlloy, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2429
			new Row(() -> MT.TungstenSintered, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2430
			new Row(() -> MT.Bedrock_HSLA_Alloy, MT.MD.RoC.mID, AttributionKind.PRIMARY), // :2431
			// ---- MD.Mek "Mekanism" (upstream MT.java:2434-2438) ----
			new Row(() -> MT.RefinedGlowstone, MT.MD.Mek.mID, AttributionKind.PRIMARY), // :2434
			new Row(() -> MT.RefinedObsidian, MT.MD.Mek.mID, AttributionKind.PRIMARY), // :2435
			new Row(() -> MT.Ge, MT.MD.Mek.mID, AttributionKind.COMMON_SECONDARY), // :2436 COMMON_ORE — SPEC secondary pair
			new Row(() -> MT.Basic, MT.MD.Mek.mID, AttributionKind.PRIMARY), // :2437
			new Row(() -> MT.Elite, MT.MD.Mek.mID, AttributionKind.PRIMARY), // :2438
			// ---- MD.TC "Thaumcraft" (upstream MT.java:2441-2449) ----
			new Row(() -> MT.InfusedVis, MT.MD.TC.mID, AttributionKind.PRIMARY), // :2441
			new Row(() -> MT.Silverwood, MT.MD.TC.mID, AttributionKind.PRIMARY), // :2442
			new Row(() -> MT.Greatwood, MT.MD.TC.mID, AttributionKind.PRIMARY), // :2443
			new Row(() -> MT.Tallow, MT.MD.TC.mID, AttributionKind.PRIMARY), // :2444
			new Row(() -> MT.VoidMetal, MT.MD.TC.mID, AttributionKind.PRIMARY), // :2445
			new Row(() -> MT.Thaumium, MT.MD.TC.mID, AttributionKind.COMMON_SECONDARY), // :2446 COMMON_ORE — shared thaum-nexus material 待复核
			new Row(() -> MT.Amber, MT.MD.TC.mID, AttributionKind.COMMON_SECONDARY), // :2447 COMMON_ORE
			new Row(() -> MT.Hg, MT.MD.TC.mID, AttributionKind.COMMON_SECONDARY), // :2448 COMMON_ORE — SPEC secondary pair
			new Row(() -> MT.OREMATS.Cinnabar, MT.MD.TC.mID, AttributionKind.COMMON_SECONDARY), // :2449 COMMON_ORE
			// ---- MD.TCTE "ThaumcraftExtras" (upstream MT.java:2452) ----
			new Row(() -> MT.DarkThaumium, MT.MD.TCTE.mID, AttributionKind.PRIMARY), // :2452
			// ---- MD.ALF "alfheim" (upstream MT.java:2474-2478) ----
			new Row(() -> MT.Mauftrium, MT.MD.ALF.mID, AttributionKind.PRIMARY), // :2474
			new Row(() -> MT.Elvorium, MT.MD.ALF.mID, AttributionKind.PRIMARY), // :2475
			new Row(() -> MT.MuspelheimPower, MT.MD.ALF.mID, AttributionKind.PRIMARY), // :2476
			new Row(() -> MT.NiflheimPower, MT.MD.ALF.mID, AttributionKind.PRIMARY), // :2477
			new Row(() -> MT.Iffesal, MT.MD.ALF.mID, AttributionKind.PRIMARY), // :2478
			// ---- MD.CANDY "candycraftmod" (upstream MT.java:2481-2484) ----
			new Row(() -> MT.PEZ, MT.MD.CANDY.mID, AttributionKind.PRIMARY), // :2481
			new Row(() -> MT.Licorice, MT.MD.CANDY.mID, AttributionKind.PRIMARY), // :2482
			new Row(() -> MT.Nougat, MT.MD.CANDY.mID, AttributionKind.PRIMARY), // :2483
			new Row(() -> MT.Marshmallow, MT.MD.CANDY.mID, AttributionKind.PRIMARY), // :2484
			// ---- MD.GC_ADV_ROCKETRY "advancedRocketry" (upstream MT.java:2487-2488) ----
			new Row(() -> MT.Iritanium, MT.MD.GC_ADV_ROCKETRY.mID, AttributionKind.PRIMARY), // :2487
			new Row(() -> MT.TitaniumAluminide, MT.MD.GC_ADV_ROCKETRY.mID, AttributionKind.PRIMARY), // :2488
			// ---- MD.HEE "HardcoreEnderExpansion" (upstream MT.java:2491-2492) ----
			new Row(() -> MT.Endium, MT.MD.HEE.mID, AttributionKind.COMMON_SECONDARY), // :2491 COMMON_ORE
			new Row(() -> MT.OREMATS.Sphalerite, MT.MD.HEE.mID, AttributionKind.COMMON_SECONDARY), // :2492 COMMON_ORE
			// ---- MD.MaCu "Mariculture" (upstream MT.java:2495-2500) ----
			new Row(() -> MT.Ti, MT.MD.MaCu.mID, AttributionKind.COMMON_SECONDARY), // :2495 — SPEC secondary pair (upstream: "don't COMMON_ORE this!"; GT6 titanium core, 20 port files)
			new Row(() -> MT.TiO2, MT.MD.MaCu.mID, AttributionKind.COMMON_SECONDARY), // :2496 COMMON_ORE
			new Row(() -> MT.FishCooked, MT.MD.MaCu.mID, AttributionKind.PRIMARY), // :2497
			new Row(() -> MT.FishRaw, MT.MD.MaCu.mID, AttributionKind.PRIMARY), // :2498
			new Row(() -> MT.FishRotten, MT.MD.MaCu.mID, AttributionKind.PRIMARY), // :2499
			new Row(() -> MT.FishOil, MT.MD.MaCu.mID, AttributionKind.COMMON_SECONDARY), // :2500 — GT6 own food-oil family GTFluids:1120, never-hide conservative
			// ---- MD.ABYSSAL "abyssalcraft" (upstream MT.java:2503-2506) ----
			new Row(() -> MT.An, MT.MD.ABYSSAL.mID, AttributionKind.COMMON_SECONDARY), // :2503 COMMON_ORE
			new Row(() -> MT.Cor, MT.MD.ABYSSAL.mID, AttributionKind.COMMON_SECONDARY), // :2504 COMMON_ORE
			new Row(() -> MT.Dr, MT.MD.ABYSSAL.mID, AttributionKind.COMMON_SECONDARY), // :2505 COMMON_ORE
			new Row(() -> MT.Etx, MT.MD.ABYSSAL.mID, AttributionKind.COMMON_SECONDARY), // :2506 COMMON_ORE
			// ---- MD.Fossil "fossil" (upstream MT.java:2509) ----
			new Row(() -> MT.AmberDominican, MT.MD.Fossil.mID, AttributionKind.COMMON_SECONDARY), // :2509 COMMON_ORE
			// ---- MD.DE "DraconicEvolution" (upstream MT.java:2512-2513) ----
			new Row(() -> MT.Draconium, MT.MD.DE.mID, AttributionKind.COMMON_SECONDARY), // :2512 COMMON_ORE
			new Row(() -> MT.DraconiumAwakened, MT.MD.DE.mID, AttributionKind.PRIMARY), // :2513
			// ---- MD.AV "Avaritia" (upstream MT.java:2516-2518) ----
			new Row(() -> MT.CrystalMatrix, MT.MD.AV.mID, AttributionKind.PRIMARY), // :2516
			new Row(() -> MT.CosmicNeutronium, MT.MD.AV.mID, AttributionKind.PRIMARY), // :2517
			new Row(() -> MT.Infinity, MT.MD.AV.mID, AttributionKind.PRIMARY), // :2518
			// ---- MD.PE "ProjectE" (upstream MT.java:2521-2522) ----
			new Row(() -> MT.DarkMatter, MT.MD.PE.mID, AttributionKind.PRIMARY), // :2521
			new Row(() -> MT.RedMatter, MT.MD.PE.mID, AttributionKind.PRIMARY), // :2522
			// ---- MD.TROPIC "tropicraft" (upstream MT.java:2525-2528) ----
			new Row(() -> MT.Zr, MT.MD.TROPIC.mID, AttributionKind.PRIMARY), // :2525
			new Row(() -> MT.Zircon, MT.MD.TROPIC.mID, AttributionKind.PRIMARY), // :2526
			new Row(() -> MT.Azurite, MT.MD.TROPIC.mID, AttributionKind.PRIMARY), // :2527
			new Row(() -> MT.Eudialyte, MT.MD.TROPIC.mID, AttributionKind.PRIMARY), // :2528
			// ---- MD.BoP "BiomesOPlenty" (upstream MT.java:2531-2534) ----
			new Row(() -> MT.Topaz, MT.MD.BoP.mID, AttributionKind.COMMON_SECONDARY), // :2531 COMMON_ORE
			new Row(() -> MT.Peridot, MT.MD.BoP.mID, AttributionKind.COMMON_SECONDARY), // :2532 COMMON_ORE
			new Row(() -> MT.Amethyst, MT.MD.BoP.mID, AttributionKind.COMMON_SECONDARY), // :2533 COMMON_ORE
			new Row(() -> MT.EnderAmethyst, MT.MD.BoP.mID, AttributionKind.COMMON_SECONDARY), // :2534 COMMON_ORE
			// ---- MD.FM "meteors" (upstream MT.java:2537-2541) ----
			new Row(() -> MT.Meteorite, MT.MD.FM.mID, AttributionKind.COMMON_SECONDARY), // :2537 COMMON_ORE
			new Row(() -> MT.FrozenIron, MT.MD.FM.mID, AttributionKind.PRIMARY), // :2538
			new Row(() -> MT.Kreknorite, MT.MD.FM.mID, AttributionKind.PRIMARY), // :2539
			new Row(() -> MT.RedMeteor, MT.MD.FM.mID, AttributionKind.PRIMARY), // :2540
			new Row(() -> MT.Frezarite, MT.MD.FM.mID, AttributionKind.PRIMARY), // :2541
			// ---- MD.ARS "arsmagica2" (upstream MT.java:2544-2551) ----
			new Row(() -> MT.Vinteum, MT.MD.ARS.mID, AttributionKind.COMMON_SECONDARY), // :2544 COMMON_ORE
			new Row(() -> MT.VinteumPurified, MT.MD.ARS.mID, AttributionKind.PRIMARY), // :2545
			new Row(() -> MT.ArcaneAsh, MT.MD.ARS.mID, AttributionKind.PRIMARY), // :2546
			new Row(() -> MT.ArcaneCompound, MT.MD.ARS.mID, AttributionKind.PRIMARY), // :2547
			new Row(() -> MT.Moonstone, MT.MD.ARS.mID, AttributionKind.COMMON_SECONDARY), // :2548 COMMON_ORE
			new Row(() -> MT.Sunstone, MT.MD.ARS.mID, AttributionKind.COMMON_SECONDARY), // :2549 COMMON_ORE
			new Row(() -> MT.Chimerite, MT.MD.ARS.mID, AttributionKind.COMMON_SECONDARY), // :2550 COMMON_ORE
			new Row(() -> MT.BlueTopaz, MT.MD.ARS.mID, AttributionKind.COMMON_SECONDARY), // :2551 COMMON_ORE
			// ---- MD.GC "GalacticraftCore" (upstream MT.java:2554-2563) ----
			new Row(() -> MT.MeteoricIron, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2554 — GT6 own meteor face (GT6SurfaceBlocks + GT6LootTables/GT6LootInjectionDatagen) 待复核
			new Row(() -> MT.MeteoricSteel, MT.MD.GC.mID, AttributionKind.PRIMARY), // :2555
			new Row(() -> MT.Desh, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2556 COMMON_ORE
			new Row(() -> MT.Cheese, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2557 COMMON_ORE (its chemical row cheese_molten stays un-annotated, mdh-3 ratchet)
			new Row(() -> MT.STONES.MoonTurf, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2558 — GT6RecipesMixer:169 CFoam light-gray group
			new Row(() -> MT.STONES.MoonRock, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2559 — GT6RecipesMixer:169 + stone-suite face
			new Row(() -> MT.STONES.MarsSand, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2560 — GT6 stone-suite face
			new Row(() -> MT.STONES.MarsRock, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2561 — GT6 stone-suite face
			new Row(() -> MT.STONES.SpaceRock, MT.MD.GC.mID, AttributionKind.COMMON_SECONDARY), // :2562 — GT6 stone-suite face
			new Row(() -> MT.Ultimate, MT.MD.GC.mID, AttributionKind.PRIMARY), // :2563
			// ---- MD.GC_GALAXYSPACE "GalaxySpace" (upstream MT.java:2595-2602) ----
			new Row(() -> MT.Duralumin, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.COMMON_SECONDARY), // :2595 COMMON_ORE
			new Row(() -> MT.Oriharukon, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.COMMON_SECONDARY), // :2596 COMMON_ORE
			new Row(() -> MT.Adamantite, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.COMMON_SECONDARY), // :2597 COMMON_ORE
			new Row(() -> MT.GlowstoneCeres, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.PRIMARY), // :2598
			new Row(() -> MT.GlowstoneIo, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.PRIMARY), // :2599
			new Row(() -> MT.GlowstoneEnceladus, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.PRIMARY), // :2600
			new Row(() -> MT.GlowstoneProteus, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.PRIMARY), // :2601
			new Row(() -> MT.GlowstonePluto, MT.MD.GC_GALAXYSPACE.mID, AttributionKind.PRIMARY), // :2602
			// ---- MD.MO "mo" (upstream MT.java:2605-2608) ----
			new Row(() -> MT.Tn, MT.MD.MO.mID, AttributionKind.PRIMARY), // :2605 — upstream: "don't COMMON_ORE this!"
			new Row(() -> MT.TritaniumAlloy, MT.MD.MO.mID, AttributionKind.COMMON_SECONDARY), // :2606 COMMON_ORE
			new Row(() -> MT.Dilithium, MT.MD.MO.mID, AttributionKind.PRIMARY), // :2607 — upstream: "don't COMMON_ORE this!"
			new Row(() -> MT.Dolamide, MT.MD.MO.mID, AttributionKind.PRIMARY), // :2608
			// ---- MD.RT "RandomThings" (upstream MT.java:2611-2612) ----
			new Row(() -> MT.SpectreIron, MT.MD.RT.mID, AttributionKind.PRIMARY), // :2611
			new Row(() -> MT.Ectoplasm, MT.MD.RT.mID, AttributionKind.PRIMARY), // :2612
			// ---- MD.ExU "ExtraUtilities" (upstream MT.java:2615-2616) ----
			new Row(() -> MT.Unstable, MT.MD.ExU.mID, AttributionKind.PRIMARY), // :2615
			new Row(() -> MT.Bedrockium, MT.MD.ExU.mID, AttributionKind.PRIMARY), // :2616
			// ---- MD.BTL "thebetweenlands" (upstream MT.java:2619-2631) ----
			new Row(() -> MT.CrimsonMiddle, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2619
			new Row(() -> MT.GreenMiddle, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2620
			new Row(() -> MT.AquaMiddle, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2621
			new Row(() -> MT.Valonite, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2622
			new Row(() -> MT.Scabyst, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2623
			new Row(() -> MT.SlimyBone, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2624
			new Row(() -> MT.STONES.Betweenstone, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2625 — GT6 stone-suite face
			new Row(() -> MT.STONES.Pitstone, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2626 — GT6 stone-suite face
			new Row(() -> MT.STONES.Cragrock, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2627 — GT6 stone-suite face
			new Row(() -> MT.STONES.Templerock, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2628 — GT6 stone-suite face
			new Row(() -> MT.Weedwood, MT.MD.BTL.mID, AttributionKind.PRIMARY), // :2629
			new Row(() -> MT.Syrmorite, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2630 COMMON_ORE
			new Row(() -> MT.Octine, MT.MD.BTL.mID, AttributionKind.COMMON_SECONDARY), // :2631 COMMON_ORE
			// ---- MD.AETHER "aether" (upstream MT.java:2634-2640) ----
			new Row(() -> MT.Skyroot, MT.MD.AETHER.mID, AttributionKind.PRIMARY), // :2634
			new Row(() -> MT.STONES.Holystone, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2635 — GT6RecipesMixer:165 CFoam white group
			new Row(() -> MT.Zanite, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2636 COMMON_ORE
			new Row(() -> MT.AmberGolden, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2637 COMMON_ORE
			new Row(() -> MT.Ambrosium, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2638 COMMON_ORE
			new Row(() -> MT.Gravitite, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2639 COMMON_ORE
			new Row(() -> MT.Continuum, MT.MD.AETHER.mID, AttributionKind.COMMON_SECONDARY), // :2640 COMMON_ORE
			// ---- MD.RP "Redpower" (upstream MT.java:2643-2656; :2657 EnergiumCyan has no put() and
			// :2655 NikolineAlloy is deferred — javadoc) ----
			new Row(() -> MT.W, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2643 — SPEC secondary pair (upstream: "don't COMMON_ORE this!"; tungsten core, 16 port files)
			new Row(() -> MT.Ag, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2644 COMMON_ORE — silver, 9 port files
			new Row(() -> MT.Indigo, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2645 — GT6-created chemical row "indigo" GTFluids:2794 (ratchet) 待复核
			new Row(() -> MT.Sapphire, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2646 — GT6 worldgen face (GTOreWorldgen/GT6OreBlocks/GT6WorldgenDatagen/GT6SurfaceBlocks)
			new Row(() -> MT.GreenSapphire, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2647 — GT6 worldgen face
			new Row(() -> MT.BlueSapphire, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2648 — GT6 worldgen face
			new Row(() -> MT.Ruby, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2649 — GT6 worldgen face + GT6RecipesImplosion
			new Row(() -> MT.BalasRuby, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2650 — GT6 worldgen face
			new Row(() -> MT.STONES.Marble, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2651 — GT6 stone suite GTStoneBlocks:95 + GT6RecipesMixer:164
			new Row(() -> MT.Brass, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2652 — shared alloy, 7 port files 待复核
			new Row(() -> MT.RedAlloy, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2653 — GTWireSpecs case-map + GT6CraftingRecipes
			new Row(() -> MT.Nikolite, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2654 COMMON_ORE — also GT6 worldgen face (GT6OreBlocks/GTOreWorldgen)
			new Row(() -> MT.BlueAlloy, MT.MD.RP.mID, AttributionKind.COMMON_SECONDARY), // :2656 — GTWireSpecs case-map
			// ---- MD.PR "ProjRed|Core" (upstream MT.java:2660) ----
			new Row(() -> MT.ElectrotineAlloy, MT.MD.PR.mID, AttributionKind.PRIMARY), // :2660
			// ---- MD.BP "bluepower" (upstream MT.java:2663) ----
			new Row(() -> MT.PurpleAlloy, MT.MD.BP.mID, AttributionKind.PRIMARY), // :2663
			// ---- MD.FZ "factorization" (upstream MT.java:2666-2670) ----
			new Row(() -> MT.Pb, MT.MD.FZ.mID, AttributionKind.COMMON_SECONDARY), // :2666 COMMON_ORE — SPEC secondary pair (15 port files)
			new Row(() -> MT.OREMATS.Galena, MT.MD.FZ.mID, AttributionKind.COMMON_SECONDARY), // :2667 COMMON_ORE
			new Row(() -> MT.H2SO4, MT.MD.FZ.mID, AttributionKind.PRIMARY), // :2668
			new Row(() -> MT.AquaRegia, MT.MD.FZ.mID, AttributionKind.PRIMARY), // :2669
			new Row(() -> MT.DarkIron, MT.MD.FZ.mID, AttributionKind.PRIMARY), // :2670
			// ---- MD.PFAA "PFAAGeologica" (upstream MT.java:2673-2677) ----
			new Row(() -> MT.Bentonite, MT.MD.PFAA.mID, AttributionKind.PRIMARY), // :2673
			new Row(() -> MT.Palygorskite, MT.MD.PFAA.mID, AttributionKind.PRIMARY), // :2674
			new Row(() -> MT.Kaolinite, MT.MD.PFAA.mID, AttributionKind.PRIMARY), // :2675
			new Row(() -> MT.OREMATS.BasalticMineralSand, MT.MD.PFAA.mID, AttributionKind.PRIMARY), // :2676
			new Row(() -> MT.OREMATS.GraniticMineralSand, MT.MD.PFAA.mID, AttributionKind.PRIMARY), // :2677
			// ---- MD.UB "UndergroundBiomes" (upstream MT.java:2680) ----
			new Row(() -> MT.Lignite, MT.MD.UB.mID, AttributionKind.COMMON_SECONDARY)); // :2680 COMMON_ORE

	/** Lazy PRIMARY index over material internal names — built after MT init, never at class-load. */
	private static volatile Map<String, String> primaryByName;

	/** The full table (batch 1 + batch 2), read-only, upstream block order preserved. */
	private static final List<Row> ALL_ROWS = buildAllRows();

	private static List<Row> buildAllRows() {
		List<Row> rRows = new ArrayList<>(ROWS.size() + BATCH2_ROWS.size());
		rRows.addAll(ROWS);
		rRows.addAll(BATCH2_ROWS);
		return List.copyOf(rRows);
	}

	/** The whole attribution table, batch 1 followed by batch 2 (read-only). */
	public static List<Row> rows() {
		return ALL_ROWS;
	}

	/**
	 * The distinct PRIMARY-row domains of the WHOLE table (batch 1 + batch 2, upstream block
	 * order preserved by the append-only prefix property) — exactly what
	 * {@code GT6ModDrivers.SEEDED_DOMAINS} walks at environment-seed time (a domain with only
	 * COMMON_SECONDARY/GT6_SELF rows would have nothing to hide and must not enter the seed).
	 *
	 * <p>Driven by the table rows, never a hand-copied list (the mdh-clearout-batch2 ruling):
	 * the batch-2 rows joined the walk when the clear-out card landed the activation. The
	 * GT5U rows carry our OWN modid ("gregtech" = self) — a domain that is by definition
	 * always loaded, so it never enters the seed (the atlas javadoc's never-seedable note).
	 */
	public static List<String> seedableDomains() {
		List<String> rDomains = new ArrayList<>();
		for (Row tRow : ALL_ROWS) {
			if (tRow.kind() != AttributionKind.PRIMARY) continue;
			if (tRow.domain().equals(MT.MD.GT5U.mID)) continue; // "gregtech" = our own modid — always loaded, never seedable
			if (!rDomains.contains(tRow.domain())) rDomains.add(tRow.domain());
		}
		return rDomains;
	}

	/**
	 * The production attribution lookup consumed by {@code GT6ModDrivers.materialDomain}:
	 * PRIMARY rows answer their owning modid, everything else answers null — a null domain is
	 * unattributed and always visible, which is how COMMON_SECONDARY and GT6_SELF rows survive
	 * an ABSENT pin (ADR-MDH2, pinned by the atlas test's absent-domain arms).
	 */
	public static String domainOf(OreDictMaterial aMaterial) {
		if (aMaterial == null) return null;
		Map<String, String> tIndex = primaryByName;
		if (tIndex == null) {
			tIndex = buildPrimaryIndex();
			primaryByName = tIndex;
		}
		return tIndex.get(aMaterial.mNameInternal);
	}

	/** Keyed by {@code mNameInternal}: the enumerate gate sees registry-resolved targets, not MT field identity. */
	private static Map<String, String> buildPrimaryIndex() {
		Map<String, String> rIndex = new LinkedHashMap<>();
		for (Row tRow : ALL_ROWS) {
			if (tRow.kind() != AttributionKind.PRIMARY) continue;
			OreDictMaterial tMaterial = tRow.material().get();
			if (tMaterial != null && tMaterial.mID >= 0) rIndex.putIfAbsent(tMaterial.mNameInternal, tRow.domain());
		}
		return rIndex;
	}

	private GT6ForeignMaterialAtlas() {
	}
}
