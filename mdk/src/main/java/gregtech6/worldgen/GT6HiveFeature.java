package gregtech6.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.biome.Biome;

import net.minecraft.server.level.WorldGenRegion;

import gregtech6.block.stone.GTStoneBlock;
import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GT6BeeHives;
import gregtech6.tileentity.bees.GT6BumbleHiveBlockEntity;

/**
 * The bumble-hive worldgen Feature (task p32-bees-lv2) — the {@code WorldgenHives} port
 * (WorldgenHives.java:48-193) collapsed onto ONE registered feature
 * ({@code gt6:bumble_hives}; upstream was three WorldgenObject rows over the same
 * generator body — overworld/nether/end, Loader_Worldgen.java:635-637 — the modern
 * split is three biome modifiers over ONE placed feature, the END_YIELD large-vein
 * precedent). Dimension routing inside place(): nether → the embedded netherrack wall
 * form (:104-111), end → the 1/3-gated end-stone scan (:112-125), everything else →
 * the overworld pair (the DIM_UNKNOWN inclusion :126 means the overworld shape IS the
 * default — the GT6VeinGenerator.dimensionSalt routing convention).
 *
 * <p><b>Determinism</b>: the whole walk rides the coordinate-seeded house stream
 * ({@link GT6VeinGenerator#veinRandom}) — column pick :59 verbatim, the per-dimension
 * rolls after it in fixed order — so the placement decisions are a PURE coordinate
 * function (decision-level deterministic by construction,
 * decisions.2026-09-18-p31-strata-lens-determinism-acceptance; block survival rides the
 * pipeline drift like every other GT feature).
 *
 * <p><b>The colour/species table</b> ({@link HiveKind}): the family constants verbatim
 * from the upstream call sites — the embedded forms (:111/:120/:135) and the surface
 * first-hit chain (:155-186). The biome-name families ride the
 * {@code #gt6:bumble_hives/<family>} tags (the tree-tag pattern: vanilla members live,
 * modded biomes are the pack-extension surface — magical/volcanic/end/nether emit
 * EMPTY, the rainbowood precedent). {@code ponytail:} every kind carries the same
 * comb_honey product — the species-comb table (bumbleProductStack) is Lv3 gene/species
 * domain; the species traces stay in the table for that card.
 *
 * <p>KJS face (card declaration): the placed/configured/biome-modifier JSONs are the
 * tier-a datapack surface; the Feature instance is the registry face deferred to the
 * kjs binding card.
 */
public class GT6HiveFeature extends Feature<NoneFeatureConfiguration> {

	/** The family tag key ({@code #gt6:bumble_hives/<snake>}) — GT6BiomeTags emits the same composition. */
	public static TagKey<Biome> hiveTag(String aFamily) {
		return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", "bumble_hives/" + aFamily));
	}

	/**
	 * The hive family row: the colour (the paint the worldgen applies — the upstream
	 * DYE_INT_* / literal arguments to placeHive), the species trace (the upstream
	 * speciesID argument — the Lv3 princess/drone/comb wiring key), and the family tag
	 * for the biome-gated kinds (null = judged by block contact, not biome).
	 */
	public enum HiveKind {
		/** The embedded stone-cave form (:135, DYE_INT_LightGray, species 500). */
		STONE(0xC0C0C0, 500, null),
		/** The over-water overhang (:156 / the lake arm :168, DYE_INT_LightBlue, species 100). */
		WATER(0x8080FF, 100, null),
		/** The magical biome family (:158, DYE_INT_Purple, species 200) — EMPTY tag, pack surface. */
		MAGICAL(0x800080, 200, "magical"),
		/** The volcanic biome family (:160, DYE_INT_Black, species 300) — EMPTY tag, pack surface. */
		VOLCANIC(0x202020, 300, "volcanic"),
		/** The end-biome-name family (:162, literal 0x00aaaa, species 400) — EMPTY tag, pack surface. */
		END_BIOME(0x00AAAA, 400, "end"),
		/** The nether-biome-name family (:164, literal 0xaa0000, species 300) — EMPTY tag, pack surface. */
		NETHER_BIOME(0xAA0000, 300, "nether"),
		/** The shroom biome family (:166/:174 mycelium contact, DYE_INT_Pink, species 800). */
		SHROOM(0xFFC0C0, 800, "shroom"),
		/** The ocean/beach/lake family (:168, DYE_INT_LightBlue, species 100). */
		SHORE(0x8080FF, 100, "shore"),
		/** The jungle family (:170, DYE_INT_Green, species 600). */
		JUNGLE(0x00FF00, 600, "jungle"),
		/** The frozen family (:172, DYE_INT_White, species 700). */
		FROZEN(0xFFFFFF, 700, "frozen"),
		/** The red-sand contact (:176, DYE_INT_Red, species 900). */
		RED_SAND(0xFF0000, 900, null),
		/** The sand/sandstone contact (:178, DYE_INT_Yellow, species 900). */
		SAND(0xFFFF00, 900, null),
		/** The gravel/rock-material contact (:180, DYE_INT_LightGray, species 500). */
		ROCK(0xC0C0C0, 500, null),
		/** The grass contact (:182, literal 0xffdd99, species 0). */
		GRASS(0xFFDD99, 0, null),
		/** The dirt/ground contact (:184, DYE_INT_Brown, species 0). */
		DIRT(0x604000, 0, null),
		/** The magical-default fallthrough (:186, DYE_INT_Purple, species 200). */
		DEFAULT(0x800080, 200, null);

		/** The paint colour the worldgen applies (0xRRGGBB, the placeHive colour argument). */
		public final int color;
		/** The upstream speciesID argument (the Lv3 bee-item wiring trace). */
		public final int species;
		/** The {@code #gt6:bumble_hives/<snake>} family fragment; null = block-contact judged. */
		public final String tagFamily;

		HiveKind(int aColor, int aSpecies, String aTagFamily) {
			color = aColor;
			species = aSpecies;
			tagFamily = aTagFamily;
		}
	}

	/** The :148 side set — ALL_SIDES_HORIZONTAL_DOWN: the four y-1 ring cells + the y-1 column cell. */
	private static final Direction[] HANG_SIDES = {
			Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	public GT6HiveFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
		WorldGenLevel tLevel = aContext.level();
		ChunkPos tWork = tLevel instanceof WorldGenRegion
				? ((WorldGenRegion) tLevel).getCenter()
				: new ChunkPos(aContext.origin());
		// the coordinate-seeded stream — column pick WorldgenHives.java:59, the dimension
		// rolls after it in the fixed order the offline determinism test replays
		Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(),
				GT6VeinGenerator.dimensionSalt(tLevel), tWork.x, tWork.z);
		int tX = tWork.getMinBlockX() + tRandom.nextInt(16); // :59
		int tZ = tWork.getMinBlockZ() + tRandom.nextInt(16); // :59
		ResourceKey<Level> tDim = dimensionOf(tLevel);
		if (tDim == Level.NETHER) return generateNether(tLevel, tRandom, tX, tZ);
		if (tDim == Level.END) return generateEnd(tLevel, tRandom, tX, tZ);
		return generateOverworld(tLevel, tRandom, tX, tZ);
	}

	/** The concrete dimension of a worldgen level (the dimensionSalt unwrapping). */
	private static ResourceKey<Level> dimensionOf(WorldGenLevel aLevel) {
		return (aLevel instanceof Level tLevel ? tLevel : ((WorldGenRegion) aLevel).getLevel()).dimension();
	}

	/** The nether embedded form (:104-111): netherrack wall, 5-of-6 opaque faces, roof-length roll. */
	private boolean generateNether(WorldGenLevel aLevel, Random aRandom, int aX, int aZ) {
		// :105 — the y255 bedrock-roof probe picks the band length (OOB reads void air:
		// the modern vanilla nether tops at y127, so the 96 band applies, the 1.7.10 shape)
		boolean tRoofed = aLevel.getBlockState(new BlockPos(aX, 255, aZ)).is(Blocks.BEDROCK);
		int tY = 16 + aRandom.nextInt(tRoofed ? 224 : 96);
		if (!inBuild(aLevel, tY)) return false;
		if (!aLevel.getBlockState(new BlockPos(aX, tY, aZ)).is(Blocks.NETHERRACK)) return false; // :106
		if (!wallEmbed(aLevel, aX, tY, aZ)) return false; // :107-110
		return placeHive(aLevel, aX, tY, aZ, HiveKind.NETHER_BIOME, aRandom); // :111
	}

	/** The end form (:112-125): the 1/3 gate, then the end-stone scan with the wall check. */
	private boolean generateEnd(WorldGenLevel aLevel, Random aRandom, int aX, int aZ) {
		if (aRandom.nextInt(3) > 0) return false; // :113
		for (int tY = 16; tY < 128; tY++) { // :114
			if (!inBuild(aLevel, tY)) break;
			if (!aLevel.getBlockState(new BlockPos(aX, tY, aZ)).is(Blocks.END_STONE)) continue;
			if (wallEmbed(aLevel, aX, tY, aZ)) { // :115-118
				return placeHive(aLevel, aX, tY, aZ, HiveKind.END_BIOME, aRandom); // :120
			}
		}
		return false;
	}

	/**
	 * The overworld pair (:126-190): the embedded stone-cave form first (:127-140), then
	 * the hanging surface form (:142-189) — the highest opaque overhang whose below-side
	 * has a free cell, coloured by the first-hit family chain.
	 */
	private boolean generateOverworld(WorldGenLevel aLevel, Random aRandom, int aX, int aZ) {
		boolean rPlaced = false;
		for (int tY = 8; tY < 28; tY++) { // :127
			BlockState tState = aLevel.getBlockState(new BlockPos(aX, tY, aZ));
			if (!tState.is(BlockTags.STONE_ORE_REPLACEABLES) || !tState.canOcclude()) continue; // :129
			if (wallEmbed(aLevel, aX, tY, aZ)) { // :130-133
				rPlaced = placeHive(aLevel, aX, tY, aZ, HiveKind.STONE, aRandom); // :135
				break;
			}
		}

		// :142 — the descending surface scan (hasNoSky ? 80 : 256-50 → the modern
		// maxBuild-50 face; the hanging hive sits at tY-1 = directly under the contact)
		for (int tY = aLevel.getMaxBuildHeight() - 50; tY > 2; tY--) {
			if (!inBuild(aLevel, tY)) continue;
			BlockPos tContactPos = new BlockPos(aX, tY, aZ);
			BlockState tContact = aLevel.getBlockState(tContactPos);
			if (!tContact.getFluidState().isEmpty()) return rPlaced; // :144 liquid → stop
			// :145 — GT stone decorative variants (cobble/bricks, the meta≠0 face) never host
			if (tContact.getBlock() instanceof GTStoneBlock tStone && tStone.variant != StoneVariant.STONE) return rPlaced;
			// :146 — the opaque non-wood non-leaf non-ice host gate
			if (!tContact.canOcclude() || tContact.is(BlockTags.LEAVES) || tContact.is(BlockTags.LOGS)
					|| tContact.is(BlockTags.ICE)) continue;

			HiveKind tKind = null;
			for (Direction tSide : HANG_SIDES) { // :148 ALL_SIDES_HORIZONTAL_DOWN (the 4 ring cells at y-1 + the column cell)
				BlockPos tBelow = tSide == Direction.DOWN
						? tContactPos.below()
						: tContactPos.below().relative(tSide);
				// :150 — the side cell must be free (no collision) for the hive to hang
				if (!aLevel.getBlockState(tBelow).getCollisionShape(aLevel, tBelow).isEmpty()) continue;
				tKind = surfaceFamily(aLevel, aX, tY, aZ, tBelow); // :153-186 the first-hit chain
				if (tKind != null) break;
			}
			if (tKind != null) {
				// :156..186 — the hive hangs at tY-1 (the free cell the side walk found is
				// AT or diagonal-BELOW tY-1; the upstream always writes the tY-1 column)
				return placeHive(aLevel, aX, tY - 1, aZ, tKind, aRandom) || rPlaced;
			}
			return rPlaced; // :188 — one contact block judged per pass (the upstream return)
		}
		return rPlaced;
	}

	/**
	 * The surface family chain (:153-186, first-hit order preserved): the side-below
	 * water face, the four EMPTY biome tags (the pack surface), the live biome tags, then
	 * the contact-block faces, then the magical default.
	 */
	private HiveKind surfaceFamily(WorldGenLevel aLevel, int aX, int aY, int aZ, BlockPos aSideBelow) {
		if (!aLevel.getFluidState(aSideBelow).isEmpty() || aLevel.getBlockState(aSideBelow).is(Blocks.WATER)) {
			return HiveKind.WATER; // :155
		}
		var tBiome = aLevel.getBiome(new BlockPos(aX, aY, aZ));
		if (tBiome.is(hiveTag("magical"))) return HiveKind.MAGICAL;   // :157
		if (tBiome.is(hiveTag("volcanic"))) return HiveKind.VOLCANIC; // :159
		if (tBiome.is(hiveTag("end"))) return HiveKind.END_BIOME;     // :161
		if (tBiome.is(hiveTag("nether"))) return HiveKind.NETHER_BIOME; // :163
		if (tBiome.is(hiveTag("shroom"))) return HiveKind.SHROOM;     // :165
		if (tBiome.is(hiveTag("shore"))) return HiveKind.SHORE;       // :167
		if (tBiome.is(hiveTag("jungle"))) return HiveKind.JUNGLE;     // :169
		if (tBiome.is(hiveTag("frozen"))) return HiveKind.FROZEN;     // :171
		BlockState tContact = aLevel.getBlockState(new BlockPos(aX, aY, aZ));
		if (tContact.is(Blocks.MYCELIUM)) return HiveKind.SHROOM;     // :173
		if (tContact.is(Blocks.RED_SAND)) return HiveKind.RED_SAND;   // :175 (the meta-1 sand)
		if (tContact.is(Blocks.SAND) || tContact.is(Blocks.SANDSTONE)) return HiveKind.SAND; // :177
		if (tContact.is(Blocks.GRAVEL) || tContact.getBlock() instanceof GTStoneBlock) return HiveKind.ROCK; // :179
		if (tContact.is(Blocks.GRASS_BLOCK)) return HiveKind.GRASS;   // :181 (the grass material)
		if (tContact.is(Blocks.DIRT) || tContact.is(Blocks.COARSE_DIRT) || tContact.is(Blocks.PODZOL)) return HiveKind.DIRT; // :183
		return HiveKind.DEFAULT; // :186 — the magical default fallthrough
	}

	/**
	 * The 5-of-6 opaque-face wall check (:67-70/:107-110/:115-118): any liquid side
	 * fails outright, exactly five opaque sides of the six must remain (the one exposed
	 * non-liquid face).
	 */
	private static boolean wallEmbed(WorldGenLevel aLevel, int aX, int aY, int aZ) {
		int tCount = 0;
		BlockPos tCenter = new BlockPos(aX, aY, aZ);
		for (Direction tSide : Direction.values()) { // ALL_SIDES_VALID
			BlockPos tPos = tCenter.relative(tSide);
			if (!aLevel.getFluidState(tPos).isEmpty()) return false; // :68/:108/:116
			if (aLevel.getBlockState(tPos).canOcclude()) tCount++;   // :69/:109/:117
		}
		return tCount == 5;
	}

	/**
	 * The placeHive collapse (:195-204): the hive block in, the family paint applied
	 * (NBT_COLOR+NBT_PAINTED, the born-painted face), slot 0 filled with the family comb
	 * ({@code ponytail:} the comb is always comb_honey — the species-comb table is Lv3;
	 * princess/drone stay empty). The count roll rides the SAME stream after the
	 * placement rolls (the getBumbleGenes consumption slot upstream).
	 */
	private boolean placeHive(WorldGenLevel aLevel, int aX, int aY, int aZ, HiveKind aKind, Random aRandom) {
		if (!inBuild(aLevel, aY)) return false;
		BlockPos tPos = new BlockPos(aX, aY, aZ);
		aLevel.setBlock(tPos, GT6BeeHives.HIVE.get().defaultBlockState(), 2);
		BlockEntity tBE = aLevel.getBlockEntity(tPos);
		if (!(tBE instanceof GT6BumbleHiveBlockEntity tHive)) return false;
		tHive.paint(aKind.color); // the born-painted worldgen face (WorldgenHives.java:203)
		Item tComb = GT6BeeCombs.comb("honey").get();
		tHive.inventory().setStackInSlot(0, new ItemStack(tComb, 1 + aRandom.nextInt(10)));
		return true;
	}

	/** The mod-dimension /place guard (the GT6NetherClayFeature precedent). */
	private static boolean inBuild(WorldGenLevel aLevel, int aY) {
		return aY >= aLevel.getMinBuildHeight() && aY <= aLevel.getMaxBuildHeight() - 1;
	}
}
