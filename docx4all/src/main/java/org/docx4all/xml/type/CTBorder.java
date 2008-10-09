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

package org.docx4all.xml.type;

import java.awt.Color;
import java.math.BigInteger;

import org.docx4all.swing.LineBorderSegment;
import org.docx4all.swing.text.StyleSheet;

/**
 *	@author Jojada Tirtowidjojo - 30/09/2008
 */
public class CTBorder {
	private org.docx4j.wml.CTBorder ctBorder;
	
	public CTBorder(org.docx4j.wml.CTBorder ctBorder) {
		this.ctBorder = ctBorder;
	}
	
	public Color getColor() {
		Color color = null;
		
		String s = ctBorder.getColor();
		if (s.equalsIgnoreCase("auto")) {
			;//pass
		} else {
			int r = Integer.valueOf(s.substring(0, 2), 16).intValue();
			int g = Integer.valueOf(s.substring(2, 4), 16).intValue();
			int b = Integer.valueOf(s.substring(4), 16).intValue();
			color = new Color(r, g, b);
		}
		return color;
	}
	
	public LineBorderSegment.Style getStyle() {
		LineBorderSegment.Style style = LineBorderSegment.Style.SOLID;
		if (ctBorder.getVal().value().toLowerCase().indexOf("dash") >= 0) {
			style = LineBorderSegment.Style.DASHED;
		}
		return style;
	}

	public int getSize() {
		int theSize = 0;
		
		//ctBorder.getSz() is in eights of a point.
		BigInteger sz = ctBorder.getSz();
		if (sz != null && sz.intValue() > 0) {
			theSize = sz.intValue();
		}
		
		return theSize;
	}
	
	public int getSizeInTwips() {
		int sz = getSize(); //eights of a point
		return sz / 8 * 20; //twips
	}
	
	public int getSizeInPixels() {
		return StyleSheet.toPixels(getSizeInTwips());
	}
	
}// CTBorder class



















