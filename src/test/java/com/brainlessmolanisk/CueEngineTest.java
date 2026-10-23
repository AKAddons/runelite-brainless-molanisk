package com.brainlessmolanisk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CueEngine")
class CueEngineTest
{
	private static WorldPoint at(int x, int y)
	{
		return new WorldPoint(x, y, 0);
	}

	private static Spot spot(WorldPoint wall)
	{
		return new Spot(wall, wall.dy(1));
	}

	private static Snapshot.SnapshotBuilder inDungeonWithBell(WorldPoint player)
	{
		return Snapshot.builder().inMolaniskArea(true).hasBell(true).player(player);
	}

	@Nested
	@DisplayName("attack cue")
	class Attack
	{
		@Test
		@DisplayName("highlights only the nearest of two lured molanisks")
		void nearestOfTwo()
		{
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100))
				.luredMolanisk(at(105, 100))
				.luredMolanisk(at(102, 100))
				.build());
			assertEquals(Cue.attack(at(102, 100)), cue);
		}

		@Test
		@DisplayName("wins over the bell even next to a holding wall")
		void beatsBell()
		{
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100))
				.holdingSpot(spot(at(101, 100)))
				.luredMolanisk(at(103, 100))
				.build());
			assertEquals(Cue.Kind.ATTACK, cue.getKind());
		}

		@Test
		@DisplayName("ignores a molanisk on another plane")
		void otherPlane()
		{
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100))
				.luredMolanisk(new WorldPoint(101, 100, 1))
				.holdingSpot(spot(at(101, 100)))
				.build());
			assertEquals(Cue.bell(spot(at(101, 100))), cue);
		}
	}

	@Nested
	@DisplayName("bell cue")
	class Bell
	{
		@Test
		@DisplayName("shows once the last molanisk is dead and a holding wall is within 3 tiles, keeping that spawn's tile")
		void afterLastKill()
		{
			Spot inRange = spot(at(103, 103));
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100)).holdingSpot(inRange).holdingSpot(spot(at(120, 100))).build());
			assertEquals(Cue.bell(inRange), cue);
		}

		@Test
		@DisplayName("shows when no wall state is known at all (fallback for assumption A-1)")
		void noWallStateKnown()
		{
			assertEquals(Cue.BELL, CueEngine.decide(inDungeonWithBell(at(100, 100)).build()));
		}

		@Test
		@DisplayName("hides while a ring is settling, so no tile flashes before the molanisk jumps")
		void settling()
		{
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100)).holdingSpot(spot(at(110, 100))).bellSettling(true).build());
			assertEquals(Cue.NONE, cue);
		}

		@Test
		@DisplayName("is replaced by a 'No slayer bell' line when there is no bell")
		void noBell()
		{
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100)).hasBell(false).holdingSpot(spot(at(101, 100))).build());
			assertEquals(Cue.NO_BELL, cue);
		}
	}

	@Nested
	@DisplayName("stand cue")
	class Stand
	{
		@Test
		@DisplayName("points at the fixed ring tile of the nearest holding spawn")
		void nearestSpawn()
		{
			Spot near = new Spot(at(110, 100), at(109, 101));
			Cue cue = CueEngine.decide(inDungeonWithBell(at(100, 100)).holdingSpot(spot(at(130, 100))).holdingSpot(near).build());
			assertEquals(Cue.stand(near), cue);
		}

		@Test
		@DisplayName("follows the closest holding spawn as the player moves")
		void followsClosest()
		{
			Spot west = spot(at(90, 100));
			Spot east = spot(at(115, 100));
			Snapshot.SnapshotBuilder base = Snapshot.builder().inMolaniskArea(true).hasBell(true).holdingSpot(west).holdingSpot(east);
			assertEquals(Cue.stand(west), CueEngine.decide(base.player(at(100, 100)).build()));
			assertEquals(Cue.stand(east), CueEngine.decide(base.player(at(106, 100)).build()));
		}
	}

	@Nested
	@DisplayName("scope")
	class Scope
	{
		@Test
		@DisplayName("draws nothing outside the molanisk area")
		void outside()
		{
			Cue cue = CueEngine.decide(Snapshot.builder().inMolaniskArea(false).hasBell(true).player(at(3222, 3218))
				.luredMolanisk(at(3223, 3218)).build());
			assertEquals(Cue.NONE, cue);
		}
	}
}
