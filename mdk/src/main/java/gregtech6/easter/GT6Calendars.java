/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregtech6.easter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * The date-flag skeleton + the April Fools' pack (task easter-s2-date-flags-fools, the
 * S2 card of research.easter-egg-census id1451).
 *
 * <p><b>The four flags</b> (CS.java:867-873 "Date based Shenanigans"): statically computed
 * ONCE at class load from the wall clock — {@code new Date().getMonth()+1/getDate()}
 * upstream, {@link #computeFlags(long)} here with the same default-timezone semantics.
 * Upstream never refreshes across midnight either, so a long-running JVM keeps stale flags:
 * same behaviour kept and declared. The windows pin the CODE truth, which is one day
 * earlier than the comments claim on three flags (:870-873 — "first two days of the month"
 * = Apr 1-2, but June >= 20 / July >= 23 / December >= 5 say "one day early then til end
 * of month"):
 * <ul>
 * <li>{@link #APRIL_FOOLS} — April 1-2</li>
 * <li>{@link #WOODMANS_BDAY} — June 20-30</li>
 * <li>{@link #XMAS_IN_JULY} — July 23-31</li>
 * <li>{@link #XMAS_IN_DECEMBER} — December 5-31</li>
 * </ul>
 *
 * <p><b>The config test channel</b> (GT_API.java:359-361): the three debug keys
 * {@code april_fools}/{@code xmas_july}/{@code xmas_december}, default false, ONE-WAY
 * force-true only (upstream {@code if (get(...)) FLAG = T;} — config can never disable a
 * live window; WOODMANS_BDAY has no config key upstream, kept). Upstream reads them from
 * the clientside GregTech.cfg through gregapi's own Config class — a face this port never
 * had — so the channel here is a plain {@link Properties} file
 * {@code config/gt6-calendars.properties} (java.util.Properties, stdlib; the port carries
 * zero ForgeConfigSpec surface and this stays a debug channel, not a settings page).
 *
 * <p><b>The April Fools rename table</b> (GT_API.java:363-477, census 2026-10-04): 112
 * explicit {@code setLocal} rows (the census' "~90" undercounted; every one of the 112
 * renamed materials exists in the port's MT registry) plus the wood-suffix loop :477
 * (every material whose current local name contains "wood" gains " >:] nice" — IronWood
 * is renamed "Irunwood" by :394 FIRST, so the loop hits it as "Irunwood >:] nice", the
 * upstream order). {@link #sFoolRenames} machine-pins the explicit count for the census.
 *
 * <p><b>The display seam</b> — setLocal alone cannot change 1.20.1 display names (they
 * route through the {@code gt6.material.<snake>} lang keys), so the consumers read through
 * {@link gregtech6.item.MaterialPrefixItem#materialFill} and
 * {@link #foolFill(OreDictPrefix, OreDictMaterial)} which switch to the live
 * {@code mNameLocal} word while {@link #APRIL_FOOLS} is up.
 *
 * <p><b>Dormant consumption points</b> (ported flags have no carrier in the port yet):
 * <ul>
 * <li>"Schrödingers Ore" (LanguageHandler.java:167) — the carrier face "Unidentified Ore"
 *     was never ported: it applies to ORE-prefix items of materials mID &lt; 10, and the
 *     port only registers ore blocks through {@code OP.ore.isGeneratingItem}, which the
 *     pseudo-materials (Empty/Photon/Neutrino/Neutron/Proton/Electron) never pass.</li>
 * <li>The spike-block LEGO face (BlockBaseSpike.java:193/199/207 rainbow render passes +
 *     DamageSourceSpike.java:40 "stepped on a LEGO!") — the port has no spike block
 *     domain (grep Spike over mdk+gregapi main: zero domain hits); lands with the spike
 *     block card.</li>
 * </ul>
 */
public class GT6Calendars {

	/** 1st of April, first two days of the month (CS.java:870). */
	public static volatile boolean APRIL_FOOLS = false;
	/** 21st of June — the CODE window opens one day early, June 20 (CS.java:871). */
	public static volatile boolean WOODMANS_BDAY = false;
	/** 24th of July — the CODE window opens one day early, July 23 (CS.java:872). */
	public static volatile boolean XMAS_IN_JULY = false;
	/** 6th of December — the CODE window opens one day early, December 5 (CS.java:873). */
	public static volatile boolean XMAS_IN_DECEMBER = false;

	/** The explicit-table row count after {@link #ensureFoolsApplied()} (the census pin; the wood loop tail is dynamic). */
	public static volatile int sFoolRenames = 0;

	private static boolean sFoolsApplied = false;

	static {
		// upstream: flags are static initializers on CS — computed once, never refreshed
		refreshFlags(System.currentTimeMillis());
	}

	/**
	 * The pure window function over an injected clock (CS.java:870-873 verbatim windows,
	 * default-zone day semantics — {@code new Date().getMonth()+1/getDate()}).
	 * Order: APRIL_FOOLS, WOODMANS_BDAY, XMAS_IN_JULY, XMAS_IN_DECEMBER.
	 */
	public static boolean[] computeFlags(long aEpochMillis) {
		LocalDate tDate = LocalDate.ofInstant(Instant.ofEpochMilli(aEpochMillis), ZoneId.systemDefault());
		int tMonth = tDate.getMonthValue(), tDay = tDate.getDayOfMonth();
		return new boolean[] {
			tMonth ==  4 && tDay <=  2, //  1st of April   , first two days of the month
			tMonth ==  6 && tDay >= 20, // 21st of June    , one day early then til end of month
			tMonth ==  7 && tDay >= 23, // 24th of July    , one day early then til end of month
			tMonth == 12 && tDay >=  5  //  6th of December, one day early then til end of month
		};
	}

	/** The one-shot clock binding (static init; not re-run — upstream never refreshes either). */
	private static void refreshFlags(long aEpochMillis) {
		boolean[] tFlags = computeFlags(aEpochMillis);
		APRIL_FOOLS = tFlags[0];
		WOODMANS_BDAY = tFlags[1];
		XMAS_IN_JULY = tFlags[2];
		XMAS_IN_DECEMBER = tFlags[3];
	}

	// -------------------------------------------------------------------------
	// The config test channel (GT_API.java:359-361) — three debug keys, default
	// false, one-way force-true only.
	// -------------------------------------------------------------------------

	/** The upstream debug key names, verbatim (GT_API.java:359-361). */
	public static final String
		KEY_APRIL_FOOLS = "april_fools", KEY_XMAS_JULY = "xmas_july", KEY_XMAS_DECEMBER = "xmas_december";

	/** The client-side override file (the port's stand-in for the GregTech.cfg [debug] section). */
	public static final String CONFIG_FILE_NAME = "gt6-calendars.properties";

	/**
	 * The one-way override latch: a true key forces its flag on, nothing ever forces one
	 * off (upstream {@code if (get(...)) FLAG = T;} shape). Unknown keys ignored;
	 * WOODMANS_BDAY has no key — parity with upstream.
	 */
	public static void applyOverrides(Properties aOverrides) {
		if (Boolean.parseBoolean(aOverrides.getProperty(KEY_APRIL_FOOLS, "false"))) APRIL_FOOLS = true;
		if (Boolean.parseBoolean(aOverrides.getProperty(KEY_XMAS_JULY, "false"))) XMAS_IN_JULY = true;
		if (Boolean.parseBoolean(aOverrides.getProperty(KEY_XMAS_DECEMBER, "false"))) XMAS_IN_DECEMBER = true;
		ensureFoolsApplied();
	}

	/**
	 * The production wire: reads {@code config/gt6-calendars.properties} if present.
	 * Swallows everything — a broken file must never kill the mod (and the FMLPaths
	 * lookup must never run outside a game). Called once from the {@code @Mod} constructor.
	 */
	public static void loadClientOverrides() {
		try {
			Properties tProps = new Properties();
			java.io.File tFile = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME).toFile();
			if (tFile.isFile()) try (java.io.FileReader tReader = new java.io.FileReader(tFile)) { tProps.load(tReader); }
			applyOverrides(tProps);
		} catch (Throwable t) {
			gregtech6.GT6Mod.LOGGER.warn("GT6 calendars: could not read {}", CONFIG_FILE_NAME, t);
		}
	}

	// -------------------------------------------------------------------------
	// The April Fools rename table (GT_API.java:363-477).
	// -------------------------------------------------------------------------

	/**
	 * Applies the rename table once, iff {@link #APRIL_FOOLS} and the material registry is
	 * up. Idempotent and retried on every display consult until it lands — the display
	 * seam can first fire before the registry fills (static-init ordering), and the config
	 * latch can flip the flag after class load; both paths converge here.
	 */
	public static void ensureFoolsApplied() {
		if (sFoolsApplied || !APRIL_FOOLS) return;
		if (MaterialRegistry.INSTANCE.MATERIAL_MAP.isEmpty()) return; // pre-init display never fools
		sFoolsApplied = true;
		sFoolRenames = 0;
		// GT_API.java:364-475, verbatim
		MT.W.setLocal("Wolframium"); sFoolRenames++;
		MT.V.setLocal("Vandalium"); sFoolRenames++;
		MT.B.setLocal("Boring"); sFoolRenames++;
		MT.S.setLocal("Sulphur"); sFoolRenames++;
		MT.K.setLocal("Kalium"); sFoolRenames++;
		MT.Na.setLocal("Natrium"); sFoolRenames++;
		MT.Ar.setLocal("Aragon"); sFoolRenames++;
		MT.Al.setLocal("Aluminum"); sFoolRenames++;
		MT.Ni.setLocal("Ferrous Metal"); sFoolRenames++;
		MT.Pt.setLocal("Shiny Metal"); sFoolRenames++;
		MT.Mithril.setLocal("Mana Infused Metal"); sFoolRenames++;
		MT.Hg.setLocal("Quicksilver"); sFoolRenames++;
		MT.Mo.setLocal("Molly-B"); sFoolRenames++;
		MT.Sb.setLocal("Anti-Money"); sFoolRenames++;
		MT.Tc.setLocal("Gregorium"); sFoolRenames++;
		MT.Si.setLocal("Silicone"); sFoolRenames++;
		MT.Cr.setLocal("Firefox"); sFoolRenames++;
		MT.Cu.setLocal("Cooper"); sFoolRenames++;
		MT.AnnealedCopper.setLocal("Anilled Cooper"); sFoolRenames++;
		MT.Mg.setLocal("Manganesium"); sFoolRenames++;
		MT.Mn.setLocal("Animenese"); sFoolRenames++;
		MT.As.setLocal("Arse Nick"); sFoolRenames++;
		MT.Br.setLocal("Bro, that's mine"); sFoolRenames++;
		MT.Kr.setLocal("Kryptonite"); sFoolRenames++;
		MT.Bi.setLocal("Biffmiff"); sFoolRenames++;
		MT.Sg.setLocal("Resistance is Futile"); sFoolRenames++;
		MT.Zr.setLocal("Diamond"); sFoolRenames++;
		MT.Au.setLocal("Pyrite"); sFoolRenames++;
		MT.Pyrite.setLocal("Gold"); sFoolRenames++;
		MT.Fe.setLocal("Irun"); sFoolRenames++;
		MT.IronWood.setLocal("Irunwood"); sFoolRenames++;
		MT.ShadowIron.setLocal("Shade Irun"); sFoolRenames++;
		MT.DarkIron.setLocal("Dank Irun"); sFoolRenames++;
		MT.MeteoricIron.setLocal("Metaur Irun"); sFoolRenames++;
		MT.GildedIron.setLocal("Guild Irun"); sFoolRenames++;
		MT.WroughtIron.setLocal("Wrecked Irun"); sFoolRenames++;
		MT.Steel.setLocal("Style"); sFoolRenames++;
		MT.RedSteel.setLocal("Rad Style"); sFoolRenames++;
		MT.BlueSteel.setLocal("Blu Style"); sFoolRenames++;
		MT.BlackSteel.setLocal("Afro Style"); sFoolRenames++;
		MT.MeteoricSteel.setLocal("Metaur Style"); sFoolRenames++;
		MT.MeteoricRedSteel.setLocal("Metaur Rad Style"); sFoolRenames++;
		MT.MeteoricBlueSteel.setLocal("Metaur Blu Style"); sFoolRenames++;
		MT.MeteoricBlackSteel.setLocal("Metaur Afro Style"); sFoolRenames++;
		MT.DamascusSteel.setLocal("Dank Style"); sFoolRenames++;
		MT.VanadiumSteel.setLocal("Vandalium Style"); sFoolRenames++;
		MT.TungstenSteel.setLocal("Wolf Style"); sFoolRenames++;
		MT.ShadowSteel.setLocal("Shade Style"); sFoolRenames++;
		MT.Steeleaf.setLocal("Style Leave"); sFoolRenames++;
		MT.Fireleaf.setLocal("Burn Leave"); sFoolRenames++;
		MT.Knightmetal.setLocal("Night Metal"); sFoolRenames++;
		MT.FierySteel.setLocal("Fury Style"); sFoolRenames++;
		MT.SteelGalvanized.setLocal("Galvanized Square Steel"); sFoolRenames++;
		MT.Thaumium.setLocal("Thaumanominum"); sFoolRenames++;
		MT.DarkThaumium.setLocal("Dank Thaumanominum"); sFoolRenames++;
		MT.VoidMetal.setLocal("Warranty Void Metal"); sFoolRenames++;
		MT.Coal.setLocal("Cool"); sFoolRenames++;
		MT.Charcoal.setLocal("Charred Cole"); sFoolRenames++;
		MT.Lapis.setLocal("Le Piss"); sFoolRenames++;
		MT.Redstone.setLocal("Blingstone"); sFoolRenames++;
		MT.Glowstone.setLocal("Klostein"); sFoolRenames++;
		MT.Emerald.setLocal("Chaos Emerald"); sFoolRenames++;
		MT.Craponite.setLocal("Pink Diamond"); sFoolRenames++;
		MT.Diamond.setLocal("Sapphire"); sFoolRenames++;
		MT.DiamondPink.setLocal("Craponite"); sFoolRenames++;
		MT.Bedrock.setLocal("Sofarock"); sFoolRenames++;
		MT.Plastic.setLocal("LEGO"); sFoolRenames++;
		MT.Teflon.setLocal("Polytetrafluoroethylene"); sFoolRenames++;
		MT.Asbestos.setLocal("Bestos"); sFoolRenames++;
		MT.AncientDebris.setLocal("Cinnabun"); sFoolRenames++;
		MT.Cinnamon.setLocal("Ancient Debris"); sFoolRenames++;
		MT.Wheat.setLocal("Gluten"); sFoolRenames++;
		MT.Milk.setLocal("Lactose"); sFoolRenames++;
		MT.WOODS.Acacia.setLocal("A Cha Cha"); sFoolRenames++;
		MT.WOODS.DarkOak.setLocal("Dork Oak"); sFoolRenames++;
		MT.WOODS.Darkwood.setLocal("Dork Wood"); sFoolRenames++;
		MT.WOODS.Cinnamon.setLocal("Ancient Debris"); sFoolRenames++;
		MT.WOODS.Foxfire.setLocal("Chrome"); sFoolRenames++;
		MT.Rb.setLocal("Ruby"); sFoolRenames++;
		MT.Ruby.setLocal("Red Sapphire"); sFoolRenames++;
		MT.KCl.setLocal("Sylveonite"); sFoolRenames++;
		MT.KNO3.setLocal("Niter"); sFoolRenames++;
		MT.NaNO3.setLocal("Nitre"); sFoolRenames++;
		MT.Glyceryl.setLocal("Nitro"); sFoolRenames++;
		MT.Gunpowder.setLocal("Crossbow Powder"); sFoolRenames++;
		MT.Lubricant.setLocal("Lube"); sFoolRenames++;
		MT.H2SO4.setLocal("Sulphuric Acid"); sFoolRenames++;
		MT.H2S2O7.setLocal("Disulphuric Acid"); sFoolRenames++;
		MT.STONES.Greenschist.setLocal("Green Shit"); sFoolRenames++;
		MT.STONES.Blueschist.setLocal("Blue Shit"); sFoolRenames++;
		MT.Nikolite.setLocal("Bluestone"); sFoolRenames++;
		MT.PigIron.setLocal("Ferrobacon"); sFoolRenames++;
		MT.TinAlloy.setLocal("Tin*"); sFoolRenames++;
		MT.Bronze.setLocal("Tinkerers Alloy"); sFoolRenames++;
		MT.ArsenicCopper.setLocal("Arsenine Alloy"); sFoolRenames++;
		MT.ArsenicBronze.setLocal("Arsenine Tinkerers Alloy"); sFoolRenames++;
		MT.BismuthBronze.setLocal("Biffmiff Tinkerers Alloy"); sFoolRenames++;
		MT.BlackBronze.setLocal("Afro Tinkerers Alloy"); sFoolRenames++;
		MT.Constantan.setLocal("Cupronickel"); sFoolRenames++;
		MT.Ge.setLocal("Platosmium"); sFoolRenames++;
		MT.Amazonite.setLocal("Bezosite"); sFoolRenames++;
		MT.NetherQuartz.setLocal("Weather Quartz"); sFoolRenames++;
		MT.MilkyQuartz.setLocal("Milk Quartz"); sFoolRenames++;
		MT.CertusQuartz.setLocal("Citrus Quartz"); sFoolRenames++;
		MT.ChargedCertusQuartz.setLocal("Charged Citrus Quartz"); sFoolRenames++;
		MT.Firestone.setLocal("Hot Garbage"); sFoolRenames++;
		MT.UUMatter.setLocal("UwU-Matter"); sFoolRenames++;
		MT.UUAmplifier.setLocal("UwU-Amplifier"); sFoolRenames++;
		MT.OREMATS.Galena.setLocal("Silverlead"); sFoolRenames++;
		MT.OREMATS.Huebnerite.setLocal("Boobnerite"); sFoolRenames++;
		MT.OREMATS.Bromargyrite.setLocal("Bromagnerite"); sFoolRenames++;
		MT.OREMATS.Chalcopyrite.setLocal("Chackapackerite"); sFoolRenames++;

		// GT_API.java:477 — the wood tail, AFTER the explicit rows (IronWood is already
		// "Irunwood" here, so the loop lands "Irunwood >:] nice", the upstream order)
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values())
			if (tMaterial.mNameLocal.toLowerCase(Locale.ROOT).contains("wood")) tMaterial.setLocal(tMaterial.mNameLocal + " >:] nice");
	}

	/** The bullet prefixes the Bolt face covers (LanguageHandler.java:171-177). */
	private static boolean isBulletGt(OreDictPrefix aPrefix) {
		return aPrefix == OP.bulletGtSmall || aPrefix == OP.bulletGtMedium || aPrefix == OP.bulletGtLarge;
	}

	/**
	 * The April-Fools fill word for a prefix x material pair — the display seam the
	 * MaterialPrefixItem.getName template fills with while {@link #APRIL_FOOLS} is up.
	 * Bullets become Bolts (LanguageHandler.java:169-179): Empty casings read
	 * "Bolt Shaft", anything else reads {@code <word> Bolt} over the (renamed) live word.
	 * Every other prefix: just the live local word. Callers guard on {@link #APRIL_FOOLS}.
	 */
	public static String foolFill(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		ensureFoolsApplied();
		if (isBulletGt(aPrefix)) return aMaterial == MT.Empty ? "Bolt Shaft" : aMaterial.mNameLocal + " Bolt";
		return aMaterial.mNameLocal;
	}

	// -------------------------------------------------------------------------
	// The login chat lines (GT_Client.java:117-122) — English verbatim, the
	// 1.7.10 chat-colour codes decomposed into styled siblings.
	// -------------------------------------------------------------------------

	/**
	 * The one-shot-per-session login messages in flag order (GT_Client.java:117-122):
	 * WOODMANS_BDAY first ({@code <#:]> Have a nice day!} — WHITE/GREEN/WHITE, :118),
	 * then APRIL_FOOLS ({@code <GregoriusT> Watch your Calendar!} — the CHAT_GREG prefix
	 * CS.java:323, WHITE/BLUE/WHITE). Empty list when no flag is up.
	 */
	public static List<Component> loginLines() {
		List<Component> rList = new ArrayList<>();
		if (WOODMANS_BDAY) {
			// :118 — LH.Chat.WHITE+"<"+LH.Chat.GREEN+">:]"+LH.Chat.WHITE+"> Have a nice day!"
			rList.add(Component.literal("<").withStyle(ChatFormatting.WHITE)
				.append(Component.literal(">:]").withStyle(ChatFormatting.GREEN))
				.append(Component.literal("> Have a nice day!").withStyle(ChatFormatting.WHITE)));
		}
		if (APRIL_FOOLS) {
			// :121 — CHAT_GREG + "Watch your Calendar!" (CHAT_GREG = CS.java:323)
			rList.add(Component.literal("<").withStyle(ChatFormatting.WHITE)
				.append(Component.literal("GregoriusT").withStyle(ChatFormatting.BLUE))
				.append(Component.literal("> Watch your Calendar!").withStyle(ChatFormatting.WHITE)));
		}
		return rList;
	}

	/** Test-only reset (offline pins need a clean slate; production never calls this). */
	public static void resetForTest() {
		APRIL_FOOLS = false;
		WOODMANS_BDAY = false;
		XMAS_IN_JULY = false;
		XMAS_IN_DECEMBER = false;
		sFoolRenames = 0;
		sFoolsApplied = false;
	}

	private GT6Calendars() {}
}
