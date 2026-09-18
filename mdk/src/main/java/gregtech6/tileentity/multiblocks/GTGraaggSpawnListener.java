package gregtech6.tileentity.multiblocks;

import net.minecraft.world.entity.Mob;

import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The GAME-bus listener of the Von da Graagg spawn suppression (task p31-graagg) —
 * the first shared server-side main-bus listener of this repo (the
 * GTWrenchHighlightListener is the client-only FORGE-bus precedent; the annotation
 * without a bus clause rides the game bus on both loaders: 1.20.1 the annotation
 * default, 21.1 the rework's event-type auto-detection — docs.neoforged.net
 * docs/1.21.1/concepts/events/, the stonecutter swap-table note).
 *
 * <p>Why EntityJoinLevelEvent and not a MobSpawnEvent.CheckSpawn port: CheckSpawn is
 * DELETED in modern (Forge 1.20.1 has only MobSpawnEvent{SpawnPlacementCheck,
 * PositionCheck, FinalizeSpawn}); PositionCheck covers natural spawn attempts only and
 * /summon fires no spawn-check event at all (EntityType.spawn → addFreshEntity is a
 * straight pass) — the task acceptance "in-range summon suppressed" is only meetable at
 * the join gate, the one hook every spawn path funnels through. This covers all
 * join-path spawns incl. spawner/egg/breeding; upstream CheckSpawn covered natural
 * attempts only (declared deviation, coordinator-approved).
 *
 * <p>The three filters, cheapest first: client events drop (BE table lives server-side),
 * non-Mobs drop (items/arrows/xp re-join constantly), chunk-reload re-joins drop —
 * {@code loadedFromDisk() == true} mobs must NEVER be suppressed (the acceptance
 * "chunk-reload survivors" arm: a reload under a powered tower is not a spawn). The
 * decision itself is {@link TileEntityVonDaGraagg#shouldSuppress} over the live
 * {@code ALL_GRAAGGS} table (empty-table fast exit — the zero-tower world is the
 * common case and this handler sees every entity join).
 */
@Mod.EventBusSubscriber(modid = "gt6")
public final class GTGraaggSpawnListener {

	private GTGraaggSpawnListener() {
	}

	@SubscribeEvent
	public static void onEntityJoinLevel(EntityJoinLevelEvent aEvent) {
		if (aEvent.getLevel().isClientSide()) return;
		if (!(aEvent.getEntity() instanceof Mob)) return;
		if (TileEntityVonDaGraagg.shouldSuppress(aEvent.getLevel(), aEvent.getEntity().blockPosition(), aEvent.loadedFromDisk())) {
			aEvent.setCanceled(true); // the spawn aborts — the entity never enters the world
		}
	}
}
