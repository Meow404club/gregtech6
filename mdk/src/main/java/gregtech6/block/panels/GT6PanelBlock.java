package gregtech6.block.panels;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 COVER PANEL block — the dyed Cover Panel family (upstream
 * {@code MultiTileEntityPanelConcrete/CFoam/Asphalt}, Loader_MultiTileEntities.java:2043-2056,
 * the 16-colour loop metas 32452-32499). The per-pair granularity is the P8 ADR ④ /
 * GT6ConcreteBlock ruling: the panel crafting economy emits per-colour OUTPUT stacks
 * ({@code CR.shaped 6x panel <- 1x source block colour i + iron screws}, :2044-2055),
 * so per-variant ITEM identities are the economy.
 *
 * <p><b>The upstream form is the declared deviation set</b> — upstream a Panel is NOT a
 * placeable decorative block ({@code MultiTileEntityPanel.canPlace -> F},
 * MultiTileEntityPanel.java:38): the item's whole function is mounting as a COVER
 * ({@code CoverRegistry.put(tPanel, new CoverTextureSimple(BlockTextureDefault.get(CONCRETE,
 * DYES[i]), ...))}, Loader_MultiTileEntities.java:2046/:2050/:2054). The port registers the
 * block+item dual form (the port registration convention — every registration home is a
 * block family), so the placement face is a declared deviation, and the dyed cover-mounting
 * face stays POOLED: the port cover plate seat is a flat sprite
 * ({@code CoverTextureSimple.mSprite}, no tint index), so the 48 dyed covers have no
 * render seat until the cover renderer grows a tinted-sprite face (residual_sweep G-pool;
 * the un-dyed {@code cover_asphalt} precedent stays the only asphalt cover).
 *
 * <p><b>Render face</b>: the upstream tint leg verbatim — ONE grayscale texture per family
 * (Textures.BlockIcons CONCRETE/CFOAM_HARDENED/ASPHALT + {@code DYES[i]},
 * MultiTileEntityPanelColored.java:33-36) coloured by the dye; the port keeps exactly that:
 * shared {@code gt6:block/concrete|cfoam_hardened|asphalt} PNGs + tintindex-0 models + the
 * {@code GT6PanelTintListener} Block/ItemColor over the FIXED dye index (the
 * GT6ConcreteTintListener shape, with the Item half — a BlockColor does NOT colour its
 * BlockItem, the ItemColors lesson). Texture seats: concrete.png / asphalt.png are the
 * upstream iconsets byte-verbatim (assets/README.md ledger); cfoam_hardened.png rides the
 * foam family's established port art (upstream iconsets/CFOAM_HARDENED.png sha256
 * 1d646405f64407271d7ff9db0237648e4ad57bd6d09e26d85eed06049f7ce2cf differs — the
 * c-foam-block-family card's declared seat, reused so the panel matches the port foam).
 *
 * <p><b>Numbers</b>: the panel rows carry NO NBT_HARDNESS/NBT_RESISTANCE — the MTE defaults
 * hardness 1.0 / resistance 3.0 apply (TileEntityBase07Paintable.java:51
 * {@code mHardness = 1.0F, mResistance = 3.0F}); harvest class aStone → pickaxe + STONE
 * sound (the Loader row {@code 0, 16, aStone}). MapColor STONE (no upstream map-color face).
 *
 * <p>Display name: the upstream dump carries NO colour word — all 16 metas of a family share
 * one name ({@code gt.multitileentity.32452-32467} = "Concrete Panel" / 混凝土覆盖板,
 * tmp/gregtech.lang:13333-13348; 32468-32483 = 建筑泡沫覆盖板; 32484-32499 = 沥青覆盖板),
 * so one atomic key per family (no dye compose).
 */
public class GT6PanelBlock extends Block {

	/** The three panel families in upstream registration order (Loader_MultiTileEntities.java:2045/:2049/:2053). */
	public enum Family {
		/** Concrete Panel — metas 32452+i, source block gt6 concrete, icon CONCRETE. */
		CONCRETE("concrete_panel", "gt6.panel.concrete", "block/concrete"),
		/** C-Foam Panel — metas 32468+i, source block gt6:cfoam, icon CFOAM_HARDENED. */
		CFOAM("cfoam_panel", "gt6.panel.cfoam", "block/cfoam_hardened"),
		/** Asphalt Panel — metas 32484+i, source block (unported) asphalt, icon ASPHALT. */
		ASPHALT("asphalt_panel", "gt6.panel.asphalt", "block/asphalt");

		/** The registry snake root ({@code concrete_panel_black} form). */
		public final String snake;
		/** The atomic display-name key (no colour word — the dump face verbatim). */
		public final String nameKey;
		/** The shared grayscale texture the family's tinted cube rides. */
		public final String texture;

		Family(String aSnake, String aNameKey, String aTexture) {
			this.snake = aSnake;
			this.nameKey = aNameKey;
			this.texture = aTexture;
		}
	}

	/** The registry snake id ({@code concrete_panel_black} form) — the GT6Panels.path source. */
	public final String snake;
	/** The panel family. */
	public final Family family;
	/** The FIXED dye index ({@code 0..15}, the CS.DYE_INDEX order 0=Black..15=White). */
	public final int dyeIndex;

	public GT6PanelBlock(String aSnake, Family aFamily, int aDyeIndex) {
		super(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.0F, 3.0F).sound(SoundType.STONE));
		this.snake = aSnake;
		this.family = aFamily;
		this.dyeIndex = aDyeIndex;
	}

	/** The family-level display name — no colour word (the dump face verbatim). */
	@Override
	public MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable(this.family.nameKey);
	}
}
