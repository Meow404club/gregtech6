package gregtech6.registry;

/**
 * Test support (task mdh-6-family-gate): pristine-load every driver-sensitive registration
 * family BEFORE any ABSENT pin is set. The five family lists freeze at class-init and their
 * registration walks are now driver-gated, so a pin active at first class touch freezes the
 * walk shrunk — and that frozen state leaks into every later census pin in the same JVM
 * fork (the GT6CraftingTableMatrixTest 120-pin red this class exists to prevent). One
 * {@code loadFamiliesPristine()} line in each pinning test class's {@code @BeforeAll} makes
 * the freeze order-independent; calling it again is a no-op (classes already loaded).
 *
 * <p>GTMaterialBlocks is deliberately absent: its enumerate recomputes live per call, it
 * has no class-init freeze to protect.
 */
public final class GT6DriverTestSupport {

    private GT6DriverTestSupport() {
    }

    /** Touches the five gated family lists in the pristine (all-PRESENT) driver state. */
    public static void loadFamiliesPristine() {
        int tTouch = GTBarrels.HIGH_TIER_METAL_DRUMS.size() + GT6Cells.BLOCKS_IN_ORDER.size()
                + GT6Hoppers.ROWS.size() + GTFluidPipes.ROWS.size() + GTMachines.CRAFTING_TABLE_ROWS.size();
        if (tTouch <= 0) throw new IllegalStateException("family classes failed to load");
    }
}
