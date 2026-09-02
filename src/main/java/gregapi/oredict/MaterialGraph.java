/**
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

package gregapi.oredict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gregapi.data.TD;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;

/**
 * Query API over the material conversion chains (the 12 "mTarget*" families) and the alloy
 * composition reference graph. Pure read-side logic on top of {@link OreDictMaterial}'s public
 * fields; the model class itself is not touched.
 *
 * PORT-SIDE NEW CLASS. Upstream GT6 (1.7.10) has no dedicated chain-query helper; the traversal
 * happens ad hoc at call sites:
 * - forward chain walk: "aMaterial = aMaterial.mTargetCrushing.mMaterial" (Loader_OreProcessing.java:311,
 *   same pattern in Drops_SmallOre.java:51);
 * - reverse lookup with alias/self/zero filtering: "for (OreDictMaterial tMat : aMat.mTargetedCrushing)
 *   if (tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCrushing.has(aMat)) tMap.put(...)"
 *   (UT.java:1047, list variant UT.java:718);
 * - self-target tests: "aMaterial.mTargetCrushing.mMaterial == aMaterial" (LanguageHandler.java:572,
 *   OreDictMaterialCondition.java:49 CRUSHING condition).
 * This class factors those idioms into named queries, keeping the upstream predicates verbatim.
 *
 * Data semantics being read (upstream OreDictMaterial.java):
 * - 12 forward targets :283-295, default "OM.stack(this, U)"; mTargetBurning defaults to amount 0
 *   (:292 "The remaining Material when being burned"), so a burning query on plain materials
 *   returns a zero-amount self stack;
 * - 12 reverse sets :299-310, each initialized to CONTAIN THE MATERIAL ITSELF;
 * - the set* setters :781-887 maintain both directions ("If aMaterial == null it will choose the
 *   previous Material instead, which is usually this"; an amount of 0 disables the process);
 * - alloy side: mComponents :258, mAlloyComponentReferences :264, mAlloyCreationRecipes :268.
 *   mAlloyComponentReferences is only filled by addAlloyingRecipe (:455-466); the alloySimple()
 *   path (:426-429) adds mComponents to mAlloyCreationRecipes but fills neither
 *   mAlloyComponentReferences nor ALLOYS. Upstream closes that gap at postInit in
 *   GT_API_Post.java:820 ("if contains(CRUCIBLE_ALLOY) && mComponents != null ->
 *   addAlloyingRecipe(mComponents)"); that wiring is ported here as
 *   {@link #applyCrucibleAlloyReferences()} so the pure-logic phase has the same complete graph.
 *
 * @author Gregorius Techneticies
 * @author ported for the GT6 modernization project (task gt-material-graph)
 */
public final class MaterialGraph {

    /** Upstream OreDictMaterialChain families; each binds the forward stack and the reverse set. */
    public enum Process {
        CRUSHING, PULVER, SMELTING, SOLIDIFYING, SMASHING, CUTTING,
        WORKING, FORGING, BURNING, BENDING, COMPRESSING, GENERIFYING;

        /** The forward stack of this process. Never null for a non-null material (OreDictMaterial.java:283-295). */
        public OreDictMaterialStack targetOf(OreDictMaterial aMaterial) {
            switch (this) {
            case CRUSHING:      return aMaterial.mTargetCrushing;
            case PULVER:        return aMaterial.mTargetPulver;
            case SMELTING:      return aMaterial.mTargetSmelting;
            case SOLIDIFYING:   return aMaterial.mTargetSolidifying;
            case SMASHING:      return aMaterial.mTargetSmashing;
            case CUTTING:       return aMaterial.mTargetCutting;
            case WORKING:       return aMaterial.mTargetWorking;
            case FORGING:       return aMaterial.mTargetForging;
            case BURNING:       return aMaterial.mTargetBurning;
            case BENDING:       return aMaterial.mTargetBending;
            case COMPRESSING:   return aMaterial.mTargetCompressing;
            case GENERIFYING:   return aMaterial.mTargetGenerifying;
            }
            throw new IllegalStateException("unreachable: " + name());
        }

        /** The reverse set of this process; contains the material itself by default (OreDictMaterial.java:299-310). */
        public Set<OreDictMaterial> targetedOf(OreDictMaterial aMaterial) {
            switch (this) {
            case CRUSHING:      return aMaterial.mTargetedCrushing;
            case PULVER:        return aMaterial.mTargetedPulver;
            case SMELTING:      return aMaterial.mTargetedSmelting;
            case SOLIDIFYING:   return aMaterial.mTargetedSolidifying;
            case SMASHING:      return aMaterial.mTargetedSmashing;
            case CUTTING:       return aMaterial.mTargetedCutting;
            case WORKING:       return aMaterial.mTargetedWorking;
            case FORGING:       return aMaterial.mTargetedForging;
            case BURNING:       return aMaterial.mTargetedBurning;
            case BENDING:       return aMaterial.mTargetedBending;
            case COMPRESSING:   return aMaterial.mTargetedCompressing;
            case GENERIFYING:   return aMaterial.mTargetedGenerifying;
            }
            throw new IllegalStateException("unreachable: " + name());
        }
    }

    private MaterialGraph() {
    }

    // ================================================================================
    // Single step queries
    // ================================================================================

    /**
     * The raw single-step target stack of aMaterial under aProcess, or null if aMaterial is null.
     * Never null otherwise; the default is the material itself with amount U (amount 0 for
     * BURNING, OreDictMaterial.java:283-295).
     */
    public static OreDictMaterialStack singleStep(OreDictMaterial aMaterial, Process aProcess) {
        if (aMaterial == null || aProcess == null) return null;
        return aProcess.targetOf(aMaterial);
    }

    /**
     * The effective conversion target: the stack's material when the edge is a real conversion
     * ("mTargetX.mMaterial != aMaterial", the check upstream uses at Loader_Recipes_Furnace.java:75),
     * null when the material targets itself (including the untouched default and the disabled
     * "setX(null/material, 0)" form).
     */
    public static OreDictMaterial conversionTarget(OreDictMaterial aMaterial, Process aProcess) {
        if (aMaterial == null || aProcess == null) return null;
        OreDictMaterialStack tTarget = aProcess.targetOf(aMaterial);
        return tTarget.mMaterial != aMaterial ? tTarget.mMaterial : null;
    }

    // ================================================================================
    // Chain expansion
    // ================================================================================

    /**
     * Walks the conversion chain aMaterial -> target -> target ... and returns the stacks along
     * the way (the starting material itself is not included). PORT-SIDE NEW: upstream walks such
     * chains only at call sites (Loader_OreProcessing.java:311), no reusable helper exists.
     *
     * Termination rules, in order:
     * 1. the current material targets itself (upstream default OreDictMaterial.java:284 and the
     *    "disabled" amount-0 form) - the self stack is NOT appended;
     * 2. the next material was already visited - a cycle closes, the closing edge is NOT appended;
     * 3. aMaxSteps hops were taken (guard against pathological graphs; the "no-blowup" bound).
     * The visited-set rule 2 already makes every walk finite, aMaxSteps is the hard belt.
     *
     * @param aMaxSteps maximum number of edges to follow, must be >= 0
     * @return unmodifiable list of the traversed target stacks, empty when there is no conversion
     */
    public static List<OreDictMaterialStack> expandChain(OreDictMaterial aMaterial, Process aProcess, int aMaxSteps) {
        if (aMaxSteps < 0) throw new IllegalArgumentException("aMaxSteps must be >= 0, got " + aMaxSteps);
        if (aMaterial == null || aProcess == null) return Collections.emptyList();
        List<OreDictMaterialStack> rPath = new ArrayList<>();
        Set<OreDictMaterial> tVisited = new HashSet<>();
        tVisited.add(aMaterial);
        OreDictMaterial tCurrent = aMaterial;
        for (int i = 0; i < aMaxSteps; i++) {
            OreDictMaterialStack tStack = aProcess.targetOf(tCurrent);
            OreDictMaterial tNext = tStack.mMaterial;
            if (tNext == tCurrent) break; // self loop: untouched default or explicitly disabled
            if (!tVisited.add(tNext)) break; // cycle: next material already on the path
            rPath.add(tStack);
            tCurrent = tNext;
        }
        return Collections.unmodifiableList(rPath);
    }

    /** Convenience overload with a generous default bound. */
    public static List<OreDictMaterialStack> expandChain(OreDictMaterial aMaterial, Process aProcess) {
        return expandChain(aMaterial, aProcess, 64);
    }

    // ================================================================================
    // Reverse queries (who converts into this material)
    // ================================================================================

    /**
     * All materials whose aProcess output is aMaterial, mapped to their output amount. The
     * predicate is upstream-verbatim (UT.java:1047): iterate the reverse set, keep entries with
     * "tMat.mTargetRegistration == tMat && tMat != aMat && tMat.mTargetCrushing.has(aMat)".
     * The three guards respectively skip alias materials (registration points elsewhere), the
     * material itself (the reverse set contains it by default, OreDictMaterial.java:299-310), and
     * disabled zero-amount edges ({@link OreDictMaterialStack#has} requires a positive amount).
     *
     * @return unmodifiable map, empty for null input
     */
    public static Map<OreDictMaterial, Long> targeting(OreDictMaterial aMaterial, Process aProcess) {
        if (aMaterial == null || aProcess == null) return Collections.emptyMap();
        Map<OreDictMaterial, Long> rMap = new HashMap<>();
        for (OreDictMaterial tMat : aProcess.targetedOf(aMaterial)) {
            OreDictMaterialStack tStack = aProcess.targetOf(tMat);
            if (tMat.mTargetRegistration == tMat && tMat != aMaterial && tStack.has(aMaterial)) {
                rMap.put(tMat, tStack.mAmount);
            }
        }
        return Collections.unmodifiableMap(rMap);
    }

    // ================================================================================
    // Alloy composition reference graph
    // ================================================================================

    /**
     * The per-unit composition of an alloy: the material stacks of mComponents divided by their
     * common divider (upstream getComponents contract, IOreDictConfigurationComponent: e.g.
     * Electrum = half a Unit Gold + half a Unit Silver). Empty for plain materials (mComponents
     * == null, OreDictMaterial.java:258) and for null input.
     */
    public static List<OreDictMaterialStack> alloyComponents(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mComponents == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(aMaterial.mComponents.getComponents()));
    }

    /** The raw (undivided) recipe amounts of an alloy's composition, same contract as {@link #alloyComponents}. */
    public static List<OreDictMaterialStack> alloyUndividedComponents(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mComponents == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(aMaterial.mComponents.getUndividedComponents()));
    }

    /** The common divider of an alloy's composition (OreDictConfigurationComponent.mCommonDivider), 0 when it has none. */
    public static long alloyCommonDivider(OreDictMaterial aMaterial) {
        if (aMaterial == null || aMaterial.mComponents == null) return 0;
        return aMaterial.mComponents.getCommonDivider();
    }

    /**
     * Direct alloy back-references: materials which have aComponent as a registered component.
     * Reads mAlloyComponentReferences (OreDictMaterial.java:264), which is only filled by
     * {@link OreDictMaterial#addAlloyingRecipe} (upstream :455-466) - run
     * {@link #applyCrucibleAlloyReferences()} once after init to get the complete picture, exactly
     * like upstream postInit does (GT_API_Post.java:820). Empty for null input.
     */
    public static Set<OreDictMaterial> alloyComponentReferences(OreDictMaterial aComponent) {
        if (aComponent == null) return Collections.emptySet();
        return Collections.unmodifiableSet(new HashSet<>(aComponent.mAlloyComponentReferences));
    }

    /**
     * Exhaustive scan variant of {@link #alloyComponentReferences}: walks the creation recipes of
     * every registered material (the mAlloyCreationRecipes traversal of Loader_Books.java:575) and
     * reports the alloys whose component stacks contain aComponent. Works without the postInit
     * wiring because the alloySimple() path (:426-429) already records mComponents as a creation
     * recipe. The mapped value is the alloy's raw component stacks. Empty for null input.
     */
    public static Map<OreDictMaterial, List<OreDictMaterialStack>> alloysContaining(OreDictMaterial aComponent) {
        if (aComponent == null) return Collections.emptyMap();
        Map<OreDictMaterial, List<OreDictMaterialStack>> rMap = new HashMap<>();
        for (OreDictMaterial tMat : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
            if (tMat == null || tMat.mAlloyCreationRecipes.isEmpty()) continue;
            for (IOreDictConfigurationComponent tConfig : tMat.mAlloyCreationRecipes) {
                for (OreDictMaterialStack tStack : tConfig.getUndividedComponents()) {
                    if (tStack.mMaterial == aComponent) {
                        rMap.put(tMat, Collections.unmodifiableList(new ArrayList<>(tConfig.getUndividedComponents())));
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableMap(rMap);
    }

    /** Read-only copy of aMaterial's alloy creation recipes (OreDictMaterial.java:268). Empty for null input. */
    public static List<IOreDictConfigurationComponent> alloyCreationRecipes(OreDictMaterial aMaterial) {
        if (aMaterial == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(aMaterial.mAlloyCreationRecipes));
    }

    /** If the material is registered in the ALLOYS set (MaterialRegistry.ALLOYS, upstream OreDictMaterial.java:53, filled by addAlloyingRecipe :455-466). */
    public static boolean isRegisteredAlloy(OreDictMaterial aMaterial) {
        return aMaterial != null && MaterialRegistry.INSTANCE.ALLOYS.contains(aMaterial);
    }

    /**
     * PORT of upstream GT_API_Post.java:814-821 (the postInit pass that completes the alloy
     * reference graph): iterate all registered materials (upstream iterates
     * OreDictMaterial.MATERIAL_MAP.values() at :816) and for every material carrying the
     * CRUCIBLE_ALLOY tag call "addAlloyingRecipe(mComponents)" when the components are known,
     * otherwise report the upstream error message verbatim on the same ERR outlet.
     *
     * VERBATIM QUIRK, kept intentionally: upstream has no dedup guard, and alloySimple()
     * (OreDictMaterial.java:426-429) already recorded mComponents as a creation recipe when the
     * material was built via setAloy/uumAloy. The postInit pass therefore appends that same
     * mComponents instance a second time (addAlloyingRecipe :455-466 "mAlloyCreationRecipes.add"
     * without dedup), exactly like upstream. Call this once after init, like upstream postInit
     * does; calling it again adds further duplicates, again like calling the upstream loop twice.
     *
     * @return the number of addAlloyingRecipe calls performed
     */
    public static int applyCrucibleAlloyReferences() {
        int rCount = 0;
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
            if (tMaterial == null || !tMaterial.contains(TD.Processing.CRUCIBLE_ALLOY)) continue;
            if (tMaterial.mComponents != null) {
                tMaterial.addAlloyingRecipe(tMaterial.mComponents);
                rCount++;
            } else {
                MaterialRegistry.ERR_LOG.println("ERROR: Alloying Recipe for " + tMaterial.mNameLocal + " cannot be added due to lack of Component Information");
            }
        }
        return rCount;
    }
}
