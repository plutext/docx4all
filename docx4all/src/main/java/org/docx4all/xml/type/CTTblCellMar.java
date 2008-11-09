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


/**
 *	@author Jojada Tirtowidjojo - 31/10/2008
 */
public class CTTblCellMar {
	
	public final static CTTblCellMar DEFAULT_VALUE;
	
	static {
		//WordprocessingML specification defines this default values
		org.docx4j.wml.TblWidth left = 
			org.docx4all.xml.type.ObjectFactory.createTblWidth("DXA", 115); //twips
		org.docx4j.wml.TblWidth top = 
			org.docx4all.xml.type.ObjectFactory.createTblWidth("DXA", 0); //twips
		org.docx4j.wml.TblWidth right = 
			org.docx4all.xml.type.ObjectFactory.createTblWidth("DXA", 115); //twips
		org.docx4j.wml.TblWidth bottom = 
			org.docx4all.xml.type.ObjectFactory.createTblWidth("DXA", 0); //twips
		org.docx4j.wml.CTTblCellMar defaultValue =
			org.docx4all.xml.type.ObjectFactory.createCTTblCellMar(left, top, right, bottom);
		DEFAULT_VALUE = new CTTblCellMar(defaultValue);

	}	
	
	private org.docx4j.wml.CTTblCellMar tblCellMar;
	
	public CTTblCellMar(org.docx4j.wml.CTTblCellMar tblCellMar) {
		this.tblCellMar = tblCellMar;
	}
	
	public Object getDocxObject() {
		return this.tblCellMar;
	}
	
	public TblWidth getLeft() {
		org.docx4j.wml.TblWidth w = tblCellMar.getLeft();
		return (w == null) ? null : new TblWidth(w);
	}
	
	public TblWidth getRight() {
		org.docx4j.wml.TblWidth w = tblCellMar.getRight();
		return (w == null) ? null : new TblWidth(w);
	}
	
	public TblWidth getTop() {
		org.docx4j.wml.TblWidth w = tblCellMar.getTop();
		return (w == null) ? null : new TblWidth(w);
	}
	
	public TblWidth getBottom() {
		org.docx4j.wml.TblWidth w = tblCellMar.getBottom();
		return (w == null) ? null : new TblWidth(w);
	}
	
}// CTTblCellMar class



















