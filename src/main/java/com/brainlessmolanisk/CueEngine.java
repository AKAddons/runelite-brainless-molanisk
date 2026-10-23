package com.brainlessmolanisk;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

/** Picks the single cue to show, in the order attack > bell > stand (REQ-BM-4). Pure: no client calls. */
public final class CueEngine
{
	/** The slayer bell works within this many tiles of the molanisk's wall. */
	static final int BELL_RANGE = 3;

	private CueEngine()
	{
	}

	public static Cue decide(Snapshot s)
	{
		WorldPoint player = s.getPlayer();
		if (!s.isInMolaniskArea() || player == null)
		{
			return Cue.NONE;
		}

		Optional<WorldPoint> molanisk = s.getLuredMolanisks().stream()
			.filter(p -> p.getPlane() == player.getPlane())
			.min(Comparator.comparingInt(player::distanceTo2D));
		if (molanisk.isPresent())
		{
			return Cue.attack(molanisk.get());
		}

		if (!s.isHasBell())
		{
			return Cue.NO_BELL;
		}

		if (s.isBellSettling())
		{
			return Cue.NONE;
		}

		List<Spot> spots = s.getHoldingSpots();
		// No wall state known at all: fall back to the brief's plain rule, the bell after the last molanisk.
		if (spots.isEmpty())
		{
			return Cue.BELL;
		}

		Comparator<Spot> nearest = Comparator.comparingInt(spot -> player.distanceTo2D(spot.getStand()));
		Optional<Spot> inRange = spots.stream().filter(spot -> inBellRange(player, spot.getWall())).min(nearest);
		if (inRange.isPresent())
		{
			return Cue.bell(inRange.get());
		}

		return spots.stream()
			.filter(spot -> spot.getStand().getPlane() == player.getPlane())
			.min(nearest)
			.map(Cue::stand)
			.orElse(Cue.NONE);
	}

	static boolean inBellRange(WorldPoint player, WorldPoint wall)
	{
		return player.getPlane() == wall.getPlane()
			&& Math.max(Math.abs(player.getX() - wall.getX()), Math.abs(player.getY() - wall.getY())) <= BELL_RANGE;
	}
}
