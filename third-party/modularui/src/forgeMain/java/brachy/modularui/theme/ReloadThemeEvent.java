package brachy.modularui.theme;

import net.minecraftforge.eventbus.api.Event;

/**
 * This event is invoked when themes are reloaded, but not on startup.
 * Do not use this to register themes. Use {@link net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent FMLConstructModEvent} instead.
 */
public class ReloadThemeEvent extends Event {}
