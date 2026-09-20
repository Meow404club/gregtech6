package gregtech6.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.client.render.GTItemPaintTint;
import gregtech6.client.render.GTMachinePaintTint;
import gregtech6.client.wire.GTWireTint;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.items.tools.GT6ToolLadder;
import gregtech6.items.tools.GTButcheryKnifeItem;
import gregtech6.items.tools.GTKnifeItem;
import gregtech6.items.tools.GTCrowbarItem;
import gregtech6.items.tools.GTSwordItem;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GT6Tools;
import gregtech6.registry.GTWires;

/**
 * Client-only event wiring. Kept in a {@code @OnlyIn(Dist.CLIENT)} class so the dedicated server
 * never loads it; the only call site is dist-guarded in the {@code @Mod} constructor. The mod bus
 * fires RegisterColorHandlersEvent only on the logical client (RegisterColorHandlersEvent.java:33-45).
 */
@OnlyIn(Dist.CLIENT)
public final class GTClientHandlers {

    private GTClientHandlers() {
    }

    /**
     * Registers the client-only mod-bus listeners (called under a Dist.CLIENT guard). Every
     * listener is registered against a concrete {@code RegisterColorHandlersEvent.Block/.Item}
     * subtype: NeoForge 21.1 rejects abstract-typed listeners at registration time
     * (bus-8.0.5 EventBus "Cannot register listeners for abstract ..." IllegalArgumentException,
     * known_bugs gtclienthandlers-2111-abstract-event), so the p9 wire listener is split in two
     * instead of one instanceof-dispatched method over the abstract parent.
     */
    public static void init(IEventBus modBus) {
        modBus.addListener(GTClientHandlers::onRegisterItemColors);
        modBus.addListener(GTClientHandlers::onRegisterBlockColors); // task p8-prefixblock-render ③: world-side tint
        modBus.addListener(GTClientHandlers::onRegisterWireBlockColors); // task p16-clienthandlers-2111: wire tints, world half (p9-wire-family-w2 semantics)
        modBus.addListener(GTClientHandlers::onRegisterWireItemColors); // task p16-clienthandlers-2111: wire tints, inventory half
        modBus.addListener(GTClientHandlers::onRegisterMachinePaintItemColors); // task p22-painted-item-domain: machine paint tint, inventory half
        modBus.addListener(GTClientHandlers::onRegisterBarrelPaintBlockColors); // task p23-barrel-paint-render: barrel paint tint, world half
        modBus.addListener(GTClientHandlers::onRegisterBarrelPaintItemColors); // task p23-barrel-paint-render: barrel paint tint, inventory half
        modBus.addListener(GTClientHandlers::onRegisterToolIdentityItemColors); // task p31-identity-seam: the crowbar material identity tint, inventory half
    }

    /** Material tint for every registered material prefix item (GTCEu TagPrefixItem.java:55-57 isomorph). */
    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor tColor = MaterialPrefixItem.tintColor();
        event.getItemColors().register(tColor, GTMaterialItems.itemArray()); // ItemColors.register(ItemColor, ItemLike...)
        // task p8-prefixblock-render: the 3773 block items tint through the same colour seam —
        // per the Forge docs a BlockColor does NOT colour its BlockItem (PrefixBlockItem.java:103
        // tints the item side upstream), so the GTMaterialPrefixBlockItem ItemColor is registered
        // explicitly over the block-item array.
        event.getItemColors().register(GTMaterialPrefixBlockItem.tintColor(),
                GTMaterialBlocks.items().values().stream().map(RegistryObject::get).toArray(Item[]::new));
    }

    /** Task p8-prefixblock-render ③: the material tint for every registered material prefix block (GTMaterialPrefixBlock.blockColor). */
    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register(GTMaterialPrefixBlock.blockColor(), GTMaterialBlocks.blockArray());
    }

    /**
     * Task p9-wire-family-w2: the wire family tints — the world half over every wire block
     * (the p7 legacy pair + the 620 family, {@code GTWires.wireBlockArray()}).
     * {@link GTWireTint} = fRGBaSolid on tint index 0, the fixed insulation gray on index 1.
     *
     * <p>Task p16-clienthandlers-2111: split from the former abstract-typed
     * {@code onRegisterWireColors} (instanceof dispatch over the parent) into a concrete
     * {@code .Block} listener — both dispatch arms preserved verbatim as separate methods.</p>
     */
    private static void onRegisterWireBlockColors(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register(GTWireTint.blockColor(), GTWires.wireBlockArray());
    }

    /**
     * Task p9-wire-family-w2 (continued): the inventory half over the same blocks' items
     * (a BlockColor does not colour its BlockItem, the prefix-card comment above).
     *
     * <p>Task p16-clienthandlers-2111: the other dispatch arm of the split, see
     * {@link #onRegisterWireBlockColors}.</p>
     */
    private static void onRegisterWireItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> tWireItems = new ArrayList<>();
        tWireItems.add(GTWires.WIRE_ELECTRIC_1X_ITEM.get());
        tWireItems.add(GTWires.WIRE_ELECTRIC_2X_ITEM.get());
        for (RegistryObject<Item> tFamilyItem : GTWires.FAMILY_ITEMS) tWireItems.add(tFamilyItem.get());
        event.getItemColors().register(GTWireTint.itemColor(), tWireItems.toArray(Item[]::new));
    }

    /**
     * Task p21-paintable-tint-render: the machine paint tint, the world half over the pinned
     * 21 machine-domain blocks ({@code GTMachines.paintableBlockArray()}).
     *
     * <p>REPLACED by task p32-render-embeddium-tint: the world tint now rides
     * {@code GTMachineTintModel} — the colour is BAKED into the quads at
     * {@code getQuads} time (the same {@code GTMachinePaintTint.tintARGB} source), because
     * the runtime {@code BlockColor} route rendered achromatic in the live client with
     * both chunk builders (the known_bugs embeddium_tint_no_shader report). This
     * registration is therefore deliberately gone; the inventory half below stays on the
     * {@code ItemColor} route (a different, field-proven consumer).
     */

    /**
     * Task p22-painted-item-domain: the machine paint tint, the INVENTORY half over the
     * 21 machine-domain blocks' items ({@code GTMachines.paintableBlockArray()} BlockItems).
     * Explicit registration is mandatory — a BlockColor does NOT colour its BlockItem AND
     * vanilla {@code ItemColors.createDefault} has no BlockItem delegation either
     * (ItemColors.java:25-93; the grass/leaves rows :72-88 are hand-written forwards).
     * {@link GTItemPaintTint} resolves tint index 0 from the stack NBT the loot copy_nbt
     * function wrote ({@code gt.color}/{@code gt.painted} under {@code BlockEntityTag});
     * unpainted stacks return the {@code -1} no-tint sentinel.
     */
    private static void onRegisterMachinePaintItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> tPaintItems = new ArrayList<>();
        for (Block tBlock : GTMachines.paintableBlockArray()) tPaintItems.add(tBlock.asItem());
        event.getItemColors().register(GTItemPaintTint.itemColor(), tPaintItems.toArray(Item[]::new));
    }

    /**
     * Task p23-barrel-paint-render: the BARREL paint tint, the world half over the pinned
     * 16 barrel-domain blocks ({@code GTBarrels.paintableBlockArray()}). The SAME
     * {@link GTMachinePaintTint#blockColor()} lambda as the machine registration — the
     * lambda is block-type-free (the PAINT model-data lookup is the gate) and every barrel
     * BE supplies it through the 03 base, so the two domains share one decision site and
     * the tint classes stay untouched (this card extends the registration face only).
     * Unpainted = the {@code -1} white-multiply identity, zero visual change.
     */
    private static void onRegisterBarrelPaintBlockColors(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register(GTMachinePaintTint.blockColor(), GTBarrels.paintableBlockArray());
    }

    /**
     * Task p23-barrel-paint-render: the BARREL paint tint, the INVENTORY half over the 16
     * barrel blocks' BlockItems ({@code GTBarrels.paintableBlockArray()}) — the explicit
     * registration is mandatory for the same no-delegation reason as the machine half
     * above (a BlockColor does not colour its BlockItem; ItemColors.java:25-93). The SAME
     * {@link GTItemPaintTint#itemColor()} lambda; the stack keys it reads
     * ({@code gt.color}/{@code gt.painted}) are written by the barrel item-NBT carrier
     * (task p23-barrel-paint-item-seam, merge order A→B — the barrel stacks stay the
     * {@code -1} sentinel until that carrier lands).
     */
    private static void onRegisterBarrelPaintItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> tBarrelPaintItems = new ArrayList<>();
        for (Block tBlock : GTBarrels.paintableBlockArray()) tBarrelPaintItems.add(tBlock.asItem());
        event.getItemColors().register(GTItemPaintTint.itemColor(), tBarrelPaintItems.toArray(Item[]::new));
    }

    /**
     * Task p31-identity-seam: the crowbar MATERIAL IDENTITY tint, the inventory half —
     * the former GT6Tools pool cut ③. {@link GTCrowbarItem#tintARGB} reads the
     * {@code GT.ToolStats} identity through the GT6ItemData seam (client-visible on
     * both legs: the 1.20.1 root NBT rides the stack sync, the 1.21.1 payload is the
     * network-synchronized component) and colours the head layer with the material
     * {@code mRGBaSolid}, the upstream steel fallback verbatim (GT_Tool_Crowbar
     * .getRGBa :148). The method reference rides the shared static seam, so the
     * offline tests pin the exact lambda the client runs.
     */
    private static void onRegisterToolIdentityItemColors(RegisterColorHandlersEvent.Item event) {
        event.getItemColors().register(GTCrowbarItem::tintARGB, GT6Tools.CROWBAR.get());
        // task p31-dig-ladder: the dig family shares ONE tint face — the GT6ToolLadder
        // static (tint index 0 = the head layer, the material mRGBaSolid with the steel
        // fallback); the universal spade rides it too (it stays single-steel, so the
        // fallback arm is its whole colour face).
        event.getItemColors().register(GT6ToolLadder::tintARGB, GT6Tools.PICKAXE.get(), GT6Tools.PICKAXE_GEM.get(),
                GT6Tools.PICKAXE_CONSTRUCTION.get(), GT6Tools.SHOVEL.get(), GT6Tools.SPADE.get(),
                GT6Tools.UNIVERSAL_SPADE.get(), GT6Tools.HOE.get(), GT6Tools.AXE.get());
        // task p31-blade-ladder: the three blade forms carry their OWN tintARGB statics —
        // the family dispatch over GT6ToolLadder.bladeTintARGB (the sword handle pass and
        // the knife secondary face; the head-pass-VOID knife renders the secondary).
        event.getItemColors().register(GTSwordItem::tintARGB, GT6Tools.SWORD.get());
        event.getItemColors().register(GTKnifeItem::tintARGB, GT6Tools.KNIFE.get());
        event.getItemColors().register(GTButcheryKnifeItem::tintARGB, GT6Tools.BUTCHERY_KNIFE.get());
        // task p31-machine-ladder: the machine family rides the ONE GT6ToolLadder head-pass
        // face (tint index 0, the material mRGBaSolid with the steel fallback). The
        // screwdriver and the hammer stay UN-tinted — their models are the composed
        // single-sprite borrows (the census-erratum ruling, GT6ItemModels), a head-pass
        // tint would recolour the whole composed icon.
        event.getItemColors().register(GT6ToolLadder::tintARGB, GT6Tools.WRENCH.get(), GT6Tools.MONKEY_WRENCH.get(),
                GT6Tools.CUTTER.get(), GT6Tools.CHISEL.get(), GT6Tools.SAW.get(), GT6Tools.SOFT_HAMMER.get(),
                GT6Tools.MAGNIFYING_GLASS.get(), GT6Tools.PINCERS.get());
    }

    /** Translation key existence check (Language.getInstance Language.java:83, has :97). */
    public static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
