/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.items.bees;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The bumblebee item (task p33-bees-lv3-a-items) — one instance per type face of the
 * {@code type%5} fractal ({@link GT6Bumbles#FACES}); the species code rides the stack
 * NBT ({@code gt.bumble.meta}, {@link GT6BumbleGenes#codeOf}).
 *
 * <p><b>The name face</b>: composed {@code format(species)} — the format lang key carries
 * the zh word order (the dump faces: 雄性/雌性 prefix, 蜂后/(死亡) suffix, (已扫描)
 * tail; the en faces stay the bare species name exactly like upstream, where the type
 * digit never showed in the display name).
 *
 * <p><b>The tooltip face</b> (MultiItemBumbles.java:502-561, do-item 4): the requirement
 * word ({@link GT6Bumbles#requirementOf}); an untagged stack shows the no-genetic-data
 * pair (:509-510); a tagged unscanned stack shows "Not scanned yet!" (:514); a scanned
 * one shows the gene readout (:516-559 — the level, the humidity/temperature window, the
 * offspring/life/eff/aggro scalars and the day/night, weather and in/outside activity
 * words). Colors ride the upstream chat-color mapping.
 */
public final class GT6BumbleItem extends Item {

	/** The stack's type face (the fractal digit — the Bumbliary {@code bumbleType(stack)%5} value). */
	public final byte mFace;
	/** The scan flag ({@code meta%10 >= 5} upstream — the gene readout gate). */
	public final boolean mScanned;

	public GT6BumbleItem(Properties aProperties, byte aFace, boolean aScanned) {
		super(aProperties);
		mFace = aFace;
		mScanned = aScanned;
	}

	/** The stack's full type byte ({@code bumbleType} :568 — the %5 fold is the face itself). */
	public byte typeOf() {
		return (byte)(mFace + (mScanned ? GT6Bumbles.SCAN_OFFSET : 0));
	}

	@Override
	public Component getName(ItemStack aStack) {
		return Component.translatable(faceFormatKey(), Component.translatable(speciesKey(aStack)));
	}

	/** The name-format key of this item's face (the zh word order carrier). */
	private String faceFormatKey() {
		for (GT6Bumbles.FaceRow tFace : GT6Bumbles.FACES) if (tFace.face() == mFace && tFace.scanned() == mScanned) return tFace.formatKey();
		return "gt6.row.bumble.name.drone";
	}

	/** The species lang key of the stack's code (0 = Wild when untagged). */
	private static String speciesKey(ItemStack aStack) {
		return "gt6.row.bumble." + GT6BumbleGenes.codeOf(aStack);
	}

	//? if forge {
	@Override
	public void appendHoverText(ItemStack aStack, @Nullable Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
		super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
		addTooltipFace(aStack, aTooltip);
	}
	//?} else {
	/*@Override
	public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the hover signature carries the Item.TooltipContext (the GT6UsbStickItem fork shape)
		super.appendHoverText(aStack, aContext, aTooltip, aFlag);
		addTooltipFace(aStack, aTooltip);
	}
	*///?}

	/** The :502-561 tooltip walk (the requirement word + the genetic readout). */
	private static void addTooltipFace(ItemStack aStack, List<Component> aTooltip) {
		GT6Bumbles.FaceRow tFace = GT6Bumbles.faceOf(aStack);
		if (tFace == null) return;
		int tCode = GT6BumbleGenes.codeOf(aStack);
		aTooltip.add(Component.literal("Requirement: ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(GT6Bumbles.requirementOf(GT6Bumbles.familyOf(tCode))).withStyle(ChatFormatting.WHITE)));
		CompoundTag tGenes = GT6BumbleGenes.readGenes(aStack);
		if (tGenes == null) {
			aTooltip.add(Component.literal("No Genetic Data to display").withStyle(ChatFormatting.RED));
			aTooltip.add(Component.literal("Generates random 'Outsider-Plains-Biome' Genes when used").withStyle(ChatFormatting.AQUA));
			return;
		}
		aTooltip.add(Component.literal("Level: ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal((GT6Bumbles.tierOf(tCode) + 1) + " of 4").withStyle(ChatFormatting.LIGHT_PURPLE)));
		if (!tFace.scanned()) {
			aTooltip.add(Component.literal("Not scanned yet!").withStyle(ChatFormatting.RED));
			return;
		}
		aTooltip.add(Component.literal("Humidity: ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(GT6BumbleGenes.getHumidityMin(tGenes) + " to " + GT6BumbleGenes.getHumidityMax(tGenes)).withStyle(ChatFormatting.WHITE))
				.append(Component.literal("   Temp: ").withStyle(ChatFormatting.RED))
				.append(Component.literal(GT6BumbleGenes.getTemperatureMin(tGenes) + "K to " + GT6BumbleGenes.getTemperatureMax(tGenes) + "K").withStyle(ChatFormatting.WHITE)));
		aTooltip.add(Component.literal("Offspring: ").withStyle(ChatFormatting.GREEN)
				.append(Component.literal("" + GT6BumbleGenes.getOffspring(tGenes)).withStyle(ChatFormatting.WHITE))
				.append(Component.literal("   Life: ").withStyle(ChatFormatting.GOLD))
				.append(Component.literal(GT6BumbleGenes.getLifeSpan(tGenes) + " ticks").withStyle(ChatFormatting.WHITE)));
		aTooltip.add(Component.literal("Eff: ").withStyle(ChatFormatting.YELLOW)
				.append(Component.literal(GT6BumbleGenes.getWorkForce(tGenes) / 100L + "%").withStyle(ChatFormatting.WHITE))
				.append(Component.literal("   Aggro: ").withStyle(ChatFormatting.RED))
				.append(Component.literal(GT6BumbleGenes.getAggressiveness(tGenes) / 100L + "%").withStyle(ChatFormatting.WHITE)));
		boolean tDay = GT6BumbleGenes.getDayActive(tGenes), tNight = GT6BumbleGenes.getNightActive(tGenes);
		if (tDay && tNight) aTooltip.add(Component.literal("Doesn't take breaks").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tDay) aTooltip.add(Component.literal("Works at Day").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tNight) aTooltip.add(Component.literal("Works at Night").withStyle(ChatFormatting.LIGHT_PURPLE));
		else aTooltip.add(Component.literal("Doesn't work at any Time (BUG!!!)").withStyle(ChatFormatting.RED));
		boolean tRain = GT6BumbleGenes.getRainproof(tGenes), tStorm = GT6BumbleGenes.getStormproof(tGenes);
		if (tRain && tStorm) aTooltip.add(Component.literal("Can fly during any Weather").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tRain) aTooltip.add(Component.literal("Can fly during Rain, but not during Storms").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tStorm) aTooltip.add(Component.literal("Can fly during Storms, but not when it Rains").withStyle(ChatFormatting.LIGHT_PURPLE));
		else aTooltip.add(Component.literal("Weak to Weather").withStyle(ChatFormatting.RED));
		boolean tOutside = GT6BumbleGenes.getOutsideActive(tGenes), tInside = GT6BumbleGenes.getInsideActive(tGenes);
		if (tOutside && tInside) aTooltip.add(Component.literal("Doesn't care whether to bee In- or Outside").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tOutside) aTooltip.add(Component.literal("Needs to bee Outside").withStyle(ChatFormatting.LIGHT_PURPLE));
		else if (tInside) aTooltip.add(Component.literal("Needs to bee Inside").withStyle(ChatFormatting.LIGHT_PURPLE));
		else aTooltip.add(Component.literal("Doesn't work anywhere (BUG!!!)").withStyle(ChatFormatting.RED));
	}
}
