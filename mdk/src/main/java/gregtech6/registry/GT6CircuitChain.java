package gregtech6.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The circuit synthesis-chain registration home — task circuit-chain-items: the 53
 * intermediate items the upstream circuit chain is built from
 * ({@code MultiItemTechnological.java:546-770}), the "有物无方" face of the 7 circuit
 * carriers ({@link GT6Batteries#CIRCUIT_ROWS}): up to this card the carriers had tags and
 * crafting columns but nothing crafted them, because every Plate/Wire/Part/Board/Crystal
 * intermediate was missing. Card-owned self-contained {@code @EventBusSubscriber(MOD)}
 * DeferredRegister attached from the construct event (the GT6Electrodes shape verbatim;
 * a SISTER class to {@code gregtech6.item.GT6Circuits} — that class is the single selector
 * carrier + configuration face, a different domain with its own DR).
 *
 * <h2>The census (the card's acceptance ①)</h2>
 * <ul>
 * <li><b>Circuit Plate ×8</b> — Empty 30000 (:546), Copper 30002, Gold 30004, Platinum
 *     30006, Magic 30012, Enderium 30014, Signalum 30016, HSLA 30099 (:569).</li>
 * <li><b>Circuit Wiring ×6</b> — Copper 30001, Gold 30003, Platinum 30005, Magic 30011,
 *     Enderium 30013, Signalum 30015 (:555-567).</li>
 * <li><b>Circuit Part ×11</b> — Basic..Ultimate 30101-30106, Magic 30111, Enderium 30113,
 *     Signalum 30115, EnderPearl 30198, EnderEye 30199 (:583-595).</li>
 * <li><b>Circuit Board ×19</b> — Basic..Ultimate 30201-30206, Magic 30211, Enderium 30213,
 *     Signalum 30215, BC ×8 30280-30287, HSLA Circuit 30298, Power Module 30299
 *     (:651-672).</li>
 * <li><b>Gap Circuit ×3</b> (task circuit-chain-recipes) — Magic 30311, Enderium 30313,
 *     Signalum 30315 (:707-709); the BC ×8 (:711-718) stay CUT.</li>
 * <li><b>Crystal ×9</b> — Crystal Circuit ×4 30401-30404 (:754-757), Crystal Processor
 *     Socket 30500 + Processor ×4 30501-30504 (:759-763).</li>
 * </ul>
 * (the card面 "~48" was the census-card estimate; the per-family columns 8/6/11/19/3/9 sum
 * to 56 — the archaeology-verified number the registration test pins.)
 *
 * <p>Id flattening (the GT6FoodCans/GT6Electrodes ruling): upstream ids were meta ids on
 * the MultiItemTechnological meta item; the port flattens to one id per item, snake of the
 * IL field name ({@code Circuit_Wire_Copper} → {@code circuit_wire_copper},
 * {@code Circuit_Part_EnderPearl} → {@code circuit_part_ender_pearl}). The rows ride
 * {@link ChainRow} in upstream meta order with the registration-line name + subtitle
 * columns verbatim (both lang faces — the en addItem wording, the zh
 * {@code tmp/gregtech.lang} dump rows 10575-10730; the test pins both hops).
 *
 * <p><b>Declared deviations</b> (the GT6Electrodes posture):
 * <ul>
 * <li>The items are PLAIN items — the upstream constructor's Thaumcraft aspect stacks
 *     ({@code TC.stack(...)}, no Thaumcraft in the port) and the OreDictItemData
 *     composition faces ride the recipe rows (card 2 circuit-chain-recipes pours the
 *     Press/Bath/LaserEngraver band), not the item.</li>
 * <li>The creative face pools with the machines tab: upstream hides the BC boards behind
 *     {@code MD.BC_SILICON.mLoaded ? null : TD.Creative.HIDDEN} and the HSLA rows behind
 *     {@code MD.RoC.mLoaded ? ...} (:662-672) — the port has no BuildCraft/RotaryCraft,
 *     and hidden-but-craftable would blind the JEI/EMI face of the card-2 recipe rows that
 *     consume them (the electrode thirteen ruling verbatim).</li>
 * <li>Acquisition lines (UNKNOWN ①/② of the census, resolved): the six wirings come from
 *     the LaserEngraver red-lens band ({@code Loader_Recipes_Other.java:156-167},
 *     {@code foil ×4} + non-consumable lens; Copper additionally {@code CR.shaped} of
 *     9 wireFine Cu, MIT:571) and the four crystal circuits from the green-lens band
 *     ({@code Loader_Recipes_Other.java:147-155}, {@code plateGem} + lens). Those rows are
 *     card-2 pour faces — this card registers the items they consume/produce.</li>
 * </ul>
 *
 * <p>KJS surface (the card declaration): this card's output is the REGISTRATION face;
 * registration faces defer to the kjs binding card (the press-electrodes template line).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CircuitChain {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One registration row — the MultiItemTechnological projection of one addItem line
	 * ({@code :546-:770}): the flattened id, the upstream meta id (the parity column), and
	 * the display columns verbatim on both locales.
	 */
	public record ChainRow(String path, int metaId, String enName, String enTooltip, String zhName, String zhTooltip) {}

	/** The family sizes the census pins (plate incl. the Empty base, crystal = 4 circuits + 5 processors). */
	public static final int PLATES = 8, WIRES = 6, PARTS = 11, BOARDS = 19, CRYSTALS = 9;

	/**
	 * The card-2 gap circuits (task circuit-chain-recipes): the three Bath outputs whose
	 * material faces live in the port tree — Circuit (Magic) 30311 ({@code OP.circuit.dat
	 * (MT.Magic)} upstream), Circuit (Enderium) 30313, Circuit (Signalum) 30315. The other
	 * eight Bath outputs (Circuit_BC_* 30380-30387) are {@code MD.BC_SILICON}-gated
	 * upstream content and stay UNREGISTERED here — the port has no BuildCraft (the CUT
	 * disposition lives in the bath.json head declaration; the card-1 circuit_board_bc_*
	 * eight are the declared input-side orphans).
	 */
	public static final int GAP_CIRCUITS = 3;

	/** The 56 rows in upstream meta order (MultiItemTechnological.java:546-770 + the :707-:709 gap circuits). */
	public static final List<ChainRow> ROWS = List.of(
			// the empty base + the wiring/plate pairs (:546-:569)
			new ChainRow("circuit_plate_empty"       , 30000, "Circuit Plate"                , "Needs Circuit Wiring"                              , "电路底板", "需要蚀刻电路"),
			new ChainRow("circuit_wire_copper"       , 30001, "Circuit Wiring (Copper)"      , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (铜)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_copper"      , 30002, "Circuit Plate (Copper)"       , "Needs Circuit Parts"                               , "电路主板 (铜)", "需要电子元件"),
			new ChainRow("circuit_wire_gold"         , 30003, "Circuit Wiring (Gold)"        , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (金)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_gold"        , 30004, "Circuit Plate (Gold)"         , "Needs Circuit Parts"                               , "电路主板 (金)", "需要电子元件"),
			new ChainRow("circuit_wire_platinum"     , 30005, "Circuit Wiring (Platinum)"    , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (铂)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_platinum"    , 30006, "Circuit Plate (Platinum)"     , "Needs Circuit Parts"                               , "电路主板 (铂)", "需要电子元件"),
			new ChainRow("circuit_wire_magic"        , 30011, "Circuit Wiring (Magic)"       , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (魔法)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_magic"       , 30012, "Circuit Plate (Magic)"        , "Needs Circuit Parts"                               , "电路主板 (魔法)", "需要电子元件"),
			new ChainRow("circuit_wire_enderium"     , 30013, "Circuit Wiring (Enderium)"    , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (末影)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_enderium"    , 30014, "Circuit Plate (Enderium)"     , "Needs Circuit Parts"                               , "电路主板 (末影)", "需要电子元件"),
			new ChainRow("circuit_wire_signalum"     , 30015, "Circuit Wiring (Signalum)"    , "Needs to be placed on an empty Circuit Plate"      , "蚀刻电路 (信素)", "把它装配到电路底板上!"),
			new ChainRow("circuit_plate_signalum"    , 30016, "Circuit Plate (Signalum)"     , "Needs Circuit Parts"                               , "电路主板 (信素)", "需要电子元件"),
			new ChainRow("circuit_plate_hsla"        , 30099, "Circuit Plate (HSLA)"         , "Needs Circuit Parts"                               , "电路主板 (HSLA钢)", "需要电子元件"),
			// the eleven parts (:583-:595)
			new ChainRow("circuit_part_basic"        , 30101, "Circuit Part (Basic)"         , "Needs to be placed on a Copper Circuit Plate"      , "基础电子元件", "装配到铜电路主板上"),
			new ChainRow("circuit_part_good"         , 30102, "Circuit Part (Good)"          , "Needs to be placed on a Copper Circuit Plate"      , "不错的电子元件", "装配到铜电路主板上"),
			new ChainRow("circuit_part_advanced"     , 30103, "Circuit Part (Advanced)"      , "Needs to be placed on a Gold Circuit Plate"        , "进阶电子元件", "装配到金电路主板上"),
			new ChainRow("circuit_part_elite"        , 30104, "Circuit Part (Elite)"         , "Needs to be placed on a Gold Circuit Plate"        , "高级电子元件", "装配到金电路主板上"),
			new ChainRow("circuit_part_master"       , 30105, "Circuit Part (Master)"        , "Needs to be placed on a Platinum Circuit Plate"    , "高新科技电子元件", "装配到铂电路主板上"),
			new ChainRow("circuit_part_ultimate"     , 30106, "Circuit Part (Ultimate)"      , "Needs to be placed on a Platinum Circuit Plate"    , "究极电子元件", "装配到铂电路主板上"),
			new ChainRow("circuit_part_magic"        , 30111, "Circuit Part (Magic)"         , "Needs to be placed on a Magical Circuit Plate"     , "魔法电子元件", "装配到魔法电路主板上"),
			new ChainRow("circuit_part_enderium"     , 30113, "Circuit Part (Enderium)"      , "Needs to be placed on an Enderium Circuit Plate"   , "末影电子元件", "装配到末影电路主板上"),
			new ChainRow("circuit_part_signalum"     , 30115, "Circuit Part (Signalum)"      , "Needs to be placed on a Signalum Circuit Plate"    , "信素电子元件", "装配到信素电路主板上"),
			new ChainRow("circuit_part_ender_pearl"  , 30198, "Circuit Part (Enderpearl)"    , "Needs to be placed on a Circuit Plate"             , "末影珍珠电子元件", "装配到电路主板上"),
			new ChainRow("circuit_part_ender_eye"    , 30199, "Circuit Part (Ender Eye)"     , "Needs to be placed on a Circuit Plate"             , "末影之眼电子元件", "装配到电路主板上"),
			// the nineteen boards (:651-:672)
			new ChainRow("circuit_board_basic"       , 30201, "Circuit Board (Basic)"        , "Needs to be soldered properly"                     , "基础电路板", "需要焊接"),
			new ChainRow("circuit_board_good"        , 30202, "Circuit Board (Good)"         , "Needs to be soldered properly"                     , "不错的电路板", "需要焊接"),
			new ChainRow("circuit_board_advanced"    , 30203, "Circuit Board (Advanced)"     , "Needs to be soldered properly"                     , "进阶电路板", "需要焊接"),
			new ChainRow("circuit_board_elite"       , 30204, "Circuit Board (Elite)"        , "Needs to be soldered properly"                     , "高级电路板", "需要焊接"),
			new ChainRow("circuit_board_master"      , 30205, "Circuit Board (Master)"       , "Needs to be soldered properly"                     , "高新科技电路板", "需要焊接"),
			new ChainRow("circuit_board_ultimate"    , 30206, "Circuit Board (Ultimate)"     , "Needs to be soldered properly"                     , "究极电路板", "需要焊接"),
			new ChainRow("circuit_board_magic"       , 30211, "Circuit Board (Magic)"        , "Needs to be soldered properly"                     , "魔法电路板", "需要焊接"),
			new ChainRow("circuit_board_enderium"    , 30213, "Circuit Board (Enderium)"     , "Needs to be soldered properly"                     , "末影电路板", "需要焊接"),
			new ChainRow("circuit_board_signalum"    , 30215, "Circuit Board (Signalum)"     , "Needs to be soldered properly"                     , "信素电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_redstone" , 30280, "Circuit Board (BC Redstone)"  , "Needs to be soldered properly"                     , "BC红石电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_iron"     , 30281, "Circuit Board (BC Iron)"      , "Needs to be soldered properly"                     , "BC铁制电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_gold"     , 30282, "Circuit Board (BC Gold)"      , "Needs to be soldered properly"                     , "BC金质电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_diamond"  , 30283, "Circuit Board (BC Diamond)"   , "Needs to be soldered properly"                     , "BC钻石电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_ender"    , 30284, "Circuit Board (BC Ender)"     , "Needs to be soldered properly"                     , "BC末影电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_quartz"   , 30285, "Circuit Board (BC Quartz)"    , "Needs to be soldered properly"                     , "BC石英电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_comparator", 30286, "Circuit Board (BC Comparator)", "Needs to be soldered properly"                    , "BC比较器电路板", "需要焊接"),
			new ChainRow("circuit_board_bc_emerald"  , 30287, "Circuit Board (BC Emerald)"   , "Needs to be soldered properly"                     , "BC绿宝石电路板", "需要焊接"),
			new ChainRow("circuit_board_hsla_circuit", 30298, "Circuit Board (HSLA Circuit)" , "Needs to be soldered properly"                     , "HSLA电路板", "需要焊接"),
			new ChainRow("circuit_board_power_module", 30299, "Circuit Board (Power Module)" , "Needs to be soldered properly"                     , "电池组电路板", "需要焊接"),
			// the three gap circuits (task circuit-chain-recipes — the :707-:709 Bath outputs;
			// the 8 BC circuits :711-:718 stay CUT, see GAP_CIRCUITS)
			new ChainRow("circuit_magic"             , 30311, "Circuit (Magic)"              , "Computes simple Data magically"                    , "魔法电子电路", "像魔法师那样处理事务!"),
			new ChainRow("circuit_enderium"          , 30313, "Circuit (Enderium)"           , "Computes simple Data somewhere else"               , "末影电子电路", "在另一个维度处理您的数据"),
			new ChainRow("circuit_signalum"          , 30315, "Circuit (Signalum)"           , "Computes simple Logic"                             , "信素电子电路", "逻辑电路板"),
			// the four crystal circuits + the five crystal processors (:754-:763)
			new ChainRow("circuit_crystal_diamond"   , 30401, "Crystal Circuit (Diamond)"    , "Logic Diamond"                                     , "钻石晶体电路", "钻石---逻辑"),
			new ChainRow("circuit_crystal_ruby"      , 30402, "Crystal Circuit (Ruby)"       , "Control Ruby"                                      , "红宝石晶体电路", "红宝石---控制"),
			new ChainRow("circuit_crystal_emerald"   , 30403, "Crystal Circuit (Emerald)"    , "Storage Emerald"                                   , "绿宝石晶体电路", "绿宝石---储存"),
			new ChainRow("circuit_crystal_sapphire"  , 30404, "Crystal Circuit (Sapphire)"   , "Conversion Sapphire"                               , "蓝宝石晶体电路", "蓝宝石---转换"),
			new ChainRow("processor_crystal_empty"   , 30500, "Crystal Processor Socket"     , "Base for Crystal Circuits"                         , "晶体处理器电路板", "晶体电子电路的基础"),
			new ChainRow("processor_crystal_diamond" , 30501, "Crystal Processor (Diamond)"  , "Logic Processor Circuit"                           , "钻石晶体处理器", "逻辑处理器电路"),
			new ChainRow("processor_crystal_ruby"    , 30502, "Crystal Processor (Ruby)"     , "Control Processor Circuit"                         , "红宝石晶体处理器", "控制器电路"),
			new ChainRow("processor_crystal_emerald" , 30503, "Crystal Processor (Emerald)"  , "Storage Processor Circuit"                         , "绿宝石晶体处理器", "数据储存电路"),
			new ChainRow("processor_crystal_sapphire", 30504, "Crystal Processor (Sapphire)" , "Conversion Processor Circuit"                      , "蓝宝石晶体处理器", "转换电路"));

	/** The 53 items keyed by path (the datagen/test lookup seam, the GT6Batteries form). */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();
	static {
		for (ChainRow tRow : ROWS) {
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new Item(new Item.Properties())));
		}
	}

	private GT6CircuitChain() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Electrodes.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/**
	 * The MACHINES_TAB join (the GT6Electrodes.onBuildTabContents verbatim form): the
	 * declared deviation of the upstream BC/RoC-gated HIDDEN faces (the class javadoc).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (ChainRow tRow : ROWS) {
				aEvent.accept(new ItemStack(ITEMS_BY_PATH.get(tRow.path()).get()));
			}
		}
	}
}
