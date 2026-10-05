package gregtech6.items.tools;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
*///?}

/**
 * The tool death-message hook (task easter-s1-death-messages) — the port seam for the
 * upstream per-tool chat lines (research.easter-egg-census domain E). The vanilla melee
 * kill lands on the hardcoded {@code playerAttack} source (Player.attack, Player.java:1096
 * — no item-side damage-source hook exists on 1.20.1/21.1), so the swap rides the last
 * CombatTracker entry: {@code actuallyHurt} records the melee entry and zeroes the health
 * (LivingEntity.java:1620-1621), the death branch fires {@link LivingDeathEvent} before
 * {@code die()}, so at append time
 * {@link net.minecraft.world.damagesource.CombatTracker#recordDamage}'s leading
 * {@code recheckStatus()} sees {@code !mob.isAlive()} and clears the stale melee entries
 * (CombatTracker.java:145-157) — the appended entry is last and is the one
 * {@code getDeathMessage()} reads (:85-103), because {@code ServerPlayer.die} composes
 * the message at :589 <b>before</b> its own cleanup {@code recheckStatus()} at :640. The
 * {@link #ToolDeathSource} override of {@code getLocalizedDeathMessage} answers the
 * custom line — the same override shape as upstream's
 * {@code DamageSources.getCombatDamage} sources (gregapi/damage/DamageSources.java:106-128).
 *
 * <p>Player victims only: the chat death-message face is {@code ServerPlayer#die} —
 * mobs never broadcast one on either the 1.7.10 or the modern side (upstream
 * EntityPlayer.onDeath parity). The Behavior_Gun.java:301 bullet-material sub-face
 * stays dormant with the deferred GTPistolItem shooting chain.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = "gt6")
//?} else {
/*@EventBusSubscriber(modid = "gt6")
*///?}
public final class GT6ToolDeathListener {

	private GT6ToolDeathListener() {
	}

	//? if forge {
	@SubscribeEvent
	public static void onDeath(LivingDeathEvent aEvent) {
		handle(aEvent.getEntity(), aEvent.getSource());
	}
	//?} else {
	/*@SubscribeEvent
	public static void onDeath(LivingDeathEvent aEvent) {
		handle(aEvent.getEntity(), aEvent.getSource());
	}*/
	//?}

	/**
	 * The leg-shared body (the event forks only carry the bus annotation difference). The
	 * melee-face gate answers the vanilla {@code playerAttack} source only — arrows,
	 * thorns and mob claws keep their own messages, so a crossbow in the off-hand never
	 * borrows the main-hand tool's line; {@code playerAttack}'s causing entity is the
	 * attacking player by construction (Player.attack).
	 */
	private static void handle(LivingEntity aVictim, DamageSource aSource) {
		if (!(aVictim instanceof ServerPlayer)) return;
		if (!aSource.is(DamageTypes.PLAYER_ATTACK) || aSource.getEntity() == aVictim) return;
		Player tKiller = (Player) aSource.getEntity();
		ItemStack tStack = tKiller.getMainHandItem();
		if (tStack.isEmpty()) return;
		//? if forge {
		String tPath = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(tStack.getItem()).getPath();
		//?} else {
		/*String tPath = BuiltInRegistries.ITEM.getKey(tStack.getItem()).getPath();
		 *///?}
		String tTemplate = GT6ToolDeathMessages.templateOf(tPath);
		if (tTemplate == null) return; // the tool-domain filter: non-table kills keep the vanilla message
		Component tMessage = GT6ToolDeathMessages.compose(
				tKiller.getName().getString(), aVictim.getName().getString(), tTemplate);
		aVictim.getCombatTracker().recordDamage(
				new ToolDeathSource(Holder.direct(aSource.type()), tKiller, tMessage), 0.0F);
	}

	/** The message-bearing source — the {@code getLocalizedDeathMessage} override fold. */
	private static final class ToolDeathSource extends DamageSource {

		private final Component mMessage;

		ToolDeathSource(Holder<DamageType> aType, Entity aKiller, Component aMessage) {
			super(aType, aKiller, aKiller);
			mMessage = aMessage;
		}

		@Override
		public Component getLocalizedDeathMessage(LivingEntity aVictim) {
			return mMessage;
		}
	}
}
