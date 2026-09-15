package gregtech6.items.armor;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

//? if forge {
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.level.Level;
//?} else {
/*import net.minecraft.world.item.Item;
*///?}

/**
 * The 24-shape parameterized Hazmat piece — task p29-w5-t8-armor-24 spec ①/⑥: ONE
 * class, the vanilla {@link ArmorItem} (implements Equipable, so the right-click
 * equip/dispense faces are the vanilla ones). FLAT registration, non-NBT — the
 * research.p29-gap-refresh-tools-misc ruling over the upstream
 * {@code new ItemArmorBase(...)} per-piece rows (Loader_Tools.java:68-96).
 *
 * <p>Leg split: 1.20.1 — the ctor takes the {@link GT6ArmorMaterials} enum (which
 * implements the ArmorMaterial INTERFACE) and the WORN texture resolves through the
 * item-level {@code getArmorTexture(stack, entity, slot, type)} override — the forge
 * HumanoidArmorLayer patch consults it via ForgeHooksClient.java:264-268 and a non-null
 * return WINS over the vanilla name-derived default (the port of the upstream
 * ItemArmorBase.java:86/:136 mArmorTexture face); 1.21.1 — the ArmorMaterial RECORD's
 * {@code Layer(assetName)} already resolves
 * {@code gt6:textures/models/armor/<suit>_layer_<1|2>.png} natively
 * (vanilla-mc 1.21.1 ArmorMaterial.Layer.resolveTexture), and the durability rides the
 * item Properties ({@code Type.getDurability} has no upstream analogue here — the :68
 * literal 128 wins).
 *
 * <p>The forge {@code type} argument (the vanilla trim-overlay pass marker) is
 * deliberately IGNORED — ItemArmorBase.java:136 answered its base texture for every
 * call, so the trim overlay pass draws the base layer verbatim (upstream has no trim
 * face).
 *
 * <p>Tooltip face: the per-piece {@code .tooltip} line — the upstream
 * {@code aEnglishTooltip} row ("Full Set protects against ...", Loader_Tools.java:68-96)
 * with the dump's zh wordings riding the reference table.
 */
public class GT6ArmorItem extends ArmorItem {

	/** The armor-model texture directory root (the HumanoidArmorLayer composition base). */
	public static final String TEXTURE_DIR = "textures/models/armor/";

	/**
	 * The worn-model texture path composition — the STATIC seam (the ArmorSetTest pins
	 * the literal offline; item instances are not constructible in the bootstrapped-frozen
	 * test JVM). gt6-namespaced layered file, legs = layer 2 (the vanilla HumanoidArmorLayer
	 * inner-model split).
	 */
	public static String armorTexturePath(String aTextureName, boolean aInnerModel) {
		String tPath = TEXTURE_DIR + aTextureName + "_layer_" + (aInnerModel ? 2 : 1) + ".png";
		return new ResourceLocation("gt6", tPath).toString();
	}

	private final String textureName;

	public GT6ArmorItem(GT6ArmorMaterials aSuit, Type aType, Properties aProperties, String aTextureName) {
		//? if forge {
		super(aSuit, aType, aProperties); // the enum IS the ArmorMaterial on this leg
		//?} else {
		/*super(aSuit.material(), aType, aProperties.durability(GT6ArmorMaterials.DURABILITY));
		*///?}
		this.textureName = aTextureName;
	}

	/** The suit texture word ({@link GT6ArmorMaterials#textureName()}). */
	public String textureName() {
		return this.textureName;
	}

	//? if forge {
	@Override
	public String getArmorTexture(ItemStack aStack, Entity aEntity, EquipmentSlot aSlot, String aType) {
		// the ItemArmorBase.java:136 verbatim face — every call answers the base texture
		return armorTexturePath(this.textureName, aSlot == EquipmentSlot.LEGS);
	}

	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		aTooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip"));
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		aTooltip.add(Component.translatable(this.getDescriptionId() + ".tooltip"));
	}
	*///?}
}
