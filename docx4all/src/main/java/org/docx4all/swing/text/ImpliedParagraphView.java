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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.FlowView;
import javax.swing.text.JTextComponent;
import javax.swing.text.LabelView;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import javax.swing.text.StyleConstants;
import javax.swing.text.TabExpander;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.TabableView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.apache.log4j.Logger;
import org.docx4all.xml.ElementML;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.PPrBase;

/**
 * This class is based on javax.swing.text.ParagraphView class downloaded from 
 * <a href="http://download.java.net/openjdk/jdk7/">OpenJDK Source Releases</a>
 */
public class ImpliedParagraphView extends FlowView implements TabExpander {
	private static Logger log = Logger.getLogger(ImpliedParagraphView.class);
	
	public ImpliedParagraphView(Element elem) {
		super(elem, View.Y_AXIS);
		setPropertiesFromAttributes();
		tempRect = new Rectangle();
		strategy = new FlowStrategy();
	}

    protected void setParagraphInsets(AttributeSet attr) {
    	//Do not set insets. They are set by parent view.
    	//See: org.docx4all.swing.text.ParagraphView
    }
    
    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
    	r = super.calculateMinorAxisRequirements(axis, r);
    	if (numberingView != null) {
    		r.preferred += numberingView.getPreferredSpan(axis);
    		r.minimum += numberingView.getMinimumSpan(axis);
    	}
    	return r;
    }

    protected void childAllocation(int index, Rectangle alloc) {
    	super.childAllocation(index, alloc);
    }
    
    public Shape getChildAllocation(int index, Shape a) {
    	Shape s = super.getChildAllocation(index, a);
    	return s;
    }
    
	/**
	 * Sets the type of justification.
	 * 
	 * @param j
	 *            one of the following values:
	 *            <ul>
	 *            <li><code>StyleConstants.ALIGN_LEFT</code>
	 *            <li><code>StyleConstants.ALIGN_CENTER</code>
	 *            <li><code>StyleConstants.ALIGN_RIGHT</code>
	 *            </ul>
	 */
	protected void setJustification(int j) {
		justification = j;
	}

	/**
	 * Sets the line spacing.
	 * 
	 * @param ls
	 *            the value is a factor of the line hight
	 */
	protected void setLineSpacing(float ls) {
		lineSpacing = ls;
	}

	/**
	 * Sets the indent on the first line.
	 * 
	 * @param fi
	 *            the value in points
	 */
	protected void setFirstLineIndent(float fi) {
		firstLineIndent = (int) fi;
	}

	/**
	 * Set the cached properties from the attributes.
	 */
	protected void setPropertiesFromAttributes() {
		this.numberingView = null;
		
		AttributeSet attr = getAttributes();
		if (attr != null) {
			setParagraphInsets(attr);
			Integer a = (Integer) attr.getAttribute(StyleConstants.Alignment);
			int alignment;
			if (a == null) {
				Document doc = getElement().getDocument();
				Object o = doc.getProperty(TextAttribute.RUN_DIRECTION);
				if ((o != null) && o.equals(TextAttribute.RUN_DIRECTION_RTL)) {
					alignment = StyleConstants.ALIGN_RIGHT;
				} else {
					alignment = StyleConstants.ALIGN_LEFT;
				}
			} else {
				alignment = a.intValue();
			}
			setJustification(alignment);
			setLineSpacing(StyleConstants.getLineSpacing(attr));
			
	    	//The types of indentation specified in numbering properties
	    	//are firstLine, hanging, left and right indentations.
	    	//FirstLine and hanging indentations are calculated in this 
			//ImpliedParagraphView.class while left and right indentations 
			//are in ParagraphView.class.
	    	PPrBase.Ind indByNumPr = null;
			PPrBase.NumPr numPr = 
				(PPrBase.NumPr) 
					attr.getAttribute(
						WordMLStyleConstants.NumPrAttribute);
			if (numPr != null) {
				this.numberingView = new NumberingView(getElement());				
				String numId = null;
				if (numPr.getNumId() != null
					&& numPr.getNumId().getVal() != null) {
					numId = numPr.getNumId().getVal().toString();
				}
				String ilvl = null;
				if (numPr.getIlvl() != null
					&& numPr.getIlvl().getVal() != null) {
					ilvl = numPr.getIlvl().getVal().toString();
				}
				
				DocumentElement paraE = 
					(DocumentElement) getElement().getParentElement();
				WordprocessingMLPackage p = 
					paraE.getElementML().getWordprocessingMLPackage();
				// Get the Ind value
				NumberingDefinitionsPart ndp = 
					p.getMainDocumentPart().getNumberingDefinitionsPart();
				// Force initialisation of maps
				ndp.getEmulator();
				
				if (numId != null && ilvl != null) {
					indByNumPr = ndp.getInd(numId, ilvl);
				}
			}
			
	    	short firstLineIndent = 0;
	    	if (!attr.isDefined(StyleConstants.FirstLineIndent)
	    		&& indByNumPr != null) {
	    		//FirstLineIndent attribute defined in attr
	    		//takes precedence over that in indByNumPr.
	    		//Therefore, process FirstLineIndent in indByNumPr 
	    		//only when it is not defined in attr.
	    		if (indByNumPr.getHanging() != null) {    		
	    			int hanging = 
	    				StyleSheet.toPixels(indByNumPr.getHanging().intValue());
	    			firstLineIndent = (short) (-1 * hanging);
	    		} else if (indByNumPr.getFirstLine() != null) {
	    			firstLineIndent = 
	    				(short)
	    					StyleSheet.toPixels(
	    						indByNumPr.getFirstLine().intValue());
	    		} else {
	    			firstLineIndent = 
	    				(short) StyleConstants.getFirstLineIndent(attr);
	    		}
	    	} else {
	    		firstLineIndent = 
	    			(short) StyleConstants.getFirstLineIndent(attr);
	    	}
	    	
			setFirstLineIndent(firstLineIndent);
		}
	}

	/**
	 * Returns the number of views that this view is responsible for. The child
	 * views of the paragraph are rows which have been used to arrange pieces of
	 * the <code>View</code>s that represent the child elements. This is the
	 * number of views that have been tiled in two dimensions, and should be
	 * equivalent to the number of child elements to the element this view is
	 * responsible for.
	 * 
	 * @return the number of views that this <code>ParagraphView</code> is
	 *         responsible for
	 */
	protected int getLayoutViewCount() {
		return layoutPool.getViewCount();
	}

	/**
	 * Returns the view at a given <code>index</code>. The child views of the
	 * paragraph are rows which have been used to arrange pieces of the
	 * <code>Views</code> that represent the child elements. This methods
	 * returns the view responsible for the child element index (prior to
	 * breaking). These are the Views that were produced from a factory (to
	 * represent the child elements) and used for layout.
	 * 
	 * @param index
	 *            the <code>index</code> of the desired view
	 * @return the view at <code>index</code>
	 */
	protected View getLayoutView(int index) {
		return layoutPool.getView(index);
	}

	/**
	 * Adjusts the given row if possible to fit within the layout span. By
	 * default this will try to find the highest break weight possible nearest
	 * the end of the row. If a forced break is encountered, the break will be
	 * positioned there.
	 * <p>
	 * This is meant for internal usage, and should not be used directly.
	 * 
	 * @param r
	 *            the row to adjust to the current layout span
	 * @param desiredSpan
	 *            the current layout span >= 0
	 * @param x
	 *            the location r starts at
	 */
	protected void adjustRow(Row r, int desiredSpan, int x) {
	}

	/**
	 * Returns the next visual position for the cursor, in either the east or
	 * west direction. Overridden from <code>CompositeView</code>.
	 * 
	 * @param pos
	 *            position into the model
	 * @param b
	 *            either <code>Position.Bias.Forward</code> or
	 *            <code>Position.Bias.Backward</code>
	 * @param a
	 *            the allocated region to render into
	 * @param direction
	 *            either <code>SwingConstants.NORTH</code> or
	 *            <code>SwingConstants.SOUTH</code>
	 * @param biasRet
	 *            an array containing the bias that were checked in this method
	 * @return the location in the model that represents the next location
	 *         visual position
	 */
	protected int getNextNorthSouthVisualPositionFrom(int pos, Position.Bias b,
			Shape a, int direction, Position.Bias[] biasRet)
			throws BadLocationException {
		int vIndex;
		if (pos == -1) {
			vIndex = (direction == NORTH) ? getViewCount() - 1 : 0;
		} else {
			if (b == Position.Bias.Backward && pos > 0) {
				vIndex = getViewIndexAtPosition(pos - 1);
			} else {
				vIndex = getViewIndexAtPosition(pos);
			}
			if (direction == NORTH) {
				if (vIndex == 0) {
					return -1;
				}
				vIndex--;
			} else if (++vIndex >= getViewCount()) {
				return -1;
			}
		}
		// vIndex gives index of row to look in.
		JTextComponent text = (JTextComponent) getContainer();
		Caret c = text.getCaret();
		Point magicPoint;
		magicPoint = (c != null) ? c.getMagicCaretPosition() : null;
		int x;
		if (magicPoint == null) {
			Shape posBounds;
			try {
				posBounds = text.getUI().modelToView(text, pos, b);
			} catch (BadLocationException exc) {
				posBounds = null;
			}
			if (posBounds == null) {
				x = 0;
			} else {
				x = posBounds.getBounds().x;
			}
		} else {
			x = magicPoint.x;
		}
		return getClosestPositionTo(pos, b, a, direction, biasRet, vIndex, x);
	}

	/**
	 * Returns the closest model position to <code>x</code>.
	 * <code>rowIndex</code> gives the index of the view that corresponds that
	 * should be looked in.
	 * 
	 * @param pos
	 *            position into the model
	 * @param a
	 *            the allocated region to render into
	 * @param direction
	 *            one of the following values:
	 *            <ul>
	 *            <li><code>SwingConstants.NORTH</code> <li><code>
	 *            SwingConstants.SOUTH</code>
	 *            </ul>
	 * @param biasRet
	 *            an array containing the bias that were checked in this method
	 * @param rowIndex
	 *            the index of the view
	 * @param x
	 *            the x coordinate of interest
	 * @return the closest model position to <code>x</code>
	 */
	// NOTE: This will not properly work if ParagraphView contains
	// other ParagraphViews. It won't raise, but this does not message
	// the children views with getNextVisualPositionFrom.
	protected int getClosestPositionTo(int pos, Position.Bias b, Shape a,
			int direction, Position.Bias[] biasRet, int rowIndex, int x)
			throws BadLocationException {
		JTextComponent text = (JTextComponent) getContainer();
		Document doc = getDocument();
		AbstractDocument aDoc = (doc instanceof AbstractDocument) ? (AbstractDocument) doc
				: null;
		View row = getView(rowIndex);
		int lastPos = -1;
		// This could be made better to check backward positions too.
		biasRet[0] = Position.Bias.Forward;
		for (int vc = 0, numViews = row.getViewCount(); vc < numViews; vc++) {
			View v = row.getView(vc);
			int start = v.getStartOffset();
			//boolean ltr = (aDoc != null) ? aDoc.isLeftToRight(start, start + 1)
			//		: true;
			boolean ltr = true;
			if (ltr) {
				lastPos = start;
				for (int end = v.getEndOffset(); lastPos < end; lastPos++) {
					float xx = text.modelToView(lastPos).getBounds().x;
					if (xx >= x) {
						while (++lastPos < end
								&& text.modelToView(lastPos).getBounds().x == xx) {
						}
						return --lastPos;
					}
				}
				lastPos--;
			} else {
				for (lastPos = v.getEndOffset() - 1; lastPos >= start; lastPos--) {
					float xx = text.modelToView(lastPos).getBounds().x;
					if (xx >= x) {
						while (--lastPos >= start
								&& text.modelToView(lastPos).getBounds().x == xx) {
						}
						return ++lastPos;
					}
				}
				lastPos++;
			}
		}
		if (lastPos == -1) {
			return getStartOffset();
		}
		return lastPos;
	}

	/**
	 * Determines in which direction the next view lays. Consider the
	 * <code>View</code> at index n. Typically the <code>View</code>s are layed
	 * out from left to right, so that the <code>View</code> to the EAST will be
	 * at index n + 1, and the <code>View</code> to the WEST will be at index n
	 * - 1. In certain situations, such as with bidirectional text, it is
	 * possible that the <code>View</code> to EAST is not at index n + 1, but
	 * rather at index n - 1, or that the <code>View</code> to the WEST is not
	 * at index n - 1, but index n + 1. In this case this method would return
	 * true, indicating the <code>View</code>s are layed out in descending
	 * order.
	 * <p>
	 * This will return true if the text is layed out right to left at position,
	 * otherwise false.
	 * 
	 * @param position
	 *            position into the model
	 * @param bias
	 *            either <code>Position.Bias.Forward</code> or
	 *            <code>Position.Bias.Backward</code>
	 * @return true if the text is layed out right to left at position,
	 *         otherwise false.
	 */
	protected boolean flipEastAndWestAtEnds(int position, Position.Bias bias) {
		return false;
	}

	// --- FlowView methods ---------------------------------------------

	/**
	 * Fetches the constraining span to flow against for the given child index.
	 * 
	 * @param index
	 *            the index of the view being queried
	 * @return the constraining span for the given view at <code>index</code>
	 * @since 1.3
	 */
	public int getFlowSpan(int index) {
		View child = getView(index);
		int adjust = 0;
		if (child instanceof Row) {
			Row row = (Row) child;
			adjust = row.getLeftInset() + row.getRightInset();
		}
		return (layoutSpan == Integer.MAX_VALUE) ? layoutSpan
				: (layoutSpan - adjust);
	}

	/**
	 * Fetches the location along the flow axis that the flow span will start
	 * at.
	 * 
	 * @param index
	 *            the index of the view being queried
	 * @return the location for the given view at <code>index</code>
	 * @since 1.3
	 */
	public int getFlowStart(int index) {
		View child = getView(index);
		int adjust = 0;
		if (child instanceof Row) {
			Row row = (Row) child;
			adjust = row.getLeftInset();
		}
		return tabBase + adjust;
	}

	/**
	 * Create a <code>View</code> that should be used to hold a a row's worth of
	 * children in a flow.
	 * 
	 * @return the new <code>View</code>
	 * @since 1.3
	 */
	protected View createRow() {
		return new Row(getElement());
	}

	// --- TabExpander methods ------------------------------------------

	/**
	 * Returns the next tab stop position given a reference position. This view
	 * implements the tab coordinate system, and calls
	 * <code>getTabbedSpan</code> on the logical children in the process of
	 * layout to determine the desired span of the children. The logical
	 * children can delegate their tab expansion upward to the paragraph which
	 * knows how to expand tabs. <code>LabelView</code> is an example of a view
	 * that delegates its tab expansion needs upward to the paragraph.
	 * <p>
	 * This is implemented to try and locate a <code>TabSet</code> in the
	 * paragraph element's attribute set. If one can be found, its settings will
	 * be used, otherwise a default expansion will be provided. The base
	 * location for for tab expansion is the left inset from the paragraphs most
	 * recent allocation (which is what the layout of the children is based
	 * upon).
	 * 
	 * @param x
	 *            the X reference position
	 * @param tabOffset
	 *            the position within the text stream that the tab occurred at
	 *            >= 0
	 * @return the trailing end of the tab expansion >= 0
	 * @see TabSet
	 * @see TabStop
	 * @see LabelView
	 */
	public float nextTabStop(float x, int tabOffset) {
		// If the text isn't left justified, offset by 10 pixels!
		if (justification != StyleConstants.ALIGN_LEFT)
			return x + 10.0f;
		x -= tabBase;
		TabSet tabs = getTabSet();
		if (tabs == null) {
			// a tab every 72 pixels.
			return (float) (tabBase + (((int) x / 72 + 1) * 72));
		}
		TabStop tab = tabs.getTabAfter(x + .01f);
		if (tab == null) {
			// no tab, do a default of 5 pixels.
			// Should this cause a wrapping of the line?
			return tabBase + x + 5.0f;
		}
		int alignment = tab.getAlignment();
		int offset;
		switch (alignment) {
		default:
		case TabStop.ALIGN_LEFT:
			// Simple case, left tab.
			return tabBase + tab.getPosition();
		case TabStop.ALIGN_BAR:
			// PENDING: what does this mean?
			return tabBase + tab.getPosition();
		case TabStop.ALIGN_RIGHT:
		case TabStop.ALIGN_CENTER:
			offset = findOffsetToCharactersInString(tabChars, tabOffset + 1);
			break;
		case TabStop.ALIGN_DECIMAL:
			offset = findOffsetToCharactersInString(tabDecimalChars,
					tabOffset + 1);
			break;
		}
		if (offset == -1) {
			offset = getEndOffset();
		}
		float charsSize = getPartialSize(tabOffset + 1, offset);
		switch (alignment) {
		case TabStop.ALIGN_RIGHT:
		case TabStop.ALIGN_DECIMAL:
			// right and decimal are treated the same way, the new
			// position will be the location of the tab less the
			// partialSize.
			return tabBase + Math.max(x, tab.getPosition() - charsSize);
		case TabStop.ALIGN_CENTER:
			// Similar to right, but half the partialSize.
			return tabBase + Math.max(x, tab.getPosition() - charsSize / 2.0f);
		}
		// will never get here!
		return x;
	}

	/**
	 * Gets the <code>Tabset</code> to be used in calculating tabs.
	 * 
	 * @return the <code>TabSet</code>
	 */
	protected TabSet getTabSet() {
		return StyleConstants.getTabSet(getElement().getAttributes());
	}

	/**
	 * Returns the size used by the views between <code>startOffset</code> and
	 * <code>endOffset</code>. This uses <code>getPartialView</code> to
	 * calculate the size if the child view implements the
	 * <code>TabableView</code> interface. If a size is needed and a
	 * <code>View</code> does not implement the <code>TabableView</code>
	 * interface, the <code>preferredSpan</code> will be used.
	 * 
	 * @param startOffset
	 *            the starting document offset >= 0
	 * @param endOffset
	 *            the ending document offset >= startOffset
	 * @return the size >= 0
	 */
	protected float getPartialSize(int startOffset, int endOffset) {
		float size = 0.0f;
		int viewIndex;
		int numViews = getViewCount();
		View view;
		int viewEnd;
		int tempEnd;

		// Have to search layoutPool!
		// PENDING: when ParagraphView supports breaking location
		// into layoutPool will have to change!
		viewIndex = getElement().getElementIndex(startOffset);
		numViews = layoutPool.getViewCount();
		while (startOffset < endOffset && viewIndex < numViews) {
			view = layoutPool.getView(viewIndex++);
			viewEnd = view.getEndOffset();
			tempEnd = Math.min(endOffset, viewEnd);
			if (view instanceof TabableView)
				size += ((TabableView) view).getPartialSpan(startOffset,
						tempEnd);
			else if (startOffset == view.getStartOffset()
					&& tempEnd == view.getEndOffset())
				size += view.getPreferredSpan(View.X_AXIS);
			else
				// PENDING: should we handle this better?
				return 0.0f;
			startOffset = viewEnd;
		}
		return size;
	}

	/**
	 * Finds the next character in the document with a character in
	 * <code>string</code>, starting at offset <code>start</code>. If there are
	 * no characters found, -1 will be returned.
	 * 
	 * @param string
	 *            the string of characters
	 * @param start
	 *            where to start in the model >= 0
	 * @return the document offset, or -1 if no characters found
	 */
	protected int findOffsetToCharactersInString(char[] string, int start) {
		int stringLength = string.length;
		int end = getEndOffset();
		Segment seg = new Segment();
		try {
			getDocument().getText(start, end - start, seg);
		} catch (BadLocationException ble) {
			return -1;
		}
		for (int counter = seg.offset, maxCounter = seg.offset + seg.count; counter < maxCounter; counter++) {
			char currentChar = seg.array[counter];
			for (int subCounter = 0; subCounter < stringLength; subCounter++) {
				if (currentChar == string[subCounter])
					return counter - seg.offset + start;
			}
		}
		// No match.
		return -1;
	}

	/**
	 * Returns where the tabs are calculated from.
	 * 
	 * @return where tabs are calculated from
	 */
	protected float getTabBase() {
		return (float) tabBase;
	}

	// ---- View methods ----------------------------------------------------

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
    	Shape s = super.modelToView(pos, a, b);
    	return s;
    }
    
	/**
	 * Renders using the given rendering surface and area on that surface. This
	 * is implemented to delgate to the superclass after stashing the base
	 * coordinate for tab calculations.
	 * 
	 * @param g
	 *            the rendering surface to use
	 * @param a
	 *            the allocated region to render into
	 * @see View#paint
	 */
	public void paint(Graphics g, Shape a) {
		Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a
				.getBounds();
		tabBase = alloc.x + getLeftInset();		
		super.paint(g, a);
		
		if (firstLineIndent < 0) {
			// line with the negative firstLineIndent value needs
			// special handling
			Shape sh = getChildAllocation(0, a);
			if ((sh != null) && sh.intersects(alloc)) {
				int x = alloc.x + getLeftInset() + firstLineIndent;
				int y = alloc.y + getTopInset();

				Rectangle clip = g.getClipBounds();
				tempRect.x = x + getOffset(X_AXIS, 0);
				tempRect.y = y + getOffset(Y_AXIS, 0);
				tempRect.width = getSpan(X_AXIS, 0) - firstLineIndent;
				tempRect.height = getSpan(Y_AXIS, 0);
				if (tempRect.intersects(clip)) {
					tempRect.x = tempRect.x - firstLineIndent;
					paintChild(g, tempRect, 0);
				}
			}
		} else if (numberingView != null) {
			Shape sh = getChildAllocation(0, a);
			if ((sh != null) && sh.intersects(alloc)) {
				int x = alloc.x + getLeftInset();
				int y = alloc.y + getTopInset();

				Rectangle clip = g.getClipBounds();
				tempRect.x = x + getOffset(X_AXIS, 0);
				tempRect.y = y + getOffset(Y_AXIS, 0);
				tempRect.width = getSpan(X_AXIS, 0);
				tempRect.height = getSpan(Y_AXIS, 0);
				if (tempRect.intersects(clip)) {
					paintChild(g, tempRect, 0);
				}
			}
		}
	}
	
	/**
	 * Determines the desired alignment for this view along an axis. This is
	 * implemented to give the alignment to the center of the first row along
	 * the y axis, and the default along the x axis.
	 * 
	 * @param axis
	 *            may be either <code>View.X_AXIS</code> or
	 *            <code>View.Y_AXIS</code>
	 * @return the desired alignment. This should be a value between 0.0 and 1.0
	 *         inclusive, where 0 indicates alignment at the origin and 1.0
	 *         indicates alignment to the full span away from the origin. An
	 *         alignment of 0.5 would be the center of the view.
	 */
	public float getAlignment(int axis) {
		switch (axis) {
		case Y_AXIS:
			float a = 0.5f;
			if (getViewCount() != 0) {
				int paragraphSpan = (int) getPreferredSpan(View.Y_AXIS);
				View v = getView(0);
				int rowSpan = (int) v.getPreferredSpan(View.Y_AXIS);
				a = (paragraphSpan != 0) ? ((float) (rowSpan / 2))
						/ paragraphSpan : 0;
			}
			return a;
		case X_AXIS:
			return 0.5f;
		default:
			throw new IllegalArgumentException("Invalid axis: " + axis);
		}
	}

	/**
	 * Breaks this view on the given axis at the given length.
	 * <p>
	 * <code>ParagraphView</code> instances are breakable along the
	 * <code>Y_AXIS</code> only, and only if <code>len</code> is after the first
	 * line.
	 * 
	 * @param axis
	 *            may be either <code>View.X_AXIS</code> or
	 *            <code>View.Y_AXIS</code>
	 * @param len
	 *            specifies where a potential break is desired along the given
	 *            axis >= 0
	 * @param a
	 *            the current allocation of the view
	 * @return the fragment of the view that represents the given span, if the
	 *         view can be broken; if the view doesn't support breaking
	 *         behavior, the view itself is returned
	 * @see View#breakView
	 */
	public View breakView(int axis, float len, Shape a) {
		if (axis == View.Y_AXIS) {
			if (a != null) {
				Rectangle alloc = a.getBounds();
				setSize(alloc.width, alloc.height);
			}
			// Determine what row to break on.

			// PENDING(prinz) add break support
			return this;
		}
		return this;
	}

	/**
	 * Gets the break weight for a given location.
	 * <p>
	 * <code>ParagraphView</code> instances are breakable along the
	 * <code>Y_AXIS</code> only, and only if <code>len</code> is after the first
	 * row. If the length is less than one row, a value of
	 * <code>BadBreakWeight</code> is returned.
	 * 
	 * @param axis
	 *            may be either <code>View.X_AXIS</code> or
	 *            <code>View.Y_AXIS</code>
	 * @param len
	 *            specifies where a potential break is desired >= 0
	 * @return a value indicating the attractiveness of breaking here; either
	 *         <code>GoodBreakWeight</code> or <code>BadBreakWeight</code>
	 * @see View#getBreakWeight
	 */
	public int getBreakWeight(int axis, float len) {
		if (axis == View.Y_AXIS) {
			// PENDING(prinz) make this return a reasonable value
			// when paragraph breaking support is re-implemented.
			// If less than one row, bad weight value should be
			// returned.
			// return GoodBreakWeight;
			return BadBreakWeight;
		}
		return BadBreakWeight;
	}

	/**
	 * Gives notification from the document that attributes were changed in a
	 * location that this view is responsible for.
	 * 
	 * @param changes
	 *            the change information from the associated document
	 * @param a
	 *            the current allocation of the view
	 * @param f
	 *            the factory to use to rebuild if the view has children
	 * @see View#changedUpdate
	 */
	public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		// update any property settings stored, and layout should be
		// recomputed
		setPropertiesFromAttributes();
		layoutChanged(X_AXIS);
		layoutChanged(Y_AXIS);
		super.changedUpdate(changes, a, f);
	}

	// --- variables -----------------------------------------------

	private int justification;
	private float lineSpacing;
	/** Indentation for the first line, from the left inset. */
	protected int firstLineIndent = 0;

	/**
	 * Used by the TabExpander functionality to determine where to base the tab
	 * calculations. This is basically the location of the left side of the
	 * paragraph.
	 */
	private int tabBase;
	
    /** used in paint. */
    private Rectangle tempRect;

	private View numberingView;
	
	/**
	 * Used to create an i18n-based layout strategy
	 */
	static Class i18nStrategy;

	/** Used for searching for a tab. */
	static char[] tabChars;
	/** Used for searching for a tab or decimal character. */
	static char[] tabDecimalChars;

	static {
		tabChars = new char[1];
		tabChars[0] = '\t';
		tabDecimalChars = new char[2];
		tabDecimalChars[0] = '\t';
		tabDecimalChars[1] = '.';
	}

	/**
	 * Internally created view that has the purpose of holding the views that
	 * represent the children of the paragraph that have been arranged in rows.
	 */
	class Row extends BoxView {

		Row(Element elem) {
			super(elem, View.X_AXIS);
			childAlloc = new Rectangle();
			rowTempRect = new Rectangle();
		}

		/**
		 * This is reimplemented to do nothing since the paragraph fills in the
		 * row with its needed children.
		 */
		protected void loadChildren(ViewFactory f) {
		}

	    protected Rectangle getInsideAllocation(Shape a) {
			if (a != null) {
				// get the bounds, hopefully without allocating
				// a new rectangle. The Shape argument should
				// not be modified... we copy it into the
				// child allocation.
				Rectangle alloc;
				if (a instanceof Rectangle) {
					alloc = (Rectangle) a;
				} else {
					alloc = a.getBounds();
				}

				childAlloc.setBounds(alloc);
				childAlloc.x += getLeftInset();
				childAlloc.y += getTopInset();
				childAlloc.width -= getLeftInset() + getRightInset();
				childAlloc.height -= getTopInset() + getBottomInset();
				return childAlloc;
			}
			return null;
		}

	    protected void childAllocation(int index, Rectangle alloc) {
	    	alloc.x += getOffset(X_AXIS, index);
	    	alloc.y += getOffset(Y_AXIS, index);
	    	alloc.width = getSpan(X_AXIS, index);
	    	alloc.height = getSpan(Y_AXIS, index);
	    }
	    
	    public void paint(Graphics g, Shape allocation) {
			Rectangle alloc = (allocation instanceof Rectangle) ? (Rectangle) allocation
					: allocation.getBounds();
			int n = getViewCount();
			int x = alloc.x + getLeftInset();
			int y = alloc.y + getTopInset();
			Rectangle clip = g.getClipBounds();
			
			if (getStartOffset() == 6) {
				int zz = 0;
				zz += 0;
			}
			for (int i = 0; i < n; i++) {
				rowTempRect.x = x + getOffset(X_AXIS, i);
				rowTempRect.y = y + getOffset(Y_AXIS, i);
				rowTempRect.width = getSpan(X_AXIS, i);
				rowTempRect.height = getSpan(Y_AXIS, i);
				int trx0 = rowTempRect.x, trx1 = trx0 + rowTempRect.width;
				int try0 = rowTempRect.y, try1 = try0 + rowTempRect.height;
				int crx0 = clip.x, crx1 = crx0 + clip.width;
				int cry0 = clip.y, cry1 = cry0 + clip.height;
				// We should paint views that intersect with clipping region
				// even if the intersection has no inside points (is a line).
				// This is needed for supporting views that have zero width,
				// like
				// views that contain only combining marks.
				if ((trx1 >= crx0) && (try1 >= cry0) && (crx1 >= trx0)
						&& (cry1 >= try0)) {
					paintChild(g, rowTempRect, i);
				}
			}
		}

		/**
		 * Fetches the attributes to use when rendering. This view isn't
		 * directly responsible for an element so it returns the outer classes
		 * attributes.
		 */
		public AttributeSet getAttributes() {
			View p = getParent();
			return (p != null) ? p.getAttributes() : null;
		}

		public float getAlignment(int axis) {
			if (axis == View.X_AXIS) {
				switch (justification) {
				case StyleConstants.ALIGN_LEFT:
					return 0;
				case StyleConstants.ALIGN_RIGHT:
					return 1;
				case StyleConstants.ALIGN_CENTER:
					return 0.5f;
				case StyleConstants.ALIGN_JUSTIFIED:
					float rv = 0.5f;
					// if we can justifiy the content always align to
					// the left.
					if (isJustifiableDocument()) {
						rv = 0f;
					}
					return rv;
				}
			}
			return super.getAlignment(axis);
		}

		/**
		 * Provides a mapping from the document model coordinate space to the
		 * coordinate space of the view mapped to it. This is implemented to let
		 * the superclass find the position along the major axis and the
		 * allocation of the row is used along the minor axis, so that even
		 * though the children are different heights they all get the same caret
		 * height.
		 * 
		 * @param pos
		 *            the position to convert
		 * @param a
		 *            the allocated region to render into
		 * @return the bounding box of the given position
		 * @exception BadLocationException
		 *                if the given position does not represent a valid
		 *                location in the associated document
		 * @see View#modelToView
		 */
		public Shape modelToView(int pos, Shape a, Position.Bias b)
				throws BadLocationException {
			Rectangle r = a.getBounds();
			View v = getViewAtPosition(pos, r);
			if ((v != null) && (!v.getElement().isLeaf())) {
				// Don't adjust the height if the view represents a branch.
				return super.modelToView(pos, a, b);
			}
			r = a.getBounds();
			int height = r.height;
			int y = r.y;
			Shape loc = super.modelToView(pos, a, b);
			r = loc.getBounds();
			r.height = height;
			r.y = y;
			return r;
		}

		/**
		 * Range represented by a row in the paragraph is only a subset of the
		 * total range of the paragraph element.
		 * 
		 * @see View#getRange
		 */
		public int getStartOffset() {
			int offs = Integer.MAX_VALUE;
			int n = getViewCount();
			for (int i = 0; i < n; i++) {
				View v = getView(i);
				offs = Math.min(offs, v.getStartOffset());
			}
			return offs;
		}

		public int getEndOffset() {
			int offs = 0;
			int n = getViewCount();
			int i = 0;
			if (getView(0) instanceof NumberingView) {
				i = 1;
			}
			for (; i < n; i++) {
				View v = getView(i);
				offs = Math.max(offs, v.getEndOffset());
			}
			return offs;
		}

		/**
		 * Perform layout for the minor axis of the box (i.e. the axis
		 * orthoginal to the axis that it represents). The results of the layout
		 * should be placed in the given arrays which represent the allocations
		 * to the children along the minor axis.
		 * <p>
		 * This is implemented to do a baseline layout of the children by
		 * calling BoxView.baselineLayout.
		 * 
		 * @param targetSpan
		 *            the total span given to the view, which whould be used to
		 *            layout the children.
		 * @param axis
		 *            the axis being layed out.
		 * @param offsets
		 *            the offsets from the origin of the view for each of the
		 *            child views. This is a return value and is filled in by
		 *            the implementation of this method.
		 * @param spans
		 *            the span of each child view. This is a return value and is
		 *            filled in by the implementation of this method.
		 * @return the offset and span for each child view in the offsets and
		 *         spans parameters
		 */
		protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets,
				int[] spans) {
			baselineLayout(targetSpan, axis, offsets, spans);
		}

		protected SizeRequirements calculateMinorAxisRequirements(int axis,
				SizeRequirements r) {
			minorRequest = baselineRequirements(axis, r);
			return minorRequest;
		}

	    protected void baselineLayout(int targetSpan, int axis, int[] offsets, int[] spans) {
	        int totalAscent = (int)(targetSpan * getAlignment(axis));
	        int totalDescent = targetSpan - totalAscent;

	        int n = getViewCount();

	        for (int i = 0; i < n; i++) {
	            View v = getView(i);
	            float align = v.getAlignment(axis);
	            float viewSpan;

	            if (v.getResizeWeight(axis) > 0) {
	                // if resizable then resize to the best fit

	                // the smallest span possible
	                float minSpan = v.getMinimumSpan(axis);
	                // the largest span possible
	                float maxSpan = v.getMaximumSpan(axis);

	                if (align == 0.0f) {
	                    // if the alignment is 0 then we need to fit into the descent
	                    viewSpan = Math.max(Math.min(maxSpan, totalDescent), minSpan);
	                } else if (align == 1.0f) {
	                    // if the alignment is 1 then we need to fit into the ascent
	                    viewSpan = Math.max(Math.min(maxSpan, totalAscent), minSpan);
	                } else {
	                    // figure out the span that we must fit into
	                    float fitSpan = Math.min(totalAscent / align,
	                                             totalDescent / (1.0f - align));
	                    // fit into the calculated span
	                    viewSpan = Math.max(Math.min(maxSpan, fitSpan), minSpan);
	                }
	            } else {
	                // otherwise use the preferred spans
	                viewSpan = v.getPreferredSpan(axis);
	            }

	            offsets[i] = totalAscent - (int)(viewSpan * align);
	            spans[i] = (int)viewSpan;
	        }
	        
	        if (axis == Y_AXIS && n >= 2 && getView(0) instanceof NumberingView) {
	        	NumberingView v = (NumberingView) getView(0);
	        	if (v.isBullet()) {
	        		float spanDiff = spans[1] - spans[0]; 
	        		offsets[0] = offsets[1] + (int) (spanDiff * v.getAlignment(Y_AXIS));
	        	}
	        }
	    }

	    protected SizeRequirements baselineRequirements(int axis, SizeRequirements r) {
	        SizeRequirements totalAscent = new SizeRequirements();
	        SizeRequirements totalDescent = new SizeRequirements();
	        
	        if (r == null) {
	            r = new SizeRequirements();
	        }
	        
	        r.alignment = 0.5f;

	        int n = getViewCount();

	        // loop through all children calculating the max of all their ascents and
	        // descents at minimum, preferred, and maximum sizes
	        for (int i = 0; i < n; i++) {
	            View v = getView(i);
	            float align = v.getAlignment(axis);
	            float span;
	            int ascent;
	            int descent;

	            // find the maximum of the preferred ascents and descents
	            span = v.getPreferredSpan(axis);
	            ascent = (int)(align * span);
	            descent = (int)(span - ascent);
	            totalAscent.preferred = Math.max(ascent, totalAscent.preferred);
	            totalDescent.preferred = Math.max(descent, totalDescent.preferred);
	            
	            if (v.getResizeWeight(axis) > 0) {
	                // if the view is resizable then do the same for the minimum and
	                // maximum ascents and descents
	                span = v.getMinimumSpan(axis);
	                ascent = (int)(align * span);
	                descent = (int)(span - ascent);
	                totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
	                totalDescent.minimum = Math.max(descent, totalDescent.minimum);

	                span = v.getMaximumSpan(axis);
	                ascent = (int)(align * span);
	                descent = (int)(span - ascent);
	                totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
	                totalDescent.maximum = Math.max(descent, totalDescent.maximum);
	            } else {
	                // otherwise use the preferred
	                totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
	                totalDescent.minimum = Math.max(descent, totalDescent.minimum);
	                totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
	                totalDescent.maximum = Math.max(descent, totalDescent.maximum);
	            }
	        }
	        
	        // we now have an overall preferred, minimum, and maximum ascent and descent

	        // calculate the preferred span as the sum of the preferred ascent and preferred descent
	        r.preferred = (int)Math.min((long)totalAscent.preferred + (long)totalDescent.preferred,
	                                    Integer.MAX_VALUE);

	        // calculate the preferred alignment as the preferred ascent divided by the preferred span
	        if (r.preferred > 0) {
	            r.alignment = (float)totalAscent.preferred / r.preferred;
	        }
	        

	        if (r.alignment == 0.0f) {
	            // if the preferred alignment is 0 then the minimum and maximum spans are simply
	            // the minimum and maximum descents since there's nothing above the baseline
	            r.minimum = totalDescent.minimum;
	            r.maximum = totalDescent.maximum;
	        } else if (r.alignment == 1.0f) {
	            // if the preferred alignment is 1 then the minimum and maximum spans are simply
	            // the minimum and maximum ascents since there's nothing below the baseline
	            r.minimum = totalAscent.minimum;
	            r.maximum = totalAscent.maximum;
	        } else {
	            // we want to honor the preferred alignment so we calculate two possible minimum
	            // span values using 1) the minimum ascent and the alignment, and 2) the minimum
	            // descent and the alignment. We'll choose the larger of these two numbers.
	            r.minimum = Math.round(Math.max(totalAscent.minimum / r.alignment,
	                                          totalDescent.minimum / (1.0f - r.alignment)));
	            // a similar calculation is made for the maximum but we choose the smaller number.
	            r.maximum = Math.round(Math.min(totalAscent.maximum / r.alignment,
	                                          totalDescent.maximum / (1.0f - r.alignment)));
	        }

	        return r;
	    }

	    private boolean isFirstRow() {
	    	boolean isFirst = true;
	    	
	    	View parent = getParent();
	    	if (parent != null) {
	    		isFirst = (this == parent.getView(0));
	    	}
	    	
	    	return isFirst;
	    }
	    
	    private boolean isLastRow() {
	    	boolean isLast = true;
	    	
	    	View parent = getParent();
	    	if (parent != null) {
	    		int i = parent.getViewCount() - 1;
	    		isLast = (this == parent.getView(i));
	    	}
	    	
	    	return isLast;
	    }
	    
		private boolean isBrokenRow() {
			boolean rv = false;
			int viewsCount = getViewCount();
			if (viewsCount > 0) {
				View lastView = getView(viewsCount - 1);
				if (lastView.getBreakWeight(X_AXIS, 0, 0) >= ForcedBreakWeight) {
					rv = true;
				}
			}
			return rv;
		}

		private boolean isJustifiableDocument() {
			//return (!Boolean.TRUE.equals(getDocument().getProperty(
			//		AbstractDocument.I18NProperty)));
			return true;
		}

		/**
		 * Whether we need to justify this {@code Row}. At this time (jdk1.6) we
		 * support justification on for non 18n text.
		 * 
		 * @return {@code true} if this {@code Row} should be justified.
		 */
		private boolean isJustifyEnabled() {
			boolean ret = (justification == StyleConstants.ALIGN_JUSTIFIED);

			// no justification for i18n documents
			ret = ret && isJustifiableDocument();

			// no justification for the last row
			ret = ret && !isLastRow();

			// no justification for the broken rows
			ret = ret && !isBrokenRow();

			return ret;
		}

		// Calls super method after setting spaceAddon to 0.
		// Justification should not affect MajorAxisRequirements
		@Override
		protected SizeRequirements calculateMajorAxisRequirements(int axis,
				SizeRequirements r) {
			int oldJustficationData[] = justificationData;
			justificationData = null;
			SizeRequirements ret = super
					.calculateMajorAxisRequirements(axis, r);
			if (isJustifyEnabled()) {
				justificationData = oldJustficationData;
			}
			majorRequest = ret;
			return ret;
		}

		@Override
		protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets,
				int[] spans) {
			int oldJustficationData[] = justificationData;
			justificationData = null;
			super.layoutMajorAxis(targetSpan, axis, offsets, spans);
			if (!isJustifyEnabled()) {
				return;
			}

			int currentSpan = 0;
			for (int span : spans) {
				currentSpan += span;
			}
			if (currentSpan == targetSpan) {
				// no need to justify
				return;
			}

			// we justify text by enlarging spaces by the {@code spaceAddon}.
			// justification is started to the right of the rightmost TAB.
			// leading and trailing spaces are not extendable.
			//
			// GlyphPainter1 uses
			// justificationData
			// for all painting and measurement.

			int extendableSpaces = 0;
			int startJustifiableContent = -1;
			int endJustifiableContent = -1;
			int lastLeadingSpaces = 0;

			int rowStartOffset = getStartOffset();
			int rowEndOffset = getEndOffset();
			int spaceMap[] = new int[rowEndOffset - rowStartOffset];
			Arrays.fill(spaceMap, 0);
			for (int i = getViewCount() - 1; i >= 0; i--) {
				View view = getView(i);
				if (view instanceof org.docx4all.swing.text.LabelView) {
					org.docx4all.swing.text.LabelView.JustificationInfo 
						justificationInfo = 
							((org.docx4all.swing.text.LabelView) view)
								.getJustificationInfo(rowStartOffset);
					final int viewStartOffset = view.getStartOffset();
					final int offset = viewStartOffset - rowStartOffset;
					for (int j = 0; j < justificationInfo.spaceMap.length(); j++) {
						if (justificationInfo.spaceMap.get(j)) {
							spaceMap[j + offset] = 1;
						}
					}
					if (startJustifiableContent > 0) {
						if (justificationInfo.end >= 0) {
							extendableSpaces += justificationInfo.trailingSpaces;
						} else {
							lastLeadingSpaces += justificationInfo.trailingSpaces;
						}
					}
					if (justificationInfo.start >= 0) {
						startJustifiableContent = justificationInfo.start
								+ viewStartOffset;
						extendableSpaces += lastLeadingSpaces;
					}
					if (justificationInfo.end >= 0 && endJustifiableContent < 0) {
						endJustifiableContent = justificationInfo.end
								+ viewStartOffset;
					}
					extendableSpaces += justificationInfo.contentSpaces;
					lastLeadingSpaces = justificationInfo.leadingSpaces;
					if (justificationInfo.hasTab) {
						break;
					}
				}
			}
			if (extendableSpaces <= 0) {
				// there is nothing we can do to justify
				return;
			}
			int adjustment = (targetSpan - currentSpan);
			int spaceAddon = (extendableSpaces > 0) ? adjustment
					/ extendableSpaces : 0;
			int spaceAddonLeftoverEnd = -1;
			for (int i = startJustifiableContent - rowStartOffset, leftover = adjustment
					- spaceAddon * extendableSpaces; leftover > 0; leftover -= spaceMap[i], i++) {
				spaceAddonLeftoverEnd = i;
			}
			if (spaceAddon > 0 || spaceAddonLeftoverEnd >= 0) {
				justificationData = (oldJustficationData != null) ? oldJustficationData
						: new int[END_JUSTIFIABLE + 1];
				justificationData[SPACE_ADDON] = spaceAddon;
				justificationData[SPACE_ADDON_LEFTOVER_END] = spaceAddonLeftoverEnd;
				justificationData[START_JUSTIFIABLE] = startJustifiableContent
						- rowStartOffset;
				justificationData[END_JUSTIFIABLE] = endJustifiableContent
						- rowStartOffset;
				super.layoutMajorAxis(targetSpan, axis, offsets, spans);
			}
		}

		// for justified row we assume the maximum horizontal span
		// is MAX_VALUE.
		@Override
		public float getMaximumSpan(int axis) {
			float ret;
			if (View.X_AXIS == axis && isJustifyEnabled()) {
				ret = Float.MAX_VALUE;
			} else {
				ret = super.getMaximumSpan(axis);
			}
			return ret;
		}

		/**
		 * Fetches the child view index representing the given position in the
		 * model.
		 * 
		 * @param pos
		 *            the position >= 0
		 * @return index of the view representing the given position, or -1 if
		 *         no view represents that position
		 */
		protected int getViewIndexAtPosition(int pos) {
			// This is expensive, but are views are not necessarily layed
			// out in model order.
			if (pos < getStartOffset() || pos >= getEndOffset())
				return -1;
			for (int counter = getViewCount() - 1; counter >= 0; counter--) {
				View v = getView(counter);
				if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
					return counter;
				}
			}
			return -1;
		}

		/**
		 * Gets the left inset.
		 * 
		 * @return the inset
		 */
		protected short getLeftInset() {
			View parentView;
			int adjustment = 0;
			if ((parentView = getParent()) != null) { // use firstLineIdent for
														// the first row
				if (this == parentView.getView(0)) {
					adjustment = firstLineIndent;
				}
			}
			int leftInset = super.getLeftInset();
			return (short) (leftInset + adjustment);
		}

	    protected short getRightInset() {
	    	return super.getRightInset();
	    }
        
	    protected short getTopInset() {
	    	return super.getTopInset();
	    }

		protected short getBottomInset() {
			return (short) (super.getBottomInset() + ((minorRequest != null) ? minorRequest.preferred
					: 0)
					* lineSpacing);
		}

		final static int SPACE_ADDON = 0;
		final static int SPACE_ADDON_LEFTOVER_END = 1;
		final static int START_JUSTIFIABLE = 2;
		// this should be the last index in justificationData
		final static int END_JUSTIFIABLE = 3;

		int justificationData[] = null;
	    private SizeRequirements majorRequest;
	    private SizeRequirements minorRequest;
	    private Rectangle childAlloc;
	    
	    /** used in paint. */
	    private Rectangle rowTempRect;
	} //Row inner class
	
	static class FlowStrategy extends javax.swing.text.FlowView.FlowStrategy {
		int damageStart = Integer.MAX_VALUE;
		Vector<View> viewBuffer;

		void setDamageStart(FlowView fv, int offset) {
			if (offset >= fv.getStartOffset() && offset < fv.getEndOffset()) {
				damageStart = Math.min(damageStart, offset);
			}
		}

		void resetDamageStart() {
			damageStart = Integer.MAX_VALUE;
		}

		/**
		 * Gives notification that something was inserted into the document in a
		 * location that the given flow view is responsible for. The strategy
		 * should update the appropriate changed region (which depends upon the
		 * strategy used for repair).
		 * 
		 * @param e
		 *            the change information from the associated document
		 * @param alloc
		 *            the current allocation of the view inside of the insets.
		 *            This value will be null if the view has not yet been
		 *            displayed.
		 * @see View#insertUpdate
		 */
		public void insertUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
			// FlowView.loadChildren() makes a synthetic call into this,
			// passing null as e
			if (e != null) {
				setDamageStart(fv, e.getOffset());
			}

			if (alloc != null) {
				Component host = fv.getContainer();
				if (host != null) {
					host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
				}
			} else {
				fv.preferenceChanged(null, true, true);
			}
		}

		/**
		 * Gives notification that something was removed from the document in a
		 * location that the given flow view is responsible for.
		 * 
		 * @param e
		 *            the change information from the associated document
		 * @param alloc
		 *            the current allocation of the view inside of the insets.
		 * @see View#removeUpdate
		 */
		public void removeUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
			setDamageStart(fv, e.getOffset());
			if (alloc != null) {
				Component host = fv.getContainer();
				if (host != null) {
					host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
				}
			} else {
				fv.preferenceChanged(null, true, true);
			}
		}

		/**
		 * Gives notification from the document that attributes were changed in
		 * a location that this view is responsible for.
		 * 
		 * @param fv
		 *            the <code>FlowView</code> containing the changes
		 * @param e
		 *            the <code>DocumentEvent</code> describing the changes done
		 *            to the Document
		 * @param alloc
		 *            Bounds of the View
		 * @see View#changedUpdate
		 */
		public void changedUpdate(FlowView fv, DocumentEvent e, Rectangle alloc) {
			setDamageStart(fv, e.getOffset());
			if (alloc != null) {
				Component host = fv.getContainer();
				if (host != null) {
					host.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
				}
			} else {
				fv.preferenceChanged(null, true, true);
			}
		}

		/**
		 * This method gives flow strategies access to the logical view of the
		 * FlowView.
		 */
		protected View getLogicalView(FlowView fv) {
			return ((ImpliedParagraphView) fv).layoutPool;
		}

		/**
		 * Update the flow on the given FlowView. By default, this causes all of
		 * the rows (child views) to be rebuilt to match the given constraints
		 * for each row. This is called by a FlowView.layout to update the child
		 * views in the flow.
		 * 
		 * @param fv
		 *            the view to reflow
		 */
		public void layout(FlowView fv) {
			View pool = getLogicalView(fv);
			int rowIndex, p0;
			int p1 = fv.getEndOffset();

			if (((ImpliedParagraphView) fv).isLayoutValid(Y_AXIS)) {
				if (damageStart == Integer.MAX_VALUE) {
					return;
				}
				// In some cases there's no view at position damageStart, so
				// step back and search again. See 6452106 for details.
				while ((rowIndex = ((ImpliedParagraphView) fv).getViewIndexAtPosition(damageStart)) < 0) {
					damageStart--;
				}
				if (rowIndex > 0) {
					rowIndex--;
				}
				p0 = fv.getView(rowIndex).getStartOffset();
			} else {
				rowIndex = 0;
				p0 = fv.getStartOffset();
			}
			reparentPoolViews(pool, p0);

			viewBuffer = new Vector<View>(10, 10);
			int rowCount = fv.getViewCount();
			while (p0 < p1) {
				View row;
				if (rowIndex >= rowCount) {
					row = ((ImpliedParagraphView) fv).createRow();
					fv.append(row);
				} else {
					row = fv.getView(rowIndex);
				}
				p0 = layoutRow(fv, rowIndex, p0);
				rowIndex++;
			}
			viewBuffer = null;

			if (rowIndex < rowCount) {
				fv.replace(rowIndex, rowCount - rowIndex, null);
			}
			resetDamageStart();
		}

		/**
		 * Creates a row of views that will fit within the layout span of the
		 * row. This is called by the layout method. This is implemented to fill
		 * the row by repeatedly calling the createView method until the
		 * available span has been exhausted, a forced break was encountered, or
		 * the createView method returned null. If the remaining span was
		 * exhaused, the adjustRow method will be called to perform adjustments
		 * to the row to try and make it fit into the given span.
		 * 
		 * @param rowIndex
		 *            the index of the row to fill in with views. The row is
		 *            assumed to be empty on entry.
		 * @param pos
		 *            The current position in the children of this views element
		 *            from which to start.
		 * @return the position to start the next row
		 */
		protected int layoutRow(FlowView fv, int rowIndex, int pos) {
			View row = fv.getView(rowIndex);
			float x = fv.getFlowStart(rowIndex);
			float spanLeft = fv.getFlowSpan(rowIndex);
			
			int end = fv.getEndOffset();
			TabExpander te = (fv instanceof TabExpander) ? (TabExpander) fv
					: null;
			final int flowAxis = fv.getFlowAxis();

			int breakWeight = BadBreakWeight;
			float breakX = 0f;
			float breakSpan = 0f;
			int breakIndex = -1;
			int n = 0;

			viewBuffer.clear();
			 
			if (rowIndex == 0 && ((ImpliedParagraphView) fv).numberingView != null) {
				viewBuffer.add(((ImpliedParagraphView) fv).numberingView);
				float chunkSpan = 
					((ImpliedParagraphView) fv).numberingView.getPreferredSpan(flowAxis);
				spanLeft -= chunkSpan;
				x += chunkSpan;
			}
			
			while (pos < end && spanLeft >= 0) {
				View v = createView(fv, pos, (int) spanLeft, rowIndex);
				if (v == null) {
					break;
				}

				int bw = v.getBreakWeight(flowAxis, x, spanLeft);
				if (bw >= ForcedBreakWeight) {
					View w = v.breakView(flowAxis, pos, x, spanLeft);
					if (w != null) {
						viewBuffer.add(w);
					} else if (n == 0) {
						// if the view does not break, and it is the only view
						// in a row, use the whole view
						viewBuffer.add(v);
					}
					break;
				} else if (bw >= breakWeight && bw > BadBreakWeight) {
					breakWeight = bw;
					breakX = x;
					breakSpan = spanLeft;
					breakIndex = n;
				}

				float chunkSpan;
				if (flowAxis == X_AXIS && v instanceof TabableView) {
					chunkSpan = ((TabableView) v).getTabbedSpan(x, te);
				} else {
					chunkSpan = v.getPreferredSpan(flowAxis);
				}

				if (chunkSpan > spanLeft && breakIndex >= 0) {
					// row is too long, and we may break
					if (breakIndex < n) {
						v = viewBuffer.get(breakIndex);
					}
					for (int i = n - 1; i >= breakIndex; i--) {
						viewBuffer.remove(i);
					}
					v = v.breakView(flowAxis, v.getStartOffset(), breakX,
							breakSpan);
				}

				spanLeft -= chunkSpan;
				x += chunkSpan;
				viewBuffer.add(v);
				pos = v.getEndOffset();
				n++;
			}

			View[] views = new View[viewBuffer.size()];
			viewBuffer.toArray(views);
			row.replace(0, row.getViewCount(), views);
			return (views.length > 0 ? row.getEndOffset() : pos);
		}

		/**
		 * Adjusts the given row if possible to fit within the layout span. By
		 * default this will try to find the highest break weight possible
		 * nearest the end of the row. If a forced break is encountered, the
		 * break will be positioned there.
		 * 
		 * @param rowIndex
		 *            the row to adjust to the current layout span.
		 * @param desiredSpan
		 *            the current layout span >= 0
		 * @param x
		 *            the location r starts at.
		 */
		protected void adjustRow(FlowView fv, int rowIndex, int desiredSpan,
				int x) {
			final int flowAxis = fv.getFlowAxis();
			View r = fv.getView(rowIndex);
			int n = r.getViewCount();
			int span = 0;
			int bestWeight = BadBreakWeight;
			int bestSpan = 0;
			int bestIndex = -1;
			View v;
			for (int i = 0; i < n; i++) {
				v = r.getView(i);
				int spanLeft = desiredSpan - span;

				int w = v.getBreakWeight(flowAxis, x + span, spanLeft);
				if ((w >= bestWeight) && (w > BadBreakWeight)) {
					bestWeight = w;
					bestIndex = i;
					bestSpan = span;
					if (w >= ForcedBreakWeight) {
						// it's a forced break, so there is
						// no point in searching further.
						break;
					}
				}
				span += v.getPreferredSpan(flowAxis);
			}
			if (bestIndex <= 0) {
				// there is nothing that can be broken, leave
				// it in it's current state.
				return;
			}

			// Break the best candidate view, and patch up the row.
			int spanLeft = desiredSpan - bestSpan;
			v = r.getView(bestIndex);
			v = v.breakView(flowAxis, v.getStartOffset(), x + bestSpan,
					spanLeft);
			View[] va = new View[1];
			va[0] = v;
			View lv = getLogicalView(fv);
			int p0 = r.getView(bestIndex).getStartOffset();
			int p1 = r.getEndOffset();
			for (int i = 0; i < lv.getViewCount(); i++) {
				View tmpView = lv.getView(i);
				if (tmpView.getEndOffset() > p1) {
					break;
				}
				if (tmpView.getStartOffset() >= p0) {
					tmpView.setParent(lv);
				}
			}
			r.replace(bestIndex, n - bestIndex, va);
		}

		void reparentPoolViews(View pool, int startPos) {
			int n = pool.getViewIndex(startPos, Position.Bias.Forward);
			if (n >= 0) {
				for (int i = n; i < pool.getViewCount(); i++) {
					pool.getView(i).setParent(pool);
				}
			}
		}

		/**
		 * Creates a view that can be used to represent the current piece of the
		 * flow. This can be either an entire view from the logical view, or a
		 * fragment of the logical view.
		 * 
		 * @param fv
		 *            the view holding the flow
		 * @param startOffset
		 *            the start location for the view being created
		 * @param spanLeft
		 *            the about of span left to fill in the row
		 * @param rowIndex
		 *            the row the view will be placed into
		 */
		protected View createView(
			javax.swing.text.FlowView fv,
			int startOffset, 
			int spanLeft, 
			int rowIndex) {

			// Get the child view that contains the given starting position
			View lv = getLogicalView(fv);
			int childIndex = lv
					.getViewIndex(startOffset, Position.Bias.Forward);
			View v = lv.getView(childIndex);

			if (v instanceof RunView) {
				RunView runV = (RunView) v;
				v = runV.getTextView(startOffset);
			}

			if (startOffset == v.getStartOffset()) {
				// return the entire view
				return v;
			}

			// return a fragment.
			v = v.createFragment(startOffset, v.getEndOffset());
			return v;
		}
	} // FlowStrategy inner static class

} //ImpliedParagraphView class


























