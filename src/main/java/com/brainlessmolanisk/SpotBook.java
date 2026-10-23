package com.brainlessmolanisk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.coords.WorldPoint;

/**
 * The fixed tile to ring from for each spawn. Learned from the player's own rings: a ring followed
 * within {@link #LEARN_TICKS} by a wall in bell range emptying records that ring tile for the wall.
 * Unlearned walls use the walkable tile in front of the wall (north, east, south, west, first wins).
 */
public class SpotBook
{
	/** Measured 2026-10-22: the wall emptied 1-2 ticks after the ring click. */
	static final int LEARN_TICKS = 4;

	private final Map<WorldPoint, WorldPoint> learned = new LinkedHashMap<>();
	private WorldPoint ringTile;
	private int ringTick = Integer.MIN_VALUE / 2;

	public WorldPoint standFor(WorldPoint wall, Predicate<WorldPoint> walkable)
	{
		WorldPoint base = learned.get(wall);
		if (base == null)
		{
			for (WorldPoint front : new WorldPoint[]{wall.dy(1), wall.dx(1), wall.dy(-1), wall.dx(-1)})
			{
				if (walkable.test(front))
				{
					base = front;
					break;
				}
			}
		}
		return base == null ? null : offTheWall(wall, base, walkable);
	}

	/**
	 * One more step away from the wall, when that tile is walkable: the tile right against the
	 * cave wall is drawn on the rock face and is not clickable (seen in play, 2026-10-22).
	 * A tile already off the wall (later rings in that run were 2 tiles out) is kept as it is.
	 */
	static WorldPoint offTheWall(WorldPoint wall, WorldPoint base, Predicate<WorldPoint> walkable)
	{
		if (base.distanceTo2D(wall) > 1)
		{
			return base;
		}
		WorldPoint out = base.dx(Integer.signum(base.getX() - wall.getX())).dy(Integer.signum(base.getY() - wall.getY()));
		return !out.equals(base) && walkable.test(out) && CueEngine.inBellRange(out, wall) ? out : base;
	}

	/** The raw learned tile, without the off-the-wall step, for the log. */
	WorldPoint learnedFor(WorldPoint wall)
	{
		return learned.get(wall);
	}

	public void onRing(int tick, WorldPoint player)
	{
		ringTick = tick;
		ringTile = player;
	}

	/** @return true when this emptying taught the book a new or changed tile. */
	public boolean onWallEmptied(int tick, WorldPoint wall)
	{
		if (ringTile == null || tick - ringTick > LEARN_TICKS || !CueEngine.inBellRange(ringTile, wall))
		{
			return false;
		}
		return !ringTile.equals(learned.put(wall, ringTile));
	}

	/** "wx,wy,wp>sx,sy,sp;..." for the plugin's config. */
	public String serialize()
	{
		StringBuilder sb = new StringBuilder();
		learned.forEach((wall, stand) -> sb.append(sb.length() == 0 ? "" : ";")
			.append(point(wall)).append('>').append(point(stand)));
		return sb.toString();
	}

	public void load(String text)
	{
		learned.clear();
		if (text == null || text.isEmpty())
		{
			return;
		}
		for (String entry : text.split(";"))
		{
			String[] halves = entry.split(">");
			if (halves.length == 2)
			{
				WorldPoint wall = parse(halves[0]);
				WorldPoint stand = parse(halves[1]);
				if (wall != null && stand != null)
				{
					learned.put(wall, stand);
				}
			}
		}
	}

	private static String point(WorldPoint p)
	{
		return p.getX() + "," + p.getY() + "," + p.getPlane();
	}

	private static WorldPoint parse(String s)
	{
		String[] parts = s.split(",");
		try
		{
			return parts.length == 3
				? new WorldPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
				: null;
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
