package gregtech6.block.panels;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

/**
 * One GT6 WOODEN COVER PANEL block (task material-mc-g3-plank-panels, the residual_sweep
 * G3 card — the {@code MultiTileEntityPanelWood} 300-row band, Loader_MultiTileEntities
 * .java:2057-2083: three 100-loops at metas 32500+i / 32352+i / 32252+i, one MTE row per
 * {@code PlankData.PLANKS[i]} texture index 0-299). The port row face walks the plank
 * authority ({@code GT6WoodDict.ROWS} — the planks-blockification single plank face) 1:1:
 * one panel per plank row the port world provides, 28 rows (the 23 upstream PlankData
 * slots the port can fill + the 5 port-native 1.20.1 vanilla-tree rows that ride the
 * GT6WoodDict identity rule — 1.7.10 predates those trees so they hold no slot).
 *
 * <p><b>Render face</b>: the upstream texture leg verbatim — a panel is a COPY of its
 * plank's texture ({@code PlankData.PLANK_ICONS[i] = new IconContainerCopied(ST.block(mPlank),
 * ST.meta_(mPlank))}, PlankEntry.java:120; MultiTileEntityPanelWood.getTexture reads the
 * icon back, :55-57). The port points the panel blockstate/item model at the EXISTING
 * plank cube model directly ({@code gt6:block/<plank id>} / {@code minecraft:block/<wood>_planks})
 * — zero new PNG or model, the reference IS the upstream semantic (the bookshelf
 * plank-ladder face, GT6BlockStates.addStaticStorages, established the same posture). No
 * tint listener: unlike the dyed G1 families there is no grayscale-times-dye leg — every
 * row binds its own tile.
 *
 * <p><b>Numbers</b>: the rows carry NO NBT_HARDNESS/NBT_RESISTANCE — the MTE defaults
 * hardness 1.0 / resistance 3.0 apply (TileEntityBase07Paintable.java:51, the G1 panel
 * reading), harvest class aWooden ({@code 0, 16, aWooden}, Loader row :2058) → WOOD sound +
 * the mineable/axe tag band, MapColor WOOD. <b>Negative ledger</b>: the rows carry NO
 * NBT_FLAMMABILITY (the MTE flammability face is opt-in per row — the Rope rows :2087-2092
 * declare it explicitly, the panel rows do not), so the port block is NOT flammable — the
 * plank family's BlockBasePlanksFlammable 20/5 face does NOT ride along.
 *
 * <p><b>Identity faces</b>: all 300 upstream rows share ONE name — the dump carries no
 * wood word ({@code gt.multitileentity.32252/32352/32500} = "Wooden Panel" / 木制覆盖板,
 * tmp/gregtech.lang:13133/:13233/:13381) — so ONE atomic key {@code gt6.panel.wood} (the
 * G1 family-key form); the wood identity is the TOOLTIP face verbatim ({@code addToolTips
 * adds the plank display name in cyan}, MultiTileEntityPanelWood.java:48-52, port:
 * the row's plank item name in AQUA — LH.Chat.CYAN = §b = the modern AQUA, the
 * GTGrassBlock mapping).
 *
 * <p><b>Declared deviations</b> (the G1 set): the placement face (upstream
 * {@code canPlace -> F}, MultiTileEntityPanel.java:38 — the panel is a cover-only item)
 * and the cover-mounting face (the port cover seat pools, GT6PanelBlock javadoc).
 */
public class GT6PlankPanelBlock extends Block {

	/** The single atomic family key — no wood word (the dump face, tmp/gregtech.lang:13381 band). */
	public static final String NAME_KEY = "gt6.panel.wood";

	/** The registry id ({@code wooden_panel_oak} form) — the GT6PlankPanels.path source. */
	public final String slug;
	/** The plank this panel copies (the PlankData.PLANKS[i] face) — lazy, the dict walk. */
	public final Supplier<Item> plank;

	public GT6PlankPanelBlock(String aSlug, Supplier<Item> aPlank) {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F, 3.0F)
				.sound(SoundType.WOOD));
		this.slug = aSlug;
		this.plank = aPlank;
	}

	/** The family-level display name — no wood word (the dump face verbatim). */
	@Override
	public MutableComponent getName() {
		return Component.translatable(NAME_KEY);
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable net.minecraft.world.level.BlockGetter aLevel,
			List<Component> aTooltip, TooltipFlag aFlag) {
		tooltipLines(aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext,
			List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the Block hover signature carries the Item.TooltipContext (the GTGrassBlock fork shape)
		tooltipLines(aTooltip);
	}
	*///?}

	/**
	 * The plank-identity tooltip (MultiTileEntityPanelWood.java:48-52 verbatim face — the
	 * plank display name in cyan/AQUA). The offline-unbound supplier face reads as NO line
	 * (the GT6WoodDict.plankOrNull silent-skip posture — the tooltip is runtime-only).
	 */
	private void tooltipLines(List<Component> aTooltip) {
		try {
			Item tPlank = this.plank.get();
			if (tPlank != null) {
				aTooltip.add(new ItemStack(tPlank).getHoverName().copy().withStyle(ChatFormatting.AQUA));
			}
		} catch (NullPointerException | IllegalStateException aE) {
			// the offline-unbound face — RegistryObject.get() when the registration never ran
		}
	}
}
