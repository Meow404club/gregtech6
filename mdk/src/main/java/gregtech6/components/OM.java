/**
 * Ported from GregTech 6 (1.7.10), file gregapi/oredict/OreDictManager.java (the central
 * component face: the ItemStack→OreDictItemData storage :468, the write API :636-672, the
 * four-arm read chain :685-711, the association filter :722-729 and the recyclable listener
 * face :70/:141-148) plus gregapi/util/OM.java (the data/anydata gates :167-186), by task
 * component-central-face. The unification-target seam (constraint handed over from the
 * component-data-model card) re-adds the ItemStack side of OreDictItemData.java:39/:171-173
 * and OreDictManager.java:467/:632.
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

package gregtech6.components;

import static gregapi.data.CS.F;
import static gregapi.data.CS.T;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictItemData;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.UT;
import gregtech6.datagen.GT6ItemTags;
import gregtech6.registry.GTMaterialItems;

/**
 * The central component face (the ADR "中央成分面" trunk, docs/adr/2026-10-06-components-subsystem.md):
 * every consumer (recycling, crucible feed, F3+H, scanner, derivation writer) routes through it.
 *
 * <p>The read chain (upstream OreDictManager.getItemData_ :685-711), arm by arm:
 * <ol>
 * <li><b>NBT_RECYCLING_MATS</b> (:690-692) — DECLARED DEFERRED per the card spec: the arm keeps
 *     its {@code aAllowOverride} gate position, but the NBT material-stack list writer/reader
 *     (upstream OreDictMaterialStack.loadList over NBT) has no port consumer yet. The arm slot is
 *     the first thing a future NBT-override card re-opens, ahead of the provider arm's merge
 *     (upstream :695 aggregates NBT data WITH provider data via the aggregation constructor).</li>
 * <li><b>Read-time self-description</b> — {@link IOreDictItemDataOverrideItem} (:694),
 *     implemented by {@link gregtech6.item.MaterialPrefixItem} and
 *     {@link gregtech6.item.GTMaterialPrefixBlockItem}.</li>
 * <li><b>The global map</b> — exact (item, damage) key (:698), then the wildcard-damage key
 *     (:700; {@link #W}, upstream CS.W = OreDictionary.WILDCARD_VALUE, CS.java:138), then the
 *     <b>vanilla family-tag arm</b> — the port's read-time replacement for the 1.7.10
 *     OreDictionary-registration fill of the same map (see {@link #resolveByFamilyTags}).</li>
 * <li><b>Damageable proportionality</b> (:702-708) — the zero-damage map entry with
 *     {@code mUseVanillaDamage} rescales Main Material and Byproducts by the REMAINING
 *     durability fraction (UT.Code.units, the {@code F} = round-down form).</li>
 * </ol>
 *
 * <p>Port deviations (all DECLARED):
 * <ul>
 * <li>The upstream {@code OreDictManager.INSTANCE} singleton and the {@code OM} facade collapse
 *     into this one static class ({@code gregtech6.components.OM}; the upstream
 *     gregapi/util/OM.java is not ported per the component-data-model handoff).</li>
 * <li>The 1.7.10 item metadata axis collapses to the vanilla-damage axis (1.20.1 has no item
 *     meta): the map key is (Item, {@link ItemStack#getDamageValue()}), which equals 0 for
 *     every non-damageable item — exactly where upstream's non-meta items sat.</li>
 * <li>{@code mBlocked} (:659): the {@code ST.block(aStack) != NB} arm reads
 *     {@code getItem() instanceof BlockItem}; the {@code FL.getFluid(aStack, T) != null} and
 *     {@code IFluidContainerItem.getCapacity > 0} arms read the modern item fluid-handler
 *     capability with content (both upstream arms = "the stack carries fluid"); the
 *     {@code isBlacklisted(aStack)} consult (:658) rides only the data's own {@code mBlackListed}
 *     flag — the unification blacklist registry (upstream sNoUnificationSet :469, fed by
 *     addToBlacklist :476-484) belongs to the unification face and has no port caller yet.</li>
 * <li>CUT, no port caller: the IC2-recycling blacklist (:664), the oredict-name registration
 *     machinery (onOreRegistration1/2, mStringToItemDataMappings :75, setTarget/addTarget —
 *     the unification face), addAssociation (:713-720), and the
 *     GAPI.mStartedPostInit listener buffering (:142 — the port registers and replays
 *     immediately; upstream's replay re-reads the map per stored stack :147, whose result is
 *     the same stored data object).</li>
 * </ul>
 */
public class OM {
	/** Upstream CS.W = OreDictionary.WILDCARD_VALUE (OreDictManager.java:700 damage-wildcard map key). */
	public static final int W = 32767;

	/** The storage: (item, damage) → data. Upstream sItemStack2DataMap (:468, an ItemStackMap keyed
	 * by (mItem, mMetaData) — ItemStackContainer.equals :103; the stack size never keys). Plain keys. */
	private static final Map<StackKey, OreDictItemData> sItemStack2DataMap = new HashMap<>();

	/** These Listeners always get notified when an Item gets recyclable ItemData attached (upstream :69-70). */
	private static final Set<IOreDictListenerRecyclable> sRecyclableListeners = new HashSet<>();
	private static final Set<IOreDictListenerRecyclable.OreDictRecyclingContainer> sRecyclableRegistrations = new HashSet<>();

	/**
	 * The reverse-tag query seam of the vanilla family-tag arm. Production binding:
	 * {@code ItemStack::getTags} (vanilla 1.20.1 ItemStack.java:224-226 — the Item holder's
	 * reverse-tag face, the Forge-documented {@code IReverseTag} API,
	 * forge-1.20.1 IReverseTag.java "A reverse tag is an object aware of what tags it is
	 * contained in"). Offline the tag manager never boots and every tag reads empty
	 * (the GT6RecipeTagFallbackTest seam discipline, {@link gregtech6.recipes.Recipe#sTagTest}):
	 * the tests inject a membership stub, the same-value ruling of
	 * decisions.p25-tag-input-fallback-rulings ②.
	 */
	public static Function<ItemStack, Stream<TagKey<Item>>> sStackTags = ItemStack::getTags;

	// --------------------------------------------------------------------------
	// the read chain (upstream OreDictManager.getItemData_ :685-711 + OM gates)
	// --------------------------------------------------------------------------

	/** Upstream OM.data (OM.java:167-169): the map arms only — no override arms. */
	@Nullable public static OreDictItemData data(ItemStack aStack) {
		if (invalid(aStack)) return null;
		return data_(aStack);
	}
	/** Upstream OM.data_ (:170-172). */
	@Nullable public static OreDictItemData data_(ItemStack aStack) {
		return getItemData_(aStack, F);
	}

	/** Upstream OM.anydata (:174-176): all arms, override allowed. */
	@Nullable public static OreDictItemData anydata(ItemStack aStack) {
		if (invalid(aStack)) return null;
		return anydata_(aStack);
	}
	/** Upstream OM.anydata_ (:177-179). */
	@Nullable public static OreDictItemData anydata_(ItemStack aStack) {
		return getItemData_(aStack, T);
	}

	/** Upstream getItemData :681-684. */
	@Nullable public static OreDictItemData getItemData(ItemStack aStack, boolean aAllowOverride) {
		if (invalid(aStack)) return null;
		return getItemData_(aStack, aAllowOverride);
	}

	/** Upstream OreDictManager.getItemData_ :685-711, arm order verbatim (the vanilla family-tag
	 * arm slots in after the map probes — the read-time stand-in for the same map's 1.7.10
	 * registration fill; explicit map declarations keep winning over tag parsing, and the
	 * mUseVanillaDamage arm keeps its slot after it, exactly as it kept its slot after the
	 * upstream registration-fed map). The returned data is the STORED instance for map hits
	 * (upstream :698-699 returns the map value by reference) and a FRESH object for the
	 * provider/tag/damage arms (upstream :694-696/:707). */
	@Nullable public static OreDictItemData getItemData_(ItemStack aStack, boolean aAllowOverride) {
		OreDictItemData rData = null;
		if (aAllowOverride) {
			// arm 1: NBT_RECYCLING_MATS (:690-692) — DEFERRED (declared above); when it lands it
			// merges with arm 2 here via the aggregation constructor (:695).
			// arm 2: the read-time self-description (:694-696).
			if (aStack.getItem() instanceof IOreDictItemDataOverrideItem) rData = ((IOreDictItemDataOverrideItem)aStack.getItem()).getOreDictItemData(aStack);
			if (rData != null) return rData;
		}
		// arm 3: the global map, exact key (:698) then the wildcard-damage key (:700).
		rData = sItemStack2DataMap.get(new StackKey(aStack.getItem(), aStack.getDamageValue()));
		if (rData != null) return rData;
		rData = sItemStack2DataMap.get(new StackKey(aStack.getItem(), W));
		if (rData != null) return rData;
		// arm 3b: the vanilla family-tag arm (the port's modern oredict-registration isomorph).
		rData = resolveByFamilyTags(aStack);
		if (rData != null) return rData;
		// arm 4: damageable proportionality (:702-708). The zero-damage entry rescales by the
		// REMAINING durability fraction when mUseVanillaDamage; a zero-damage entry without the
		// flag returns unscaled (:703-708 — the rescale is local, the stored entry never mutates).
		if (aAllowOverride && aStack.isDamageableItem()) {
			rData = sItemStack2DataMap.get(new StackKey(aStack.getItem(), 0));
			if (rData != null && rData.mUseVanillaDamage) {
				OreDictMaterialStack[] tByProducts = new OreDictMaterialStack[rData.mByProducts.length];
				for (int i = 0; i < tByProducts.length; i++) tByProducts[i] = new OreDictMaterialStack(rData.mByProducts[i].mMaterial, UT.Code.units(aStack.getMaxDamage()-aStack.getDamageValue(), aStack.getMaxDamage(), rData.mByProducts[i].mAmount, F));
				return new OreDictItemData(new OreDictMaterialStack(rData.mMaterial.mMaterial, UT.Code.units(aStack.getMaxDamage()-aStack.getDamageValue(), aStack.getMaxDamage(), rData.mMaterial.mAmount, F)), tByProducts);
			}
		}
		return rData;
	}

	/** Upstream getAssociation :722-729: the read result filtered to prefixed, material-carrying,
	 * non-ore data — the "can this stack be a unification/set gate" predicate. */
	@Nullable public static OreDictItemData getAssociation(ItemStack aStack, boolean aOverwrite) {
		if (invalid(aStack)) return null;
		return getAssociation_(aStack, aOverwrite);
	}
	@Nullable public static OreDictItemData getAssociation_(ItemStack aStack, boolean aOverwrite) {
		OreDictItemData rData = getItemData_(aStack, aOverwrite);
		return rData != null && rData.validData() && rData.mPrefix != OP.ore ? rData : null;
	}

	/**
	 * The vanilla family-tag arm (the ADR leg-B shrink: "stack → its owning family tag → resolve
	 * within the family"). 1.7.10 fed the SAME global map from the OreDictionary event stream
	 * (every mod's "ingotIron" registration parsed to (ingot, Iron) and landed in
	 * sItemStack2DataMap); the modern registration stream IS the tag tree, so the read consults
	 * the stack's reverse tags: a material-family tag id ({@code <forge|c>:<family>/<materialSnake>},
	 * the {@code VANILLA_INTERSECTION} shape — GT6ItemTags.java:453-468, e.g. forge-1.20.1
	 * Tags.java:314 {@code INGOTS_IRON = tag("ingots/iron")}) parses back to the (prefix, material)
	 * pair and answers the Prefix data. Zero content table: no per-item entries, the reverse
	 * prefix map derives from {@link GT6ItemTags#itemTagFamily} (the SAME source the datagen and
	 * the recipe fallback read, so the family list cannot drift), the material side from the
	 * registered materials' snake names. DECLARED v1 bounds: the item-path families only — the
	 * storage_blocks family is one tag id shared by three prefixes (GT6ItemTags.storageTagFamily
	 * :934-938: blockIngot/blockGem/blockDust), so its reverse needs a per-material ruling that
	 * would be a content table (the red line); it stays deferred like the NBT arm.
	 */
	@Nullable public static OreDictItemData resolveByFamilyTags(ItemStack aStack) {
		ensureTagLookups();
		for (TagKey<Item> tTag : sStackTags.apply(aStack).toList()) {
			net.minecraft.resources.ResourceLocation tId = tTag.location();
			if (!GT6ItemTags.MATERIALS_NAMESPACE.equals(tId.getNamespace()) && !GT6ItemTags.COMMON_NAMESPACE.equals(tId.getNamespace())) continue;
			String tPath = tId.getPath();
			int tSplit = tPath.indexOf('/');
			if (tSplit <= 0) continue;
			OreDictPrefix tPrefix = sFamilyToPrefix.get(tPath.substring(0, tSplit));
			if (tPrefix == null) continue;
			OreDictMaterial tMaterial = sMaterialBySnake.get(tPath.substring(tSplit + 1));
			if (tMaterial == null) continue;
			return new OreDictItemData(tPrefix, tMaterial);
		}
		return null;
	}

	private static Map<String, OreDictPrefix> sFamilyToPrefix = null;
	private static Map<String, OreDictMaterial> sMaterialBySnake = null;

	/** The lazy reverse lookups (the tag arm runs long after the material universe booted). First wins. */
	private static synchronized void ensureTagLookups() {
		if (sFamilyToPrefix != null) return;
		Map<String, OreDictPrefix> tFamilies = new HashMap<>();
		for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
			String tFamily = GT6ItemTags.itemTagFamily(tPrefix);
			if (tFamily != null) tFamilies.putIfAbsent(tFamily.substring(0, tFamily.indexOf('/')), tPrefix);
		}
		Map<String, OreDictMaterial> tMaterials = new HashMap<>();
		for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_MAP.values()) tMaterials.putIfAbsent(GTMaterialItems.snakeCase(tMaterial.mNameInternal), tMaterial);
		sFamilyToPrefix = tFamilies;
		sMaterialBySnake = tMaterials;
	}

	// --------------------------------------------------------------------------
	// the write API (upstream OreDictManager :636-672)
	// --------------------------------------------------------------------------

	/** Upstream addItemData :636-640: write only into the absence of data. */
	public static boolean addItemData(ItemStack aStack, OreDictItemData aData) {
		if (invalid(aStack)) return F;
		return addItemData_(aStack, aData);
	}
	/** Upstream addItemData_ :641-644 (no validity check on the stack; null data still declines). */
	public static boolean addItemData_(ItemStack aStack, OreDictItemData aData) {
		if (getItemData_(aStack, F) == null && aData != null) return setItemData_(aStack, aData);
		return F;
	}

	/** Upstream setItemData :646-649: overwrite semantics, guarded. */
	public static boolean setItemData(ItemStack aStack, OreDictItemData aData) {
		if (invalid(aStack) || aData == null) return F;
		return setItemData_(aStack, aData);
	}

	/**
	 * Upstream setItemData_ :650-672, verbatim semantics:
	 * the Wood exception first (:651-652 — existing non-Wood data REFUSES the overwrite; Wood
	 * and ANY.Wood data may be re-declared, the planks-from-every-mod rule); the stack-size
	 * amortization next (:653-657 — a multi-stack's amounts divide by the size, the data keys
	 * per ONE item, and the local stack becomes a 1-count copy :656); then the
	 * mBlackListed/mBlocked computation (:658-659) and the recyclable notification gate
	 * (:666-670 — only prefix-less data or a RECYCLABLE prefix notifies).
	 * MUTATES the passed data (the amortization divides in place — upstream behavior), and the
	 * STORED instance is the passed instance (:660) — handed-over constraint ① (the data-model
	 * card): prefix-constructed data keeps the Prefix's own Byproduct stacks by reference
	 * (OreDictItemData.java:80), so a caller caching weights/views off this data
	 * (getAllMaterialWeights' no-prefix arm passes the stored stack) must copy on its side; the
	 * central face itself caches nothing but the caller's own instance, upstream verbatim.
	 */
	public static boolean setItemData_(ItemStack aStack, OreDictItemData aData) {
		OreDictItemData tData = getAssociation_(aStack, F);
		if (tData != null && tData.mMaterial.mMaterial != MT.Wood && tData.mMaterial.mMaterial != ANY.Wood) return F;
		if (aStack.getCount() > 1) {
			if (aData.mMaterial != null) aData.mMaterial.mAmount /= aStack.getCount();
			for (OreDictMaterialStack tMaterial : aData.mByProducts) tMaterial.mAmount /= aStack.getCount();
			// upstream :656 (ST.amount(1, aStack) = copy at size 1): the key below never sees the
			// size, but the recyclable container does — registrations carry ONE-count stacks.
			aStack = aStack.copyWithCount(1);
		}
		// upstream :658 (the unification-blacklist registry consult) is cut with that face (see the
		// class deviations) — mBlackListed keeps only the value the caller set on the data.
		if (!aData.mBlocked) aData.mBlocked = aData.mBlackListed || aStack.getItem() instanceof BlockItem || carriesFluid(aStack);
		sItemStack2DataMap.put(new StackKey(aStack.getItem(), aStack.getDamageValue()), aData);
		if (!aData.validPrefix() || aData.mPrefix.contains(TD.Prefix.RECYCLABLE)) {
			IOreDictListenerRecyclable.OreDictRecyclingContainer tRegistration = new IOreDictListenerRecyclable.OreDictRecyclingContainer(aStack, aData);
			for (IOreDictListenerRecyclable tListener : sRecyclableListeners) tListener.onRecycleableRegistration(tRegistration);
			sRecyclableRegistrations.add(tRegistration);
		}
		return T;
	}

	/**
	 * The derivation-face re-entry seam (ADR docs/adr/2026-10-06-components-subsystem.md red
	 * line 3: "/reload 重入必须幂等（derived 集清除重建）"). Upstream never re-enters (1.7.10
	 * derivation ran once per JVM at registration time), so it needed no removal face; the
	 * port's reload deriver re-enters on every /reload and must be able to retire ITS OWN
	 * previous writes when the recipe graph moved under them. Removes the stored data at this
	 * stack's (item, damage) key AND prunes the matching recyclable registrations (the
	 * container is identity-held, IOreDictListenerRecyclable.OreDictRecyclingContainer has no
	 * equals — a stale entry would replay forever to every late listener). Only callers that
	 * can prove they authored the entry may call this (the deriver tracks its own written
	 * keys); explicit declarations are untouchable by derivation, upstream add-only semantics.
	 * No-op on absent keys. Task component-derivation-reload.
	 */
	public static boolean removeItemData(ItemStack aStack) {
		if (invalid(aStack)) return F;
		StackKey tKey = new StackKey(aStack.getItem(), aStack.getDamageValue());
		boolean rAny = sItemStack2DataMap.remove(tKey) != null;
		rAny |= sRecyclableRegistrations.removeIf(tRegistration ->
				tRegistration.mStack.getItem() == aStack.getItem() && tRegistration.mStack.getDamageValue() == aStack.getDamageValue());
		return rAny;
	}

	/**
	 * The fluid arms of upstream :659 ({@code FL.getFluid(aStack, T) != null} plus the
	 * {@code IFluidContainerItem.getCapacity > 0} leg): the modern item fluid-handler
	 * capability with content — forge FluidUtil.getFluidContained (the in-repo precedent,
	 * AbstractCoverLogisticsFluid.java:43), neo Capabilities.FluidHandler.ITEM :45.
	 *
	 * <p>DECLARED environment probe: the Forge capability token is transformer-implemented, so
	 * in an untransformed (offline test) JVM the first consult dies at the ForgeCapabilities
	 * class-init ("This will be implemented by a transformer"). The one-time catch parks the
	 * arm for that JVM — a live (transformed) runtime initializes the capability class cleanly
	 * and never takes this branch.
	 */
	private static boolean sFluidArmLive = T;

	private static boolean carriesFluid(ItemStack aStack) {
		if (!sFluidArmLive) return F;
		try {
			//? if forge {
			return net.minecraftforge.fluids.FluidUtil.getFluidContained(aStack).isPresent();
			//?} else {
			/*var tHandler = aStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM, null);
			return tHandler != null && !tHandler.getFluidInTank(0).isEmpty();
			 *///?}
		} catch (Throwable t) {
			sFluidArmLive = F;
			return F;
		}
	}

	// --------------------------------------------------------------------------
	// the recyclable listener face (upstream :69-70, :141-148)
	// --------------------------------------------------------------------------

	/** Upstream addListener(IOreDictListenerRecyclable) :141-148, without the GAPI post-init
	 * buffering: the listener registers immediately and replays all past registrations
	 * (the upstream catch-up semantic; the port has no init phase to defer to). */
	public static void addListener(IOreDictListenerRecyclable aListener) {
		if (aListener != null && sRecyclableListeners.add(aListener)) for (IOreDictListenerRecyclable.OreDictRecyclingContainer tEvent : sRecyclableRegistrations) aListener.onRecycleableRegistration(tEvent);
	}

	/** The registration log the listeners replay from (upstream mRecyclableRegistrations :70). */
	public static List<IOreDictListenerRecyclable.OreDictRecyclingContainer> recyclingRegistrations() {
		return new ArrayList<>(sRecyclableRegistrations);
	}

	// --------------------------------------------------------------------------
	// the unification-target seam (the component-data-model handoff: OreDictItemData's cut
	// ItemStack field :39, its getStack :171-173 and the sName2StackMap lazy fill :632)
	// --------------------------------------------------------------------------

	/** Upstream sName2StackMap (:467): oredict name → the canonical unification stack. The FILL
	 * face is the unification face (upstream setTarget_); until that card lands the map stays
	 * empty and the seam below degrades to the prefix arm of getStack — the same degradation
	 * the upstream code shows on an unfilled map. */
	private static final Map<String, ItemStack> sName2StackMap = new HashMap<>();

	/** The {@code mUnificationTarget} field isomorph (:39): the root model is ItemStack-free, so
	 * the lazy per-data cache lives here, keyed by data identity. Upstream caches the first
	 * lookup onto the data (:632; resetUnificationTarget :781 clears); the lookup is a pure
	 * function of {@code toString()} and the map, so the side cache is observably identical —
	 * including copy() sharing (a copied data re-looks-up to the same target, upstream :177). */
	@Nullable public static ItemStack unificationTarget(OreDictItemData aData) {
		if (aData == null) return null;
		ItemStack rStack = sUnificationTargetCache.get(aData);
		if (rStack == null) {
			rStack = sName2StackMap.get(aData.toString());
			if (rStack != null) sUnificationTargetCache.put(aData, rStack);
		}
		return rStack;
	}
	private static final Map<OreDictItemData, ItemStack> sUnificationTargetCache = new HashMap<>();

	/** The upstream :632 fill, test-scoped until the unification card lands (its real filler is
	 * setTarget_). Package-private on purpose. */
	static void putUnificationTarget(String aOreDictName, ItemStack aStack) {
		sName2StackMap.put(aOreDictName, aStack);
	}

	/** Upstream :781 (resetUnificationTarget's cache clear), the invalidation face of the seam. */
	public static void clearUnificationTargets() {
		sUnificationTargetCache.clear();
	}

	/**
	 * Upstream OreDictItemData.getStack :171-173: a stack of this data's material at the given
	 * amount — the unification target when one is known, else the prefix's canonical item
	 * (the {@code mPrefix.mat(...)} isomorph = the GTMaterialItems registration index,
	 * {@link GTMaterialItems#get}). Prefix-less data (upstream would NPE at :172) yields EMPTY.
	 */
	public static ItemStack getStack(@Nullable OreDictItemData aData, long aAmount) {
		if (aData == null) return ItemStack.EMPTY;
		ItemStack rStack = unificationTarget(aData);
		if (rStack != null) return aAmount <= 0 ? ItemStack.EMPTY : rStack.copyWithCount((int)aAmount);
		if (!aData.validPrefix() || !aData.validMaterial()) return ItemStack.EMPTY;
		var tItem = GTMaterialItems.get(aData.mPrefix, aData.mMaterial.mMaterial);
		return tItem == null || aAmount <= 0 ? ItemStack.EMPTY : new ItemStack(tItem.get(), (int)aAmount);
	}

	// --------------------------------------------------------------------------

	/** Upstream ST.invalid. */
	private static boolean invalid(ItemStack aStack) {
		return aStack == null || aStack.isEmpty();
	}

	/** The map key: (item identity, damage axis) — the 1.20.1 collapse of upstream
	 * ItemStackContainer (mItem, mMetaData), equals/hashCode over both members (:103-104). */
	record StackKey(Item item, int damage) {}
}
