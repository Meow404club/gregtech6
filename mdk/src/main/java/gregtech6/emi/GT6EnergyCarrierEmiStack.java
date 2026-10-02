package gregtech6.emi;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.stack.EmiStack;

import gregapi.code.TagData;
import gregtech6.jei.GT6RecipeMapViewerMeta;

/**
 * The energy-carrier pseudo stack of the EMI leg (task viewer-energy-jump-gear, the
 * r6-30 phase-2 design): the nine accepted-energy carriers ride ONE minimal
 * {@link EmiStack} subclass keyed on the {@link TagData} singleton — never in the
 * ingredient list (nothing registers them into EMI's stack list), they exist only as
 * the {@code EmiInfoRecipe} pages ({@link GT6EmiPlugin}) and the
 * {@code EmiApi.displayRecipes} target the gear widget jumps to. On-screen visibility is
 * the r6-30-flagged POC face, field_test's to verify.
 *
 * <p>The override set is exactly the {@code EmiStack} abstract face plus the render
 * hook, shaped on the internal EmptyEmiStack precedent (emi 1.1.24 xplat source):
 * equality/hash ride the base {@code getKey()} contract — canonical TagData singletons,
 * so the registration instance and a click-time lookup unify in EMI's recipe index.
 */
public final class GT6EnergyCarrierEmiStack extends EmiStack {

	/** The per-carrier memo — one shared instance per TagData for registration AND clicks. */
	private static final Map<TagData, GT6EnergyCarrierEmiStack> S_STACKS = new ConcurrentHashMap<>();

	/** The shared instance for one carrier (TagData singletons make this total and stable). */
	public static GT6EnergyCarrierEmiStack of(TagData aCarrier) {
		return S_STACKS.computeIfAbsent(aCarrier, GT6EnergyCarrierEmiStack::new);
	}

	private final TagData mCarrier;

	private GT6EnergyCarrierEmiStack(TagData aCarrier) {
		mCarrier = aCarrier;
	}

	/** The carried face — the jump target the widget and the info recipe share. */
	public TagData carrier() {
		return mCarrier;
	}

	@Override
	public EmiStack copy() {
		return this; // immutable — the copy IS the identity
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	// the NBT/component face is the one leg fork (javap on both pinned emi 1.1.24
	// artifacts): 1.20.1's EmiStack abstracts getNbt(), 1.21.1 abstracts
	// getComponentChanges() instead — the pseudo carrier carries neither.
	//? if forge {
	@Override
	public net.minecraft.nbt.CompoundTag getNbt() {
		return null;
	}
	//?} else {
	/*@Override
	public net.minecraft.core.component.DataComponentPatch getComponentChanges() {
		return net.minecraft.core.component.DataComponentPatch.EMPTY;
	}
	*///?}

	@Override
	public Object getKey() {
		return mCarrier;
	}

	@Override
	public ResourceLocation getId() {
		return ResourceLocation.fromNamespaceAndPath("gt6", idPath(mCarrier));
	}

	/** The {@code energy/<code>} id tail, short-code derived like the JEI helper's uid. */
	private static String idPath(TagData aCarrier) {
		return "energy/" + GT6RecipeMapViewerMeta.energyTypeShortCode(aCarrier).toLowerCase(Locale.ROOT);
	}

	/** The colored short code — the shared {@link GT6RecipeMapViewerMeta#energyUnit} face. */
	@Override
	public Component getName() {
		return GT6RecipeMapViewerMeta.energyUnit(mCarrier);
	}

	@Override
	public List<Component> getTooltipText() {
		return List.of(getName());
	}

	/** Draws the colored short code at the requested position (the 16x16 ingredient space). */
	@Override
	public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
		draw.drawString(Minecraft.getInstance().font, GT6RecipeMapViewerMeta.energyUnit(mCarrier), x + 1, y + 4, 0xFFFFFFFF, false);
	}
}
