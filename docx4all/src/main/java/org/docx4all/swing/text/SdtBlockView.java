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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import org.docx4all.util.SwingUtil;

/**
 *	@author Jojada Tirtowidjojo - 30/04/2008
 */
public class SdtBlockView extends BoxView {
	private final static short INSET_LEFT = 15;
	private final static short INSET_TOP = 15;
	private final static short INSET_RIGHT = 15;
	private final static short INSET_BOTTOM = 15;
	
	private boolean isBorderVisible;
	
	public SdtBlockView(Element elem) {
		super(elem, View.Y_AXIS);
		setInsets(INSET_TOP, INSET_LEFT, INSET_BOTTOM, INSET_RIGHT);
		isBorderVisible = false;
	}
	
	public void setBorderVisible(boolean b) {
		isBorderVisible = b;
	}
	
	public boolean isBorderVisible() {
		return isBorderVisible;
	}
	
    public void paint(Graphics g, Shape allocation) {
    	super.paint(g, allocation);
    	
    	if (isBorderVisible()) {
    		try {
    			Shape border = getBorderOutline(allocation);
    			Color orig = g.getColor();
    			g.setColor(Color.BLUE);
    			if (border instanceof Polygon) {
    				g.drawPolygon((Polygon) border);
    			} else {
    				Rectangle rect = SwingUtil.getBounds(border);
    				g.drawRect(rect.x, rect.y, rect.width, rect.height);
    			}
    			g.setColor(orig);
    		} catch (BadLocationException exc) {
    			;//ignore
    		}
    	}
    }
    
    /**
	 * Construct a Polygon that represents the border of this view.
	 * 
	 * The polygon is a union of the border of each paragraph line inside this
	 * view. If a paragraph is left justified, this method aims to construct a
	 * polygon that has six vertices and looks like the following:
	 * <1>------------------------------<2> 
	 *  | Left justified paragraph line1 | 
	 *  |       <4>---------------------<3>
	 *  | line 2 | 
	 *  <6>-----<5>
	 * 
	 * For other types of paragraph alignment, the polygon will be simply a
	 * rectangle.
	 * 
	 * @param allocation
	 * @return
	 * @throws BadLocationException
	 */
    private Polygon getBorderOutline(Shape allocation) throws BadLocationException {
    	Polygon theOutline = new Polygon();
    	
    	Rectangle alloc = SwingUtil.getBounds(allocation);
    	
    	List<View> rowsList = SwingUtil.getParagraphRowViews(this);
    	
    	View row = rowsList.get(0);
    	
    	Rectangle tempRect = getRowBorderOutline(row, allocation);
		int leftMost = tempRect.x;
		int rightMost = tempRect.x + tempRect.width;
		
		for (int i=1; i < rowsList.size() - 1; i++) {
			row = (View) rowsList.get(i);
	    	
			tempRect = getRowBorderOutline(row, allocation);
			leftMost = Math.min(leftMost, tempRect.x);
			rightMost = Math.max(rightMost, tempRect.x + tempRect.width);
		}
		
		if (rowsList.size() > 1) {
			//the last row is processed separately
			row = (View) rowsList.get(rowsList.size() - 1);

			tempRect = getRowBorderOutline(row, allocation);
			int startX = tempRect.x;
			int endX = tempRect.x + tempRect.width;
			int endY = tempRect.y;
			int endHeight = tempRect.height;
			
			int alignment = StyleConstants.getAlignment(row.getAttributes());
			if (alignment == StyleConstants.ALIGN_LEFT) {
				//Construct a border with 6 vertices
				leftMost = Math.min(leftMost, startX);
				rightMost = Math.max(rightMost, endX);
				
				leftMost -= getLeftInset();
				rightMost += (getRightInset() - 1);
				
				theOutline.addPoint(leftMost, alloc.y); //vertice #1
				theOutline.addPoint(rightMost, alloc.y);//vertice #2
				theOutline.addPoint(rightMost, endY); //vertice #3
				theOutline.addPoint(endX + getRightInset() - 1, endY); //vertice #4
				theOutline.addPoint(
					endX + getRightInset() - 1, 
					endY + endHeight + getBottomInset()); //vertice #5
				theOutline.addPoint(
					leftMost, 
					endY + endHeight + getBottomInset()); //vertice #6
				
			} else {
				//Border is simply a rectangle
				leftMost = Math.min(leftMost, startX);
				rightMost = Math.max(rightMost, endX);
				leftMost -= getLeftInset();
				rightMost += (getRightInset() - 1);
				theOutline.addPoint(leftMost, alloc.y);
				theOutline.addPoint(rightMost, alloc.y);
				theOutline.addPoint(rightMost, alloc.y + alloc.height);
				theOutline.addPoint(leftMost, alloc.y + alloc.height);
			}
		} else {
			//Border is simply a rectangle
			leftMost -= getLeftInset();
			rightMost += (getRightInset() - 1);
			theOutline.addPoint(leftMost, alloc.y);
			theOutline.addPoint(rightMost, alloc.y);
			theOutline.addPoint(rightMost, alloc.y + alloc.height);
			theOutline.addPoint(leftMost, alloc.y + alloc.height);
		}
		
		return theOutline;
    }
    
    private Rectangle getRowBorderOutline(View v, Shape allocation)  throws BadLocationException {
    	Rectangle theRect = null;
    	if (v instanceof TableView.TableRowView) {
    		TableView table = (TableView) v.getParent();
    		int idx = getViewIndex(table.getStartOffset(), Position.Bias.Forward);
    		Shape alloc = getChildAllocation(idx, allocation);
    		
    		idx = table.getViewIndex(v.getStartOffset(), Position.Bias.Forward);
    		theRect = table.getChildAllocation(idx, alloc).getBounds();
    	} else {
    		theRect = 
    			SwingUtil.getBounds(
    				modelToView(v.getStartOffset(), allocation, Position.Bias.Forward));
    		Rectangle r = 
    			SwingUtil.getBounds(
    				modelToView(v.getEndOffset(), allocation, Position.Bias.Backward));
    		theRect.width = r.x - theRect.x;
    	}
    	return theRect;
    }
}// SdtBlockView class



















