
package com.esotericsoftware.tablelayout.swing;

import com.esotericsoftware.tablelayout.SwingBaseTableLayout;
import com.esotericsoftware.tablelayout.SwingBaseTableLayout.Debug;
import com.esotericsoftware.tablelayout.SwingCell;
import com.esotericsoftware.tablelayout.SwingAbstractToolkit;
import com.esotericsoftware.tablelayout.SwingValue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class SwingTable extends JComponent {
	static {
		SwingAbstractToolkit.instance = new SwingToolkit();
	}

	private final SwingTableLayout layout;

	public SwingTable () {
		this(new SwingTableLayout());
	}

	public SwingTable (final SwingTableLayout layout) {
		this.layout = layout;
		layout.setTable(this);

		setLayout(new LayoutManager() {
			private Dimension minSize = new Dimension(), prefSize = new Dimension();

			public Dimension preferredLayoutSize (Container parent) {
				layout.layout(); // BOZO - Cache layout?
				prefSize.width = (int)layout.getMinWidth();
				prefSize.height = (int)layout.getMinHeight();
				return prefSize;
			}

			public Dimension minimumLayoutSize (Container parent) {
				layout.layout(); // BOZO - Cache layout?
				minSize.width = (int)layout.getMinWidth();
				minSize.height = (int)layout.getMinHeight();
				return minSize;
			}

			public void layoutContainer (Container ignored) {
				layout.layout();
			}

			public void addLayoutComponent (String name, Component comp) {
			}

			public void removeLayoutComponent (Component comp) {
			}
		});
	}

	/** Removes all Components and cells from the table. */
	public void clear () {
		layout.clear();
		invalidate();
	}

	public SwingCell addCell (String text) {
		return addCell(new JLabel(text));
	}

	/** Adds a cell with a placeholder Component. */
	public SwingCell addCell () {
		return addCell((Component)null);
	}

	/** Adds a new cell to the table with the specified Component.
	 * @see SwingTableLayout#add(Component)
	 * @param Component May be null to add a cell without an Component. */
	public SwingCell addCell (Component Component) {
		return layout.add(Component);
	}

	/** Adds a new cell to the table with the specified Components in a {@link SwingStack}.
	 * @param components May be null to add a cell without an Component. */
	public SwingCell stack (Component... components) {
		SwingStack stack = new SwingStack();
		for (int i = 0, n = components.length; i < n; i++)
			stack.add(components[i]);
		return addCell(stack);
	}

	/** Indicates that subsequent cells should be added to a new row and returns the cell values that will be used as the defaults
	 * for all cells in the new row.
	 * @see SwingTableLayout#row() */
	public SwingCell row () {
		return layout.row();
	}

	/** Gets the cell values that will be used as the defaults for all cells in the specified column.
	 * @see SwingTableLayout#columnDefaults(int) */
	public SwingCell columnDefaults (int column) {
		return layout.columnDefaults(column);
	}

	/** The cell values that will be used as the defaults for all cells.
	 * @see SwingTableLayout#defaults() */
	public SwingCell defaults () {
		return layout.defaults();
	}

	/** Positions and sizes children of the Component being laid out using the cell associated with each child.
	 * @see SwingTableLayout#layout() */
	public void layout () {
		layout.layout();
	}

	/** Removes all Components and cells from the table (same as {@link #clear()}) and additionally resets all table properties and
	 * cell, column, and row defaults.
	 * @see SwingTableLayout#reset() */
	public void reset () {
		layout.reset();
	}

	/** Returns the cell for the specified Component, anywhere in the table hierarchy.
	 * @see SwingTableLayout#getCell(Component) */
	public SwingCell getCell (Component Component) {
		return layout.getCell(Component);
	}

	/** Returns the cells for this table.
	 * @see SwingTableLayout#getCells() */
	public List<SwingCell> getCells () {
		return layout.getCells();
	}

	/** Padding around the table.
	 * @see SwingTableLayout#pad(SwingValue) */
	public SwingTable pad (SwingValue pad) {
		layout.pad(pad);
		return this;
	}

	/** Padding around the table.
	 * @see SwingTableLayout#pad(SwingValue, SwingValue, SwingValue, SwingValue) */
	public SwingTable pad (SwingValue top, SwingValue left, SwingValue bottom, SwingValue right) {
		layout.pad(top, left, bottom, right);
		return this;
	}

	/** Padding at the top of the table.
	 * @see SwingTableLayout#padTop(SwingValue) */
	public SwingTable padTop (SwingValue padTop) {
		layout.padTop(padTop);
		return this;
	}

	/** Padding at the left of the table.
	 * @see SwingTableLayout#padLeft(SwingValue) */
	public SwingTable padLeft (SwingValue padLeft) {
		layout.padLeft(padLeft);
		return this;
	}

	/** Padding at the bottom of the table.
	 * @see SwingTableLayout#padBottom(SwingValue) */
	public SwingTable padBottom (SwingValue padBottom) {
		layout.padBottom(padBottom);
		return this;
	}

	/** Padding at the right of the table.
	 * @see SwingTableLayout#padRight(SwingValue) */
	public SwingTable padRight (SwingValue padRight) {
		layout.padRight(padRight);
		return this;
	}

	/** Padding around the table.
	 * @see SwingTableLayout#pad(float) */
	public SwingTable pad (int pad) {
		layout.pad(pad);
		return this;
	}

	/** Padding around the table.
	 * @see SwingTableLayout#pad(float, float, float, float) */
	public SwingTable pad (int top, int left, int bottom, int right) {
		layout.pad(top, left, bottom, right);
		return this;
	}

	/** Padding at the top of the table.
	 * @see SwingTableLayout#padTop(float) */
	public SwingTable padTop (int padTop) {
		layout.padTop(padTop);
		return this;
	}

	/** Padding at the left of the table.
	 * @see SwingTableLayout#padLeft(float) */
	public SwingTable padLeft (int padLeft) {
		layout.padLeft(padLeft);
		return this;
	}

	/** Padding at the bottom of the table.
	 * @see SwingTableLayout#padBottom(float) */
	public SwingTable padBottom (int padBottom) {
		layout.padBottom(padBottom);
		return this;
	}

	/** Padding at the right of the table.
	 * @see SwingTableLayout#padRight(float) */
	public SwingTable padRight (int padRight) {
		layout.padRight(padRight);
		return this;
	}

	/** Alignment of the table within the Component being laid out. Set to {@link SwingBaseTableLayout#CENTER},
	 * {@link SwingBaseTableLayout#TOP}, {@link SwingBaseTableLayout#BOTTOM} , {@link SwingBaseTableLayout#LEFT} , {@link SwingBaseTableLayout#RIGHT},
	 * or any combination of those.
	 * @see SwingTableLayout#align(int) */
	public SwingTable align (int align) {
		layout.align(align);
		return this;
	}

	/** Sets the alignment of the table within the Component being laid out to {@link SwingBaseTableLayout#CENTER}.
	 * @see SwingTableLayout#center() */
	public SwingTable center () {
		layout.center();
		return this;
	}

	/** Sets the alignment of the table within the Component being laid out to {@link SwingBaseTableLayout#TOP}.
	 * @see SwingTableLayout#top() */
	public SwingTable top () {
		layout.top();
		return this;
	}

	/** Sets the alignment of the table within the Component being laid out to {@link SwingBaseTableLayout#LEFT}.
	 * @see SwingTableLayout#left() */
	public SwingTable left () {
		layout.left();
		return this;
	}

	/** Sets the alignment of the table within the Component being laid out to {@link SwingBaseTableLayout#BOTTOM}.
	 * @see SwingTableLayout#bottom() */
	public SwingTable bottom () {
		layout.bottom();
		return this;
	}

	/** Sets the alignment of the table within the Component being laid out to {@link SwingBaseTableLayout#RIGHT}.
	 * @see SwingTableLayout#right() */
	public SwingTable right () {
		layout.right();
		return this;
	}

	/** Turns on all debug lines.
	 * @see SwingTableLayout#debug() */
	public SwingTable debug () {
		layout.debug();
		return this;
	}

	/** Turns on debug lines.
	 * @see SwingTableLayout#debug() */
	public SwingTable debug (Debug debug) {
		layout.debug(debug);
		return this;
	}

	public Debug getDebug () {
		return layout.getDebug();
	}

	public SwingValue getPadTop () {
		return layout.getPadTopValue();
	}

	public SwingValue getPadLeft () {
		return layout.getPadLeftValue();
	}

	public SwingValue getPadBottom () {
		return layout.getPadBottomValue();
	}

	public SwingValue getPadRight () {
		return layout.getPadRightValue();
	}

	public int getAlign () {
		return layout.getAlign();
	}

	public SwingTableLayout getTableLayout () {
		return layout;
	}

	public void invalidate () {
		super.invalidate();
		layout.invalidate();
	}
}
