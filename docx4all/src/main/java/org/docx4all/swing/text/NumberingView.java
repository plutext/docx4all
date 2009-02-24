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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;

import org.apache.log4j.Logger;
import org.docx4all.xml.ElementML;
import org.docx4j.listnumbering.Emulator;
import org.docx4j.listnumbering.Emulator.ResultTriple;
import org.docx4j.wml.PPrBase;

import sun.swing.SwingUtilities2;

/**
 *	@author Jojada Tirtowidjojo - 10/01/2009
 */
public class NumberingView extends View {
	private static Logger log = Logger.getLogger(NumberingView.class);
	
	private final static int DEFAULT_BULLET_WIDTH = 8;
	private final static int DEFAULT_BULLET_HEIGHT = 8;
	private final static int DEFAULT_NUMBERING_GAP = 15;
	
	private ResultTriple numbering;
	private Font font;
	private int firstLineIndent;
	
	public NumberingView(Element impliedPara) {
		super(impliedPara);
		setPropertiesFromAttributes();
	}
	
    public boolean isBullet() {
    	return this.numbering.isBullet();
    }
    
    public float getAlignment(int axis) {
    	float align = super.getAlignment(axis);
    	if (axis == View.Y_AXIS) {
    		FontMetrics fm = getContainer().getFontMetrics(getFont());
    		float h = fm.getHeight();
    		float d = fm.getDescent();
    		align = (h > 0) ? (h - d) / h : 0;
    	}
    	return align;
    }

    public Color getBackground() {
		AttributeSet attr = getAttributes();
		return ((StyledDocument) getDocument()).getBackground(attr);		
	}

    public Color getForeground() {
		AttributeSet attr = getAttributes();
		return ((StyledDocument) getDocument()).getForeground(attr);		
	}

    public Font getFont() {
    	return this.font;
	}
    
    /**
     * Renders a portion of a text style run.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     */
    public void paint(Graphics g, Shape a) {
        Rectangle r = (a instanceof Rectangle) ? (Rectangle)a : a.getBounds();
        
        Rectangle clip = g.getClipBounds();
        boolean intersect = clip.intersects(r);
    	
        if (intersect) {
        	if (this.numbering.isBullet() 
        		&& "Symbol".equalsIgnoreCase(this.numbering.getNumFont())) {
        		drawBullet(g, r.x, r.y, r.width, r.height, getAlignment(Y_AXIS));        		
        	} else {
        		drawNumberingString(g, r.x, r.y, r.width, r.height, getAlignment(Y_AXIS));
        	}
        }
    }
    
    private void drawBullet(
    	Graphics g, int ax, int ay, int aw, int ah, float align) {

    	int x = ax;
		int y = Math.max(ay, ay + (int) (align * ah) - DEFAULT_BULLET_HEIGHT);
		g.fillOval(x, y, DEFAULT_BULLET_WIDTH, DEFAULT_BULLET_HEIGHT);
	}

    private void drawNumberingString(
         Graphics g, int ax, int ay, int aw, int ah, float align) {
        
    	Font font = getFont();
		FontMetrics fm = getContainer().getFontMetrics(font);

    	int x = ax;
   	    int y = ay + fm.getHeight() - fm.getDescent();

       	Font origFont = g.getFont();
       	g.setFont(font);       	
   	    SwingUtilities2.drawString(
   	    	(JComponent) getContainer(), g, this.numbering.getNumString(), x, y);
   	    g.setFont(origFont);
   }
    
   /**
     * Determines the preferred span for this view along an
     * axis. 
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @return   the span the view would like to be rendered into >= 0.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.  
     *           The parent may choose to resize or break the view.
     */
    public float getPreferredSpan(int axis) {
    	int theSpan = 0;
    	
    	if (getContainer() == null) {
    		;//return
    	} else if (axis == View.X_AXIS) {
	    	FontMetrics fm = getContainer().getFontMetrics(getFont());
	    	//theSpan = fm.stringWidth(this.numbering.getNumString());
	       	theSpan = 
	       	    SwingUtilities2.stringWidth(
	       	    	(JComponent) getContainer(), 
	        	    fm, 
	        	    this.numbering.getNumString())
	        	+ DEFAULT_NUMBERING_GAP;
			
			if (Math.abs(this.firstLineIndent) > theSpan) {
				theSpan = Math.abs(this.firstLineIndent);
			}			
		} else {
    		FontMetrics fm = getContainer().getFontMetrics(getFont());
			theSpan = fm.getHeight();
		}   		
    	
    	return theSpan;
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert >= 0
     * @param a   the allocated region to render into
     * @param b   either <code>Position.Bias.Forward</code>
     *                or <code>Position.Bias.Backward</code>
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not represent a
     *   valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
    	return a;
    }
    
    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate >= 0
     * @param y the Y coordinate >= 0
     * @param a the allocated region to render into
     * @param biasReturn either <code>Position.Bias.Forward</code>
     *  or <code>Position.Bias.Backward</code> is returned as the
     *  zero-th element of this array
     * @return the location within the model that best represents the
     *  given point of view >= 0
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] biasReturn) {
    	return getStartOffset();
    }

    /**
     * Determines how attractive a break opportunity in 
     * this view is.  This can be used for determining which
     * view is the most attractive to call <code>breakView</code>
     * on in the process of formatting.  A view that represents
     * text that has whitespace in it might be more attractive
     * than a view that has no whitespace, for example.  The
     * higher the weight, the more attractive the break.  A
     * value equal to or lower than <code>BadBreakWeight</code>
     * should not be considered for a break.  A value greater
     * than or equal to <code>ForcedBreakWeight</code> should
     * be broken.
     * <p>
     * This is implemented to provide the default behavior
     * of returning <code>BadBreakWeight</code> unless the length
     * is greater than the length of the view in which case the 
     * entire view represents the fragment.  Unless a view has
     * been written to support breaking behavior, it is not
     * attractive to try and break the view.  An example of
     * a view that does support breaking is <code>LabelView</code>.
     * An example of a view that uses break weight is 
     * <code>ParagraphView</code>.
     *
     * @param axis may be either <code>View.X_AXIS</code> or
     *		<code>View.Y_AXIS</code>
     * @param pos the potential location of the start of the 
     *   broken view >= 0.  This may be useful for calculating tab
     *   positions
     * @param len specifies the relative length from <em>pos</em>
     *   where a potential break is desired >= 0
     * @return the weight, which should be a value between
     *   ForcedBreakWeight and BadBreakWeight
     * @see LabelView
     * @see ParagraphView
     * @see #BadBreakWeight
     * @see #GoodBreakWeight
     * @see #ExcellentBreakWeight
     * @see #ForcedBreakWeight
     */
    public int getBreakWeight(int axis, float pos, float len) {
    	return BadBreakWeight;
    }

    /**
     * Breaks this view on the given axis at the given length.
     * This is implemented to attempt to break on a whitespace
     * location, and returns a fragment with the whitespace at
     * the end.  If a whitespace location can't be found, the
     * nearest character is used.
     *
     * @param axis may be either View.X_AXIS or View.Y_AXIS
     * @param p0 the location in the model where the
     *  fragment should start it's representation >= 0.
     * @param pos the position along the axis that the
     *  broken view would occupy >= 0.  This may be useful for
     *  things like tab calculations.
     * @param len specifies the distance along the axis
     *  where a potential break is desired >= 0.  
     * @return the fragment of the view that represents the
     *  given span, if the view can be broken.  If the view
     *  doesn't support breaking behavior, the view itself is
     *  returned.
     * @see View#breakView
     */
    public View breakView(int axis, int p0, float pos, float len) {
    	return this;
    }
    
    /**
     * Creates a view that represents a portion of the element.
     * This is potentially useful during formatting operations
     * for taking measurements of fragments of the view.  If 
     * the view doesn't support fragmenting (the default), it 
     * should return itself.  
     * <p>
     * This view does support fragmenting.  It is implemented
     * to return a nested class that shares state in this view 
     * representing only a portion of the view.
     *
     * @param p0 the starting offset >= 0.  This should be a value
     *   greater or equal to the element starting offset and
     *   less than the element ending offset.
     * @param p1 the ending offset > p0.  This should be a value
     *   less than or equal to the elements end offset and
     *   greater than the elements starting offset.
     * @return the view fragment, or itself if the view doesn't
     *   support breaking into fragments
     * @see LabelView
     */
    public View createFragment(int p0, int p1) {
    	return this;
    }

    /**
     * Provides a way to determine the next visually represented model
     * location that one might place a caret.  Some views may not be
     * visible, they might not be in the same order found in the model, or
     * they just might not allow access to some of the locations in the
     * model.
     *
     * @param pos the position to convert >= 0
     * @param a the allocated region to render into
     * @param direction the direction from the current position that can
     *  be thought of as the arrow keys typically found on a keyboard.
     *  This may be SwingConstants.WEST, SwingConstants.EAST, 
     *  SwingConstants.NORTH, or SwingConstants.SOUTH.  
     * @return the location within the model that best represents the next
     *  location visual position.
     * @exception BadLocationException
     * @exception IllegalArgumentException for an invalid direction
     */
    public int getNextVisualPositionFrom(
    	int pos, 
    	Position.Bias b, 
    	Shape a,
		int direction, 
		Position.Bias[] biasRet) throws BadLocationException {

	    switch (direction) {
	    case View.NORTH:
	    case View.SOUTH:
	    case View.EAST:
	    case View.WEST:
	    	return -1;
	    default:
			throw new IllegalArgumentException("Bad direction: " + direction);
		}
	}

	protected void setPropertiesFromAttributes() {
		AttributeSet attr = 
			getElement().getParentElement().getAttributes();
		PPrBase.NumPr numPr = 
			(PPrBase.NumPr) 
				attr.getAttribute(
						WordMLStyleConstants.NumPrAttribute);
		ElementML elemML = 
			(ElementML) 
				attr.getAttribute(
					WordMLStyleConstants.ElementMLAttribute);
		String pStyle = 
			(String)
				attr.getAttribute(
					WordMLStyleConstants.PStyleAttribute);
		String numId = null;
		if (numPr.getNumId() != null) {
			numId = numPr.getNumId().getVal().toString();
		}
		String ilvl = null;
		if (numPr.getIlvl() != null) {
			ilvl = numPr.getIlvl().getVal().toString();
		}
		this.numbering = 
			Emulator.getNumber(
				elemML.getWordprocessingMLPackage(), 
				pStyle, 
				numId, 
				ilvl);
		
		this.firstLineIndent = (int) StyleConstants.getFirstLineIndent(attr);
		
	    if (this.numbering.getNumFont() != null) {
    		MutableAttributeSet temp = new SimpleAttributeSet();
    		StyleConstants.setBold(temp, StyleConstants.isBold(attr));
    		StyleConstants.setItalic(temp, StyleConstants.isItalic(attr));
    		StyleConstants.setFontSize(temp, StyleConstants.getFontSize(attr));
    		StyleConstants.setFontFamily(temp, this.numbering.getNumFont());
    		this.font = ((StyledDocument) getDocument()).getFont(temp);
		} else {
			this.font = ((StyledDocument) getDocument()).getFont(attr);
		}
	    
	}
	
}// NumberingView class





























