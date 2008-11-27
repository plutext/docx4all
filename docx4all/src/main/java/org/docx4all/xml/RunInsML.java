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
public class RunInsML extends ElementML {
	
	public RunInsML(Object docxObject) {
		this(docxObject, false);
	}
	
	public RunInsML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public BigInteger getId() {
		return ((org.docx4j.wml.RunIns) this.docxObject).getId();
	}
	
	public void setId(BigInteger id) {
		((org.docx4j.wml.RunIns) this.docxObject).setId(id);
	}
	
	public XMLGregorianCalendar getDate() {
		return ((org.docx4j.wml.RunIns) this.docxObject).getDate();
	}
	
	public void setDate(XMLGregorianCalendar date) {
		((org.docx4j.wml.RunIns) this.docxObject).setDate(date);
	}
	
	public String getAuthor() {
		return ((org.docx4j.wml.RunIns) this.docxObject).getAuthor();
	}
	
	public void setAuthor(String author) {
		((org.docx4j.wml.RunIns) this.docxObject).setAuthor(author);
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
		
		return new RunInsML(obj, this.isDummy);
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
		
		if (this.docxObject instanceof org.docx4j.wml.RunIns) {
			theChildren = ((org.docx4j.wml.RunIns) this.docxObject).getCustomXmlOrSmartTagOrSdt();
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.RunIns runIns = null;
		
		if (docxObject == null) {
			;// implied ParagraphML
			
		} else if (docxObject instanceof org.docx4j.wml.RunIns) {
			runIns = (org.docx4j.wml.RunIns) docxObject;
			this.isDummy = false;
			
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = "
					+ docxObject);
		}

		initChildren(runIns);
	}
	
	private void initChildren(org.docx4j.wml.RunIns ins) {
		this.children = null;
		
		if (ins == null) {
			return;
		}
		
		List<Object> pKids = ins.getCustomXmlOrSmartTagOrSdt();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(RunInsML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	

}// RunInsML class



















