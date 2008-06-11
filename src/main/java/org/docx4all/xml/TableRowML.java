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
import org.docx4j.wml.TrPr;

/**
 *	@author Jojada Tirtowidjojo - 03/06/2008
 */
public class TableRowML extends ElementML {
	private TableRowPropertiesML trPr;
	
	public TableRowML(Object docxObject) {
		this(docxObject, false);
	}
	
	public TableRowML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	/**
	 * Gets table row properties of this row.
	 * 
	 * @return a PropertiesContainerML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getTableRowProperties() {
		return this.trPr;
	}
	
	public void setTableRowProperties(TableRowPropertiesML trPr) {
		if (trPr != null && trPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.trPr = trPr;
		
		org.docx4j.wml.TrPr newDocxPr = null;
		if (trPr != null) {
			trPr.setParent(TableRowML.this);
			newDocxPr = (org.docx4j.wml.TrPr) trPr.getDocxObject();
		}
		((org.docx4j.wml.Tr) this.docxObject).setTrPr(newDocxPr);
		if (newDocxPr != null) {
			newDocxPr.setParent(this.docxObject);
		}
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new TableRowML(obj, this.isDummy);
	}

	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof TableCellML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof TableCellML)) {
			throw new IllegalArgumentException("NOT a TableCellML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null 
			&& !(parent instanceof TableML)) {
			throw new IllegalArgumentException("Parent type = " + parent.getClass().getSimpleName());
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;

		if (this.docxObject == null) {
			;//do nothing
		} else {
			org.docx4j.wml.Tr row = 
				(org.docx4j.wml.Tr) JAXBIntrospector.getValue(this.docxObject);
			theChildren = row.getEGContentCellContent();
		}

		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.Tr row = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied TableML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.Tr) {
				row = (org.docx4j.wml.Tr) value;
				this.isDummy = false;
			} else {
				throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		if (row != null) {
			initTableRowProperties(row);
			initChildren(row);
		}
	}

	private void initTableRowProperties(org.docx4j.wml.Tr row) {
		this.trPr = null;
		
		TrPr trowPr = row.getTrPr();
		if (trowPr != null) {
			this.trPr = new TableRowPropertiesML(trowPr);
			this.trPr.setParent(TableRowML.this);
		}
	}
	
	private void initChildren(org.docx4j.wml.Tr row) {
		this.children = null;
		
		List<Object> list = row.getEGContentCellContent();
		if (!list.isEmpty()) {
			this.children = new ArrayList<ElementML>(list.size());
			for (Object obj : list) {
				Object value = JAXBIntrospector.getValue(obj);
				
				if (value instanceof org.docx4j.wml.Tc) {
					ElementML ml = new TableCellML(value);
					ml.setParent(TableRowML.this);
					this.children.add(ml);
				} else {
					//Ignore this at the moment.
				}
			}
		}
	}// initChildren()

}// TableRowML class



















