package com.brainlessmolanisk;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(BrainlessMolaniskConfig.GROUP)
public interface BrainlessMolaniskConfig extends Config
{
	String GROUP = "brainlessmolanisk";
	String LEARNED_SPOTS = "learnedSpots";
	String OPEN_DISCORD = "openDiscord";
	String DISCORD_URL = "https://discord.gg/6GuS6J8em3";

	/** A settings page cannot hold a link; ticking this opens the invite and the plugin unticks it. */
	@ConfigItem(keyName = OPEN_DISCORD, name = "Join the Discord", description = "Opens the AK Addons Discord invite in your browser", position = 0)
	default boolean openDiscord()
	{
		return false;
	}

	/** Ring tiles learned per spawn (SpotBook format). Seeded with the two rings of 2026-10-22. */
	@ConfigItem(keyName = LEARNED_SPOTS, name = "", description = "", hidden = true)
	default String learnedSpots()
	{
		return "2704,5215,0>2705,5216,0;2713,5213,0>2712,5214,0";
	}

	@ConfigItem(keyName = "showAttack", name = "Highlight molanisk", description = "Outline the molanisk to attack", position = 1)
	default boolean showAttack()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "attackColor", name = "Molanisk colour", description = "Outline colour of the molanisk to attack", position = 2)
	default Color attackColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(keyName = "showStand", name = "Highlight tile", description = "Mark the tile to stand on before ringing", position = 3)
	default boolean showStand()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "standColor", name = "Tile colour", description = "Colour of the tile to stand on", position = 4)
	default Color standColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(keyName = "showBell", name = "Highlight bell", description = "Outline the slayer bell when it is time to ring it", position = 5)
	default boolean showBell()
	{
		return true;
	}

	@Alpha
	@ConfigItem(keyName = "bellColor", name = "Bell colour", description = "Outline colour of the slayer bell", position = 6)
	default Color bellColor()
	{
		return Color.GREEN;
	}
}
