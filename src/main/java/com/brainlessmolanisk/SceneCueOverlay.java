package com.brainlessmolanisk;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;

/** Draws the ATTACK cue (molanisk outline) and the spawn's ring tile, label and wall (STAND and BELL). */
class SceneCueOverlay extends Overlay
{
	private final Client client;
	private final BrainlessMolaniskPlugin plugin;
	private final BrainlessMolaniskConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	SceneCueOverlay(Client client, BrainlessMolaniskPlugin plugin, BrainlessMolaniskConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Cue cue = plugin.getCue();
		if (cue.getKind() == Cue.Kind.ATTACK && config.showAttack())
		{
			NPC npc = plugin.getAttackTarget();
			if (npc != null)
			{
				outlines.drawOutline(npc, 2, config.attackColor(), 4);
			}
		}
		// The ring tile stays drawn while the bell is up, so there is always a ground tile to go by.
		else if ((cue.getKind() == Cue.Kind.STAND || cue.getKind() == Cue.Kind.BELL) && cue.getTarget() != null
			&& config.showStand())
		{
			TileObject wall = plugin.getStandWall();
			if (wall != null)
			{
				outlines.drawOutline(wall, 3, config.standColor(), 4);
			}
			LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), cue.getTarget());
			Polygon poly = lp == null ? null : Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, config.standColor(),
					ColorUtil.colorWithAlpha(config.standColor(), 90), new BasicStroke(3));
				Point text = Perspective.getCanvasTextLocation(client, graphics, lp, "Ring here", 0);
				if (text != null)
				{
					OverlayUtil.renderTextLocation(graphics, text, "Ring here", config.standColor());
				}
			}
		}
		return null;
	}
}
