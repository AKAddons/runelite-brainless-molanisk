package com.brainlessmolanisk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpotBook")
class SpotBookTest
{
	private static final WorldPoint WALL = new WorldPoint(2704, 5215, 0);
	private static final WorldPoint RING_TILE = new WorldPoint(2705, 5216, 0);

	@Test
	@DisplayName("marks the tile one step off the wall in front of an unlearned wall")
	void frontTile()
	{
		SpotBook book = new SpotBook();
		assertEquals(WALL.dy(2), book.standFor(WALL, p -> true));
		assertEquals(WALL.dx(-1), book.standFor(WALL, p -> p.equals(WALL.dx(-1))), "no room to step out: stays next to the wall");
		assertNull(book.standFor(WALL, p -> false));
	}

	@Test
	@DisplayName("learns the ring tile when the wall empties right after a ring in range, and marks one step out from it")
	void learnsFromRing()
	{
		SpotBook book = new SpotBook();
		book.onRing(29, RING_TILE);
		assertTrue(book.onWallEmptied(31, WALL));
		assertEquals(RING_TILE, book.learnedFor(WALL));
		assertEquals(new WorldPoint(2706, 5217, 0), book.standFor(WALL, p -> true));
		assertFalse(book.onWallEmptied(32, WALL), "the same tile again is not news");
	}

	@Test
	@DisplayName("keeps a learned tile that is already off the wall")
	void alreadyOff()
	{
		WorldPoint wall = new WorldPoint(2747, 5206, 0);
		WorldPoint twoOut = new WorldPoint(2745, 5206, 0);
		assertEquals(twoOut, SpotBook.offTheWall(wall, twoOut, p -> true));
	}

	@Test
	@DisplayName("does not learn from a wall emptying long after the ring or out of bell range")
	void ignoresUnrelated()
	{
		SpotBook book = new SpotBook();
		book.onRing(29, RING_TILE);
		assertFalse(book.onWallEmptied(29 + SpotBook.LEARN_TICKS + 1, WALL));
		assertFalse(book.onWallEmptied(30, new WorldPoint(2720, 5215, 0)));
		assertFalse(new SpotBook().onWallEmptied(30, WALL), "no ring at all");
	}

	@Test
	@DisplayName("survives a save and load, and skips broken entries")
	void roundTrip()
	{
		SpotBook book = new SpotBook();
		book.onRing(1, RING_TILE);
		book.onWallEmptied(2, WALL);
		SpotBook loaded = new SpotBook();
		loaded.load(book.serialize() + ";junk;1,2>x,y,z");
		assertEquals(RING_TILE, loaded.learnedFor(WALL));
		assertEquals(book.serialize(), loaded.serialize());
	}
}
