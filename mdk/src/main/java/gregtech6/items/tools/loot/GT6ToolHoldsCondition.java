package gregtech6.items.tools.loot;

import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * The "main hand holds THIS tool" loot condition — task p29-w5-t1-dig-six shared
 * infrastructure ①a (the wave ruling decisions.p30-w5-split-rulings: the drop-conversion
 * standard seam rides the Forge {@code IGlobalLootModifier} chain, each per-tool JSON
 * gated by THIS condition so the conversion fires only when the breaking tool is the
 * named item). Upstream face: {@code convertBlockDrops} received the breaking stack
 * directly (GT_Tool_Spade.java:81) — the 1.20.1 loot context carries it as
 * {@link LootContextParams#TOOL} (the Forge {@code CanToolPerformAction} read shape,
 * CanToolPerformAction.java:48-51), and this condition pins it by ITEM IDENTITY.
 *
 * <p>JSON (identical shape both legs — the 1.20.1 Gson serializer writes the
 * {@code condition} dispatch key, the 1.21.1 codec dispatches on the SAME
 * {@code "condition"} key, LootItemCondition.java:14-15 of the 21.1 sources):
 * {@code {"condition": "gt6:holds_tool", "tool": "gt6:spade"}}.
 *
 * <p>Consumers: t2/t4/t5/t6 gate their own per-tool modifier JSONs with this condition
 * (the consumer contract lives on {@link GT6ToolLootModifiers}); the type registers
 * through the {@code gt6:holds_tool} key on the loot-condition-type registry
 * (the registration wiring sits in {@link GT6ToolLootModifiers}).
 */
public class GT6ToolHoldsCondition implements LootItemCondition {

	//? if forge {
	/**
	 * The 1.20.1 Gson serializer face (the CanToolPerformAction.Serializer verbatim shape:
	 * the id string in, the id string out — the {@code condition} key itself is written by
	 * the loot Gson adapter, not here).
	 */
	public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<GT6ToolHoldsCondition> {

		@Override
		public void serialize(JsonObject aJson, GT6ToolHoldsCondition aCondition, @SuppressWarnings("unused") JsonSerializationContext aContext) {
			aJson.addProperty("tool", String.valueOf(BuiltInRegistries.ITEM.getKey(aCondition.mTool)));
		}

		@Override
		public GT6ToolHoldsCondition deserialize(JsonObject aJson, @SuppressWarnings("unused") JsonDeserializationContext aContext) {
			return new GT6ToolHoldsCondition(BuiltInRegistries.ITEM.get(new ResourceLocation(aJson.get("tool").getAsString())));
		}
	}
	//?} else {
	/*// 21.1: the codec face — the SAME JSON keys the forge leg writes (the field is
	//"tool", the dispatch key "condition"), so the shared canonical tree decodes on both.
	//Declared BEFORE {@link #TYPE}: the static initializer would otherwise be an illegal
	//forward reference on this leg (the forge leg never resolves it — commented).
	public static final MapCodec<GT6ToolHoldsCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(aInst -> aInst.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("tool").forGetter(aCondition -> aCondition.mTool))
			.apply(aInst, GT6ToolHoldsCondition::new));
	*///?}

	/** The registry key — the JSON dispatch id {@code gt6:holds_tool}. */
	//? if forge {
	public static final LootItemConditionType TYPE = new LootItemConditionType(new Serializer());
	//?} else {
	/*public static final LootItemConditionType TYPE = new LootItemConditionType(MAP_CODEC);
	//21.1: the loot-condition carrier went codec (javap 21.1.249 LootItemConditionType —
	//the 1.20.1 Serializer ctor is gone); the dispatch key stays "condition".
	*///?}

	/** The exact tool item the breaking stack must be (identity, not class — per-tool JSON). */
	final Item mTool;

	public GT6ToolHoldsCondition(Item aTool) {
		mTool = aTool;
	}

	/** The condition builder face (the CanToolPerformAction.canToolPerformAction shape). */
	public static LootItemCondition.Builder holdsTool(Item aTool) {
		return () -> new GT6ToolHoldsCondition(aTool);
	}

	@Override
	public LootItemConditionType getType() {
		return TYPE;
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return Set.of(LootContextParams.TOOL);
	}

	@Override
	public boolean test(LootContext aContext) {
		ItemStack tTool = aContext.getParamOrNull(LootContextParams.TOOL);
		return tTool != null && tTool.is(mTool);
	}
}
