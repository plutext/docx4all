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
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class DocumentML extends ElementML {
	private static Logger log = Logger.getLogger(DocumentML.class);
	
	private final WordprocessingMLPackage docPackage;
	
	public DocumentML(WordprocessingMLPackage docPackage) {
		super((docPackage != null) ? docPackage.getMainDocumentPart().getJaxbElement() : null, false);
		this.docPackage = docPackage;
	}

	public Object clone() {
		WordprocessingMLPackage clonedPackage = null;
		
		if (this.docPackage != null) {
			MainDocumentPart documentPart = 
				this.docPackage.getMainDocumentPart();			
			org.docx4j.jaxb.document.Document doc = 
				(org.docx4j.jaxb.document.Document)
				XmlUtils.deepCopy(documentPart.getJaxbElement());
			clonedPackage = ObjectFactory.createDocumentPackage(doc);
		}
		
		return new DocumentML(clonedPackage);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof BodyML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child) {
		if (!(child instanceof BodyML)) {
			throw new IllegalArgumentException("NOT a BodyML");
		}
		
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		if (idx != 0) {
			throw new IllegalArgumentException("idx=" + idx + ". Zero is expected.");
		}
		
		super.addChild(idx, child);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		return false;
	}
	
	public void addSibling(ElementML elem, boolean after) {
		throw new UnsupportedOperationException("DocumentML cannot have sibling.");
	}
	
	public void setDocxParent(Object docxParent) {
		throw new UnsupportedOperationException("DocumentML is root.");
	}
	
	public void setParent(ElementML parent) {
		throw new UnsupportedOperationException("DocumentML is root.");
	}
	
	protected List<Object> getDocxChildren() {
		if (this.docxObject == null) {
			return null;
		}
		
		List<Object> theChildren = new ArrayList<Object>(1);
		org.docx4j.jaxb.document.Document doc = 
			(org.docx4j.jaxb.document.Document) this.docxObject;
		theChildren.add(doc.getBody());
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.jaxb.document.Document doc = null;
		
		if (docxObject == null) {
			;//implied DocumentML
		} else if (docxObject instanceof org.docx4j.jaxb.document.Document) {
			doc = (org.docx4j.jaxb.document.Document) docxObject;
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
			
		initChildren(doc);
	}
	
	private void initChildren(org.docx4j.jaxb.document.Document doc) {
		if (doc == null) {
			return;
		}
		
		if (doc.getBody() != null) {
			this.children = new ArrayList<ElementML>(1);
			BodyML bodyML = new BodyML(doc.getBody());
			bodyML.setParent(DocumentML.this);
			this.children.add(bodyML);
		}
	}// initChildren()
	
}// DocumentML class




















