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
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.List;

import javax.swing.event.DocumentEvent;
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
    			Polygon border = getBorderOutline(allocation);
    			Color orig = g.getColor();
    			g.setColor(Color.BLUE);
    			g.drawPolygon(border);
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
	 * <1>------------------------------<2> | Left justified paragraph line1 | |
	 * <4>---------------------<3> | line 2 | <6>-----<5>
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
		Rectangle start = 
			SwingUtil.getBounds(
				modelToView(row.getStartOffset(), allocation, Position.Bias.Forward));
		int leftMost = start.x;
		Rectangle end = 
			SwingUtil.getBounds(
				modelToView(row.getEndOffset(), allocation, Position.Bias.Backward));
		int rightMost = end.x;
		
		for (int i=1; i < rowsList.size() - 1; i++) {
			row = (View) rowsList.get(i);
			start = 
				SwingUtil.getBounds(
					modelToView(row.getStartOffset(), allocation, Position.Bias.Forward));
			leftMost = Math.min(leftMost, start.x);
			end = 
				SwingUtil.getBounds(
					modelToView(row.getEndOffset(), allocation, Position.Bias.Backward));
			rightMost = Math.max(rightMost, end.x);
		}
		
		if (rowsList.size() > 1) {
			//the last row is processed separately
			row = (View) rowsList.get(rowsList.size() - 1);
			start = 
				SwingUtil.getBounds(
					modelToView(row.getStartOffset(), allocation, Position.Bias.Forward));
			end = 
				SwingUtil.getBounds(
					modelToView(row.getEndOffset(), allocation, Position.Bias.Backward));
			
			int alignment = StyleConstants.getAlignment(row.getAttributes());
			if (alignment == StyleConstants.ALIGN_LEFT) {
				//Construct a border with 6 vertices
				leftMost = Math.min(leftMost, start.x);
				rightMost = Math.max(rightMost, end.x);
				
				leftMost -= getLeftInset();
				rightMost += (getRightInset() - 1);
				
				theOutline.addPoint(leftMost, alloc.y); //vertice #1
				theOutline.addPoint(rightMost, alloc.y);//vertice #2
				theOutline.addPoint(rightMost, end.y); //vertice #3
				theOutline.addPoint(end.x + getRightInset() - 1, end.y); //vertice #4
				theOutline.addPoint(
					end.x + getRightInset() - 1, 
					end.y + end.height + getBottomInset()); //vertice #5
				theOutline.addPoint(
					leftMost, 
					end.y + end.height + getBottomInset()); //vertice #6
				
			} else {
				//Border is simply a rectangle
				leftMost = Math.min(leftMost, start.x);
				rightMost = Math.max(rightMost, end.x);
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
    
}// SdtBlockView class



















