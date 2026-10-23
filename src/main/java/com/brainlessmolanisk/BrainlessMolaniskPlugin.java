package com.brainlessmolanisk;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "Brainless Molanisk",
	description = "Highlights the one thing to click next on a molanisk task: the molanisk, the tile, or the slayer bell",
	tags = {"slayer", "molanisk", "bell", "dorgesh-kaan"}
)
public class BrainlessMolaniskPlugin extends Plugin
{
	/**
	 * Ticks all cues but ATTACK stay hidden after a ring, while the molanisk jumps off the wall.
	 * Measured 2026-10-22: the molanisk spawned 5 ticks after the ring click both times.
	 */
	static final int BELL_SETTLE_TICKS = 8;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SceneCueOverlay sceneOverlay;

	@Inject
	private BellOverlay bellOverlay;

	@Inject
	private NoBellOverlay noBellOverlay;

	@Inject
	@Named("developerMode")
	private boolean developerMode;

	@Inject
	private BrainlessMolaniskConfig config;

	@Inject
	private ConfigManager configManager;

	private final Set<TileObject> walls = new HashSet<>();
	private final SpotBook spotBook = new SpotBook();

	/** The wall object of the spawn the STAND or BELL cue points at, for the overlay's outline. */
	@Getter
	private TileObject standWall;

	@Getter
	private Cue cue = Cue.NONE;

	@Getter
	private NPC attackTarget;

	private int lastRingTick = Integer.MIN_VALUE / 2;

	@Provides
	BrainlessMolaniskConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BrainlessMolaniskConfig.class);
	}

	@Override
	protected void startUp()
	{
		spotBook.load(config.learnedSpots());
		overlayManager.add(sceneOverlay);
		overlayManager.add(bellOverlay);
		overlayManager.add(noBellOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(sceneOverlay);
		overlayManager.remove(bellOverlay);
		overlayManager.remove(noBellOverlay);
		if (cue.getKind() == Cue.Kind.STAND && client.hasHintArrow())
		{
			client.clearHintArrow();
		}
		walls.clear();
		cue = Cue.NONE;
		attackTarget = null;
		standWall = null;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		WorldView wv = client.getTopLevelWorldView();
		if (player == null || wv == null)
		{
			setCue(Cue.NONE, null, null);
			return;
		}

		List<NPC> lured = new ArrayList<>();
		boolean molaniskSeen = false;
		for (NPC npc : wv.npcs())
		{
			if (npc.getId() != NpcID.MOLANISK)
			{
				continue;
			}
			molaniskSeen = true;
			if (!npc.isDead() && (npc.getInteracting() == player || player.getInteracting() == npc))
			{
				lured.add(npc);
			}
		}

		Snapshot.SnapshotBuilder snap = Snapshot.builder()
			.inMolaniskArea(molaniskSeen || !walls.isEmpty())
			.hasBell(hasBell())
			.bellSettling(client.getTickCount() - lastRingTick < BELL_SETTLE_TICKS)
			.player(player.getWorldLocation());
		for (NPC npc : lured)
		{
			snap.luredMolanisk(npc.getWorldLocation());
		}
		for (TileObject wall : walls)
		{
			if (resolvedId(wall) == ObjectID.MOLANISK_ONWALL)
			{
				WorldPoint stand = spotBook.standFor(wall.getWorldLocation(), this::isWalkable);
				if (stand != null)
				{
					snap.holdingSpot(new Spot(wall.getWorldLocation(), stand));
				}
			}
		}

		Cue next = CueEngine.decide(snap.build());
		NPC target = null;
		TileObject wallObject = null;
		if (next.getKind() == Cue.Kind.ATTACK)
		{
			target = lured.stream().filter(n -> n.getWorldLocation().equals(next.getTarget())).findFirst().orElse(null);
		}
		else if (next.getWall() != null)
		{
			wallObject = walls.stream()
				.filter(w -> w.getWorldLocation().equals(next.getWall()) && resolvedId(w) == ObjectID.MOLANISK_ONWALL)
				.findFirst().orElse(null);
		}
		setCue(next, target, wallObject);
	}

	private void setCue(Cue next, NPC target, TileObject wallObject)
	{
		if (developerMode && !next.equals(cue))
		{
			log.info("[BM] cue {} -> {}", cue, next);
		}
		updateHintArrow(cue, next);
		cue = next;
		attackTarget = target;
		standWall = wallObject;
	}

	/** The game's own yellow arrow over the ring tile while walking to it, so a far spawn is findable. */
	private void updateHintArrow(Cue previous, Cue next)
	{
		boolean wanted = next.getKind() == Cue.Kind.STAND && config.showStand();
		boolean had = previous.getKind() == Cue.Kind.STAND;
		if (wanted && !next.getTarget().equals(previous.getTarget()))
		{
			client.setHintArrow(next.getTarget());
		}
		else if (!wanted && had && client.hasHintArrow())
		{
			client.clearHintArrow();
		}
	}

	private boolean hasBell()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INV);
		return inv != null && inv.contains(ItemID.MOLANISK_BELL);
	}

	private boolean isWalkable(WorldPoint p)
	{
		WorldView wv = client.getTopLevelWorldView();
		CollisionData[] maps = wv.getCollisionMaps();
		LocalPoint lp = LocalPoint.fromWorld(wv, p);
		if (maps == null || lp == null || p.getPlane() != wv.getPlane())
		{
			return false;
		}
		int flags = maps[p.getPlane()].getFlags()[lp.getSceneX()][lp.getSceneY()];
		return (flags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}

	private int resolvedId(TileObject object)
	{
		ObjectComposition comp = client.getObjectDefinition(object.getId());
		if (comp.getImpostorIds() != null)
		{
			comp = comp.getImpostor();
		}
		return comp == null ? -1 : comp.getId();
	}

	private static boolean isMolaniskObject(int id)
	{
		return id >= ObjectID.MOLANISK_NOTONWALL && id <= ObjectID.MOLANISK_CAVEWALL_STONES01;
	}

	private void wallSpawned(TileObject object, String kind)
	{
		if (!isMolaniskObject(object.getId()))
		{
			return;
		}
		walls.add(object);
		if (developerMode)
		{
			log.info("[BM] {} spawned id={} resolved={} at {}", kind, object.getId(), resolvedId(object), object.getWorldLocation());
		}
		if (resolvedId(object) == ObjectID.MOLANISK_NOTONWALL
			&& spotBook.onWallEmptied(client.getTickCount(), object.getWorldLocation()))
		{
			configManager.setConfiguration(BrainlessMolaniskConfig.GROUP, BrainlessMolaniskConfig.LEARNED_SPOTS, spotBook.serialize());
			if (developerMode)
			{
				log.info("[BM] learned ring tile for wall {}: {}", object.getWorldLocation(), spotBook.learnedFor(object.getWorldLocation()));
			}
		}
	}

	private void wallDespawned(TileObject object, String kind)
	{
		if (walls.remove(object) && developerMode)
		{
			log.info("[BM] {} despawned id={} at {}", kind, object.getId(), object.getWorldLocation());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		wallSpawned(e.getGameObject(), "game");
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		wallDespawned(e.getGameObject(), "game");
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned e)
	{
		wallSpawned(e.getWallObject(), "wall");
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned e)
	{
		wallDespawned(e.getWallObject(), "wall");
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned e)
	{
		wallSpawned(e.getDecorativeObject(), "decorative");
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned e)
	{
		wallDespawned(e.getDecorativeObject(), "decorative");
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned e)
	{
		wallSpawned(e.getGroundObject(), "ground");
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned e)
	{
		wallDespawned(e.getGroundObject(), "ground");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (BrainlessMolaniskConfig.GROUP.equals(e.getGroup()) && BrainlessMolaniskConfig.OPEN_DISCORD.equals(e.getKey())
			&& Boolean.parseBoolean(e.getNewValue()))
		{
			LinkBrowser.browse(BrainlessMolaniskConfig.DISCORD_URL);
			configManager.setConfiguration(BrainlessMolaniskConfig.GROUP, BrainlessMolaniskConfig.OPEN_DISCORD, false);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		// A scene load re-fires every spawn event.
		if (e.getGameState() == GameState.LOADING || e.getGameState() == GameState.LOGIN_SCREEN)
		{
			walls.clear();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e)
	{
		if (e.getItemId() == ItemID.MOLANISK_BELL && "Ring".equals(e.getMenuOption()))
		{
			lastRingTick = client.getTickCount();
			spotBook.onRing(lastRingTick, client.getLocalPlayer().getWorldLocation());
			if (developerMode)
			{
				log.info("[BM] bell rung at {} tick {}", client.getLocalPlayer().getWorldLocation(), lastRingTick);
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (developerMode && cue.getKind() != Cue.Kind.NONE
			&& (e.getType() == ChatMessageType.GAMEMESSAGE || e.getType() == ChatMessageType.SPAM))
		{
			log.info("[BM] chat {}: {}", e.getType(), e.getMessage());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();
		if (developerMode && npc.getId() == NpcID.MOLANISK)
		{
			log.info("[BM] molanisk spawned index={} at {} interacting={}", npc.getIndex(), npc.getWorldLocation(),
				npc.getInteracting() == null ? null : npc.getInteracting().getName());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();
		if (developerMode && npc.getId() == NpcID.MOLANISK)
		{
			log.info("[BM] molanisk despawned index={} at {} dead={}", npc.getIndex(), npc.getWorldLocation(), npc.isDead());
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		if (developerMode && e.getActor() instanceof NPC && ((NPC) e.getActor()).getId() == NpcID.MOLANISK)
		{
			NPC npc = (NPC) e.getActor();
			log.info("[BM] molanisk index={} anim={} interacting={}", npc.getIndex(), npc.getAnimation(),
				npc.getInteracting() == null ? null : npc.getInteracting().getName());
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated e)
	{
		int id = e.getGraphicsObject().getId();
		if (developerMode && id >= 1029 && id <= 1032)
		{
			log.info("[BM] spotanim {} at {}", id, e.getGraphicsObject().getLocation());
		}
	}
}
