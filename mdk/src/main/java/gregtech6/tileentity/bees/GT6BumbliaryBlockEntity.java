package gregtech6.tileentity.bees;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.stone.GTStoneBlock;
import gregtech6.items.bees.GT6BumbleGenes;
import gregtech6.items.bees.GT6Bumbles;
import gregtech6.items.bees.GT6BumbleItem;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GT6BeeHives;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.worldgen.GT6HiveFeature;

/**
 * The Bumbliary breeding TE (task p33-bees-lv3-b-bumbliary) — the port of the upstream
 * {@code MultiTileEntityBumbliary} (MultiTileEntityBumbliary.java:59-510, MTE 32741) and
 * its {@code Advanced} sibling (:32007) — ONE class over the {@code mAdvanced} flag, the
 * two BET registrations pick the layout (:333-337 36-slot vs the Advanced :333-337 20-slot).
 *
 * <p><b>The breeding state machine</b> (the :105-279 onTick2 server walk, the acceptance
 * pins): a princess in the ROYAL slot ({@code type%5==1}) ticks a 600-tick retry window;
 * a drone ({@code type%5==0}) anywhere in the DRONE slots pairs it — the countdown flips
 * to 1200, the offspring array fills with
 * {@code getOffspring(genes) + 1 + rng(5)/2} stacks (:218-220), the princess rides the
 * crown (:261) and the drone dies into a DEAD slot (:256-259). Same-species pairs copy
 * the royal code with the per-offspring mutation roll (:222-231, the
 * {@link GT6Bumbles#mutateChance} ladder over {@link GT6Bumbles#mutateCode}); cross-species
 * pairs split every offspring over the rng(4) father/mother/combine/combine walk
 * (:233-249) through the :370-437 pairing table ({@link #combineCode}, the table-driven
 * form — the upstream nested switch verbatim as data). Every offspring then takes the
 * heredity gene roll (:252, {@link GT6BumbleGenes#childGenes}). The crowned queen
 * ({@code type%5==2}) burns {@code mLife} against the environment check (:118-121), dies
 * into the DEAD slots (:122-133), releases the offspring (:135-143) and a princess from
 * the DRONE slots re-fills the ROYAL slot (the highest aggressiveness, :147-163).
 *
 * <p><b>The environment faces</b> (the card's same-surface ruling): the temperature and
 * humidity probes ARE the card-A climate faces — {@link GT6BumbleGenes#envTemp(Biome)}
 * and {@link GT6BumbleGenes#rainfallOf(boolean, float)} — so the check side and the
 * gene-roll side drift together. The sky flag collapses the upstream six-side rain probe
 * (:97/:109) onto the vanilla {@code canSeeSky} (ponytail: same outside/inside verdict,
 * the per-side rain-lit detail is unobservable through the gene keys).
 *
 * <p><b>The flower requirements</b> ({@link #canProduce}, the :216-349
 * {@code bumbleCanProduce} walk trimmed to the vanilla+GT6 face, table-driven): the
 * per-family predicate table ({@link #REQUIREMENTS}) replaces the hardcoded switch —
 * the BoP/NeLi/EtFu block lookups collapse onto their vanilla counterparts
 * (nether-wart family, the end chorus/dragon-egg face, cactus, mushrooms), the magical
 * biome family reuses the p32 {@code #gt6:bumble_hives/magical} tag, the rubber-resin
 * family is EMPTY (no resin holes in the port — the t1 tree ruling) and the default
 * family rides {@link BlockTags#FLOWERS}. The scan randomisation (:222-224) folds to a
 * fixed order (ponytail: the order only steers WHICH matching block wins; the boolean
 * verdict is order-independent).
 *
 * <p><b>Folded faces</b>: the GUI pair (the :389-509 containers) stays out until the MUI
 * wave — the vanilla {@link Container} view over the inventory is the insert/extract face
 * (the {@code isItemValidForSlotGUI} :357-364 rules ride the insert filter, only
 * princess/drone slots accept, and only their type faces); the aggro attack walk
 * (:165-167/:371-373) pools with the entity-damage domain (the RCON/breeding faces never
 * touch it); the click/scoop penalty (:282-315) rides the GUI wave with it.
 */
public class GT6BumbliaryBlockEntity extends TileEntityBase03TicksAndSync implements Container {

	// upstream NBT keys (CS.java:1175/:1230/:1251 verbatim, the :66-85 read/write pair)
	/** The queen life counter (upstream NBT_PROGRESS). */
	public static final String NBT_PROGRESS = "gt.progress";
	/** The breeding countdown (upstream NBT_COOLDOWN). */
	public static final String NBT_COOLDOWN = "gt.cooldown";
	/** The offspring array (upstream NBT_INV_OUT, indexed children). */
	public static final String NBT_INV_OUT = "gt.invout";

	/** The CS.DEFAULT_ENVIRONMENT_TEMPERATURE start value (CS.java:135, {@code C+20}). */
	public static final long DEFAULT_ENV_TEMPERATURE = 293;

	// the primary layout (MultiTileEntityBumbliary.java:333-337 verbatim)
	/** The primary ROYAL slot. */
	public static final int SLOT_ROYAL = 13;
	/** The primary main DRONE slot. */
	public static final int SLOT_DRONE = 22;
	static final int[] SLOTS_COMBS = {0, 1, 2, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19, 20, 24, 25, 26};
	static final int[] SLOTS_DRONE = {3, 4, 5, 12, 14, 21, 23};
	static final int[] SLOTS_DEAD = {27, 28, 29, 30, 31, 32, 33, 34, 35};

	// the advanced layout (MultiTileEntityBumbliaryAdvanced.java:333-338 verbatim)
	static final int[] ADV_COMBS = {0, 4, 5, 9, 10, 11, 14};
	static final int[] ADV_DRONES = {1, 2, 3, 6, 8, 11, 13};
	static final int[] ADV_DEAD = {15, 16, 17, 18, 19};
	static final int ADV_SLOT_ROYAL = 7;
	static final int ADV_SLOT_DRONE = 12;

	/** The retry window while only a princess sits in the ROYAL slot (:198-199). */
	public static final long NO_DRONE_WINDOW = 600;
	/** The pairing/production window after a drone matched (:119/:215/:270/:276). */
	public static final long PAIRED_WINDOW = 1200;
	/** The environment re-probe cadence (:108). */
	public static final long ENVIRONMENT_PERIOD = 1200;

	/** The advanced flag: the 20-slot layout, the tighter produce scan (:169 {@code aDistance 1}),
	 *  the halved product roll (:171 {@code rng(20000)}) and the soft countdown resets (:119/:270/:276). */
	protected final boolean mAdvanced;

	/** The live climate probes (:60-62; refreshed at placement and every 1200 ticks, not persisted — the :95-101 shape). */
	public boolean mSky = false;
	public long mTemperature = DEFAULT_ENV_TEMPERATURE;
	public float mHumidity = 1.0F;
	/** The queen life counter (:61). */
	public long mLife = 0;
	/** The breeding countdown (:61). */
	public long mBreedingCountDown = 0;
	/** True on the tick the queen died (the ITileEntityRunningSuccessfully face, :354). */
	public boolean mEndedQueen = false;
	/** The offspring produced by the last pairing (:63). */
	public ItemStack[] mOffSpring = {};

	/** The house random (the CS.RNGSUS role, per-TE so tests can seed determinism). */
	public final Random mRng = new Random();

	public GT6BumbliaryBlockEntity(BlockPos aPos, BlockState aState) {
		this(false, GT6BeeHives.BUMBLIARY_BE.get(), aPos, aState);
	}

	public GT6BumbliaryBlockEntity(boolean aAdvanced, BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
		mAdvanced = aAdvanced;
		setInventory(new GTItemStackHandler(slotCount(), this::setChanged) {
			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				return acceptsInSlot(aSlot, aStack);
			}
		});
	}

	// -------------------------------------------------------------------------
	// the layout faces
	// -------------------------------------------------------------------------

	/** The inventory size (:339 36 / the Advanced :339 20). */
	public int slotCount() {
		return mAdvanced ? 20 : 36;
	}

	/** The ROYAL slot of this variant. */
	public int slotRoyal() {
		return mAdvanced ? ADV_SLOT_ROYAL : SLOT_ROYAL;
	}

	/** The main DRONE slot of this variant. */
	public int slotDrone() {
		return mAdvanced ? ADV_SLOT_DRONE : SLOT_DRONE;
	}

	static int[] combsOf(boolean aAdvanced) {
		return aAdvanced ? ADV_COMBS : SLOTS_COMBS;
	}

	static int[] dronesOf(boolean aAdvanced) {
		return aAdvanced ? ADV_DRONES : SLOTS_DRONE;
	}

	static int[] deadOf(boolean aAdvanced) {
		return aAdvanced ? ADV_DEAD : SLOTS_DEAD;
	}

	/** The insert rule (:357-364): the ROYAL slot takes princesses, the main DRONE slot and
	 *  the DRONE slot group take drones (the upstream :333-336 maps — 22/12 is the main slot,
	 *  the group is the seven side slots), nothing else. */
	public boolean acceptsInSlot(int aSlot, ItemStack aStack) {
		if (!(aStack.getItem() instanceof GT6BumbleItem tBee)) return false;
		byte tFace = (byte)(tBee.typeOf() % 5);
		if (aSlot == slotRoyal()) return tFace == GT6Bumbles.TYPE_PRINCESS;
		if (aSlot == slotDrone()) return tFace == GT6Bumbles.TYPE_DRONE;
		for (int tSlot : dronesOf(mAdvanced)) if (tSlot == aSlot) return tFace == GT6Bumbles.TYPE_DRONE;
		return false;
	}

	// -------------------------------------------------------------------------
	// the climate probes (the same-surface ruling — the card-A faces verbatim)
	// -------------------------------------------------------------------------

	/** The :95-101/:108-112 refresh over the card-A climate faces. */
	public void refreshEnvironment() {
		if (!hasLevel()) return;
		Level tLevel = getLevel();
		mSky = tLevel.canSeeSky(getBlockPos().above()); // the six-side rain probe fold (:97/:109)
		Biome tBiome = tLevel.getBiome(getBlockPos()).value();
		mTemperature = GT6BumbleGenes.envTemp(tBiome); // the WD.envTemp formula, card-A shared surface
		mHumidity = GT6BumbleGenes.rainfallOf(tBiome.hasPrecipitation(), tBiome.getBaseTemperature()); // the shared humidity classifier
	}

	/** The :375-377 gate: temperature window, humidity window, outside/inside activity. */
	public boolean checkEnvironment(CompoundTag aGenes) {
		return GT6BumbleGenes.getTemperatureMin(aGenes) <= mTemperature && mTemperature <= GT6BumbleGenes.getTemperatureMax(aGenes)
				&& GT6BumbleGenes.getHumidityMin(aGenes) <= mHumidity && mHumidity <= GT6BumbleGenes.getHumidityMax(aGenes) // the UT.Code.inside_ face
				&& (mSky ? GT6BumbleGenes.getOutsideActive(aGenes) : GT6BumbleGenes.getInsideActive(aGenes));
	}

	/** The :379-385 work gate: weather proofing under the sky, the day/night activity. */
	public boolean checkWork(CompoundTag aGenes) {
		if (hasLevel() && mSky) {
			if (getLevel().isThundering() && !GT6BumbleGenes.getStormproof(aGenes)) return false;
			if (getLevel().isRaining() && mHumidity > 0 && !GT6BumbleGenes.getRainproof(aGenes)) return false;
		}
		return hasLevel() && (getLevel().isDay() ? GT6BumbleGenes.getDayActive(aGenes) : GT6BumbleGenes.getNightActive(aGenes));
	}

	// -------------------------------------------------------------------------
	// the tick walk (the :105-279 onTick2 server branch tree)
	// -------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide) refreshEnvironment();
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide || !hasLevel()) return;
		mEndedQueen = false;
		if (getLevel().getGameTime() % ENVIRONMENT_PERIOD == 0) refreshEnvironment(); // :108-112

		int tRoyalSlot = slotRoyal();
		ItemStack tRoyalStack = mInventory.getStackInSlot(tRoyalSlot);
		if (tRoyalStack.getItem() instanceof GT6BumbleItem tRoyalItem) {
			byte tRoyalFace = (byte)(tRoyalItem.typeOf() % 5); // the bumbleType%5 fractal
			CompoundTag tRoyalTag = GT6BumbleGenes.getOrCreateGenes(tRoyalStack, mRng); // the :116 lazy face
			if (mLife > 0 && tRoyalFace == GT6Bumbles.TYPE_QUEEN) {
				mBreedingCountDown = raisedWindow(mAdvanced, mBreedingCountDown); // :119 — the Advanced soft reset
				if (checkEnvironment(tRoyalTag)) {
					if (--mLife <= 0) queenDied(); else produceTick(tRoyalItem, tRoyalStack);
				} else {
					killRoyal(tRoyalStack); // :189-192 — the environment killed the queen
				}
			} else {
				mLife = 0;
				mOffSpring = new ItemStack[0];
				if (tRoyalFace == GT6Bumbles.TYPE_PRINCESS) {
					if (--mBreedingCountDown <= 0) {
						mBreedingCountDown = NO_DRONE_WINDOW; // :199
						ItemStack tBreedStack = null;
						int tBreedSlot = slotDrone();
						ItemStack tDroneStack = mInventory.getStackInSlot(tBreedSlot);
						if (tDroneStack.getItem() instanceof GT6BumbleItem && beeFace(tDroneStack) == GT6Bumbles.TYPE_DRONE) {
							tBreedStack = tDroneStack; // :202-203
						} else {
							for (int tDroneSlot : dronesOf(mAdvanced)) { // :205-213, same-species preferred
								ItemStack tStack = mInventory.getStackInSlot(tDroneSlot);
								if (tStack.getItem() instanceof GT6BumbleItem && beeFace(tStack) == GT6Bumbles.TYPE_DRONE) {
									tBreedStack = tStack;
									tBreedSlot = tDroneSlot;
									if (GT6Bumbles.sameSpecies(GT6BumbleGenes.codeOf(tRoyalStack), GT6BumbleGenes.codeOf(tStack))) break;
								}
							}
						}
						if (tBreedStack != null) breed(tRoyalStack, tBreedStack, tBreedSlot);
					}
				} else {
					mBreedingCountDown = raisedWindow(mAdvanced, mBreedingCountDown); // :270
				}
			}
		} else {
			mLife = 0;
			mOffSpring = new ItemStack[0];
			mBreedingCountDown = raisedWindow(mAdvanced, mBreedingCountDown); // :276
		}
		setChanged();
	}

	/** The drone face of a stack (the bumbleType%5 fold). */
	static byte beeFace(ItemStack aStack) {
		return aStack.getItem() instanceof GT6BumbleItem tBee ? (byte)(tBee.typeOf() % 5) : -1;
	}

	/** The :122-163 queen death walk: royals and drones die into the DEAD slots, the offspring
	 *  release (the environment verdict again), then the most aggressive princess re-fills the ROYAL slot. */
	private void queenDied() {
		mEndedQueen = true;
		int[] tDead = deadOf(mAdvanced), tDrones = dronesOf(mAdvanced);
		for (int tDeadSlot : tDead) if (isBee(tDeadSlot)) mInventory.setStackInSlot(tDeadSlot, ItemStack.EMPTY); // :124 slotKill
		killRoyal(mInventory.getStackInSlot(slotRoyal())); // :126-128 — dead into the DEAD slots
		for (int tDroneSlot : tDrones) if (isBee(tDroneSlot)) { // :130-133
			ItemStack tDroneDead = transform(mInventory.getStackInSlot(tDroneSlot), GT6Bumbles.TYPE_DEAD);
			mInventory.setStackInSlot(tDroneSlot, ItemStack.EMPTY);
			addToSlots(tDroneDead, tDead);
		}
		for (ItemStack tOffSpring : mOffSpring) { // :135-143
			if (tOffSpring.isEmpty() || !(tOffSpring.getItem() instanceof GT6BumbleItem)) continue;
			if (checkEnvironment(GT6BumbleGenes.getOrCreateGenes(tOffSpring, mRng))) {
				if (!addToSlot(slotDrone(), tOffSpring)) addToSlots(tOffSpring, tDrones);
			} else {
				ItemStack tOffDead = transform(tOffSpring, GT6Bumbles.TYPE_DEAD);
				addToSlots(tOffDead, deadOf(mAdvanced));
			}
		}
		mOffSpring = new ItemStack[0];
		// :147-163 — the most aggressive princess among the DRONE slots rides the crown
		int tPrincessSlot = -1;
		for (int tDroneSlot : tDrones) {
			ItemStack tStack = mInventory.getStackInSlot(tDroneSlot);
			if (tStack.getItem() instanceof GT6BumbleItem && beeFace(tStack) == GT6Bumbles.TYPE_PRINCESS) {
				if (tPrincessSlot < 0) tPrincessSlot = tDroneSlot;
				else {
					CompoundTag tCand = GT6BumbleGenes.getOrCreateGenes(tStack, mRng);
					CompoundTag tCur = GT6BumbleGenes.getOrCreateGenes(mInventory.getStackInSlot(tPrincessSlot), mRng);
					if (GT6BumbleGenes.getAggressiveness(tCand) > GT6BumbleGenes.getAggressiveness(tCur)) tPrincessSlot = tDroneSlot;
				}
			}
		}
		if (tPrincessSlot >= 0) {
			ItemStack tPrincess = mInventory.getStackInSlot(tPrincessSlot).copy();
			tPrincess.setCount(1);
			mInventory.setStackInSlot(slotRoyal(), tPrincess); // :161
			shrink(tPrincessSlot, 1); // :162
		}
	}

	/** The :165-186 queen work walk — the aggro attack branch (:165-167) pools with the
	 *  entity-damage domain; the comb production branch runs the flower check. */
	private void produceTick(GT6BumbleItem aRoyalItem, ItemStack aRoyalStack) {
		if (mLife % 1200 == 600 && mRng.nextInt(10000) < GT6BumbleGenes.getWorkForce(GT6BumbleGenes.getOrCreateGenes(aRoyalStack, mRng)) && checkWork(GT6BumbleGenes.getOrCreateGenes(aRoyalStack, mRng))) {
			int tCode = GT6BumbleGenes.codeOf(aRoyalStack);
			int tDistance = mAdvanced ? 1 : 3; // :169 (the 3x3x3 advanced / 7x7x7 primary range)
			if (canProduce(getLevel(), getBlockPos(), GT6Bumbles.familyOf(tCode), tDistance) != null) {
				int tChanceDenominator = mAdvanced ? 20000 : 10000; // :171
				if (mRng.nextInt(tChanceDenominator) < productChance(tCode)) { // the :481-489 tier ladder
					ItemStack tProduct = productStack(GT6Bumbles.familyOf(tCode)); // the :189-213 table
					if (!tProduct.isEmpty()) addToSlots(tProduct, combsOf(mAdvanced));
				}
			}
		}
	}

	/** The :214-267 pairing walk. */
	private void breed(ItemStack aRoyalStack, ItemStack aBreedStack, int aBreedSlot) {
		mBreedingCountDown = PAIRED_WINDOW; // :215
		int tRoyalCode = GT6BumbleGenes.codeOf(aRoyalStack), tBreedCode = GT6BumbleGenes.codeOf(aBreedStack);
		long tOffspringGene = GT6BumbleGenes.getOffspring(GT6BumbleGenes.getOrCreateGenes(aRoyalStack, mRng));
		int tPrincessCount = princessCount(mRng); // :218
		int[] tCodes = offspringCodes(mRng, tRoyalCode, tBreedCode, tOffspringGene, tPrincessCount); // :220-250 — the code walk
		CompoundTag tChildGenes = GT6BumbleGenes.childGenes(aRoyalStack, aBreedStack, mRng); // the :252 heredity roll
		mOffSpring = new ItemStack[tCodes.length];
		for (int i = 0; i < mOffSpring.length; i++) {
			ItemStack tOffSpring = beeStack(i < tPrincessCount ? GT6Bumbles.TYPE_PRINCESS : GT6Bumbles.TYPE_DRONE, tCodes[i]);
			GT6BumbleGenes.setGenes(tOffSpring, tChildGenes.copy()); // :252 — every offspring takes the heredity genes
			mOffSpring[i] = tOffSpring;
		}
		mLife = GT6BumbleGenes.getLifeSpan(GT6BumbleGenes.getOrCreateGenes(aRoyalStack, mRng)); // :254
		// :256-259 — the drone dies (2 of a stacked main-slot drone, 1 otherwise)
		int tLoss = aBreedSlot == slotDrone() && aBreedStack.getCount() > 1 ? 2 : 1;
		ItemStack tDead = transform(aBreedStack, GT6Bumbles.TYPE_DEAD);
		tDead.setCount(tLoss);
		addToSlots(tDead, deadOf(mAdvanced));
		shrink(aBreedSlot, tLoss);
		// :261 — the princess rides the crown
		mInventory.setStackInSlot(slotRoyal(), transform(aRoyalStack, GT6Bumbles.TYPE_QUEEN));
		// :263-266 — the leftover non-drones among the DRONE slots die
		for (int tDroneSlot : dronesOf(mAdvanced)) {
			ItemStack tStack = mInventory.getStackInSlot(tDroneSlot);
			if (tStack.getItem() instanceof GT6BumbleItem && beeFace(tStack) != GT6Bumbles.TYPE_DRONE) {
				ItemStack tLeftDead = transform(tStack, GT6Bumbles.TYPE_DEAD);
				mInventory.setStackInSlot(tDroneSlot, ItemStack.EMPTY);
				addToSlots(tLeftDead, deadOf(mAdvanced));
			}
		}
	}

	/** Kills the ROYAL slot's bee into a DEAD slot (:189-192 and the :126-128 royal half). */
	private void killRoyal(ItemStack aRoyalStack) {
		if (!(aRoyalStack.getItem() instanceof GT6BumbleItem)) return;
		ItemStack tDead = transform(aRoyalStack, GT6Bumbles.TYPE_DEAD);
		mInventory.setStackInSlot(slotRoyal(), ItemStack.EMPTY);
		addToSlots(tDead, deadOf(mAdvanced));
	}

	// -------------------------------------------------------------------------
	// the bee-stack algebra (the MultiItemBumbles faces over the 8-id flat family)
	// -------------------------------------------------------------------------

	/** The stack is a live GT6 bee. */
	static boolean isBee(GTItemStackHandler aInv, int aSlot) {
		return aInv.getStackInSlot(aSlot).getItem() instanceof GT6BumbleItem;
	}

	private boolean isBee(int aSlot) {
		return mInventory.getStackInSlot(aSlot).getItem() instanceof GT6BumbleItem;
	}

	/** The type-face transform (the :565-566 kill/crown walk): the scan half survives, the type digit swaps. */
	static ItemStack transform(ItemStack aBee, byte aNewFace) {
		GT6BumbleItem tBee = (GT6BumbleItem)aBee.getItem();
		return beeStack((byte)((tBee.typeOf() / 5) * 5 + aNewFace), GT6BumbleGenes.codeOf(aBee));
	}

	/** A fresh bee stack of the type face + species code (the :570-571 {@code bumbleDrone/bumblePrincess} face). */
	public static ItemStack beeStack(byte aTypeOf, int aCode) {
		for (int i = 0; i < GT6Bumbles.FACES.size(); i++) {
			GT6Bumbles.FaceRow tFace = GT6Bumbles.FACES.get(i);
			if ((byte)(tFace.face() + (tFace.scanned() ? GT6Bumbles.SCAN_OFFSET : 0)) == aTypeOf) {
				ItemStack rStack = new ItemStack(GT6Bumbles.BEE_ITEMS.get(i).get());
				GT6BumbleGenes.setCode(rStack, aCode);
				return rStack;
			}
		}
		return ItemStack.EMPTY;
	}

	// -------------------------------------------------------------------------
	// the pairing table (the :370-437 bumbleCombine switch as data, symmetric pairs)
	// -------------------------------------------------------------------------

	/** The :378-435 pairing rows — {codeA, codeB, result} over the tier-3 species codes; the
	 *  type digit is not in the table (the face carries it). Both switch directions are in
	 *  the upstream source and agree, so one symmetric lookup covers them. */
	public static final int[][] COMBINE_ROWS = {
			{30, 130, 10100}, {30, 530, 10000}, {30, 930, 10200}, {130, 530, 10400},
			{330, 430, 10300}, {330, 10530, 20000}, {430, 10530, 20200}, {530, 10530, 20300},
			{630, 930, 10500}, {730, 10530, 20100}};

	/** The :436-437 bumbleCombine face: the pairing-table hit, else the A copy (the :436
	 *  default — {@code (aMetaDataA/10)*10} is A's own code; the type rides the face). */
	public static int combineCode(int aCodeA, int aCodeB) {
		for (int[] tRow : COMBINE_ROWS) {
			if (tRow[0] == aCodeA && tRow[1] == aCodeB) return tRow[2];
			if (tRow[0] == aCodeB && tRow[1] == aCodeA) return tRow[2];
		}
		return aCodeA;
	}

	// -------------------------------------------------------------------------
	// the breeding decision core (the pure code-domain face the tests pin)
	// -------------------------------------------------------------------------

	/** The :218 princess share of one brood: {@code 1 + rng(5)/2} — 1,1,2,2,3 over rng 0..4. */
	public static int princessCount(Random aRng) {
		return 1 + aRng.nextInt(5) / 2;
	}

	/**
	 * The :220-250 offspring code walk. The brood size is
	 * {@code offspringGene + princessCount} (:220 — the gene of the royal carries the
	 * base count). Same-species pairs copy the royal code with the per-offspring mutation
	 * roll (:222-231, {@code rng(10000)} against {@link GT6Bumbles#mutateChance}, the
	 * step through {@link GT6Bumbles#mutateCode}); cross-species pairs split every
	 * offspring over the rng(4) father/mother/combine-AB/combine-BA walk (:233-249). The
	 * first {@code princessCount} codes are the princess faces, the rest drones.
	 */
	public static int[] offspringCodes(Random aRng, int aRoyalCode, int aDroneCode, long aOffspringGene, int aPrincessCount) {
		int[] rCodes = new int[(int)aOffspringGene + aPrincessCount]; // :220 — the gene carries the base count
		boolean tSame = GT6Bumbles.sameSpecies(aRoyalCode, aDroneCode);
		for (int i = 0; i < rCodes.length; i++) {
			if (tSame) {
				rCodes[i] = aRoyalCode; // :222-224 — the copy
				if (aRng.nextInt(10000) < GT6Bumbles.mutateChance(rCodes[i])) rCodes[i] = GT6Bumbles.mutateCode(rCodes[i], aRng); // :227-229
			} else {
				rCodes[i] = switch (aRng.nextInt(4)) { // :233-249 — the four-way split
					case 0 -> aRoyalCode;
					case 1 -> aDroneCode;
					case 2 -> combineCode(aRoyalCode, aDroneCode);
					default -> combineCode(aDroneCode, aRoyalCode);
				};
			}
		}
		return rCodes;
	}

	/**
	 * The countdown window resets (:119/:270/:276): the primary stomps the window to 1200
	 * unconditionally, the Advanced only raises it (a running pairing is never interrupted).
	 */
	public static long raisedWindow(boolean aAdvanced, long aCurrent) {
		if (!aAdvanced) return PAIRED_WINDOW;
		return aCurrent < PAIRED_WINDOW ? PAIRED_WINDOW : aCurrent;
	}

	// -------------------------------------------------------------------------
	// the product faces (the :481-489 chance ladder + the :189-213 comb table)
	// -------------------------------------------------------------------------

	/** The :481-489 product chance ladder over the code's tier. */
	public static int productChance(int aCode) {
		return switch (GT6Bumbles.tierOf(aCode)) {
			case 0 -> 2500;
			case 1 -> 5000;
			case 2 -> 7500;
			default -> 10000;
		};
	}

	/** The :189-213 comb table (family → the GT6BeeCombs id; default Honey). */
	public static ItemStack productStack(int aFamily) {
		String tName = switch (aFamily) {
			case 1 -> "water";
			case 2 -> "magic";
			case 3 -> "nether";
			case 4 -> "end";
			case 5 -> "rock";
			case 6 -> "jungle";
			case 7 -> "frozen";
			case 8 -> "shroom";
			case 9 -> "sandy";
			case 100 -> "clay";
			case 101 -> "sticky";
			case 102 -> "royal";
			case 103 -> "soul";
			case 104 -> "amnesic";
			case 105 -> "military";
			case 200 -> "pyro";
			case 201 -> "cryo";
			case 202 -> "aero";
			case 203 -> "tera";
			default -> "honey";
		};
		return new ItemStack(GT6BeeCombs.comb(tName).get());
	}

	// -------------------------------------------------------------------------
	// the flower requirements (the :216-349 walk, vanilla+GT6 trim, table-driven)
	// -------------------------------------------------------------------------

	/** One requirement row: the block/biome verdict at a world position. */
	public interface Requirement {
		boolean matches(Level aLevel, BlockPos aPos);
	}

	/** The water row (the :226-230 any-water face). */
	static boolean anyWater(Level aLevel, BlockPos aPos) {
		return aLevel.getFluidState(aPos).is(FluidTags.WATER);
	}

	/** The default flowers row (the :341-346 + :617-631 checkFlowers face trimmed to the
	 *  vanilla flower tag — the flower-pot unwrap and the GT flower propagation are cut). */
	static boolean flowers(Level aLevel, BlockPos aPos) {
		return aLevel.getBlockState(aPos).is(BlockTags.FLOWERS);
	}

	/** The stone row (the :276-285 face): the vanilla stone/cobble/brick quartet; the GT6
	 *  surface-stone half rides the block class. */
	static boolean stones(Level aLevel, BlockPos aPos) {
		BlockState tState = aLevel.getBlockState(aPos);
		return tState.is(Blocks.STONE) || tState.is(Blocks.COBBLESTONE) || tState.is(Blocks.MOSSY_COBBLESTONE)
				|| tState.is(Blocks.STONE_BRICKS) || tState.is(Blocks.MOSSY_STONE_BRICKS)
				|| tState.getBlock() instanceof GTStoneBlock;
	}

	/** The :216-349 requirement table (family → verdict): the per-family hardcoded switch
	 *  as data. Rows not in the table take the FLOWERS default (:341). */
	public static final java.util.Map<Integer, Requirement> REQUIREMENTS = new java.util.HashMap<>();

	static {
		REQUIREMENTS.put(1, GT6BumbliaryBlockEntity::anyWater);
		REQUIREMENTS.put(2, (aLevel, aPos) -> aLevel.getBiome(aPos).is(GT6HiveFeature.hiveTag("magical"))); // the p32 empty-pack tag = the magical verdict
		REQUIREMENTS.put(3, netherWarts()); REQUIREMENTS.put(200, netherWarts());
		REQUIREMENTS.put(4, endFace()); REQUIREMENTS.put(202, endFace());
		REQUIREMENTS.put(5, GT6BumbliaryBlockEntity::stones); REQUIREMENTS.put(203, GT6BumbliaryBlockEntity::stones);
		REQUIREMENTS.put(6, (aLevel, aPos) -> aLevel.getBlockState(aPos).is(Blocks.COCOA));
		REQUIREMENTS.put(7, snowy()); REQUIREMENTS.put(201, snowy());
		REQUIREMENTS.put(8, mushrooms());
		REQUIREMENTS.put(9, (aLevel, aPos) -> aLevel.getBlockState(aPos).is(Blocks.CACTUS)); // the BoP/ARS cacti trim
		REQUIREMENTS.put(105, (aLevel, aPos) -> aLevel.getBlockState(aPos).is(Blocks.CACTUS));
		REQUIREMENTS.put(100, (aLevel, aPos) -> aLevel.getBlockState(aPos).is(Blocks.CLAY));
		REQUIREMENTS.put(101, (aLevel, aPos) -> false); // the resin-hole family — no resin holes in the port (the t1 tree ruling)
		REQUIREMENTS.put(103, (aLevel, aPos) -> aLevel.getBlockState(aPos).is(Blocks.SOUL_SAND));
	}

	static Requirement netherWarts() { // the :241-260 face — the NeLi warts trim to the vanilla pair
		return (aLevel, aPos) -> {
			BlockState tState = aLevel.getBlockState(aPos);
			return tState.is(Blocks.NETHER_WART) || tState.is(Blocks.CRIMSON_ROOTS) || tState.is(Blocks.WARPED_ROOTS)
					|| tState.is(Blocks.CRIMSON_FUNGUS) || tState.is(Blocks.WARPED_FUNGUS);
		};
	}

	static Requirement endFace() { // the :261-275 face — the chorus/dragon-egg/End-dimension verdict
		return (aLevel, aPos) -> aLevel.dimension() == Level.END
				|| aLevel.getBlockState(aPos).is(Blocks.CHORUS_FLOWER) || aLevel.getBlockState(aPos).is(Blocks.DRAGON_EGG);
	}

	static Requirement snowy() { // the :291-296 face
		return (aLevel, aPos) -> {
			BlockState tState = aLevel.getBlockState(aPos);
			return tState.is(Blocks.ICE) || tState.is(Blocks.SNOW) || tState.is(Blocks.SNOW_BLOCK) || tState.is(Blocks.PACKED_ICE);
		};
	}

	static Requirement mushrooms() { // the :297-303 face — the BoP mushrooms trim
		return (aLevel, aPos) -> {
			BlockState tState = aLevel.getBlockState(aPos);
			return tState.is(Blocks.MYCELIUM) || tState.is(Blocks.RED_MUSHROOM) || tState.is(Blocks.BROWN_MUSHROOM)
					|| tState.is(Blocks.RED_MUSHROOM_BLOCK) || tState.is(Blocks.BROWN_MUSHROOM_BLOCK);
		};
	}

	/**
	 * The bumbleCanProduce face: the oxygen guard (:218 — fully buried = no produce; the
	 * WD.oxygen probe folds to the non-solid neighbour verdict), then the family scan over
	 * the {@code [-distance..distance]} cube (the :222-224 random scan order folds to the
	 * fixed Y-X-Z order — the boolean verdict is order-independent).
	 *
	 * @return the matching position, or null (the requirement is unmet).
	 */
	@Nullable
	public static BlockPos canProduce(Level aLevel, BlockPos aCenter, int aFamily, int aDistance) {
		boolean tAir = false;
		for (Direction tSide : Direction.values()) if (!aLevel.getBlockState(aCenter.relative(tSide)).isSolid()) { tAir = true; break; }
		if (!tAir) return null; // the :218-219 fully-buried guard
		Requirement tRequirement = REQUIREMENTS.getOrDefault(aFamily, GT6BumbliaryBlockEntity::flowers);
		for (int dy = -aDistance; dy <= aDistance; dy++)
			for (int dx = -aDistance; dx <= aDistance; dx++)
				for (int dz = -aDistance; dz <= aDistance; dz++)
					if (tRequirement.matches(aLevel, aCenter.offset(dx, dy, dz))) return aCenter.offset(dx, dy, dz);
		return null;
	}

	// -------------------------------------------------------------------------
	// the inventory plumbing
	// -------------------------------------------------------------------------

	/** Merges or stores the stack into the first matching slot of the group; empties the source on success. */
	private void addToSlots(ItemStack aStack, int[] aSlots) {
		if (aStack.isEmpty()) return;
		for (int tSlot : aSlots) {
			ItemStack tThere = mInventory.getStackInSlot(tSlot);
			if (!tThere.isEmpty() && ItemStack.isSameItemSameTags(tThere, aStack)) {
				int tMove = Math.min(aStack.getCount(), mInventory.getSlotLimit(tSlot) - tThere.getCount());
				if (tMove > 0) {
					tThere.grow(tMove);
					aStack.shrink(tMove);
					if (aStack.isEmpty()) return;
				}
			}
		}
		for (int tSlot : aSlots) {
			if (mInventory.getStackInSlot(tSlot).isEmpty()) {
				mInventory.setStackInSlot(tSlot, aStack.copy());
				aStack.setCount(0);
				return;
			}
		}
	}

	/** The single-slot merge-or-store (:137-138's addStackToSlot face); empties the source on success. */
	private boolean addToSlot(int aSlot, ItemStack aStack) {
		if (aStack.isEmpty()) return true;
		ItemStack tThere = mInventory.getStackInSlot(aSlot);
		if (tThere.isEmpty()) {
			mInventory.setStackInSlot(aSlot, aStack.copy());
			aStack.setCount(0);
			return true;
		}
		if (ItemStack.isSameItemSameTags(tThere, aStack) && tThere.getCount() < mInventory.getSlotLimit(aSlot)) {
			tThere.grow(Math.min(aStack.getCount(), mInventory.getSlotLimit(aSlot) - tThere.getCount()));
			aStack.setCount(0);
			return true;
		}
		return false;
	}

	private void shrink(int aSlot, int aCount) {
		ItemStack tStack = mInventory.getStackInSlot(aSlot);
		if (!tStack.isEmpty()) {
			tStack.shrink(aCount);
			if (tStack.getCount() <= 0) mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
		}
	}

	// -------------------------------------------------------------------------
	// NBT (:66-85 verbatim key faces; the inventory rides the gt.inv list — the
	// GT6BumbleHiveBlockEntity ADR ruling 4 key face)
	// -------------------------------------------------------------------------

	/** The inventory list key (upstream CS.NBT_INV_LIST = "gt.inv", the hive-BE shape). */
	public static final String NBT_INV = "gt.inv";

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_PROGRESS, mLife);
		aNBT.putLong(NBT_COOLDOWN, mBreedingCountDown);
		if (mInventory != null) {
			//? if forge {
			aNBT.put(NBT_INV, mInventory.serializeNBT());
			//?} else {
			/*aNBT.put(NBT_INV, mInventory.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: the handler NBT takes the registries
			*///?}
		}
		if (mOffSpring.length > 0) {
			aNBT.putInt(NBT_INV_OUT, mOffSpring.length); // the upstream count + indexed children
			for (int i = 0; i < mOffSpring.length; i++) if (!mOffSpring[i].isEmpty()) putItemStack(aNBT, NBT_INV_OUT + "." + i, mOffSpring[i]);
		}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_PROGRESS, Tag.TAG_ANY_NUMERIC)) mLife = aNBT.getLong(NBT_PROGRESS);
		if (aNBT.contains(NBT_COOLDOWN, Tag.TAG_ANY_NUMERIC)) mBreedingCountDown = aNBT.getLong(NBT_COOLDOWN);
		if (mInventory != null && aNBT.contains(NBT_INV, Tag.TAG_COMPOUND)) {
			//? if forge {
			mInventory.deserializeNBT(aNBT.getCompound(NBT_INV));
			//?} else {
			/*mInventory.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INV)); // 21.1: provider-first
			*///?}
		}
		if (aNBT.contains(NBT_INV_OUT, Tag.TAG_ANY_NUMERIC)) {
			mOffSpring = new ItemStack[aNBT.getInt(NBT_INV_OUT)];
			for (int i = 0; i < mOffSpring.length; i++) mOffSpring[i] = getItemStack(aNBT, NBT_INV_OUT + "." + i);
		}
	}

	// the stack NBT seam (the GT6BumbleGenes per-leg shape, block-side)
	static void putItemStack(CompoundTag aNBT, String aKey, ItemStack aStack) {
		//? if forge {
		CompoundTag tTag = new CompoundTag();
		aStack.save(tTag);
		aNBT.put(aKey, tTag);
		//?} else {
		/*CompoundTag tTag = (CompoundTag)aStack.save(TileEntityBase03TicksAndSync.NBT_ACCESS);
		aNBT.put(aKey, tTag);
		*///?}
	}

	static ItemStack getItemStack(CompoundTag aNBT, String aKey) {
		//? if forge {
		if (!aNBT.contains(aKey, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
		return ItemStack.of(aNBT.getCompound(aKey));
		//?} else {
		/*if (!aNBT.contains(aKey, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
		return ItemStack.parseOptional(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(aKey));
		*///?}
	}

	// -------------------------------------------------------------------------
	// the vanilla Container view (the insert/extract face while the GUI wave pends)
	// -------------------------------------------------------------------------

	@Override
	public int getContainerSize() {
		return slotCount();
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < slotCount(); i++) if (!mInventory.getStackInSlot(i).isEmpty()) return false;
		return true;
	}

	@Override
	public ItemStack getItem(int aSlot) {
		return mInventory.getStackInSlot(aSlot);
	}

	@Override
	public ItemStack removeItem(int aSlot, int aCount) {
		ItemStack tStack = mInventory.getStackInSlot(aSlot);
		if (tStack.isEmpty()) return ItemStack.EMPTY;
		ItemStack tTake = tStack.split(aCount);
		if (tStack.isEmpty()) mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
		return tTake;
	}

	@Override
	public ItemStack removeItemNoUpdate(int aSlot) {
		ItemStack tStack = mInventory.getStackInSlot(aSlot);
		mInventory.setStackInSlot(aSlot, ItemStack.EMPTY);
		return tStack;
	}

	@Override
	public void setItem(int aSlot, ItemStack aStack) {
		if (!acceptsInSlot(aSlot, aStack)) return; // the :357-364 rule
		mInventory.setStackInSlot(aSlot, aStack.isEmpty() ? ItemStack.EMPTY : aStack);
	}

	@Override
	public boolean stillValid(Player aPlayer) {
		return hasLevel() && aPlayer.distanceToSqr(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5) <= 64.0;
	}

	@Override
	public void clearContent() {
		for (int i = 0; i < slotCount(); i++) mInventory.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	public String getTileEntityName() {
		return mAdvanced ? "gt.multitileentity.bumbliary.advanced" : "gt.multitileentity.bumbliary"; // :387 verbatim / the Advanced :388
	}
}
