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
import javax.xml.namespace.QName;

import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.TcPr;

/**
 *	@author Jojada Tirtowidjojo - 03/06/2008
 */
public class TableCellML extends ElementML {
	private TableCellPropertiesML tcPr;
	
	public TableCellML(Object docxObject) {
		this(docxObject, false);
	}
	
	public TableCellML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	/**
	 * Gets table cell properties of this cell.
	 * 
	 * @return a PropertiesContainerML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getTableCellProperties() {
		return this.tcPr;
	}
	
	public void setTableCellProperties(TableCellPropertiesML tcPr) {
		if (tcPr != null && tcPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.tcPr = tcPr;
		
		org.docx4j.wml.TcPr newDocxPr = null;
		if (tcPr != null) {
			tcPr.setParent(TableCellML.this);
			newDocxPr = (org.docx4j.wml.TcPr) tcPr.getDocxObject();
		}
		((org.docx4j.wml.Tc) this.docxObject).setTcPr(newDocxPr);
		if (newDocxPr != null) {
			newDocxPr.setParent(this.docxObject);
		}
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new TableCellML(obj, this.isDummy);
	}

	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		//Currently only supporting nested TableML and ParagraphML.
		//See: initChildren()
		if (!(child instanceof TableML)
			&& !(child instanceof ParagraphML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null 
			&& !(parent instanceof TableRowML)) {
			throw new IllegalArgumentException("Parent type = " + parent.getClass().getSimpleName());
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;

		if (this.docxObject == null) {
			;//do nothing
		} else {
			org.docx4j.wml.Tc cell = 
				(org.docx4j.wml.Tc) JAXBIntrospector.getValue(this.docxObject);
			theChildren = cell.getEGBlockLevelElts();
		}

		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.Tc cell = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied TableML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.Tc) {
				cell = (org.docx4j.wml.Tc) value;
				this.isDummy = false;
			} else {
				throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		if (cell != null) {
			initTableCellProperties(cell);
			initChildren(cell);
		}
	}

	private void initTableCellProperties(org.docx4j.wml.Tc cell) {
		this.tcPr = null;
		
		TcPr tcellPr = cell.getTcPr();
		if (tcellPr != null) {
			this.tcPr = new TableCellPropertiesML(tcellPr);
			this.tcPr.setParent(TableCellML.this);
		}
	}
	
	private void initChildren(org.docx4j.wml.Tc cell) {
		this.children = null;
		
		List<Object> list = cell.getEGBlockLevelElts();
		if (!list.isEmpty()) {
			this.children = new ArrayList<ElementML>(list.size());
			
			ElementML ml = null;
			for (Object obj : list) {
				Object value = JAXBIntrospector.getValue(obj);
				
				//if (value instanceof org.docx4j.wml.SdtBlock) {
					//ml = new SdtBlockML(obj);
				//} else
				if (value instanceof org.docx4j.wml.Tbl) {
					ml = new TableML(obj);
					ml.setParent(TableCellML.this);
					this.children.add(ml);
					
				} else if (value instanceof org.docx4j.wml.CTMarkupRange) {
					//suppress <w:bookmarkStart> and <w:bookmarkEnd>
					JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
					QName name = inspector.getElementName(obj);
					if (name != null 
						&& (name.getLocalPart() == "bookmarkStart" 
							|| name.getLocalPart() == "bookmarkEnd")) {
						//suppress
					} else {
						ml = new ParagraphML(obj);
						ml.setParent(TableCellML.this);
						this.children.add(ml);
					}
				} else {
					ml = new ParagraphML(obj);
					ml.setParent(TableCellML.this);
					this.children.add(ml);
				}
			}
		}
	}// initChildren()

}// TableCellML class



















