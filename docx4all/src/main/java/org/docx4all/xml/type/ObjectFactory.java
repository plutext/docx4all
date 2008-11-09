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

import java.math.BigInteger;

/**
 *	@author Jojada Tirtowidjojo - 03/11/2008
 */
public class ObjectFactory {
	private final static org.docx4j.wml.ObjectFactory _jaxbFactory = 
		new org.docx4j.wml.ObjectFactory();

	public final static org.docx4j.wml.TblWidth createTblWidth(String type, int value) {
		org.docx4j.wml.TblWidth tw = _jaxbFactory.createTblWidth();
		tw.setType(type);
		tw.setW(BigInteger.valueOf(value));
		return tw;
	}
	
	public final static org.docx4j.wml.CTTblCellMar createCTTblCellMar(
		org.docx4j.wml.TblWidth left,
		org.docx4j.wml.TblWidth top,
		org.docx4j.wml.TblWidth right,
		org.docx4j.wml.TblWidth bottom) {
		
		org.docx4j.wml.CTTblCellMar tm = _jaxbFactory.createCTTblCellMar();
		tm.setLeft(left);
		tm.setTop(top);
		tm.setRight(right);
		tm.setBottom(bottom);
		return tm;
	}
	
	public final static org.docx4j.wml.TblBorders createTblBorders(
		org.docx4j.wml.CTBorder left,
		org.docx4j.wml.CTBorder top,
		org.docx4j.wml.CTBorder right,
		org.docx4j.wml.CTBorder bottom,
		org.docx4j.wml.CTBorder insideH,
		org.docx4j.wml.CTBorder insideV) {
		
		org.docx4j.wml.TblBorders tb = _jaxbFactory.createTblBorders();
		tb.setLeft(left);
		tb.setTop(top);
		tb.setRight(right);
		tb.setBottom(bottom);
		tb.setInsideH(insideH);
		tb.setInsideV(insideV);
		return tb;
	}
	
	public final static org.docx4j.wml.CTBorder createCTBorder() {
		return _jaxbFactory.createCTBorder();
	}
	
}// ObjectFactory class



















