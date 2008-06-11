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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBIntrospector;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblPr;

/**
 *	@author Jojada Tirtowidjojo - 03/06/2008
 */
public class TableML extends ElementML {
	
	private TablePropertiesML tblPr;
	private TableGridML tblGrid;
	
	public TableML(Object docxObject) {
		this(docxObject, false);
	}
	
	public TableML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	/**
	 * Gets table properties of this table.
	 * 
	 * @return a PropertiesContainerML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getTableProperties() {
		return this.tblPr;
	}
	
	public void setTableProperties(TablePropertiesML tblPr) {
		if (tblPr != null && tblPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.tblPr = tblPr;
		
		org.docx4j.wml.TblPr newDocxPr = null;
		if (tblPr != null) {
			tblPr.setParent(TableML.this);
			newDocxPr = (org.docx4j.wml.TblPr) tblPr.getDocxObject();
		}
		((org.docx4j.wml.Tbl) this.docxObject).setTblPr(newDocxPr);
		if (newDocxPr != null) {
			newDocxPr.setParent(this.docxObject);
		}
	}
	
	/**
	 * Gets table grid of this table.
	 * 
	 * @return a TableGridML, if any
	 *         null, otherwise 
	 */
	public TableGridML getTableGrid() {
		return this.tblGrid;
	}
	
	public void setTableGrid(TableGridML tblGrid) {
		if (tblGrid != null && tblGrid.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.tblGrid = tblGrid;
		
		org.docx4j.wml.TblGrid newDocxGrid = null;
		if (tblGrid != null) {
			tblGrid.setParent(TableML.this);
			newDocxGrid = (org.docx4j.wml.TblGrid) tblGrid.getDocxObject();
		}
		((org.docx4j.wml.Tbl) this.docxObject).setTblGrid(newDocxGrid);
		if (newDocxGrid != null) {
			newDocxGrid.setParent(this.docxObject);
		}
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new TableML(obj, this.isDummy);
	}

	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof TableRowML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof TableRowML)) {
			throw new IllegalArgumentException("NOT a TableRowML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null 
			&& !(parent instanceof BodyML)
			&& !(parent instanceof SdtBlockML)) {
			throw new IllegalArgumentException("Parent type = " + parent.getClass().getSimpleName());
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;

		if (this.docxObject == null) {
			;//do nothing
		} else {
			org.docx4j.wml.Tbl table = 
				(org.docx4j.wml.Tbl) JAXBIntrospector.getValue(this.docxObject);
			theChildren = table.getEGContentRowContent();
		}

		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.Tbl table = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied TableML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.Tbl) {
				table = (org.docx4j.wml.Tbl) value;
				this.isDummy = false;
			} else {
				throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		if (table != null) {
			initTableProperties(table);
			initTableGrid(table);
			initChildren(table);
		}
	}

	private void initTableProperties(org.docx4j.wml.Tbl table) {
		this.tblPr = null;

		TblPr prop = table.getTblPr();
		if (prop != null) {
			this.tblPr = new TablePropertiesML(prop);
			this.tblPr.setParent(TableML.this);
		}
	}
	
	private void initTableGrid(org.docx4j.wml.Tbl table) {
		this.tblGrid = null;
		
		TblGrid grid = table.getTblGrid();
		if (grid != null) {
			this.tblGrid = new TableGridML(grid);
			this.tblGrid.setParent(TableML.this);
		}
	}
	
	private void initChildren(org.docx4j.wml.Tbl table) {
		this.children = null;
		
		List<Object> list = table.getEGContentRowContent();
		if (!list.isEmpty()) {
			this.children = new ArrayList<ElementML>(list.size());
			for (Object obj : list) {
				if (obj instanceof org.docx4j.wml.Tr) {
					ElementML ml = new TableRowML(obj);
					ml.setParent(TableML.this);
					this.children.add(ml);
				} else {
					//Ignore this at the moment.
				}
			}
		}
	}// initChildren()


}// TableML class



















