package com.brainlessmolanisk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

/** Draws the NO_BELL cue: one line, only inside the molanisk area. */
class NoBellOverlay extends OverlayPanel
{
	private final BrainlessMolaniskPlugin plugin;

	@Inject
	NoBellOverlay(BrainlessMolaniskPlugin plugin)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getCue().getKind() != Cue.Kind.NO_BELL)
		{
			return null;
		}
		panelComponent.getChildren().add(LineComponent.builder().left("No slayer bell").leftColor(Color.ORANGE).build());
		return super.render(graphics);
	}
}
