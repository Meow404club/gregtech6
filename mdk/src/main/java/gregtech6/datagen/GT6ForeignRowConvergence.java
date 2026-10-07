package gregtech6.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6ForeignMaterialAtlas;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

/**
 * The row-level convergence face (task parse-errors-registration-convergence; coordinator
 * ruling 2026-10-07). One predicate, two JSON forms:
 *
 * <p><b>The divergence.</b> The live registration universe is seeded NARROW —
 * {@code GT6ModDrivers.seedFromEnvironment} pins every {@link GT6ForeignMaterialAtlas} PRIMARY
 * domain the install lacks to ABSENT and the walk mounts ({@code GTMaterialItems.enumerate()},
 * {@code GTMaterialBlocks.enumerate()}) drop those pairs on a real install — while the
 * committed datagen tree walks the default full universe (the datagen-JVM seed short-circuit,
 * GT6ModDrivers.seedFromEnvironment). Until this class the committed recipe/loot JSONs
 * therefore referenced ids no bare install ever registered: the 12018 "Unknown item" parse
 * errors (B1 chemtube + B2 exotic-material rows, one root cause) and the loot-side sibling
 * (B5), headers {@code tool_head_hammer_spectre_iron} / {@code stick_astral_silver} /
 * {@code foil_shadow_iron} / {@code tool_head_construction_pickaxe_meteoflame_red_steel} /
 * {@code rock_gt_dolamide} / {@code block_ingot_obsidian_refined}.
 *
 * <p><b>The two forms</b> (the load paths decide, not taste):
 * <ul>
 * <li><b>recipes</b> carry the top-level {@code "conditions"} array the Forge 1.20.1
 *     RecipeManager patch processes BEFORE the serializer runs
 *     ({@code CraftingHelper.processConditions(json, "conditions", context)} — a failed
 *     condition skips the row silently at DEBUG, RecipeManager.java.patch:29): the recipe emit
 *     seam (GT6CraftingRecipes.run, the ONE consumer every crafting family flows through)
 *     wraps rows whose referenced gt6 ids belong to gated domains with
 *     {@code [{"type":"forge:mod_loaded","modid":<domain>}]} — AND semantics, so the row loads
 *     exactly when every referenced item is registered (the live seed uses the same
 *     {@code ModList.isLoaded(modid)} predicate, the correspondence is by construction);</li>
 * <li><b>loot tables</b> have NO load-time condition mechanism (ForgeHooks.loadLootTable
 *     deserializes straight into LootTable; no top-level key is read), so the loot row faces
 *     SKIP gated pairs at datagen instead ({@link #gatingDomain} at the walk seams of
 *     GT6LootTables.GT6BlockLoot / GT6OreLootTables / GT6LootInjectionDatagen): the committed
 *     tree carries no loot JSON referencing a would-be-absent id — the upstream bare-install
 *     shape (no item, no loot row; Loader_Loot addLoot :566-569 skip face), with the declared
 *     cost that an install carrying the owning mod also misses the row.</li>
 * </ul>
 *
 * <p>The predicate is registration-axis-free BY RULING: {@link #gatingDomain} reads the static
 * atlas only, never the driver state — the datagen JVM and the live JVM must answer identically
 * or the committed tree drifts with the environment (the ADR-MDH1 datagen side).
 */
public final class GT6ForeignRowConvergence {

    /** item id (the {@code itemIdOf} form, no namespace) -> owning modid, for every gated gt6 pair. */
    private static Map<String, String> sGatedIds;

    private GT6ForeignRowConvergence() {
    }

    /**
     * The owning modid of a material when the live seed can hide it (atlas PRIMARY, seedable
     * domain), else {@code null}. Alias-merged like the bridge (MaterialRegistry.get). The
     * non-seedable self-modid domain (GT5U "gregtech" rows) never gates — the live seed never
     * pins our own modid, so the row always loads.
     */
    public static String gatingDomain(OreDictMaterial aMaterial) {
        if (aMaterial == null) return null;
        OreDictMaterial tMaterial = MaterialRegistry.INSTANCE.get(aMaterial);
        if (tMaterial == null || tMaterial.mID < 0) return null;
        String tDomain = GT6ForeignMaterialAtlas.domainOf(tMaterial);
        if (tDomain == null) return null;
        return GT6ForeignMaterialAtlas.seedableDomains().contains(tDomain) ? tDomain : null;
    }

    /** The gated id map, lazily over the item + block walks (first use is post-initMaterials). */
    private static Map<String, String> gatedIds() {
        if (sGatedIds == null) {
            Map<String, String> rIds = new HashMap<>();
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
                String tDomain = gatingDomain(tPair.material());
                if (tDomain != null) rIds.put(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()), tDomain);
            }
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
                String tDomain = gatingDomain(tPair.material());
                if (tDomain != null) rIds.put(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()), tDomain);
            }
            sGatedIds = rIds;
        }
        return sGatedIds;
    }

    /** The modids a serialized recipe row must require: the gated domains of its gt6 item refs, sorted. */
    public static List<String> rowDomains(JsonObject aRecipeJson) {
        Set<String> tDomainSet = new java.util.TreeSet<>();
        collectItemIds(aRecipeJson, gatedIds(), tDomainSet);
        return new ArrayList<>(tDomainSet);
    }

    /**
     * Recursive walk over every item-reference member; gated ids contribute their domain. Three
     * keys: {@code "item"} (1.20.1 recipe ingredients/results + the loot modifier rows), {@code
     * "id"} (the 1.21 result codec form the node tree writes, RecipeManager 1.21 ItemStack
     * STRICT_CODEC) and {@code "name"} (the 1.20.1 loot entry face — the actual B5 parser
     * key). Non-refs sharing the key names survive: values are looked up against the gated
     * gt6 id map, everything else misses.
     */
    private static void collectItemIds(JsonObject aJson, Map<String, String> aGated, Set<String> aDomains) {
        for (Map.Entry<String, com.google.gson.JsonElement> tEntry : aJson.entrySet()) {
            com.google.gson.JsonElement tElement = tEntry.getValue();
            if (tElement.isJsonObject()) {
                collectItemIds(tElement.getAsJsonObject(), aGated, aDomains);
            } else if (tElement.isJsonArray()) {
                collectItemIds(tElement, aGated, aDomains);
            } else if (tElement.isJsonPrimitive()
                    && ("item".equals(tEntry.getKey()) || "id".equals(tEntry.getKey()) || "name".equals(tEntry.getKey()))) {
                String tId = tElement.getAsString();
                String tNamespaceFree = tId.startsWith("gt6:") ? tId.substring(4) : tId;
                String tDomain = aGated.get(tNamespaceFree);
                if (tDomain != null) aDomains.add(tDomain);
            }
        }
    }

    /**
     * The array form: Forge grouped ingredients nest (the any-iron variant lists are arrays
     * inside the ingredients array — the alternative-items form, two levels deep from the row
     * root) — recurse through both members and nested arrays.
     */
    private static void collectItemIds(com.google.gson.JsonElement aJson, Map<String, String> aGated, Set<String> aDomains) {
        for (com.google.gson.JsonElement tChild : aJson.getAsJsonArray()) {
            if (tChild.isJsonObject()) collectItemIds(tChild.getAsJsonObject(), aGated, aDomains);
            else if (tChild.isJsonArray()) collectItemIds(tChild, aGated, aDomains);
        }
    }

    //? if forge {
    /**
     * The recipe emit seam: the row unchanged when it references no gated id (the identical
     * instance — zero cost for the vanilla-only rows), else a conditions-carrying wrapper.
     */
    public static net.minecraft.data.recipes.FinishedRecipe converged(net.minecraft.data.recipes.FinishedRecipe aRow) {
        List<String> tDomains = rowDomains(aRow.serializeRecipe());
        if (tDomains.isEmpty()) return aRow;
        return new ConvergedRow(aRow, tDomains);
    }

    /**
     * The wrapper: delegates everything, then inserts the {@code "conditions"} array — the key
     * the RecipeManager patch reads, one {@code forge:mod_loaded} entry per gated domain.
     */
    private record ConvergedRow(net.minecraft.data.recipes.FinishedRecipe aRow, List<String> aDomains)
            implements net.minecraft.data.recipes.FinishedRecipe {

        @Override
        public void serializeRecipeData(JsonObject aJson) {
            aRow.serializeRecipeData(aJson);
            com.google.gson.JsonArray tConditions = new com.google.gson.JsonArray();
            for (String tDomain : aDomains) {
                JsonObject tCondition = new JsonObject();
                tCondition.addProperty("type", "forge:mod_loaded");
                tCondition.addProperty("modid", tDomain);
                tConditions.add(tCondition);
            }
            aJson.add("conditions", tConditions);
        }

        @Override
        public net.minecraft.resources.ResourceLocation getId() {
            return aRow.getId();
        }

        @Override
        public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
            return aRow.getType();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return aRow.serializeAdvancement();
        }

        @Override
        public net.minecraft.resources.ResourceLocation getAdvancementId() {
            return aRow.getAdvancementId();
        }
    }
    //?} else {
    /*// 21.1: the row carries its conditions through the native RecipeOutput.accept vararg —
    // {@link #conditioned} encodes the Recipe with the same vanilla dispatch codec the
    // RecipeManager loads by, analyzes it with the shared {@link #rowDomains}, and answers the
    // WithConditions wrapper the anonymous accept sites already build (CONDITIONAL_CODEC reads
    // it back; an unmet condition leaves the Optional empty = DEBUG skip, RecipeManager.java
    // :58-66). The five FORGE_GATED_ONLY_CANONICAL providers never reach here (no neo rows).
    public static net.neoforged.neoforge.common.conditions.WithConditions<net.minecraft.world.item.crafting.Recipe<?>> conditioned(
            net.minecraft.world.item.crafting.Recipe<?> aRecipe, net.minecraft.core.HolderLookup.Provider aRegistries,
            net.neoforged.neoforge.common.conditions.ICondition... aExisting) {
        com.google.gson.JsonObject tJson = (com.google.gson.JsonObject) net.minecraft.world.item.crafting.Recipe.CODEC
                .encodeStart(aRegistries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), aRecipe)
                .result().orElseThrow(() -> new IllegalStateException("recipe encode failed: " + aRecipe));
        java.util.List<net.neoforged.neoforge.common.conditions.ICondition> tConditions =
                new java.util.ArrayList<>(java.util.List.of(aExisting));
        for (String tDomain : rowDomains(tJson))
            tConditions.add(new net.neoforged.neoforge.common.conditions.ModLoadedCondition(tDomain));
        return new net.neoforged.neoforge.common.conditions.WithConditions<>(aRecipe, tConditions.toArray(new net.neoforged.neoforge.common.conditions.ICondition[0]));
    }
    *///?}
}
