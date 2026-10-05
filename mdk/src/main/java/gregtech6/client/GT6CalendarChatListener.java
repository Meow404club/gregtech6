package gregtech6.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.easter.GT6Calendars;

/**
 * The client login chat hook of the calendar eggs (task easter-s2-date-flags-fools) —
 * upstream GT_Client.java:63-70 fires the messages on the FIRST_CLIENT_PLAYER_TICK (once
 * per game session); the port equivalent is {@link ClientPlayerNetworkEvent.LoggingIn}
 * (once per server join, the closest client-side hook — the deviation is declared: a
 * reconnect re-fires where upstream would stay silent for the session).
 *
 * <p>The lines come verbatim from {@link GT6Calendars#loginLines()} (GT_Client.java:117-122).
 * Client-only FORGE-bus subscriber (the GTWrenchHighlightListener posture); the dedicated
 * server never registers it.
 */
@Mod.EventBusSubscriber(modid = "gt6", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GT6CalendarChatListener {

	@SubscribeEvent
	public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn aEvent) {
		for (net.minecraft.network.chat.Component tLine : GT6Calendars.loginLines())
			aEvent.getPlayer().displayClientMessage(tLine, false);
	}

	private GT6CalendarChatListener() {}
}
