package com.brainlessmolanisk;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BrainlessMolaniskPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BrainlessMolaniskPlugin.class);
		RuneLite.main(args);
	}
}
