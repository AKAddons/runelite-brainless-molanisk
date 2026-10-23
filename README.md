# Brainless Molanisk

A RuneLite plugin for molanisk slayer tasks in the Dorgesh-Kaan South Dungeon. It always
shows the one thing to click next, so the task can be done with very little attention:

1. **The molanisk to attack.** While a molanisk you lured with the bell is alive, it gets a
   cyan outline (the nearest one, if there are several).
2. **The slayer bell.** Once the last one is dead and you stand within 3 tiles of a wall
   that still holds a molanisk, the slayer bell in your inventory gets a green outline.
3. **The tile to ring from.** Otherwise the closest wall that holds a molanisk gets a yellow
   outline, its ring tile is marked "Ring here", and the game's hint arrow points at it.

Walls that hold a molanisk are read from the game itself (the wall changes when its
molanisk jumps off and when it comes back), so the plugin never points at an empty spawn.

**Ring tiles are learned.** When you ring the bell and a wall in range empties right after,
the tile you rang from becomes that spawn's ring tile, saved in your RuneLite config. A
spawn you have not rung at yet uses the tile one step out from the wall.

If you are in the dungeon without a slayer bell, a "No slayer bell" line is shown instead.

The plugin only draws on screen. It never clicks, types or changes menu entries.

## Settings

Each cue (molanisk, tile, bell) can be turned off, and each has its own colour.

## Building

JDK 11. `./gradlew test` runs the unit tests; `./gradlew run` starts a developer client with
the plugin loaded.

## Credits

The icon uses the molanisk model from Old School RuneScape (© Jagex Ltd), rendered as on the
[OSRS Wiki](https://oldschool.runescape.wiki/w/Molanisk).

Licensed under the BSD 2-Clause License, see `LICENSE`.
