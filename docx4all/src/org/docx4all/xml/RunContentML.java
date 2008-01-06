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

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.jaxbcontexts.DocumentContext;

/**
 *	@author Jojada Tirtowidjojo - 06/12/2007
 */
public class RunContentML extends ElementML {
	private static Logger log = Logger.getLogger(RunContentML.class);
	
	protected String textContent;
	
	public RunContentML(Object docxObject) {
		this(docxObject, false);
	}
	
	public RunContentML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public String getTextContent() {
		return this.textContent;
	}
	
	public void setTextContent(String textContent) {
		this.textContent = textContent;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		return new RunContentML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		//cannot have child
		return false;
	}
	
	public void addChild(int idx, ElementML child) {
		throw new UnsupportedOperationException("Cannot have a child.");
	}
	
	public void setParent(ElementML parent) {
		if (!(parent instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {
		if (this.docxObject == null) {
			;//do nothing
		} else if (this.docxObject instanceof org.docx4j.jaxb.document.Br) {
			org.docx4j.jaxb.document.Br br = 
				(org.docx4j.jaxb.document.Br) this.docxObject;
			br.setParent(docxParent);

		} else if (docxObject instanceof org.docx4j.jaxb.document.Cr) {
			org.docx4j.jaxb.document.Cr cr = 
				(org.docx4j.jaxb.document.Cr) this.docxObject;
			cr.setParent(docxParent);

		} else if (docxObject instanceof org.docx4j.jaxb.document.NoBreakHyphen) {
			org.docx4j.jaxb.document.NoBreakHyphen nbh = 
				(org.docx4j.jaxb.document.NoBreakHyphen) this.docxObject;
			nbh.setParent(docxParent);

		} else if (docxObject instanceof org.docx4j.jaxb.document.SoftHyphen) {
			org.docx4j.jaxb.document.SoftHyphen sh = 
				(org.docx4j.jaxb.document.SoftHyphen) this.docxObject;
			sh.setParent(docxParent);

		} else if (docxObject instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();
			if ("org.docx4j.jaxb.document.Text".equals(typeName)) {
				org.docx4j.jaxb.document.Text t = 
					(org.docx4j.jaxb.document.Text) jaxbElem.getValue();
				t.setParent(docxParent);
				
			} else {
				throw new IllegalArgumentException(
						"Unsupported Docx Object Type = " + typeName);
			}
		} else {
			;//should not come here. See init().
		}
	}// setDocxParent()
	
	public List<Object> getDocxChildren() {
		return null;//do not have children
	}
	
	protected void init(Object docxObject) {
		if (docxObject == null) {
			;//implied RunContentML
			
		} else if (docxObject instanceof org.docx4j.jaxb.document.Br) {
			// TODO: Full support of BR element
			this.textContent = Constants.NEWLINE;
			
		} else if (docxObject instanceof org.docx4j.jaxb.document.Cr) {
			this.textContent = Constants.NEWLINE;
										
		} else if (docxObject instanceof org.docx4j.jaxb.document.NoBreakHyphen) {
			//Unsupported yet
			QName name = DocumentContext.jc.createJAXBIntrospector().getElementName(docxObject);
			this.textContent = XmlUtil.getEnclosingTagPair(name);
			this.isDummy = true;

		} else if (docxObject instanceof org.docx4j.jaxb.document.SoftHyphen) {
			//Unsupported yet
			QName name = DocumentContext.jc.createJAXBIntrospector().getElementName(docxObject);
			this.textContent = XmlUtil.getEnclosingTagPair(name);
			this.isDummy = true;

		} else if (docxObject instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();
			if ("org.docx4j.jaxb.document.Text".equals(typeName)) {
				org.docx4j.jaxb.document.Text t = 
					(org.docx4j.jaxb.document.Text) 
						jaxbElem.getValue();
				String s = t.getValue();
				if (s != null && s.length() > 0) {
					this.textContent = s;
				} else {
					this.textContent = Constants.TEXT_ELEMENT_EMPTY_VALUE;
				}
			} else {
				//Create a dummy RunContentML for this unsupported element
				// TODO: A more informative text content in dummy RunContentML
				this.textContent = XmlUtil.getEnclosingTagPair(jaxbElem.getName());
				this.isDummy = true;
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
	}// init()
	
}// RunContentML class



















