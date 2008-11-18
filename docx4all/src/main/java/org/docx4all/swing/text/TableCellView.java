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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.docx4all.swing.LineBorderSegment;
import org.docx4all.swing.LineBorderSegment.Direction;
import org.docx4all.xml.type.CTHeight;
import org.docx4all.xml.type.CTTblCellMar;
import org.docx4all.xml.type.TblBorders;
import org.docx4all.xml.type.TblWidth;
import org.docx4all.xml.type.TcBorders;
import org.docx4j.wml.STHeightRule;
import org.docx4j.wml.STVerticalJc;
import org.docx4j.wml.TcPrInner;

/**
 * @author Jojada Tirtowidjojo - 22/09/2008
 */
public class TableCellView extends BoxView {
	private int rowSpan = 1;
	
	public TableCellView(Element elem) {
		super(elem, Y_AXIS);
	}

	public int getRowSpan() {
		return rowSpan;
	}
	
	public void setRowSpan(int span) {
		this.rowSpan = span;
	}
	
    /**
     * Gets the alignment.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the alignment
     */
    public float getAlignment(int axis) {
		switch (axis) {
		case View.X_AXIS:
			return 0;
		case View.Y_AXIS:
			if (getViewCount() == 0) {
				return 0;
			}
			float span = getPreferredSpan(View.Y_AXIS);
			View v = getView(0);
			float above = v.getPreferredSpan(View.Y_AXIS);
			float a = (((int) span) != 0) ? (above * v
					.getAlignment(View.Y_AXIS))
					/ span : 0;
			return a;
		default:
			throw new IllegalArgumentException("Invalid axis: " + axis);
		}
	}
    
    /**
     * Gets the resize weight.  A value of 0 or less is not resizable.
	 * 
	 * @param axis
	 *            may be either X_AXIS or Y_AXIS
	 * @return the weight
	 * @exception IllegalArgumentException
	 *                for an invalid axis
	 */
	public int getResizeWeight(int axis) {
		if (isHidden()) {
			return 0;
		}
		
		switch (axis) {
		case View.X_AXIS:
			return 1;
		case View.Y_AXIS:
			return 0;
		default:
			throw new IllegalArgumentException("Invalid axis: " + axis);
		}
	}
    
    /**
     * Establishes the parent view for this view.  This is
     * guaranteed to be called before any other methods if the
     * parent view is functioning properly.
     * <p> 
     * This is implemented
     * to forward to the superclass as well as call the
     * {@link #setPropertiesFromAttributes()}.  
     * The call is made at this time to ensure
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
    
    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		super.changedUpdate(changes, a, f);
		int pos = changes.getOffset();
		if (pos <= getStartOffset()
				&& getEndOffset() <= (pos + changes.getLength())) {
			setPropertiesFromAttributes();
		}
	}

    public void paint(Graphics g, Shape allocation) {
    	if (isHidden()) {
    		return;
    	}
    	
    	Rectangle alloc = (Rectangle) allocation;
    	
    	int x = alloc.x;
    	int y = alloc.y;
    	int w = alloc.width;
    	int h = alloc.height;
    	
    	AttributeSet attr = getAttributes();
    	
    	if (attr.isDefined(StyleConstants.Background)) {
    		Color orig = g.getColor();
        	Color bg = StyleConstants.getBackground(attr);
    		g.setColor(bg);
            g.fillRect(x, y, w, h);
            g.setColor(orig);
    	}
    	
    	TcBorders tcBorders = WordMLStyleConstants.getTcBorders(attr);
    	if (tcBorders != null) {
    		tcBorders.setAutoColor(getContainer().getForeground());
    		
    		LineBorderSegment line = 
    			tcBorders.getLineBorderSegment(LineBorderSegment.Side.LEFT);
    		if (line != null) {
    			line.paint(g, x, y, Direction.DOWN, h);
    		}
    		line = tcBorders.getLineBorderSegment(LineBorderSegment.Side.TOP);
    		if (line != null) {
    			line.paint(g, x, y, Direction.RIGHT, w);
    		}
    		line = tcBorders.getLineBorderSegment(LineBorderSegment.Side.RIGHT);
    		if (line != null) {
    			line.paint(g, x + w, y, Direction.DOWN, h);
    		}
    		line = tcBorders.getLineBorderSegment(LineBorderSegment.Side.BOTTOM);
    		if (line != null) {
    			line.paint(g, x, y + h, Direction.RIGHT, w);
    		}
    	} else {
    		
    		TblBorders tblBorders = WordMLStyleConstants.getTblBorders(attr);
    		if (tblBorders != null) {
    			tblBorders.setAutoColor(getContainer().getForeground());
    			
        		LineBorderSegment line = 
        			tblBorders.getLineBorderSegment(LineBorderSegment.Side.LEFT);
        		if (line != null) {
        			line.paint(g, x, y, Direction.DOWN, h);
        		}
        		line = tblBorders.getLineBorderSegment(LineBorderSegment.Side.TOP);
        		if (line != null) {
        			line.paint(g, x, y, Direction.RIGHT, w);
        		}
        		line = tblBorders.getLineBorderSegment(LineBorderSegment.Side.RIGHT);
        		if (line != null) {
        			line.paint(g, x + w, y, Direction.DOWN, h);
        		}
        		line = tblBorders.getLineBorderSegment(LineBorderSegment.Side.BOTTOM);
        		if (line != null) {
        			line.paint(g, x, y + h, Direction.RIGHT, w);
        		}
    		}
    		
    	}
    	
    	super.paint(g, alloc);
    }
    
    public boolean isHidden() {
		TcPrInner.VMerge vmerge = WordMLStyleConstants.getTcVMerge(getAttributes());
		return (vmerge != null && !"restart".equalsIgnoreCase(vmerge.getVal()));
    }
    
    protected LineBorderSegment getLineBorderSegment(
    	TblBorders tblBorders, TcBorders tcBorders, LineBorderSegment.Side side) {
    	
    	LineBorderSegment tcLine = tcBorders.getLineBorderSegment(side);
    	
    	LineBorderSegment tblLine =tblBorders.getLineBorderSegment(side);
    	
    	return null;
    }
    
    /**
     * Update any cached values that come from attributes.
     */
    protected void setPropertiesFromAttributes() {
    	if (isHidden()) {
    		return;
    	}
    	
    	AttributeSet attr = getAttributes();
    	setParagraphInsets(attr);
    }

    protected void setParagraphInsets(AttributeSet attr) {
		int ignoreThisParam = 0;
		
    	short leftInset = 0;
    	TblWidth w = WordMLStyleConstants.getTcLeftMargin(attr);
    	if (w != null && w.getType() == TblWidth.Type.DXA) { 
    		leftInset = (short) w.getWidthInPixel(ignoreThisParam);
    	} else {
    		leftInset = 
    			(short) CTTblCellMar.DEFAULT_VALUE.getLeft().getWidthInPixel(ignoreThisParam);
    	}
    	
    	short rightInset = 0;
    	w = WordMLStyleConstants.getTcRightMargin(attr);
    	if (w != null && w.getType() == TblWidth.Type.DXA) { 
    		rightInset = (short) w.getWidthInPixel(ignoreThisParam);
    	} else {
    		rightInset = 
    			(short) CTTblCellMar.DEFAULT_VALUE.getRight().getWidthInPixel(ignoreThisParam);
    	}
    	
    	short topInset = 0;
    	w = WordMLStyleConstants.getTcTopMargin(attr);
    	if (w != null && w.getType() == TblWidth.Type.DXA) { 
    		topInset = (short) w.getWidthInPixel(ignoreThisParam);
    	} else {
    		topInset = 
    			(short) CTTblCellMar.DEFAULT_VALUE.getTop().getWidthInPixel(ignoreThisParam);
    	}
    	
    	short bottomInset = 0;
    	w = WordMLStyleConstants.getTcBottomMargin(attr);
    	if (w != null && w.getType() == TblWidth.Type.DXA) { 
    		bottomInset = (short) w.getWidthInPixel(ignoreThisParam);
    	} else {
    		bottomInset = 
    			(short) CTTblCellMar.DEFAULT_VALUE.getBottom().getWidthInPixel(ignoreThisParam);
    	}

    	setInsets(topInset, leftInset, bottomInset, rightInset);
    }
    
	/**
	 * Perform layout for the major axis of the box (i.e. the axis that it
	 * represents). The results of the layout should be placed in the given
	 * arrays which represent the allocations to the children along the major
	 * axis. This is called by the superclass to recalculate the positions of
	 * the child views when the layout might have changed.
	 * <p>
	 * This is implemented to delegate to the superclass to tile the children.
	 * If the target span is greater than was needed, the offsets are adjusted
	 * to align the children (i.e. position according to 
	 * WordMLStyleConstants.TcVAlignAttribute attribute).
	 * 
	 * @param targetSpan
	 *            the total span given to the view, which should be used to
	 *            layout the children
	 * @param axis
	 *            the axis being laid out
	 * @param offsets
	 *            the offsets from the origin of the view for each of the child
	 *            views; this is a return value and is filled in by the
	 *            implementation of this method
	 * @param spans
	 *            the span of each child view; this is a return value and is
	 *            filled in by the implementation of this method
	 * @return the offset and span for each child view in the offsets and spans
	 *         parameters
	 */
	protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets,
			int[] spans) {
		if (isHidden()) {
			return;
		}
		
		super.layoutMajorAxis(targetSpan, axis, offsets, spans);

		int totalChildSpan = 0;
		int n = spans.length;
		for (int i = 0; i < n; i++) {
			totalChildSpan += spans[i];
		}

		//calculate offset adjustment needed to align children
		int adjust = 0;
		if (totalChildSpan < targetSpan) {
			STVerticalJc valign = WordMLStyleConstants.getTcVAlign(getAttributes());
			if (valign == null 
				|| valign == STVerticalJc.BOTH) {
				//According WordprocessingML spec,
				//STVerticalJc.BOTH is an invalid value in Table Cell.
				//Therefore, consider it as undefined.
			} else if (valign == STVerticalJc.CENTER) {
				adjust = (targetSpan - totalChildSpan) / 2;
			} else if (valign == STVerticalJc.BOTTOM) {
				adjust = targetSpan - totalChildSpan;
			}
		}

		if (adjust != 0) {
			for (int i = 0; i < n; i++) {
				offsets[i] += adjust;
			}
		}
	}

	/**
	 * Calculate the requirements needed along the major axis. This is called by
	 * the superclass whenever the requirements need to be updated (i.e. a
	 * preferenceChanged was messaged through this view).
	 * <p>
	 * This is implemented to delegate to the superclass, but indicate the
	 * maximum size is very large (i.e. the cell is willing to expend to occupy
	 * the full height of the row).
	 * 
	 * @param axis
	 *            the axis being layed out.
	 * @param r
	 *            the requirements to fill in. If null, a new one should be
	 *            allocated.
	 */
	protected SizeRequirements calculateMajorAxisRequirements(int axis,
			SizeRequirements r) {
		if (r == null) {
			r = new SizeRequirements();
		}
		
		if (isHidden()) {
			return r;
		}
		
		CTHeight height = 
			WordMLStyleConstants.getTrHeight(getElement().getParentElement().getAttributes());
		if (height == null 
			//|| height.getRule() == null
			|| height.getRule() == STHeightRule.AUTO) {
			//TODO: WordprocessingML specification tells that
			//if hRule attribute is omitted (ie: height.getRule() == null)
			//then its value shall be assumed to be auto.
			//However, Ms-Word2007 does not do that.
			//Therefore, let Docx4all imitate Ms-Word2007 until
			//we decide otherwise.
			r = super.calculateMajorAxisRequirements(axis, r);
			r.maximum = Integer.MAX_VALUE;
			
		} else {
			//Rule is STHeightRule.EXACT or STHeightRule.AT_LEAST.
			//We want to gracefully handle a case where
			//STHeightRule.EXACT is defined but the specified
			//height is not big enough.
			//Therefore, we treat STHeightRule.EXACT the same
			//as how we treat STHeightRule.AT_LEAST.
			r.minimum = r.preferred = height.getValueInPixels();
			r.maximum = Integer.MAX_VALUE;
			
			SizeRequirements constraintByParent = 
				super.calculateMajorAxisRequirements(axis, null);
			// Offset by the margins so that pref/min/max return the
			// right value.
			int margin = getTopInset() + getBottomInset();
			r.minimum -= margin;
			r.preferred -= margin;
			fitIntoConstrain(r, constraintByParent);
		}
		return r;
	}

    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets,
			int[] spans) {
		int n = getViewCount();
		for (int i = 0; i < n; i++) {
			View v = getView(i);
			int min = (int) v.getMinimumSpan(axis);
			int max;

	    	TblWidth w = WordMLStyleConstants.getTcWidth(getAttributes());
			// check for percentage span
	    	if (w != null && w.getType() == TblWidth.Type.PCT) {
	    		min = Math.max(w.getWidthInPixel(targetSpan), min);
	    		max = min;	    		
	    	} else {
	    		max = (int) v.getMaximumSpan(axis);
	    	}
	    	
			// assign the offset and span for the child
			if (max < targetSpan) {
				// can't make the child this wide, align it
				float align = v.getAlignment(axis);
				offsets[i] = (int) ((targetSpan - max) * align);
				spans[i] = max;
			} else {
				// make it the target width, or as small as it can get.
				offsets[i] = 0;
				spans[i] = Math.max(min, targetSpan);
			}
		}
	}

    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
    	if (r == null) {
    	    r = new SizeRequirements();
    	}

    	if (isHidden()) {
    		return r;
		}
		
    	TblWidth w = WordMLStyleConstants.getTcWidth(getAttributes());
    	if (w == null
    		|| w.getType() == TblWidth.Type.AUTO
    		|| w.getType() == TblWidth.Type.PCT) {
    		r = super.calculateMinorAxisRequirements(axis, r);
    		
    	} else if (w.getType() == TblWidth.Type.NIL) {
    		r.minimum = r.preferred = r.maximum = 0;
    		
    	} else {
    		//TblWidth.Type.DXA
    		int irrelevantParam = 0;
			r.minimum = r.preferred = r.maximum = w.getWidthInPixel(irrelevantParam);
			
			SizeRequirements constraintByParent = 
				super.calculateMinorAxisRequirements(axis, null);
			// Offset by the margins so that pref/min/max return the
			// right value.
			int margin = getLeftInset() + getRightInset();
			r.minimum -= margin;
			r.preferred -= margin;
			r.maximum -= margin;
			fitIntoConstrain(r, constraintByParent);
    	}
    	
        //for the cell the minimum should be derived from the child views 
        int n = getViewCount();
        int min = 0;
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            min = Math.max((int) v.getMinimumSpan(axis), min);                
        }            
        r.minimum = Math.min(r.minimum, min);
        
        return r;
    }
        
    private static void fitIntoConstrain(SizeRequirements want, SizeRequirements constrain) {
		if (constrain.minimum > want.minimum) {
			want.minimum = want.preferred = constrain.minimum;
			want.maximum = Math.max(want.maximum, constrain.maximum);
		}
	}

}// TableCellView class


























