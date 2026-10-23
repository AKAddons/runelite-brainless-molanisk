package com.brainlessmolanisk;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** What the cue engine sees on one tick, read from the client by the plugin. */
@Value
@Builder
public class Snapshot
{
	boolean inMolaniskArea;
	boolean hasBell;
	/** A ring happened within the settle window, so the molanisk may still be jumping. */
	boolean bellSettling;
	WorldPoint player;
	@Singular
	List<WorldPoint> luredMolanisks;
	/** Spawns whose wall holds a molanisk right now. */
	@Singular
	List<Spot> holdingSpots;
}
