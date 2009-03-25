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

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;

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
		return new BodyML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = false;
		
		if ((child instanceof ParagraphML)
			|| (child instanceof SdtBlockML)) {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
		
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof ParagraphML)
			&& !(child instanceof TableML)
			&& !(child instanceof SdtBlockML)) {
			throw new IllegalArgumentException("Child type = " + child.getClass().getSimpleName());
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		return false;
	}
	
	public void addSibling(ElementML elem, boolean after) {
		throw new UnsupportedOperationException("BodyML cannot have sibling.");
	}
	
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof DocumentML)) {
			throw new IllegalArgumentException("NOT a DocumentML.");
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		if (this.docxObject == null) {
			return null;
		}
		
		org.docx4j.wml.Body body = 
			(org.docx4j.wml.Body) this.docxObject;
		return body.getEGBlockLevelElts();
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.Body body = null;
		
		if (docxObject == null) {
			;//implied BodyML
			
		} else if (docxObject instanceof org.docx4j.wml.Body) {
			body = (org.docx4j.wml.Body) docxObject;
			this.isDummy = false;

		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initChildren(body);
	}

	private void initChildren(org.docx4j.wml.Body body) {
		if (body == null) {
			return;
		}
		
		List <Object> bodyChildren = body.getEGBlockLevelElts();
		if (!bodyChildren.isEmpty()) {
			this.children = new ArrayList<ElementML>(bodyChildren.size());
			
			ElementML ml = null;
			for (Object obj : bodyChildren) {
				Object value = JAXBIntrospector.getValue(obj);
				
				if (value instanceof org.docx4j.wml.SdtBlock) {
					ml = new SdtBlockML(obj);
				} else if (value instanceof org.docx4j.wml.Tbl) {
					ml = new TableML(obj);
				} else {
					ml = new ParagraphML(obj);
				}
				
				ml.setParent(BodyML.this);
				this.children.add(ml);
			}
		}
	}// initChildren()
	

}// BodyML class



















