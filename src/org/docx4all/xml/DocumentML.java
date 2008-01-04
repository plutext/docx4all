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
		super(docPackage.getMainDocumentPart().getDocumentObj(), false);
		this.docPackage = docPackage;
	}

	public Object clone() {
		WordprocessingMLPackage clonedPackage = null;
		
		if (this.docPackage != null) {
			MainDocumentPart documentPart = 
				this.docPackage.getMainDocumentPart();			
			org.docx4j.jaxb.document.Document doc = 
				(org.docx4j.jaxb.document.Document)
				XmlUtils.deepCopy(documentPart.getDocumentObj());
			clonedPackage = ObjectFactory.createDocumentPackage(doc);
		}
		
		return new DocumentML(clonedPackage);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof ParagraphML)) {
			canAdd = false;
		} else if (this.children == null) {
			canAdd = (idx == 0);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child) {
		if (!(child instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML");
		}
		super.addChild(idx, child);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		return false;
	}
	
	public void addSibling(ElementML elem, boolean after) {
		;//cannot have sibling;
	}
	
	public void setDocxParent(Object docxParent) {
		;//cannot have docx parent;
	}
	
	public void setParent(ElementML parent) {
		;//cannot have parent
	}
	
	protected List<Object> getDocxChildren() {
		if (this.docxObject == null) {
			return null;
		}
		
		org.docx4j.jaxb.document.Document doc = 
			(org.docx4j.jaxb.document.Document) this.docxObject;
		return doc.getBody().getBlockLevelElements();
	}
	
	protected void init(Object docxObject) {
		initChildren((org.docx4j.jaxb.document.Document) docxObject);
	}
	
	private void initChildren(org.docx4j.jaxb.document.Document doc) {
		if (doc == null) {
			return;
		}
		
		List <Object> bodyChildren = doc.getBody().getBlockLevelElements();
		if (!bodyChildren.isEmpty()) {
			this.children = new ArrayList<ElementML>(bodyChildren.size());
			for (Object o : bodyChildren) {
				ParagraphML paraML = new ParagraphML(o);
				paraML.setParent(DocumentML.this);
				this.children.add(paraML);
			}
		}
	}// initChildren()
	
}// DocumentML class




















