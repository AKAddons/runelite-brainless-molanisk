package com.brainlessmolanisk;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** A molanisk spawn: the wall it hides in and the fixed tile to ring the bell from. */
@Value
public class Spot
{
	WorldPoint wall;
	WorldPoint stand;
}
