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

package org.docx4all.xml;

import java.util.List;

import org.docx4j.XmlUtils;
import org.docx4j.wml.TblGrid;

/**
 *	@author Jojada Tirtowidjojo - 11/06/2008
 */
public class TableGridML extends ElementML {
	
	public TableGridML(TblGrid grid) {
		super(grid, false);
	}
	
	public Object clone() {
		TblGrid obj = null;
		if (this.docxObject != null) {
			obj = (TblGrid) XmlUtils.deepCopy(this.docxObject);
		}
		return new TableGridML(obj);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		//Although in docx hierarchy, TblGrid may have <w:gridCol>
		//elements as children but we do not perform cut-n-paste operation
		//to <w:gridCol> elements.
		return false;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		throw new UnsupportedOperationException("Cannot have a child.");
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof TableML)) {
			throw new IllegalArgumentException("NOT a TableML.");
		}
		this.parent = parent;
	}
	
	public List<Object> getDocxChildren() {
		//Although in docx hierarchy, TblGrid may have <w:gridCol>
		//elements as children but we do not perform cut-n-paste operation
		//to <w:gridCol> elements.
		return null;
	}
		
	protected void init(Object docxObject) {
		//TODO: Implement this.
		//I am thinking of treating TableGrid as a kind of property container
		//from which we can specify/query the number of column grids and their 
		//individual size.
		//Feel free to provide your own property field(s) and its setter/getter methods 
	}
	
}// TableGridML class



















