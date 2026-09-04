package gregtech6.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.item.GTBarrelBlockItem;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.tank.GTBarrelItemFluidHandler;
import gregtech6.tileentity.tank.TileEntityBase08Barrel;

/**
 * The item-side tank face (task p12-fluid-item-carrier acceptance a): the
 * fill→NBT→load→drain round trip keeping amount AND identity, the count!=1 guard
 * (FluidHandlerItemStack.java:108), the long-internal-amount bindInt boundary over a
 * 10B drum capacity, and the wood-barrel drains-to-true-empty semantics
 * (keepsFilter=F → identity cleared, tank key removed, tag dropped — the item stacks
 * like a never-filled one again). The handler is constructed directly over a vanilla
 * item stack (a mod Block/BlockItem cannot be constructed after the offline boot —
 * the GTOfflineTestBase intrusive-holder consequence); the capability dispatch itself
 * is exercised live by {@code /gt6tank show} on a real dropped barrel item.
 */
public class GTBarrelItemFluidHandlerTest extends GTOfflineTestBase {

	static final ItemStack STACK = new ItemStack(Items.GLASS_BOTTLE); // any vanilla item — the handler is item-agnostic

	@BeforeAll
	static void seedFluidRegistry() {
		// the vanilla water/lava FluidStacks resolve against the boot-populated registry
	}

	@Test
	public void roundTripKeepsAmountAndIdentity() {
		GTBarrelItemFluidHandler tFirst = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 16000);
		assertEquals(8000, tFirst.fill(new FluidStack(Fluids.WATER, 8000), FluidAction.EXECUTE));

		CompoundTag tTag = tFirst.serializeNBT();
		assertTrue(tTag.contains(TileEntityBase08Barrel.NBT_TANK, Tag.TAG_COMPOUND), "the payload rides the BE's own NBT_TANK key (the same-key deviation)");
		assertEquals("minecraft:water", tTag.getCompound(TileEntityBase08Barrel.NBT_TANK).getString("FluidName"));

		GTBarrelItemFluidHandler tSecond = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 16000);
		tSecond.deserializeNBT(tTag);
		FluidStack tDrained = tSecond.drain(8000, FluidAction.EXECUTE);
		assertEquals(8000, tDrained.getAmount(), "the amount survives the NBT hop");
		assertTrue(tDrained.getFluid() == Fluids.WATER, "the identity survives the NBT hop");
		assertTrue(tSecond.getFluidInTank(0).isEmpty(), "the tank is truly empty after the full drain");
	}

	/** The declaration behind the key deviation: what the BE world tank writes, the item handler reads, and back. */
	@Test
	public void sameKeyRoundTripWithTheBlockEntityTank() {
		FluidTankGT tBeTank = new FluidTankGT(16000);
		tBeTank.fill(new FluidStack(Fluids.WATER, 1234), FluidAction.EXECUTE);
		CompoundTag tTag = tBeTank.writeToNBT(new CompoundTag(), TileEntityBase08Barrel.NBT_TANK);

		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 16000);
		tHandler.deserializeNBT(tTag);
		assertEquals(1234, tHandler.getFluidInTank(0).getAmount(), "BE write → item read, same key");

		tHandler.fill(new FluidStack(Fluids.WATER, 66), FluidAction.EXECUTE);
		FluidTankGT tBack = new FluidTankGT(16000).readFromNBT(tHandler.serializeNBT(), TileEntityBase08Barrel.NBT_TANK);
		assertEquals(1300, tBack.amount(), "item write → BE read, same key (1300 = 1234 + 66)");
	}

	/** A merged pile has no single tank to write into (FluidHandlerItemStack.java:108). */
	@Test
	public void countGuardRejectsFillAndDrain() {
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE, 2), 16000);
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE), "count!=1 fill refused");
		assertTrue(tHandler.drain(1000, FluidAction.EXECUTE).isEmpty(), "count!=1 drain refused");
		assertTrue(tHandler.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).isEmpty(), "count!=1 identity drain refused");
	}

	/** The long internal amount behind the int surface — the 10B drum capacity, the LAmount overflow key, the bindInt boundary. */
	@Test
	public void longAmountSurvivesPastTheIntBoundary() {
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 10000000000L);
		assertEquals(2000000000, tHandler.fill(new FluidStack(Fluids.WATER, 2000000000), FluidAction.EXECUTE));
		assertEquals(2000000000, tHandler.fill(new FluidStack(Fluids.WATER, 2000000000), FluidAction.EXECUTE));

		// the int surface clamps at the bindInt boundary; the exact 63-bit value rides LAmount
		assertEquals(Integer.MAX_VALUE, tHandler.getFluidInTank(0).getAmount(), "the stack amount is bindInt(4e9)");
		assertEquals(Integer.MAX_VALUE, tHandler.getTankCapacity(0), "the capacity surface is bindInt(10e9)");
		CompoundTag tTankTag = tHandler.serializeNBT().getCompound(TileEntityBase08Barrel.NBT_TANK);
		assertEquals(4000000000L, tTankTag.getLong(FluidTankGT.NBT_L_AMOUNT), "the LAmount overflow key carries the exact long");

		GTBarrelItemFluidHandler tBack = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 10000000000L);
		tBack.deserializeNBT(tHandler.serializeNBT());
		assertEquals(Integer.MAX_VALUE, tBack.drain(Integer.MAX_VALUE, FluidAction.EXECUTE).getAmount());
		assertEquals(4000000000L - Integer.MAX_VALUE, tBack.drain(Integer.MAX_VALUE, FluidAction.EXECUTE).getAmount(),
				"the full long amount drains across two int-bounded draws");
	}

	/** Near-capacity truncation: a fill beyond the tank is clamped, never voided (voidExcess off). */
	@Test
	public void fillNearCapacityTruncatesAtTheBoundary() {
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 16000);
		assertEquals(16000, tHandler.fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), FluidAction.EXECUTE), "the oversized fill lands exactly at capacity");
		assertEquals(0, tHandler.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE), "the tank is full — nothing more");
	}

	/** The wood barrel's keepsFilter=F item face: drain to 0 clears the identity, removes the key, drops the tag. */
	@Test
	public void woodBarrelDrainsToTrueEmptyAndCleansTheTag() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(tStack, 16000);
		tHandler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		//? if forge {
		assertTrue(tStack.hasTag(), "the filled container carries its tag");
		//?} else {
		/*assertTrue(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "the filled container carries its payload component");
		*///?}

		tHandler.drain(100, FluidAction.EXECUTE);
		assertTrue(tHandler.getFluidInTank(0).isEmpty(), "keepsFilter=F: the identity is cleared at 0 L");
		assertFalse(tHandler.serializeNBT().contains(TileEntityBase08Barrel.NBT_TANK), "the tank key is removed (writeToNBT remove branch)");
		//? if forge {
		assertFalse(tStack.hasTag(), "the emptied container drops the whole tag — identical to a never-filled one, stacking restored");
		//?} else {
		/*assertFalse(tStack.get(gregtech6.registry.GT6DataComponents.BARREL_CONTENT) != null, "the emptied container drops the payload component — identical to a never-filled one, stacking restored");
		*///?}
		assertTrue(GTBarrelBlockItem.hasContent(tStack) == false, "the :290 stacking predicate reads empty again");
	}

	/** The stickiness seam (the BE load :132 item counterpart): preventDraining keeps the tank key at 0 L. */
	@Test
	public void preventDrainingKeepsTheTankKeyAtZero() {
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(new ItemStack(Items.GLASS_BOTTLE), 16000).setPreventDraining(true);
		tHandler.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
		tHandler.drain(100, FluidAction.EXECUTE);
		assertTrue(tHandler.serializeNBT().contains(TileEntityBase08Barrel.NBT_TANK), "preventDraining persists the key at 0 L (writeToNBT :85 branch)");
	}

	/** Template container semantics: the handler holds the live stack and hands it back (FluidHandlerItemStack.java:37/:52-55). */
	@Test
	public void containerIsHeldByReferenceAndReturned() {
		ItemStack tStack = new ItemStack(Items.GLASS_BOTTLE);
		GTBarrelItemFluidHandler tHandler = new GTBarrelItemFluidHandler(tStack, 16000);
		assertSame(tStack, tHandler.getContainer(), "getContainer() returns the live stack (FluidUtil picks the mutations up)");
		assertNotSame(tStack, tStack.copy());

		// the ctor reads an already-filled container tag back
		ItemStack tFilled = new ItemStack(Items.GLASS_BOTTLE);
		//? if forge {
		tFilled.getOrCreateTag().put(TileEntityBase08Barrel.NBT_TANK, new FluidStack(Fluids.LAVA, 500).writeToNBT(new CompoundTag()));
		//?} else {
		/*CompoundTag tPre = new CompoundTag();
		tPre.put(TileEntityBase08Barrel.NBT_TANK, new FluidStack(Fluids.LAVA, 500).save(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, new CompoundTag())); // 21.1: the codec save face
		net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.BARREL_CONTENT, tFilled, tPre); // 21.1: the payload rides the CustomData component
		*///?}
		assertEquals(500, new GTBarrelItemFluidHandler(tFilled, 16000).getFluidInTank(0).getAmount(), "a pre-filled container tag loads at construction");
	}
}
