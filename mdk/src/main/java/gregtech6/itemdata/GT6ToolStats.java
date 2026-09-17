package gregtech6.itemdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.nbt.CompoundTag;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The tool-stats payload — the FIRST key registered in the {@link GT6ItemData}
 * seam (task p31-identity-seam, guardrail ① "the metatool lands first"): the
 * material identity of a GT6 tool, riding the upstream verbatim
 * {@code "GT.ToolStats"} compound shape (MultiItemTool.java:180-192 write,
 * :351-362 read):
 * <ul>
 * <li>{@code "a"} short primary material id (when {@code mID > 0}), else
 * {@code "b"} string primary material name — the short-stays-short save-compat
 * discipline, the same precedent as the MaterialStackNBT {@code "i"} write;</li>
 * <li>{@code "c"}/{@code "d"} the same pair for the secondary (rod/handle)
 * material, written only when one exists (:184-186);</li>
 * <li>{@code "j"} long max tool damage in upstream units
 * {@code mToolDurability * 100 * multiplier} (:182); the port's vanilla-point
 * mapping is 100 units = 1 point (Steel 512 → 51200 units → the pinned 512
 * points, the ratio the crowbar durability seam rides).</li>
 * </ul>
 * The electric trio {@code "e"}/{@code "f"}/{@code "g"} (:187-191) is NOT
 * carried — the electric/pocket tools are the declared single-steel-tier
 * deviation (no material identity to attach), and the codec grows additively
 * when that changes.
 *
 * <p>FAIL-VISIBLE (guardrail ③): a payload present but under-specified — no
 * primary key, no {@code "j"}, or a material name/id that no longer resolves —
 * is a DECODE ERROR, never a silent {@code MT.NULL} fallback (the upstream
 * {@code getPrimaryMaterial(stack, aDefault)} silent-default shape is exactly
 * what this seam replaces). Missing payload altogether = the explicit-missing
 * arm: {@link GT6ItemData#find} returns empty, {@link GT6ItemData#get} throws.
 */
public record GT6ToolStats(OreDictMaterial primaryMaterial, OreDictMaterial secondaryMaterial, long maxDamage) {

	/** The per-key codec: the compound itself is the DFU form, so both legs serialize the exact upstream NBT. */
	public static final Codec<GT6ToolStats> CODEC = CompoundTag.CODEC
			.flatXmap(GT6ToolStats::fromTag, tStats -> DataResult.success(tStats.toTag()));

	/** The seam key — NBT name verbatim upstream (:192), DC path {@code gt6:tool_stats} on the 1.21.1 leg. */
	public static final GT6DataKey<GT6ToolStats> KEY = new GT6DataKey<>("GT.ToolStats", "tool_stats", CODEC);

	/**
	 * The getToolWithStats isomorph (MultiItemTool.java:180-182): the max-damage
	 * snapshot {@code mToolDurability * 100 * multiplier} in upstream units.
	 */
	public static GT6ToolStats of(OreDictMaterial aPrimary, OreDictMaterial aSecondary, float aDurabilityMultiplier) {
		return new GT6ToolStats(aPrimary, aSecondary, (long) ((aPrimary.mToolDurability * 100L) * aDurabilityMultiplier));
	}

	/** The upstream :180-186/:192 compound, key-for-key and type-for-type (short stays short). */
	public CompoundTag toTag() {
		CompoundTag tToolNBT = new CompoundTag();
		if (primaryMaterial.mID > 0) tToolNBT.putShort("a", primaryMaterial.mID);
		else tToolNBT.putString("b", primaryMaterial.toString());
		if (secondaryMaterial != null) {
			if (secondaryMaterial.mID > 0) tToolNBT.putShort("c", secondaryMaterial.mID);
			else tToolNBT.putString("d", secondaryMaterial.toString());
		}
		tToolNBT.putLong("j", maxDamage);
		return tToolNBT;
	}

	/** The upstream :351-362 read, upgraded from the silent default to the fail-visible DataResult. */
	private static DataResult<GT6ToolStats> fromTag(CompoundTag aTag) {
		OreDictMaterial tPrimary = aTag.contains("a") ? checked(aTag.getShort("a"))
				: aTag.contains("b") ? checked(aTag.getString("b")) : null;
		if (tPrimary == null) return DataResult.error(() -> "GT.ToolStats without a resolvable primary material (a/b): " + aTag);
		OreDictMaterial tSecondary = null;
		if (aTag.contains("c")) {
			tSecondary = checked(aTag.getShort("c"));
			if (tSecondary == null) return DataResult.error(() -> "GT.ToolStats key c does not resolve: " + aTag);
		} else if (aTag.contains("d")) {
			tSecondary = checked(aTag.getString("d"));
			if (tSecondary == null) return DataResult.error(() -> "GT.ToolStats key d does not resolve: " + aTag);
		}
		if (!aTag.contains("j")) return DataResult.error(() -> "GT.ToolStats payload without the max-damage key j: " + aTag);
		return DataResult.success(new GT6ToolStats(tPrimary, tSecondary, aTag.getLong("j")));
	}

	/** The registry miss (MaterialRegistry.get lands on MT.NULL) maps to null, i.e. a DECODE ERROR — never a silent default. */
	private static OreDictMaterial checked(long aMaterialID) {
		OreDictMaterial tMaterial = OreDictMaterial.get(aMaterialID);
		return tMaterial == MT.NULL ? null : tMaterial;
	}

	private static OreDictMaterial checked(String aMaterialName) {
		OreDictMaterial tMaterial = OreDictMaterial.get(aMaterialName);
		return tMaterial == MT.NULL ? null : tMaterial;
	}
}
