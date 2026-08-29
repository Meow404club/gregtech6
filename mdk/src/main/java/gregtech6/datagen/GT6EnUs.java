package gregtech6.datagen;

import java.util.HashSet;
import java.util.Set;

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.GT6Mod;
import gregtech6.item.MaterialPrefixItem;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * en_us lang, two template tables with no per-item combination explosion (GTCEu data/lang/
 * ItemLang.java:37-54 shape, ADR-P2-4):
 * <ul>
 * <li>{@code gt6.tagprefix.<prefix_snake>} = "{pre}%s{post}" for every OP prefix — the template
 *     the runtime fills via {@code Component.translatable} at {@code getName} time, composed from
 *     the prefix's mMaterialPre/mMaterialPost (OreDictPrefix.java:108, set at :338-339), which is
 *     upstream GT6's item name composition (mMaterialPre + material name + mMaterialPost);</li>
 * <li>{@code gt6.material.<material_snake>} = the English local name — exactly the field
 *     {@code MaterialPrefixItem.getName} fills the template with
 *     (MaterialPrefixItem.java:65, mNameLocal);</li>
 * <li>{@code itemGroup.gt6.materials} — the creative tab title.</li>
 * </ul>
 *
 * <p>The per-item override keys {@code gt6.<prefix>_<material>} are intentionally NOT generated:
 * they are the hand-translation layer, and their absence makes the runtime
 * {@code Language.has(specialKey)} check (MaterialPrefixItem.java:64) fall through to the
 * template. LanguageProvider sorts keys (TreeMap) and writes via DataProvider.saveStable, so the
 * output is deterministic across runs.
 */
public final class GT6EnUs extends LanguageProvider {

    public GT6EnUs(PackOutput output) {
        super(output, GT6DataGenerators.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.gt6.materials", "GT6 Materials");
        addPrefixTemplates();
        addMaterialNames();
    }

    /** Table a: one "%s"-template per prefix, all of OP.VALUES (post-OP.init()). */
    private void addPrefixTemplates() {
        Set<String> tSeen = new HashSet<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            String tKey = "gt6.tagprefix." + MaterialPrefixItem.snakeCase(tPrefix.mNameInternal);
            if (!tSeen.add(tKey)) { // snake_case collisions are unlikely but must not pass silently
                GT6Mod.LOGGER.warn("GT6 datagen: duplicate prefix template key {} for {}", tKey, tPrefix.mNameInternal);
                continue;
            }
            add(tKey, templateOf(tPrefix));
        }
    }

    /** mMaterialPre + "%s" + mMaterialPost; nulls normalised (upstream composition is plain concat). */
    private static String templateOf(OreDictPrefix prefix) {
        String tPre = prefix.mMaterialPre == null ? "" : prefix.mMaterialPre;
        String tPost = prefix.mMaterialPost == null ? "" : prefix.mMaterialPost;
        return tPre + "%s" + tPost;
    }

    /** Table b: one entry per registration-target material (alias slots merged like the bridge). */
    private void addMaterialNames() {
        Set<String> tSeen = new HashSet<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias merge, MaterialRegistry.java:182-185
            if (tMaterial == null || tMaterial.mID < 0 || tMaterial.mNameLocal == null) continue;
            String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tMaterial.mNameInternal);
            if (!tSeen.add(tKey)) continue; // first (lowest mID) definition wins
            add(tKey, tMaterial.mNameLocal);
        }
    }
}
