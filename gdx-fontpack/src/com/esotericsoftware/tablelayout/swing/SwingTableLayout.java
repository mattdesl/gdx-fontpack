
package com.esotericsoftware.tablelayout.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.tablelayout.SwingBaseTableLayout;
import com.esotericsoftware.tablelayout.SwingCell;
import com.esotericsoftware.tablelayout.swing.SwingToolkit.DebugRect;

class SwingTableLayout extends SwingBaseTableLayout<Component, SwingTable, SwingTableLayout, SwingToolkit> {
	ArrayList<DebugRect> debugRects;

	public SwingTableLayout () {
		super((SwingToolkit)SwingToolkit.instance);
	}

	public SwingTableLayout (SwingToolkit toolkit) {
		super(toolkit);
	}

	public void layout () {
		SwingTable table = getTable();
		Insets insets = table.getInsets();
		super.layout(insets.left, insets.top, //
			table.getWidth() - insets.left - insets.right, //
			table.getHeight() - insets.top - insets.bottom);

		List<SwingCell> cells = getCells();
		for (int i = 0, n = cells.size(); i < n; i++) {
			SwingCell c = cells.get(i);
			if (c.getIgnore()) continue;
			Component component = (Component)c.getWidget();
			component.setLocation((int)c.getWidgetX(), (int)c.getWidgetY());
			component.setSize((int)c.getWidgetWidth(), (int)c.getWidgetHeight());
		}

		if (getDebug() != Debug.none) SwingToolkit.startDebugTimer();
	}

	public void invalidate () {
		super.invalidate();
		if (getTable().isValid()) getTable().invalidate();
	}

	public void invalidateHierarchy () {
		if (getTable().isValid()) getTable().invalidate();
	}

	void drawDebug () {
		Graphics2D g = (Graphics2D)getTable().getGraphics();
		if (g == null) return;
		g.setColor(Color.red);
		for (DebugRect rect : debugRects) {
			if (rect.type == Debug.cell) g.setColor(Color.red);
			if (rect.type == Debug.widget) g.setColor(Color.green);
			if (rect.type == Debug.table) g.setColor(Color.blue);
			g.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
	}
}
