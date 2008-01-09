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

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 *	@author Jojada Tirtowidjojo - 08/01/2008
 */
public class BodyML extends ElementML {
	private static Logger log = Logger.getLogger(BodyML.class);
	
	public BodyML(Object docxObject) {
		this(docxObject, false);
	}
	
	public BodyML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		return new BodyML(obj);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof ParagraphML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
		
	public void addChild(int idx, ElementML child) {
		if (!(child instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		return false;
	}
	
	public void addSibling(ElementML elem, boolean after) {
		throw new UnsupportedOperationException("BodyML cannot have sibling.");
	}
	
	public void setParent(ElementML parent) {
		if (!(parent instanceof DocumentML)) {
			throw new IllegalArgumentException("NOT a DocumentML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {
		if (this.docxObject == null) {
			;//do nothing
		} else if (docxParent instanceof WordprocessingMLPackage){
			((org.docx4j.jaxb.document.Body) this.docxObject).setParent(docxParent);
		} else {
			throw new IllegalArgumentException(
					"docxParent = " + docxParent.getClass().getName());
		}
	}// setDocxParent()
	
	protected List<Object> getDocxChildren() {
		if (this.docxObject == null) {
			return null;
		}
		
		org.docx4j.jaxb.document.Body body = 
			(org.docx4j.jaxb.document.Body) this.docxObject;
		return body.getBlockLevelElements();
	}
	
	protected void init(Object docxObject) {
		org.docx4j.jaxb.document.Body body = null;
		
		if (docxObject == null) {
			;//implied BodyML
			
		} else if (docxObject instanceof org.docx4j.jaxb.document.Body) {
			body = (org.docx4j.jaxb.document.Body) docxObject;
			this.isDummy = false;

		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initChildren(body);
	}

	private void initChildren(org.docx4j.jaxb.document.Body body) {
		if (body == null) {
			return;
		}
		
		List <Object> bodyChildren = body.getBlockLevelElements();
		if (!bodyChildren.isEmpty()) {
			this.children = new ArrayList<ElementML>(bodyChildren.size());
			for (Object o : bodyChildren) {
				ParagraphML paraML = new ParagraphML(o);
				paraML.setParent(BodyML.this);
				this.children.add(paraML);
			}
		}
	}// initChildren()
	

}// BodyML class



















