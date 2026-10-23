package com.brainlessmolanisk;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

/** Draws the BELL cue: an outline on the slayer bell in the inventory. */
class BellOverlay extends WidgetItemOverlay
{
	private final BrainlessMolaniskPlugin plugin;
	private final BrainlessMolaniskConfig config;
	private final ItemManager itemManager;

	@Inject
	BellOverlay(BrainlessMolaniskPlugin plugin, BrainlessMolaniskConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (itemId != ItemID.MOLANISK_BELL || plugin.getCue().getKind() != Cue.Kind.BELL || !config.showBell())
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		graphics.drawImage(itemManager.getItemOutline(itemId, widgetItem.getQuantity(), config.bellColor()),
			(int) bounds.getX(), (int) bounds.getY(), null);
	}
}
