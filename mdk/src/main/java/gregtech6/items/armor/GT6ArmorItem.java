package gregtech6.items.armor;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The 24-shape parameterized Hazmat piece — task p29-w5-t8-armor-24 spec ①/⑥: ONE
 * class, the vanilla {@link ArmorItem} ({@code ArmorItem(ArmorMaterial, Type,
 * Properties)} — vanilla ArmorItem.java:69; implements Equipable at :29, so the
 * right-click equip/dispense faces are the vanilla ones). FLAT registration, non-NBT —
 * the research.p29-gap-refresh-tools-misc ruling over the upstream
 * {@code new ItemArmorBase(...)} per-piece rows (Loader_Tools.java:68-96).
 *
 * <p>Texture face: the upstream ItemArmorBase.java:86/:136 {@code mArmorTexture}
 * override, ported onto the Forge item extension
 * {@code Item.getArmorTexture(stack, entity, slot, type)} — the forge-1.20.1
 * HumanoidArmorLayer patch consults it through
 * {@code ForgeHooksClient.getArmorTexture} (ForgeHooksClient.java:264-268, the patch
 * line {@code s1 = ...getArmorTexture(entity, stack, s1, slot, type)}) and a non-null
 * return WINS over the vanilla name-derived default. The path is the layered model
 * convention ({@code textures/models/armor/<suit>_layer_<1|2>.png} — the layer number
 * is the vanilla HumanoidArmorLayer.java:121 composition, legs = inner = 2) resolved in
 * the gt6 namespace; the placeholder PNGs ride the bake script (the P20
 * placeholder-PNG judging precedent, the real art is a texture-sprint pool item).
 *
 * <p>The {@code type} argument (the vanilla trim-overlay pass marker) is deliberately
 * IGNORED — ItemArmorBase.java:136 answered its base texture for every call, so the
 * trim overlay pass draws the base layer verbatim (upstream has no trim face).
 *
 * <p>Tooltip face: the per-piece {@code .tooltip} line — the upstream
 * {@code aEnglishTooltip} row ("Full Set protects against ...", Loader_Tools.java:68-96)
 * with the dump's zh wordings riding the reference table.
 */
public class GT6ArmorItem extends ArmorItem {

	/** The armor-model texture directory root (the HumanoidArmorLayer.java:121 base). */
	public static final String TEXTURE_DIR = "textures/models/armor/";

	private final String textureName;

	public GT6ArmorItem(ArmorMaterial aMaterial, Type aType, Properties aProperties, String aTextureName) {
		super(aMaterial, aType, aProperties);
		this.textureName = aTextureName;
	}

	/** The suit texture word ({@link GT6ArmorMaterials#textureName()}). */
	public String textureName() {
		return this.textureName;
	}

	/**
	 * The worn-model texture path — gt6-namespaced layered file, legs = layer 2 (the
	 * vanilla HumanoidArmorLayer inner-model split).
	 */
	public String armorTexturePath(boolean aInnerModel) {
		String tPath = TEXTURE_DIR + this.textureName + "_layer_" + (aInnerModel ? 2 : 1) + ".png";
		return new ResourceLocation("gt6", tPath).toString();
	}

	@Override
	public String getArmorTexture(ItemStack aStack, Entity aEntity, EquipmentSlot aSlot, String aType) {
		// the ItemArmorBase.java:136 verbatim face — every call answers the base texture
		return this.armorTexturePath(aSlot == EquipmentSlot.LEGS);
	}

	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		aTooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip"));
	}
}
