package gregtech6.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The single owner of the GT6 BlockState Property constants that more than one block class
 * shares by name (ADR-P16-2, task p16-blockstates-2111-prop-intern). Each consumer block
 * declares its own constant as an alias of the constant here, so all holders share one real
 * instance and any cross-class read is identity-safe.
 *
 * <p>Why identity matters (the mechanism, so nobody re-derives the wrong theory): vanilla
 * never interned BooleanProperty.create — 1.20.1 was a bare new too
 * (tmp/vanilla-1.20.1/.../BooleanProperty.java:19-21, no BY_NAME cache). Same-named
 * distinct instances only *worked* on 1.20.1 by luck: StateHolder.values was an
 * ImmutableMap looked up by value equality (StateHolder.java:40) and 1.20.1
 * BooleanProperty had value equals (:33-46). 1.21.x switched StateHolder.values to a
 * Reference2ObjectArrayMap (identity lookup, :36) and dropped the value equals — a
 * same-named foreign instance now throws "Cannot get property ... as it does not exist"
 * (first observed at GT6BlockStates.addMachine :275-276, reading GTOvenBlock.ACTIVE over a
 * GTBasicMachineBlock state, on 1.21.1 runData). The fix is therefore real single
 * instances via these aliases — never a reliance on equals/intern semantics.
 *
 * <p>The property NAMES are unchanged, so datagen blockstate JSON is byte-identical (the
 * variant keys come from each block's own StateDefinition, independent of which constant
 * instance a datagen lambda reads).
 */
public final class GTBlockProperties {

	/** Actually processing (upstream mActive, visual bit 0) — aliased by GTOvenBlock and GTBasicMachineBlock. */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/** Powered / has work (upstream mRunning, visual bit 1) — aliased by GTOvenBlock and GTBasicMachineBlock. */
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order) — aliased by GTWireBlock and GTFluidPipeBlock. */
	public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

	private GTBlockProperties() {
	}
}
