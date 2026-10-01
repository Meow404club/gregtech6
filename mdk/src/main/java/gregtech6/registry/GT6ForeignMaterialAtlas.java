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
 * EnderIO 16, ThermalExpansion 16. Batch 2 (NOT yet transcribed — the remaining domains, in
 * upstream line order): EtFu 7 (:2080-2086), Salt 1 (:2110), GrC 1 (:2113), NePl 3
 * (:2116-2118), NeLi 6 (:2121-2126), EnLi 3 (:2129-2131), GT5U 4 (:2195-2198, modid
 * "gregtech" = own, excluded from the seed), IHL 9 (:2226-2234), BC 2 (:2237-2238), FR 15
 * (:2241-2255), FRMB 3 (:2258-2260), BINNIE 2 (:2263-2264), TFC 13 (:2267-2279), TF 14
 * (:2282-2295), ERE 4 (:2298-2301), RC 8 (:2304-2311), IE 2 (:2314-2315), AE 5
 * (:2336-2340), PnC 1 (:2343), SC2 2 (:2346-2347), TiC 6 (:2350-2355), AA 1 (:2358), MFR 3
 * (:2379-2381), BR 5 (:2384-2388), ReC 2 (:2411-2412), RoC 15 unique (:2415-2431, Prismane
 * and Lonsdaleite are upstream duplicate rows), Mek 5 (:2434-2438), TC 9 (:2441-2449),
 * TCTE 1 (:2452), ALF 5 (:2474-2478), CANDY 4 (:2481-2484), GC_ADV_ROCKETRY 2
 * (:2487-2488), HEE 2 (:2491-2492), MaCu 6 (:2495-2500), ABYSSAL 4 (:2503-2506), Fossil 1
 * (:2509), DE 2 (:2512-2513), AV 3 (:2516-2518), PE 2 (:2521-2522), TROPIC 4 (:2525-2528),
 * BoP 4 (:2531-2534), FM 5 (:2537-2541), ARS 8 (:2544-2551), GC 10 (:2554-2563),
 * GC_GALAXYSPACE 8 (:2595-2602), MO 4 (:2605-2608), RT 2 (:2611-2612), ExU 2 (:2615-2616),
 * BTL 13 (:2619-2631), AETHER 7 (:2634-2640), RP 14 (:2643-2656), PR 1 (:2660), BP 1
 * (:2663), FZ 5 (:2666-2670), PFAA 5 (:2673-2677), UB 1 (:2680). Also batch-2 candidates
 * outside the MD-constant shape: the domain-less {@code COMMON_ORE} rows (:2201
 * Superconductor, :2204 Os, :2719-2721 Force/Forcicium/Forcillium) and the 15
 * {@code unused()} HBM rows (:4056-4070, mID &lt; 0 = no registered items).
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

	/** Lazy PRIMARY index over material internal names — built after MT init, never at class-load. */
	private static volatile Map<String, String> primaryByName;

	/** The batch-1 rows (read-only). Batch-2 rows join this list as later cards transcribe them. */
	public static List<Row> rows() {
		return ROWS;
	}

	/**
	 * The distinct PRIMARY-row domains — exactly what {@code GT6ModDrivers.SEEDED_DOMAINS}
	 * walks at environment-seed time (a domain with only COMMON_SECONDARY/GT6_SELF rows would
	 * have nothing to hide and must not enter the seed).
	 */
	public static List<String> seedableDomains() {
		List<String> rDomains = new ArrayList<>();
		for (Row tRow : ROWS) {
			if (tRow.kind() == AttributionKind.PRIMARY && !rDomains.contains(tRow.domain())) rDomains.add(tRow.domain());
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
		for (Row tRow : ROWS) {
			if (tRow.kind() != AttributionKind.PRIMARY) continue;
			OreDictMaterial tMaterial = tRow.material().get();
			if (tMaterial != null && tMaterial.mID >= 0) rIndex.putIfAbsent(tMaterial.mNameInternal, tRow.domain());
		}
		return rIndex;
	}

	private GT6ForeignMaterialAtlas() {
	}
}
