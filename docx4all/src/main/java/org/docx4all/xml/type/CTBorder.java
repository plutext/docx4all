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
	private Color autoColor;
	
	public CTBorder(org.docx4j.wml.CTBorder ctBorder) {
		this.ctBorder = ctBorder;
	}
	
	public Object getDocxObject() {
		return this.ctBorder;
	}
	
	public void setAutoColor(Color c) {
		this.autoColor = c;
	}
	
	public Color getAutoColor() {
		return this.autoColor;
	}
	
	public Color getColor() {
		Color color = null;
		
		String s = ctBorder.getColor();
		if (s.equalsIgnoreCase("auto")) {
			color = getAutoColor();
		} else {
			int r = Integer.valueOf(s.substring(0, 2), 16).intValue();
			int g = Integer.valueOf(s.substring(2, 4), 16).intValue();
			int b = Integer.valueOf(s.substring(4), 16).intValue();
			color = new Color(r, g, b);
		}
		return color;
	}
	
	public LineBorderSegment.Style getStyle() {
		//TODO: Currently only supporting two styles: SOLID and DASHED.
		LineBorderSegment.Style style = LineBorderSegment.Style.SOLID;
		if (ctBorder.getVal().value().toLowerCase().indexOf("dash") >= 0) {
			style = LineBorderSegment.Style.DASHED;
		}
		//Other styles are being considered as SOLID. 
		return style;
	}

	public int getSize() {
		int theSize = 0;
		
		BigInteger sz = ctBorder.getSz();
		if (sz != null && sz.intValue() > 0) {
			//ctBorder.getSz() is in eights of a point.
			theSize = sz.intValue();
		}
		
		//Because we are still supporting Line Border style,
		//current minimum size is 2 and maximum size is 96 
		//eights of a point
		theSize = Math.max(theSize, 2);
		theSize = Math.min(theSize, 96);
		
		//TODO: For Art Border style, size is in point.
		//Its miniumum size is 1 and maximum size is 31
		
		return theSize;
	}
	
	public int getSizeInTwips() {
		//TODO: For Art Border style, size is in point.
		//Because we are still supporting Line Border style,
		//getSize() is in eights of a point.
		int sz = getSize(); //eights of a point
		return sz * 8 / 20; //twips
	}
	
	public int getSizeInPixels() {
		return StyleSheet.toPixels(getSizeInTwips());
	}
	
}// CTBorder class



















