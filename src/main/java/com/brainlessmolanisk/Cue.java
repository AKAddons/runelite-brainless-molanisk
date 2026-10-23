package com.brainlessmolanisk;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/**
 * The one thing to click next. {@code target} is the molanisk's tile (ATTACK) or the spawn's ring
 * tile (STAND, and BELL when a spawn is known); {@code wall} is that spawn's wall.
 */
@Value
public class Cue
{
	public enum Kind
	{
		NONE,
		NO_BELL,
		ATTACK,
		BELL,
		STAND
	}

	public static final Cue NONE = new Cue(Kind.NONE, null, null);
	public static final Cue NO_BELL = new Cue(Kind.NO_BELL, null, null);
	public static final Cue BELL = new Cue(Kind.BELL, null, null);

	Kind kind;
	WorldPoint target;
	WorldPoint wall;

	static Cue attack(WorldPoint molanisk)
	{
		return new Cue(Kind.ATTACK, molanisk, null);
	}

	static Cue stand(Spot spot)
	{
		return new Cue(Kind.STAND, spot.getStand(), spot.getWall());
	}

	/** The bell, with the spawn it will lure from, so its ring tile stays drawn. */
	static Cue bell(Spot spot)
	{
		return new Cue(Kind.BELL, spot.getStand(), spot.getWall());
	}
}
