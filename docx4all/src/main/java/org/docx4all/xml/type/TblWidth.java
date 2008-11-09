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

import org.docx4all.swing.text.StyleSheet;

/**
 *	@author Jojada Tirtowidjojo - 29/10/2008
 */
public class TblWidth {
	private org.docx4j.wml.TblWidth tblWidth;
	
	public TblWidth(org.docx4j.wml.TblWidth tblWidth) {
		this.tblWidth = tblWidth;
	}

	public Object getDocxObject() {
		return this.tblWidth;
	}
	
	public Type getType() {
		Type theType = null;
		
		String s = this.tblWidth.getType();

		if (s == null) {
			//WordprocessingML spec default value
			theType = Type.DXA;
		} else if (s.equalsIgnoreCase("auto")) {
			theType = Type.AUTO;
		} else if (s.equalsIgnoreCase("nil")) {
			theType = Type.NIL;
		} else if (s.equalsIgnoreCase("pct")) {
			theType = Type.PCT;
		} else {
			//defaulted to DXA
			theType = Type.DXA;
		}
		return theType;
	}

	/**
	 * @return the value of w:w attribute of <w:tblWidth> element
	 */
	public int getWidth() {
		if (this.tblWidth == null
			|| this.tblWidth.getW() == null) {
			return 0;
		}
		return this.tblWidth.getW().intValue();
	}
	
	/**
	 * Given the text extents of the page in pixels,
	 * this method converts the value of getWidth() 
	 * into pixels. The value of text extents is only
	 * relevant if this.getType() is of Type.PCT.
	 * As such, user may pass in arbitrary parameter value
	 * if he/she is sure that this.getType() is NOT of
	 * Type.PCT. 
	 * 
	 * @param textExtentsOfPageInPixels
	 * @return -1 if getType() is Type.AUTO;
	 *         0  if getType() is Type.NIL;
	 *         width in pixels, otherwise.
	 */
	public int getWidthInPixel(int textExtentsOfPageInPixels) {
		int theWidth = -1;
		
		Type type = getType();
		if (type == Type.AUTO) {
			;
		} else if (type == Type.DXA) {
			//textExtentsOfPageInPixels value is irrelevant.
			int w = getWidth(); //in twips
			theWidth = StyleSheet.toPixels(w);
		} else if (type == Type.PCT) {
			int w = getWidth(); //in fiftieth of a percent
			theWidth = w * 2 / 100 * textExtentsOfPageInPixels;
		} else {
			theWidth = 0;
		}
		return theWidth;
	}
	
	public enum Type {
		AUTO ("auto"),
		NIL ("nil"),
		DXA ("dxa"),
		PCT ("pct");
		
    	private final String value;
    	Type(String value) {
    		this.value = value;
    	}
    	
    	public String value() {
    		return value;
    	}
	}
}// TblWidth class



















