package gregtech6.multiblock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * One layered declaration, fronted per size (task p16-pattern-layers ②): the family
 * holds the {@link GTMultiBlockPattern.Builder} layer sequence as a pure declaration
 * and expands it to an immutable {@link GTMultiBlockPattern} for any layer count in the
 * declared window — {@link #build(int)} expands fresh on every call, {@link #forSize(int)}
 * is the same expansion cached per size (the {@code getStructurePattern} lazy-stable
 * precedent, the {@code bindingIsLazyStable} pin: same n, same immutable instance).
 *
 * <p><b>Where this comes from (mechanism-level clean-room).</b> The kTFRUAddon
 * {@code ExpandableLayer} (ExpandableLayer.java:47-58) carries its size as RUNTIME probe
 * state (the repeat count is discovered by trial-validating layers at check time, then
 * re-discovered every check); its per-size handle is the {@code setExtraData} string
 * (:61-68). The re-declaration per ADR 2026-09-05-p16-formation-scoping ② inverts that:
 * the size is a BUILD-TIME parameter, the expansion freezes an ordinary dumb immutable
 * cell list, and the runtime (the shared checker, the ghost preview — the P12 consumers)
 * never learns layers existed. The size-parametrised MACHINE that would consume this
 * per-instance is deliberately NOT in this card (GT6 canon machines are fixed-size; the
 * pool holds the wrench/GUI size consumer).
 *
 * <p>The declaration is a {@link Consumer} of a fresh builder (a builder is a one-shot
 * freeze — the family assembles one per expansion); typical shape:
 *
 * <pre>{@code
 * GTMultiBlockPatternFamily tFamily = GTMultiBlockPatternFamily.of(tBuilder -> tBuilder
 *         .layer(tBase).layer(tWalls)
 *         .repeatable(2, 4, tRing -> GTMultiBlockPattern.Layer.builder()...build()));
 * GTMultiBlockPattern tCanonical = tFamily.forSize(2);
 * }</pre>
 */
public final class GTMultiBlockPatternFamily {

	/** The frozen declaration — replayed onto a fresh {@link GTMultiBlockPattern.Builder} per expansion. */
	private final Consumer<GTMultiBlockPattern.Builder> mDeclaration;

	/**
	 * The per-size expansion cache ({@link #forSize}); failures are never cached. Concurrent
	 * because a future size-parametrised machine's {@code getStructurePattern} is consumed on
	 * worker threads: {@link ConcurrentHashMap#computeIfAbsent} is atomic per key, which keeps
	 * the same-n-same-immutable-instance contract under racing first calls (the declaration
	 * replay never recurses into this map, so no CHM self-deadlock).
	 */
	private final Map<Integer, GTMultiBlockPattern> mExpansions = new ConcurrentHashMap<>();

	private GTMultiBlockPatternFamily(Consumer<GTMultiBlockPattern.Builder> aDeclaration) {
		mDeclaration = aDeclaration;
	}

	/** Binds a layered declaration (replayed onto a fresh builder per expansion — a builder is a one-shot freeze). */
	public static GTMultiBlockPatternFamily of(Consumer<GTMultiBlockPattern.Builder> aDeclaration) {
		if (aDeclaration == null) throw new IllegalArgumentException("a family needs a declaration");
		return new GTMultiBlockPatternFamily(aDeclaration);
	}

	/**
	 * Expands the declaration to {@code aSize} layers, fresh on every call — the pure
	 * function behind {@link #forSize}. Out-of-window sizes (or any other declaration
	 * bug) throw from the underlying builder and leave no trace.
	 */
	public GTMultiBlockPattern build(int aSize) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		mDeclaration.accept(tBuilder);
		return tBuilder.build(aSize);
	}

	/**
	 * The pattern for {@code aSize}, expanded once and cached — the handle a
	 * size-parametrised controller's {@code getStructurePattern(int)} would back onto
	 * (same n, same immutable instance). Out-of-window sizes throw every call (a failed
	 * expansion is never cached).
	 */
	public GTMultiBlockPattern forSize(int aSize) {
		return mExpansions.computeIfAbsent(aSize, this::build);
	}
}
