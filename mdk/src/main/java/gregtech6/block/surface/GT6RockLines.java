package gregtech6.block.surface;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The rock magnifier census table (task easter-s4-rock-lines) — the upstream
 * MultiTileEntityRock magnifying-glass chat (placeables/MultiTileEntityRock.java:95-107),
 * verbatim. The ten dimension quips answer the WORLDGEN rock (mRock == null, :95-105);
 * the Flintstones row answers a flint-carrying rock (the itemFlint arm, :107).
 *
 * <p>Port dispatch: the vanilla trio rides the {@code Level.dimension()} key; the six
 * modded arms (Aether/Alfheim/Tropicraft + the Moon/Mars/Space biome rows) have no
 * dimension/biome registries in this port and stay unreachable — their LINES stay
 * pinned here so the census lives in one seam and a future cross-mod band wires them
 * without re-transcription. The chat-color face (upstream {@code LH.Chat.GRAY} prefixes)
 * rides out with the GT6Prospector chat precedent — the port rock chat is plain literals.
 *
 * <p>Date flags: the CS.java:870-873 rows the rock consumes, as pure month/day
 * functions. This is the LOCAL DEGRADATION seam for the not-yet-landed S2
 * (work/easter-s2-date-flags-fools): when S2 lands its four-flag set + client config
 * override, this trio folds into that seam (the review seat aligns the merge; the rock
 * never consumes S2's fourth row XMAS_IN_DECEMBER, CS.java:873).
 */
public final class GT6RockLines {

	// the ten dimension quips, upstream :95-105 order verbatim
	public static final String NETHER = "This is definitely a Rack"; // :95
	public static final String OVERWORLD = "This is definitely a Rock"; // :96
	public static final String END = "There is definitely an End"; // :97
	public static final String AETHER = "Holy $#!T, it's a Rock.."; // :98
	public static final String ALFHEIM = "Wait that Rock is alive?!"; // :99
	public static final String TROPIC = "Seems to be a Chunk o'Head"; // :100
	public static final String MOON = "This is definitely not made of Cheese"; // :101
	public static final String MARS = "This is definitely from Mars"; // :102
	public static final String SPACE = "This is definitely a Space Rock"; // :103
	public static final String FALLBACK = "This definitely is a Rock"; // :104

	// the flint row, upstream :107 verbatim
	public static final String FLINT = "It's a Flint";
	public static final String FLINTSTONES = "Flintstones, meet the Flintstones, they're the modern Stone Age family";

	private GT6RockLines() {}

	/**
	 * The dimension quip (upstream :95-105): the vanilla trio by dimension key, the
	 * :104 fallback for everything else (the modded arms stay unreachable, class doc).
	 */
	public static String dimensionLine(ResourceKey<Level> aDimension) {
		if (aDimension == Level.NETHER) return NETHER;
		if (aDimension == Level.OVERWORLD) return OVERWORLD;
		if (aDimension == Level.END) return END;
		return FALLBACK;
	}

	/**
	 * The Flintstones row (upstream :107): {@code APRIL_FOOLS || rng(divisor)==0} — the
	 * April-Fools leg short-circuits and never rolls the die; otherwise the bday
	 * 1/10, Xmas-in-July 1/100 and plain 1/1000 bands off {@link #flintDivisor}.
	 */
	public static String flintLine(RandomSource aRandom, boolean aAprilFools, boolean aWoodmansBday, boolean aXmasInJuly) {
		if (aAprilFools) return FLINTSTONES;
		return aRandom.nextInt(flintDivisor(aWoodmansBday, aXmasInJuly)) == 0 ? FLINTSTONES : FLINT;
	}

	/** The :107 divisor ternary verbatim: {@code WOODMANS_BDAY ? 10 : XMAS_IN_JULY ? 100 : 1000}. */
	static int flintDivisor(boolean aWoodmansBday, boolean aXmasInJuly) {
		return aWoodmansBday ? 10 : aXmasInJuly ? 100 : 1000;
	}
}
