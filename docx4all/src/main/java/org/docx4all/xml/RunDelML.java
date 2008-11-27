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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.docx4j.XmlUtils;

/**
 *	@author Jojada Tirtowidjojo - 17/06/2008
 */
public class RunDelML extends ElementML {
	
	public RunDelML(Object docxObject) {
		this(docxObject, false);
	}
	
	public RunDelML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public BigInteger getId() {
		return ((org.docx4j.wml.RunDel) this.docxObject).getId();
	}
	
	public void setId(BigInteger id) {
		((org.docx4j.wml.RunDel) this.docxObject).setId(id);
	}
	
	public XMLGregorianCalendar getDate() {
		return ((org.docx4j.wml.RunDel) this.docxObject).getDate();
	}
	
	public void setDate(XMLGregorianCalendar date) {
		((org.docx4j.wml.RunDel) this.docxObject).setDate(date);
	}
	
	public String getAuthor() {
		return ((org.docx4j.wml.RunDel) this.docxObject).getAuthor();
	}
	
	public void setAuthor(String author) {
		((org.docx4j.wml.RunDel) this.docxObject).setAuthor(author);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		boolean canAdd = false;
		
		if (elem instanceof RunML 
			|| elem instanceof RunDelML 
			|| elem instanceof RunDelML
			|| elem instanceof HyperlinkML) {
			canAdd = super.canAddSibling(elem, after);
		}
		
		return canAdd;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new RunDelML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML.");
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		if (this.docxObject instanceof org.docx4j.wml.RunDel) {
			theChildren = ((org.docx4j.wml.RunDel) this.docxObject).getCustomXmlOrSmartTagOrSdt();
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.RunDel runDel = null;
		
		if (docxObject == null) {
			;// implied ParagraphML
			
		} else if (docxObject instanceof org.docx4j.wml.RunDel) {
			runDel = (org.docx4j.wml.RunDel) docxObject;
			this.isDummy = false;
			
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = "
					+ docxObject);
		}

		initChildren(runDel);
	}
	
	private void initChildren(org.docx4j.wml.RunDel del) {
		this.children = null;
		
		if (del == null) {
			return;
		}
		
		List<Object> pKids = del.getCustomXmlOrSmartTagOrSdt();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(RunDelML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	
}// RunDelML class



















