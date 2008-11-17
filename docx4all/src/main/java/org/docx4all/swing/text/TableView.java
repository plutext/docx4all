/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of Docx4all.

    Docx4all is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU General Public License 
    as published by the Free Software Foundation.

    Docx4all is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with Docx4all.  If not, see <http://www.gnu.org/licenses/>.
    
 */
package org.docx4all.swing.text;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import org.docx4all.swing.LineBorderSegment;
import org.docx4all.xml.type.TblBorders;
import org.docx4all.xml.type.TblWidth;
import org.docx4j.wml.TcPrInner;

/**
 * This class is based on javax.swing.text.html.TableView class downloaded from 
 * <a href="http://download.java.net/openjdk/jdk7/">OpenJDK Source Releases</a>
 */
public class TableView extends BoxView {
	
    private int borderWidth;
    private int cellSpacing;

    private boolean cellWidthInPercentage;
    
    /**
     * Do any of the table cells span multiple rows?  If
     * true, the RowRequirementIterator will do additional
     * work to adjust the requirements of rows spanned by
     * a single table cell.  This is updated with each call to
     * updateGrid().
     */
    private boolean multiRowCells;

    int[] columnSpans;
    int[] columnOffsets;
    /**
     * SizeRequirements for all the columns.
     */
    SizeRequirements totalColumnRequirements;
    SizeRequirements[] columnRequirements;

    RowIterator rowIterator = new RowIterator();
    ColumnIterator colIterator = new ColumnIterator();

    Vector<TableRowView> rows;

    boolean gridValid;
    static final private BitSet EMPTY = new BitSet();

    /**
     * Constructs a TableView for the given element.
     *
     * @param elem the element that this view is responsible for
     */
    public TableView(Element elem) {
    	super(elem, View.Y_AXIS);
    	rows = new Vector<TableRowView>();
    	gridValid = false;
        totalColumnRequirements = new SizeRequirements();
    }

    /**
     * Creates a new table row.
     *
     * @param elem an element
     * @return the row
     */
    protected TableRowView createTableRow(Element elem) {
	    return new TableRowView(elem);
    }

    /**
     * The number of columns in the table.
     */
    public int getColumnCount() {
    	return columnSpans.length;
    }

    /**
     * Fetches the span (width) of the given column.  
     * This is used by the nested cells to query the 
     * sizes of grid locations outside of themselves.
     */
    public int getColumnSpan(int col) {
    	if (col < columnSpans.length) {
    		return columnSpans[col];
    	}
    	return 0;
    }

    /**
     * The number of rows in the table.
     */
    public int getRowCount() {
    	return rows.size();
    }

    /**
     * Fetch the span of multiple rows.  This includes
     * the border area.
     */
    public int getMultiRowSpan(int row0, int row1) {
    	TableRowView rv0 = getRow(row0);
    	TableRowView rv1 = getRow(row1);
    	if ((rv0 != null) && (rv1 != null)) {
    		int index0 = rv0.viewIndex;
    		int index1 = rv1.viewIndex;
    		int span = getOffset(Y_AXIS, index1) - getOffset(Y_AXIS, index0) + 
    		getSpan(Y_AXIS, index1);
    		return span;
    	}
    	return 0;
    }

    TableRowView getRow(int row) {
    	if (row < rows.size()) {
    		return rows.elementAt(row);
    	}
    	return null;
    }

    protected View getViewAtPoint(int x, int y, Rectangle alloc) {
    	int n = getViewCount();
    	View v = null;
    	Rectangle allocation = new Rectangle();
    	for (int i = 0; i < n; i++) {
    		allocation.setBounds(alloc);
    		childAllocation(i, allocation);
    		v = getView(i);
    		if (v instanceof TableRowView) {
    			v = ((TableRowView)v).findViewAtPoint(x, y, allocation);
    			if (v != null) {
    				alloc.setBounds(allocation);
    				return v;
    			}
    		}
    	}
        return super.getViewAtPoint(x, y, alloc);
    }

    /**
     * Determines the number of columns occupied by
     * the table cell represented by given element.
     */
    protected int getColumnGridSpan(View v) {
		AttributeSet attr = v.getElement().getAttributes();

		TcPrInner.GridSpan gs = WordMLStyleConstants.getTcGridSpan(attr);
		if (gs != null && gs.getVal() != null) {
			return gs.getVal().intValue();
		}

		return 1;
	}

    protected void invalidateGrid() {
    	gridValid = false;
    }

    /**
     * Update any cached values that come from attributes.
     */
    protected void setPropertiesFromAttributes() {
    	AttributeSet attr = getAttributes();
    	setParagraphInsets(attr);
   	
    	cellSpacing = WordMLStyleConstants.getTblCellSpacing(attr);
    	TblBorders borders = WordMLStyleConstants.getTblBorders(attr);
    	if (borders != null) {
    		borderWidth = borders.getCTBorder(LineBorderSegment.Side.LEFT).getSizeInPixels();
    	} else {
    		borderWidth = 0;
    	}
    }

    /**
     * Fill in the grid locations that are placeholders
     * for multi-column, multi-row, and missing grid
     * locations.
     */
    void updateGrid() {
		if (!gridValid) {
			cellWidthInPercentage = false;
			multiRowCells = false;

			// determine which views are table rows and clear out
			// grid points marked filled.
			rows.removeAllElements();
			int n = getViewCount();
			for (int i = 0; i < n; i++) {
				View v = getView(i);
				if (v instanceof TableRowView) {
					TableRowView rv = (TableRowView) v;
					rows.addElement(rv);
					rv.clearFilledColumns();
					rv.clearVMergeRestartColumns();
					rv.rowIndex = rows.size() - 1;
					rv.viewIndex = i;
				}
			}

			int maxColumns = 0;
			int nrows = rows.size();
			for (int row = 0; row < nrows; row++) {
				TableRowView rv = getRow(row);
				
				int col = 0;
				int grid = WordMLStyleConstants.getTrGridBefore(rv.getAttributes());
				for (; col < grid; col++) {
					rv.fillColumn(col);
				}
				
				for (int cell = 0; cell < rv.getViewCount(); cell++, col++) {
					View cv = rv.getView(cell);
					if (!cellWidthInPercentage) {
						AttributeSet a = cv.getAttributes();
						TblWidth tcWidth = WordMLStyleConstants.getTcWidth(a);
						if (tcWidth != null && tcWidth.getType() == TblWidth.Type.PCT) {
							cellWidthInPercentage = true;
						}
					}
					
					// advance to a free column
					for (; rv.isFilled(col); col++)
						;
					
					TcPrInner.VMerge vmerge = 
						WordMLStyleConstants.getTcVMerge(cv.getAttributes());
					int colSpan = getColumnGridSpan(cv);
					if (vmerge != null) {
						if ("restart".equalsIgnoreCase(vmerge.getVal())) {
							//TableCellView cv is where 
							//the vertical merge starts
							rv.vmergeRestartColumn(col);
							for (int i = 1; i < colSpan; i++) {
								rv.fillColumn(col + i);
							}
						} else {
							//TableCellView cv is a continuation of 
							//vertical merge.
							//Try to find the cell that starts the vertical merge
							//and increase its row span attribute.
							TableRowView trv = null;
							for (int i = row - 1; i >= 0; i--) {
								trv = getRow(i);
								if (trv.isVMergeRestartColumn(col)) {
									i = -1;//break
								}
							}
							if (trv != null) {
								//vmergeRestartCell is the cell that starts 
								//the vertical merge
								TableCellView vmergeRestartCell = 
									(TableCellView) trv.findVMergeRestartCell(col);
								int rowSpan = vmergeRestartCell.getRowSpan();
								//Increase row span attribute
								vmergeRestartCell.setRowSpan(rowSpan + 1);
								
								for (int i = 0; i < colSpan; i++) {
									rv.fillColumn(col + i);
								}
								multiRowCells = true;
							} else {
								//Ignore bad vmerge setting.
							}
						}
					}
					
					if (colSpan > 1) {
						col += colSpan - 1;
					}
				} //for (cell) loop
				
				grid = WordMLStyleConstants.getTrGridAfter(rv.getAttributes());
				for (; col < col + grid; col++) {
					rv.fillColumn(col);
				}
				
				maxColumns = Math.max(maxColumns, col);
			} //for (row) loop

			// setup the column layout/requirements
			columnSpans = new int[maxColumns];
			columnOffsets = new int[maxColumns];
			columnRequirements = new SizeRequirements[maxColumns];
			for (int i = 0; i < maxColumns; i++) {
				columnRequirements[i] = new SizeRequirements();
				columnRequirements[i].maximum = Integer.MAX_VALUE;
			}
			gridValid = true;
		}
	}
    
    /**
     * Layout the columns to fit within the given target span.
     *
     * @param targetSpan the given span for total of all the table
     *  columns
     * @param reqs the requirements desired for each column.  This
     *  is the column maximum of the cells minimum, preferred, and
     *  maximum requested span
     * @param spans the return value of how much to allocated to
     *  each column
     * @param offsets the return value of the offset from the
     *  origin for each column
     * @return the offset from the origin and the span for each column 
     *  in the offsets and spans parameters
     */
    protected void layoutColumns(int targetSpan, int[] offsets, int[] spans, 
				 SizeRequirements[] reqs) {
        //clean offsets and spans
        Arrays.fill(offsets, 0);
        Arrays.fill(spans, 0);
        colIterator.setLayoutArrays(offsets, spans, targetSpan);
        CSS.calculateTiledLayout(colIterator, targetSpan);
    }

    /**
     * Calculate the requirements for each column.  The calculation
     * is done as two passes over the table.  The table cells that
     * occupy a single column are scanned first to determine the
     * maximum of minimum, preferred, and maximum spans along the
     * give axis.  Table cells that span multiple columns are excluded
     * from the first pass.  A second pass is made to determine if
     * the cells that span multiple columns are satisfied.  If the
     * column requirements are not satisified, the needs of the 
     * multi-column cell is mixed into the existing column requirements.
     * The calculation of the multi-column distribution is based upon
     * the proportions of the existing column requirements and taking
     * into consideration any constraining maximums.
     */
    void calculateColumnRequirements(int axis) {
		// clean columnRequirements
		for (SizeRequirements req : columnRequirements) {
			req.minimum = 0;
			req.preferred = 0;
			req.maximum = Integer.MAX_VALUE;
		}

		// pass 1 - single column cells
		boolean hasMultiColumn = false;
		int nrows = getRowCount();
		for (int i = 0; i < nrows; i++) {
			TableRowView row = getRow(i);
			int col = 0;
			int ncells = row.getViewCount();
			for (int cell = 0; cell < ncells; cell++) {
				TableCellView cv = (TableCellView) row.getView(cell);
				if (!cv.isHidden()) {
					for (; row.isFilled(col); col++)
						; // advance to a free column
				
					int colSpan = getColumnGridSpan(cv);
					if (colSpan == 1) {
						checkSingleColumnCell(axis, col, cv);
					} else {
						hasMultiColumn = true;
						col += colSpan - 1;
					}
					col++;
				}
			}
		}

		// pass 2 - multi-column cells
		if (hasMultiColumn) {
			for (int i = 0; i < nrows; i++) {
				TableRowView row = getRow(i);
				int col = 0;
				int ncells = row.getViewCount();
				for (int cell = 0; cell < ncells; cell++) {
					TableCellView cv = (TableCellView) row.getView(cell);
					if (!cv.isHidden()) {
						for (; row.isFilled(col); col++)
							; // advance to a free column
						int colSpan = getColumnGridSpan(cv);
						if (colSpan > 1) {
							checkMultiColumnCell(axis, col, colSpan, cv);
							col += colSpan - 1;
						}
						col++;
					}
				}
			}
		}
	}

    /**
     * check the requirements of a table cell that spans a single column.
     */
    void checkSingleColumnCell(int axis, int col, View v) {
    	SizeRequirements req = columnRequirements[col];
    	int vspan = (int) v.getMinimumSpan(axis);
    	req.minimum = Math.max(vspan, req.minimum);
	
    	vspan = (int) v.getPreferredSpan(axis);
    	req.preferred = Math.max(vspan, req.preferred);
    }

    /**
     * check the requirements of a table cell that spans multiple
     * columns.
     */
    void checkMultiColumnCell(int axis, int col, int ncols, View v) {
	// calculate the totals
	long min = 0;
	long pref = 0;
	long max = 0;
	for (int i = 0; i < ncols; i++) {
	    SizeRequirements req = columnRequirements[col + i];
	    min += req.minimum;
	    pref += req.preferred;
	    max += req.maximum;
	}

	// check if the minimum size needs adjustment.
	int cmin = (int) v.getMinimumSpan(axis); 
	if (cmin > min) {
	    /*
	     * the columns that this cell spans need adjustment to fit
	     * this table cell.... calculate the adjustments.
	     */
	    SizeRequirements[] reqs = new SizeRequirements[ncols];
	    for (int i = 0; i < ncols; i++) {
		reqs[i] = columnRequirements[col + i];
	    }
	    int[] spans = new int[ncols];
	    int[] offsets = new int[ncols];
	    SizeRequirements.calculateTiledPositions(cmin, null, reqs, 
						     offsets, spans);
	    // apply the adjustments
	    for (int i = 0; i < ncols; i++) {
		SizeRequirements req = reqs[i];
		req.minimum = Math.max(spans[i], req.minimum);
		req.preferred = Math.max(req.minimum, req.preferred);
		req.maximum = Math.max(req.preferred, req.maximum);
	    }
	}

	// check if the preferred size needs adjustment.
	int cpref = (int) v.getPreferredSpan(axis); 
	if (cpref > pref) {
	    /*
	     * the columns that this cell spans need adjustment to fit
	     * this table cell.... calculate the adjustments.
	     */
	    SizeRequirements[] reqs = new SizeRequirements[ncols];
	    for (int i = 0; i < ncols; i++) {
		reqs[i] = columnRequirements[col + i];
	    }
	    int[] spans = new int[ncols];
	    int[] offsets = new int[ncols];
	    SizeRequirements.calculateTiledPositions(cpref, null, reqs, 
						     offsets, spans);
	    // apply the adjustments
	    for (int i = 0; i < ncols; i++) {
		SizeRequirements req = reqs[i];
		req.preferred = Math.max(spans[i], req.preferred);
		req.maximum = Math.max(req.preferred, req.maximum);
	    }
	}

    }

    // --- BoxView methods -----------------------------------------

    /**
     * Calculate the requirements for the minor axis.  This is called by
     * the superclass whenever the requirements need to be updated (i.e.
     * a preferenceChanged was messaged through this view).  
     * <p>
     * This is implemented to calculate the requirements as the sum of the 
     * requirements of the columns and then adjust it if the 
     * CSS width or height attribute is specified and applicable to
     * the axis.
     */
    protected SizeRequirements calculateMinorAxisRequirements(
    	int axis,
		SizeRequirements r) {
    	
		updateGrid();

		// calculate column requirements for each column
		calculateColumnRequirements(axis);

		// the requirements are the sum of the columns.
		if (r == null) {
			r = new SizeRequirements();
		}
		long min = 0;
		long pref = 0;
		int n = columnRequirements.length;
		for (int i = 0; i < n; i++) {
			SizeRequirements req = columnRequirements[i];
			min += req.minimum;
			pref += req.preferred;
		}
		
		int adjust = (n + 1) * cellSpacing + 2 * borderWidth;
		min += adjust;
		pref += adjust;
		r.minimum = (int) min;
		r.preferred = (int) pref;
		r.maximum = (int) pref;

		AttributeSet attr = getAttributes();
		
		TblWidth tblWidth = WordMLStyleConstants.getTblWidth(attr);
		if (tblWidth != null
			&& tblWidth.getType() != TblWidth.Type.PCT
			&& tblWidth.getType() != TblWidth.Type.AUTO) {
			int ignoreThisParam = 0;
			int width = tblWidth.getWidthInPixel(ignoreThisParam);
			if (width < (int) min) {
				r.maximum = r.minimum = r.preferred = (int) min;				
			} else {
				r.maximum = r.minimum = r.preferred = (int) width;				
			}
		}
		
		// set the alignment
		int alignment = StyleConstants.getAlignment(attr);
		if (alignment == StyleConstants.ALIGN_LEFT) {
			r.alignment = 0;
		} else if (alignment == StyleConstants.ALIGN_CENTER) {
			r.alignment = 0.5f;
		} else if (alignment == StyleConstants.ALIGN_RIGHT) {
			r.alignment = 1;
		} else {
			r.alignment = 0;
		}
		
		totalColumnRequirements.minimum = r.minimum;
		totalColumnRequirements.preferred = r.preferred;
		totalColumnRequirements.maximum = r.maximum;
		totalColumnRequirements.alignment = r.alignment;

		return r;
	}

    /**
     * Calculate the requirements for the major axis.  This is called by
     * the superclass whenever the requirements need to be updated (i.e.
     * a preferenceChanged was messaged through this view).  
     * <p>
     * This is implemented to provide the superclass behavior adjusted for
     * multi-row table cells.
     */
    protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
    	rowIterator.updateAdjustments();
    	r = CSS.calculateTiledRequirements(rowIterator, r);
    	r.maximum = r.preferred;
    	return r;
    }

    /**
     * Perform layout for the minor axis of the box (i.e. the
     * axis orthoginal to the axis that it represents).  The results 
     * of the layout should be placed in the given arrays which represent 
     * the allocations to the children along the minor axis.  This 
     * is called by the superclass whenever the layout needs to be 
     * updated along the minor axis.
     * <p>
     * This is implemented to call the 
     * <a href="#layoutColumns">layoutColumns</a> method, and then
     * forward to the superclass to actually carry out the layout
     * of the tables rows.
     *
     * @param targetSpan the total span given to the view, which
     *  whould be used to layout the children
     * @param axis the axis being layed out
     * @param offsets the offsets from the origin of the view for
     *  each of the child views.  This is a return value and is
     *  filled in by the implementation of this method
     * @param spans the span of each child view;  this is a return
     *  value and is filled in by the implementation of this method
     * @return the offset and span for each child view in the
     *  offsets and spans parameters
     */
    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets,	int[] spans) {
		// make grid is properly represented
		updateGrid();

		// all of the row layouts are invalid, so mark them that way
		int n = getRowCount();
		for (int i = 0; i < n; i++) {
			TableRowView row = getRow(i);
			row.layoutChanged(axis);
		}

		// calculate column spans
		layoutColumns(targetSpan, columnOffsets, columnSpans,
				columnRequirements);

		// continue normal layout
		super.layoutMinorAxis(targetSpan, axis, offsets, spans);
	}


    /**
     * Perform layout for the major axis of the box (i.e. the
     * axis that it represents).  The results 
     * of the layout should be placed in the given arrays which represent 
     * the allocations to the children along the minor axis.  This 
     * is called by the superclass whenever the layout needs to be 
     * updated along the minor axis.
     * <p>
     * This method is where the layout of the table rows within the
     * table takes place.  This method is implemented to call the use 
     * the RowIterator and the CSS collapsing tile to layout 
     * with border spacing and border collapsing capabilities.
     *
     * @param targetSpan the total span given to the view, which
     *  whould be used to layout the children
     * @param axis the axis being layed out
     * @param offsets the offsets from the origin of the view for
     *  each of the child views; this is a return value and is
     *  filled in by the implementation of this method
     * @param spans the span of each child view; this is a return
     *  value and is filled in by the implementation of this method
     * @return the offset and span for each child view in the
     *  offsets and spans parameters
     */
    protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
    	rowIterator.setLayoutArrays(offsets, spans);
    	CSS.calculateTiledLayout(rowIterator, targetSpan);
    }

    /**
     * Fetches the child view that represents the given position in
     * the model.  This is implemented to walk through the children
     * looking for a range that contains the given position.  In this
     * view the children do not necessarily have a one to one mapping 
     * with the child elements.
     *
     * @param pos  the search position >= 0
     * @param a  the allocation to the table on entry, and the
     *   allocation of the view containing the position on exit
     * @return  the view representing the given position, or 
     *   null if there isn't one
     */
    protected View getViewAtPosition(int pos, Rectangle a) {
		int n = getViewCount();
		for (int i = 0; i < n; i++) {
			View v = getView(i);
			int p0 = v.getStartOffset();
			int p1 = v.getEndOffset();
			if ((pos >= p0) && (pos < p1)) {
				// it's in this view.
				if (a != null) {
					childAllocation(i, a);
				}
				return v;
			}
		}
		if (pos == getEndOffset()) {
			View v = getView(n - 1);
			if (a != null) {
				this.childAllocation(n - 1, a);
			}
			return v;
		}
		return null;
	}

    // --- View methods ---------------------------------------------

    /**
     * Renders using the given rendering surface and area on that
     * surface.  This is implemented to delegate to the css box
     * painter to paint the border and background prior to the 
     * interior.  The superclass culls rendering the children
     * that don't directly intersect the clip and the row may
     * have cells hanging from a row above in it.  The table
     * does not use the superclass rendering behavior and instead
     * paints all of the rows and lets the rows cull those 
     * cells not intersecting the clip region.
     *
     * @param g the rendering surface to use
     * @param allocation the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape allocation) {
    	// paint the border 
    	Rectangle alloc = allocation.getBounds();
    	setSize(alloc.width, alloc.height);

    	/*
    	int x = alloc.x + getLeftInset();
    	int y = alloc.y + getTopInset();
    	int w = alloc.width - (getLeftInset() + getRightInset());
    	int h = alloc.height - (getTopInset() + getBottomInset());
    	
    	AttributeSet attr = getAttributes();
    	TblBorders borders = WordMLStyleConstants.getTblBorders(attr);
    	if (borders != null) {
    		LineBorderSegment line = borders.getLeft();
    		if (line != null) {
    			line.paint(g, alloc.x, alloc.y, Direction.DOWN, alloc.height);
    		}
    		line = borders.getTop();
    		if (line != null) {
    			line.paint(g, x, y, Direction.RIGHT, w);
    		}
    		
    		line = borders.getRight();
    		if (line != null) {
    			line.paint(g, x + w, y, Direction.DOWN, h);
    		}
    		line = borders.getBottom();
    		if (line != null) {
    			line.paint(g, x, y + h, Direction.RIGHT, w);
    		}
    	}*/
    	
        // paint interior
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
        	View v = getView(i);
        	v.paint(g, getChildAllocation(i, allocation));
        }
        
    }

    /**
     * Establishes the parent view for this view.  This is
     * guaranteed to be called before any other methods if the
     * parent view is functioning properly.
     * <p> 
     * This is implemented
     * to forward to the superclass as well as call the
     * <a href="#setPropertiesFromAttributes">setPropertiesFromAttributes</a>
     * method to set the paragraph properties from the css
     * attributes.  The call is made at this time to ensure
     * the ability to resolve upward through the parents 
     * view attributes.
     *
     * @param parent the new parent, or null if the view is
     *  being removed from a parent it was previously added
     *  to
     */
    public void setParent(View parent) {
    	super.setParent(parent);
        if (parent != null) {
        	setPropertiesFromAttributes();
        }
    }

    protected void forwardUpdate(
    	DocumentEvent.ElementChange ec,
		DocumentEvent e, 
		Shape a, 
		ViewFactory f) {
    	
		super.forwardUpdate(ec, e, a, f);
		// A change in any of the table cells usually effects the whole table,
		// so redraw it all!
		if (a != null) {
			Component c = getContainer();
			if (c != null) {
				Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a
						.getBounds();
				c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
			}
		}
	}

    /**
     * Change the child views.  This is implemented to
     * provide the superclass behavior and invalidate the
     * grid so that rows and columns will be recalculated.
     */
    public void replace(int offset, int length, View[] views) {
    	super.replace(offset, length, views);
    	invalidateGrid();
    }
	
    class ColumnIterator implements CSS.LayoutIterator {
    	/**
    	 * Current column index
    	 */
    	private int col;

    	/**
    	 * percentage values (may be null since there
    	 * might not be any).
    	 */
    	private int[] percentages;

    	private int[] adjustmentWeights;

    	private int[] offsets;
    	private int[] spans;
    	
    	/**
    	 * Disable percentage adjustments which should only apply
    	 * when calculating layout, not requirements.
    	 */
    	void disablePercentages() {
    		percentages = null;
    	}

    	/**
    	 * Update percentage adjustments if they are needed.
    	 */
    	private void updatePercentagesAndAdjustmentWeights(int span) {
			adjustmentWeights = new int[columnRequirements.length];
			for (int i = 0; i < columnRequirements.length; i++) {
				adjustmentWeights[i] = 0;
			}
			
			if (cellWidthInPercentage) {
				percentages = new int[columnRequirements.length];
			} else {
				percentages = null;
			}
			
			int nrows = getRowCount();
			for (int rowIndex = 0; rowIndex < nrows; rowIndex++) {
				TableRowView row = getRow(rowIndex);
				int col = 0;
				int ncells = row.getViewCount();
				for (int cell = 0; cell < ncells; cell++, col++) {
					TableCellView cv = (TableCellView) row.getView(cell);
					if (cv.isHidden()) {
						col--;
					} else {
						for (; row.isFilled(col); col++)
							; // advance to a free column
						int colSpan = getColumnGridSpan(cv);
						AttributeSet a = cv.getAttributes();
						TblWidth tcwidth = WordMLStyleConstants.getTcWidth(a);
						if (tcwidth != null) {
							int len = tcwidth.getWidthInPixel(span);
							len = (int) (len / colSpan + 0.5f);
							for (int i=0; i < colSpan; i++) {
								if (tcwidth.getType() == TblWidth.Type.PCT) {
									percentages[col + i] = 
										Math.max(percentages[col + i], len);
									adjustmentWeights[col + i] = 
										Math.max(adjustmentWeights[col + i], WorstAdjustmentWeight);
								} else {
									adjustmentWeights[col + i] = Math.max(
										adjustmentWeights[col + i],
										WorstAdjustmentWeight - 1);
								}
							}
						}
						col += colSpan - 1;
					}
				}
			}
		} 

    	/**
		 * Set the layout arrays to use for holding layout results
		 */
		public void setLayoutArrays(int offsets[], int spans[], int targetSpan) {
			this.offsets = offsets;
			this.spans = spans;
			updatePercentagesAndAdjustmentWeights(targetSpan);
		}

		// --- TileIterator methods -------------------

		public int getCount() {
			return columnRequirements.length;
		}

		public void setIndex(int i) {
			col = i;
		}

		public void setOffset(int offs) {
			offsets[col] = offs;
		}

		public int getOffset() {
			return offsets[col];
		}

		public void setSpan(int span) {
			spans[col] = span;
		}

		public int getSpan() {
			return spans[col];
		}

		public float getMinimumSpan(float parentSpan) {
			// do not care for percentages, since min span can't
			// be less than columnRequirements[col].minimum,
			// but can be less than percentage value.
			return columnRequirements[col].minimum;
		}

		public float getPreferredSpan(float parentSpan) {
			if ((percentages != null) && (percentages[col] != 0)) {
				return Math.max(percentages[col],
						columnRequirements[col].minimum);
			}
			return columnRequirements[col].preferred;
		}

		public float getMaximumSpan(float parentSpan) {
			return columnRequirements[col].maximum;
		}

		public float getBorderWidth() {
		    return borderWidth;        
		}                              

		public float getLeadingCollapseSpan() {
			return cellSpacing;
		}

		public float getTrailingCollapseSpan() {
			return cellSpacing;
		}

		public int getAdjustmentWeight() {
			return adjustmentWeights[col];
		}
    } //ColumnIterator inner class

    class RowIterator implements CSS.LayoutIterator {
		/**
		 * Current row index
		 */
		private int row;

		/**
		 * Adjustments to the row requirements to handle multi-row table cells.
		 */
		private int[] adjustments;

		private int[] offsets;
		private int[] spans;
		
		void updateAdjustments() {
			int axis = Y_AXIS;
			if (multiRowCells) {
				// adjust requirements of multi-row cells
				int n = getRowCount();
				adjustments = new int[n];
				for (int i = 0; i < n; i++) {
					TableRowView rv = getRow(i);
					if (rv.multiRowCells == true) {
						int ncells = rv.getViewCount();
						for (int j = 0; j < ncells; j++) {
							TableCellView v = 
								(TableCellView) rv.getView(j);
							int nrows = v.getRowSpan();
							if (nrows > 1) {
								int spanNeeded = (int) v.getPreferredSpan(axis);
								adjustMultiRowSpan(spanNeeded, nrows, i);
							}
						}
					}
				}
			} else {
				adjustments = null;
			}
		}

		/**
		 * Fixup preferences to accomodate a multi-row table cell if not already
		 * covered by existing preferences. This is a no-op if not all of the
		 * rows needed (to do this check/fixup) have arrived yet.
		 */
		void adjustMultiRowSpan(int spanNeeded, int nrows, int rowIndex) {
			if ((rowIndex + nrows) > getCount()) {
				// rows are missing (could be a bad rowspan specification)
				// or not all the rows have arrived. Do the best we can with
				// the current set of rows.
				nrows = getCount() - rowIndex;
				if (nrows < 1) {
					return;
				}
			}
			int span = 0;
			for (int i = 0; i < nrows; i++) {
				TableRowView rv = getRow(rowIndex + i);
				span += rv.getPreferredSpan(Y_AXIS);
			}
			if (spanNeeded > span) {
				int adjust = (spanNeeded - span);
				int rowAdjust = adjust / nrows;
				int firstAdjust = rowAdjust + (adjust - (rowAdjust * nrows));
				TableRowView rv = getRow(rowIndex);
				adjustments[rowIndex] = Math.max(adjustments[rowIndex],
						firstAdjust);
				for (int i = 1; i < nrows; i++) {
					adjustments[rowIndex + i] = Math.max(adjustments[rowIndex
							+ i], rowAdjust);
				}
			}
		}

		void setLayoutArrays(int[] offsets, int[] spans) {
			this.offsets = offsets;
			this.spans = spans;
		}

		// --- RequirementIterator methods -------------------

		public void setOffset(int offs) {
			TableRowView rv = getRow(row);
			if (rv != null) {
				offsets[rv.viewIndex] = offs;
			}
		}

		public int getOffset() {
			TableRowView rv = getRow(row);
			if (rv != null) {
				return offsets[rv.viewIndex];
			}
			return 0;
		}

		public void setSpan(int span) {
			TableRowView rv = getRow(row);
			if (rv != null) {
				spans[rv.viewIndex] = span;
			}
		}

		public int getSpan() {
			TableRowView rv = getRow(row);
			if (rv != null) {
				return spans[rv.viewIndex];
			}
			return 0;
		}

		public int getCount() {
			return rows.size();
		}

		public void setIndex(int i) {
			row = i;
		}

		public float getMinimumSpan(float parentSpan) {
			return getPreferredSpan(parentSpan);
		}

		public float getPreferredSpan(float parentSpan) {
			TableRowView rv = getRow(row);
			if (rv != null) {
				int adjust = (adjustments != null) ? adjustments[row] : 0;
				return rv.getPreferredSpan(TableView.this.getAxis()) + adjust;
			}
			return 0;
		}

		public float getMaximumSpan(float parentSpan) {
			return getPreferredSpan(parentSpan);
		}

		public float getBorderWidth() {
		    return borderWidth;        
		}                              

		public float getLeadingCollapseSpan() {
			return cellSpacing;
		}

		public float getTrailingCollapseSpan() {
			return cellSpacing;
		}

		public int getAdjustmentWeight() {
			return 0;
		}
	} //RowIterator

    /**
     * View of a row in a row-centric table.
     */
    public class TableRowView extends BoxView {

		/** columns filled by multi-column or multi-row cells */
		BitSet fillColumns, vmergeRestartColumns;

		/**
		 * The row index within the overall grid
		 */
		int rowIndex;

		/**
		 * The view index (for row index to view index conversion). This is set
		 * by the updateGrid method.
		 */
		int viewIndex;

		/**
		 * Does this table row have cells that span multiple rows?
		 */
		boolean multiRowCells;

		/**
		 * Constructs a TableView for the given element.
		 * 
		 * @param elem
		 *            the element that this view is responsible for
		 */
		public TableRowView(Element elem) {
			super(elem, View.X_AXIS);
			fillColumns = new BitSet();
			vmergeRestartColumns = new BitSet();
		}

		void clearFilledColumns() {
			fillColumns.and(EMPTY);
		}

		void fillColumn(int col) {
			fillColumns.set(col);
		}

		boolean isFilled(int col) {
			return fillColumns.get(col);
		}

		void clearVMergeRestartColumns() {
			vmergeRestartColumns.and(EMPTY);
		}
		
		void vmergeRestartColumn(int col) {
			vmergeRestartColumns.set(col);
		}
		
		boolean isVMergeRestartColumn(int col) {
			return vmergeRestartColumns.get(col);
		}
		
		View findVMergeRestartCell(int col) {
			View theCell = null;
			
			if (isVMergeRestartColumn(col)) {
				int n = getViewCount();
				
				int cellColumnStart = 0;
				for (int i=0; i < n && theCell == null; i++) {
					theCell = getView(i);
					int colSpan = getColumnGridSpan(theCell);
					int cellColumnEnd = cellColumnStart + colSpan - 1;
					if (cellColumnStart <= col && col <= cellColumnEnd) {
						;//return theCell
					} else {
						theCell = null;
						cellColumnStart = cellColumnEnd + 1;
					}					
				}
			}
			
			return theCell;
		}
		
		View findViewAtPoint(int x, int y, Rectangle alloc) {
			int n = getViewCount();
			for (int i = 0; i < n; i++) {
				Shape childAlloc = getChildAllocation(i, alloc);
				if (childAlloc != null && childAlloc.contains(x, y)) {
					childAllocation(i, alloc);
					return getView(i);
				}
			}
			return null;
		}

		/**
		 * This is called by a child to indicate its preferred span has changed.
		 * This is implemented to execute the superclass behavior and well as
		 * try to determine if a row with a multi-row cell hangs across this
		 * row. If a multi-row cell covers this row it also needs to propagate a
		 * preferenceChanged so that it will recalculate the multi-row cell.
		 * 
		 * @param child
		 *            the child view
		 * @param width
		 *            true if the width preference should change
		 * @param height
		 *            true if the height preference should change
		 */
		public void preferenceChanged(View child, boolean width, boolean height) {
			super.preferenceChanged(child, width, height);
			if (TableView.this.multiRowCells && height) {
				for (int i = rowIndex - 1; i >= 0; i--) {
					TableRowView rv = TableView.this.getRow(i);
					if (rv.multiRowCells) {
						rv.preferenceChanged(null, false, true);
						break;
					}
				}
			}
		}

		// The major axis requirements for a row are dictated by the column
		// requirements. These methods use the value calculated by
		// TableView.
		protected SizeRequirements calculateMajorAxisRequirements(int axis,
				SizeRequirements r) {
			SizeRequirements req = new SizeRequirements();
			req.minimum = totalColumnRequirements.minimum;
			req.maximum = totalColumnRequirements.maximum;
			req.preferred = totalColumnRequirements.preferred;
			req.alignment = 0f;
			return req;
		}

		public float getMinimumSpan(int axis) {
			float value;

			if (axis == View.X_AXIS) {
				value = totalColumnRequirements.minimum + getLeftInset()
						+ getRightInset();
			} else {
				value = super.getMinimumSpan(axis);
			}
			return value;
		}

		public float getMaximumSpan(int axis) {
			float value;

			if (axis == View.X_AXIS) {
				// We're flexible.
				value = (float) Integer.MAX_VALUE;
			} else {
				value = super.getMaximumSpan(axis);
			}
			return value;
		}

		public float getPreferredSpan(int axis) {
			float value;

			if (axis == View.X_AXIS) {
				value = totalColumnRequirements.preferred + getLeftInset()
						+ getRightInset();
			} else {
				value = super.getPreferredSpan(axis);
			}
			return value;
		}

		/**
		 * Change the child views. This is implemented to provide the superclass
		 * behavior and invalidate the grid so that rows and columns will be
		 * recalculated.
		 */
		public void replace(int offset, int length, View[] views) {
			super.replace(offset, length, views);
			invalidateGrid();
		}

		/**
		 * Calculate the height requirements of the table row. The requirements
		 * of multi-row cells are not considered for this calculation. The table
		 * itself will check and adjust the row requirements for all the rows
		 * that have multi-row cells spanning them. This method updates the
		 * multi-row flag that indicates that this row and rows below need
		 * additional consideration.
		 */
		protected SizeRequirements calculateMinorAxisRequirements(int axis,
				SizeRequirements r) {
			// return super.calculateMinorAxisRequirements(axis, r);
			long min = 0;
			long pref = 0;
			long max = 0;
			multiRowCells = false;
			int n = getViewCount();
			for (int i = 0; i < n; i++) {
				TableCellView v = (TableCellView) getView(i);
				int nrows = v.getRowSpan();
				if (nrows > 1) {
					multiRowCells = true;
					max = Math.max((int) v.getMaximumSpan(axis), max);
				} else {
					min = Math.max((int) v.getMinimumSpan(axis), min);
					pref = Math.max((int) v.getPreferredSpan(axis), pref);
					max = Math.max((int) v.getMaximumSpan(axis), max);
				}
			}

			if (r == null) {
				r = new SizeRequirements();
				r.alignment = 0.5f;
			}
			r.preferred = (int) pref;
			r.minimum = (int) min;
			r.maximum = (int) max;
			return r;
		}

		/**
		 * Perform layout for the major axis of the box (i.e. the axis that it
		 * represents). The results of the layout should be placed in the given
		 * arrays which represent the allocations to the children along the
		 * major axis.
		 * <p>
		 * This is re-implemented to give each child the span of the column
		 * width for the table, and to give cells that span multiple columns the
		 * multi-column span.
		 * 
		 * @param targetSpan
		 *            the total span given to the view, which whould be used to
		 *            layout the children
		 * @param axis
		 *            the axis being layed out
		 * @param offsets
		 *            the offsets from the origin of the view for each of the
		 *            child views; this is a return value and is filled in by
		 *            the implementation of this method
		 * @param spans
		 *            the span of each child view; this is a return value and is
		 *            filled in by the implementation of this method
		 * @return the offset and span for each child view in the offsets and
		 *         spans parameters
		 */
		protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets,
				int[] spans) {
			int col = 0;
			int ncells = getViewCount();
			for (int cell = 0; cell < ncells; cell++) {
				TableCellView cv = (TableCellView) getView(cell);
				if (cv.isHidden()) {
					continue;
				}
				
				for (; isFilled(col); col++)
					; // advance to a free column
				int colSpan = getColumnGridSpan(cv);
				spans[cell] = columnSpans[col];
				offsets[cell] = columnOffsets[col];
				if (colSpan > 1) {
					int n = columnSpans.length;
					for (int j = 1; j < colSpan; j++) {
						// Because the table may be only partially formed, some
						// of the columns may not yet exist. Therefore we check
						// the bounds.
						if ((col + j) < n) {
							spans[cell] += columnSpans[col + j];
							spans[cell] += cellSpacing;
						}
					}
					col += colSpan - 1;
				}
				col++;
			}
		}

		/**
		 * Perform layout for the minor axis of the box (i.e. the axis
		 * orthoginal to the axis that it represents). The results of the layout
		 * should be placed in the given arrays which represent the allocations
		 * to the children along the minor axis. This is called by the
		 * superclass whenever the layout needs to be updated along the minor
		 * axis.
		 * <p>
		 * This is implemented to delegate to the superclass, then adjust the
		 * span for any cell that spans multiple rows.
		 * 
		 * @param targetSpan
		 *            the total span given to the view, which whould be used to
		 *            layout the children
		 * @param axis
		 *            the axis being layed out
		 * @param offsets
		 *            the offsets from the origin of the view for each of the
		 *            child views; this is a return value and is filled in by
		 *            the implementation of this method
		 * @param spans
		 *            the span of each child view; this is a return value and is
		 *            filled in by the implementation of this method
		 * @return the offset and span for each child view in the offsets and
		 *         spans parameters
		 */
		protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets,
				int[] spans) {
			super.layoutMinorAxis(targetSpan, axis, offsets, spans);
			int col = 0;
			int ncells = getViewCount();
			for (int cell = 0; cell < ncells; cell++, col++) {
				TableCellView cv = (TableCellView) getView(cell);
				if (cv.isHidden()) {
					col--;
				} else {
					for (; isFilled(col); col++)
						; // advance to a free column
					int colSpan = getColumnGridSpan(cv);
					int rowSpan = cv.getRowSpan();
					if (rowSpan > 1) {
						int row0 = rowIndex;
						int row1 = Math.min(rowIndex + rowSpan - 1,
							getRowCount() - 1);
						spans[cell] = getMultiRowSpan(row0, row1);
					}
					if (colSpan > 1) {
						col += colSpan - 1;
					}
				}
			}
		}

		/**
		 * Determines the resizability of the view along the given axis. A value
		 * of 0 or less is not resizable.
		 * 
		 * @param axis
		 *            may be either View.X_AXIS or View.Y_AXIS
		 * @return the resize weight
		 * @exception IllegalArgumentException
		 *                for an invalid axis
		 */
		public int getResizeWeight(int axis) {
			return 1;
		}

		/**
		 * Fetches the child view that represents the given position in the
		 * model. This is implemented to walk through the children looking for a
		 * range that contains the given position. In this view the children do
		 * not necessarily have a one to one mapping with the child elements.
		 * 
		 * @param pos
		 *            the search position >= 0
		 * @param a
		 *            the allocation to the table on entry, and the allocation
		 *            of the view containing the position on exit
		 * @return the view representing the given position, or null if there
		 *         isn't one
		 */
		protected View getViewAtPosition(int pos, Rectangle a) {
			int n = getViewCount();
			for (int i = 0; i < n; i++) {
				View v = getView(i);
				int p0 = v.getStartOffset();
				int p1 = v.getEndOffset();
				if ((pos >= p0) && (pos < p1)) {
					// it's in this view.
					if (a != null) {
						childAllocation(i, a);
					}
					return v;
				}
			}
			if (pos == getEndOffset()) {
				View v = getView(n - 1);
				if (a != null) {
					this.childAllocation(n - 1, a);
				}
				return v;
			}
			return null;
		}
	} // RowView class
    
} //TableView class



























