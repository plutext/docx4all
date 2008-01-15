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

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.wml.PPr;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphML extends ElementML {
	private static Logger log = Logger.getLogger(ParagraphML.class);

	private ParagraphPropertiesML pPr;
	
	public ParagraphML(Object docxObject) {
		this(docxObject, false);
	}
	
	public ParagraphML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public PropertiesContainerML getParagraphProperties() {
		return this.pPr;
	}
	
	public void setParagraphProperties(ParagraphPropertiesML pPr) {
		if (pPr != null && pPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		
		this.pPr = pPr;
		if (this.docxObject instanceof org.docx4j.wml.P) {
			org.docx4j.wml.PPr newDocxPPr = null;
			if (pPr != null) {
				pPr.setParent(ParagraphML.this);
				newDocxPPr = 
					(org.docx4j.wml.PPr) pPr.getDocxObject();
			}
			org.docx4j.wml.P p = 
				(org.docx4j.wml.P) this.docxObject;
			p.setPPr(newDocxPPr);
			
			if (newDocxPPr != null) {
				newDocxPPr.setParent(p);
			}
		}
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new ParagraphML(obj);
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
	
	public void addChild(int idx, ElementML child) {
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof BodyML)) {
			throw new IllegalArgumentException("NOT a BodyML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {		
		if (this.docxObject == null) {
			;//do nothing
		} else if (this.docxObject instanceof JAXBElement) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) this.docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();

			if ("org.docx4j.wml.P".equals(typeName)) {
				org.docx4j.wml.P p = 
					(org.docx4j.wml.P) jaxbElem.getValue();
				p.setParent(docxParent);
			} else if ("org.docx4j.wml.Tbl".equals(typeName)) {
				org.docx4j.wml.Tbl tbl =
					(org.docx4j.wml.Tbl) jaxbElem.getValue();
				tbl.setParent(docxParent);
			} else if ("org.docx4j.wml.RunTrackChange".equals(typeName)) {
				org.docx4j.wml.RunTrackChange rtc =
					(org.docx4j.wml.RunTrackChange) jaxbElem.getValue();
				rtc.setParent(docxParent);
			} else {
				throw new IllegalArgumentException(
						"Unsupported Docx Object Type = " + typeName);
			}
		} else if (this.docxObject instanceof org.docx4j.wml.Sdt) {
			org.docx4j.wml.Sdt sdt = 
				(org.docx4j.wml.Sdt) docxObject;
			sdt.setParent(docxParent);
		} else {
			;//should not come here. See init().
		}
	}// setDocxParent()
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		if (this.docxObject == null) {
			;//do nothing
		} else if (this.docxObject instanceof JAXBElement) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) this.docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();

			if ("org.docx4j.wml.P".equals(typeName)) {
				org.docx4j.wml.P p = 
					(org.docx4j.wml.P) jaxbElem.getValue();
				theChildren = p.getParagraphContent();
			} else if ("org.docx4j.wml.Tbl".equals(typeName)) {
				org.docx4j.wml.Tbl tbl =
					(org.docx4j.wml.Tbl) jaxbElem.getValue();
				theChildren = new ArrayList<Object>();
				theChildren.addAll(tbl.getEGContentRowContent());
			} else if ("org.docx4j.wml.RunTrackChange".equals(typeName)) {
				org.docx4j.wml.RunTrackChange rtc =
					(org.docx4j.wml.RunTrackChange) jaxbElem.getValue();
				theChildren = rtc.getEGContentRunContent();
			} else {
				throw new IllegalArgumentException(
						"Unsupported Docx Object Type = " + typeName);
			}
		} else if (this.docxObject instanceof org.docx4j.wml.Sdt) {
			org.docx4j.wml.Sdt sdt = 
				(org.docx4j.wml.Sdt) docxObject;
			org.docx4j.wml.SdtContent content = sdt.getSdtContent();
			if (content != null) {
				theChildren = new ArrayList<Object>();
				theChildren.add(content);
			}
		} else {
			;//should not come here. See init().
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.P para = null;
		
		if (docxObject == null) {
			;//implied ParagraphML
			
		} else if (docxObject instanceof JAXBElement<?>) {
			JAXBElement<?> jaxbElem = (JAXBElement<?>) docxObject;
			String typeName = jaxbElem.getDeclaredType().getName();
			
			if ("org.docx4j.wml.P".equals(typeName)) {
				para = (org.docx4j.wml.P) jaxbElem.getValue();
				this.isDummy = false;
				
			} else {
				//Create a dummy ParagraphML for this unsupported element
				// TODO: A more informative text content in dummy ParagraphML
				String renderedText = 
					XmlUtil.getEnclosingTagPair(jaxbElem.getName());
				para = ObjectFactory.createP(renderedText);
				this.isDummy = true;
			}
			
		} else if (docxObject instanceof org.docx4j.wml.Sdt) {
			//Unsupported yet
			// TODO: A more informative text content in dummy ParagraphML
			String s = "<w:sdt></w:sdt>";
			para = ObjectFactory.createP(s);
			this.isDummy = true;
	
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initParagraphProperties(para);
		initChildren(para);
	}
	
	private void initParagraphProperties(org.docx4j.wml.P para) {
		this.pPr = null;
		if (para != null) {
			//if not an implied ParagraphML
			PPr pProp = para.getPPr();
			if (pProp != null) {
				this.pPr = new ParagraphPropertiesML(pProp);
				this.pPr.setParent(ParagraphML.this);
			}
		}
	}
	
	private void initChildren(org.docx4j.wml.P para) {
		this.children = null;
		
		if (para == null) {
			return;
		}
		
		List<Object> pKids = para.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(ParagraphML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	
}// ParagraphML class





















