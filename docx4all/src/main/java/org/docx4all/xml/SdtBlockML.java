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

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;

/**
 *	@author Jojada Tirtowidjojo - 16/04/2008
 */
public class SdtBlockML extends ElementML {
	private static Logger log = Logger.getLogger(SdtBlockML.class);

	private SdtPrML sdtPr;
	
	public SdtBlockML(Object docxObject) {
		this(docxObject, false);
	}
	
	public SdtBlockML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}

	/**
	 * Gets the paragraph property element of this paragraph.
	 * 
	 * @return a ParagraphPropertiesML, if any
	 *         null, otherwise 
	 */
	public SdtPrML getSdtProperties() {
		return this.sdtPr;
	}

	public void setSdtProperties(SdtPrML sdtPr) {
		if (sdtPr != null && sdtPr.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}

		this.sdtPr = sdtPr;

		if (this.docxObject != null) {
			org.docx4j.wml.SdtPr newDocxSdtPr = null;
			if (sdtPr != null) {
				sdtPr.setParent(SdtBlockML.this);
				newDocxSdtPr = (org.docx4j.wml.SdtPr) sdtPr.getDocxObject();
			}
			org.docx4j.wml.SdtBlock sdtBlock = 
				(org.docx4j.wml.SdtBlock) JAXBIntrospector.getValue(this.docxObject);
			sdtBlock.setSdtPr(newDocxSdtPr);

			if (newDocxSdtPr != null) {
				newDocxSdtPr.setParent(this.docxObject);
			}
		}
	}

	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}

		return new SdtBlockML(obj, this.isDummy);
	}

	public boolean canAddSibling(ElementML elem, boolean after) {
		boolean canAdd = false;
		
		if (elem instanceof SdtBlockML
			|| elem instanceof ParagraphML
			|| elem instanceof TableML) {
			//TODO:Current implementation disallows other types of sibling
			canAdd = super.canAddSibling(elem, after);
		}
		
		return canAdd;
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;

		if (!(child instanceof ParagraphML
				|| child instanceof TableML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}

		return canAdd;
	}

	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof ParagraphML
				|| child instanceof TableML)) {
			throw new IllegalArgumentException("NOT a ParagraphML nor a TableML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}

	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof BodyML)) {
			throw new IllegalArgumentException("NOT a BodyML.");
		}
		this.parent = parent;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());
		sb.append(" - Id=");
		sb.append(getSdtProperties().getPlutextId());
		sb.append(" - Tag=");
		sb.append(getSdtProperties().getTagValue());
		
		return sb.toString();
	}
	
	/**
	 * The real (direct) parent of those docx children 
	 * listed in getDocxChildren().
	 * 
	 * @return docxObject
	 */
	public Object getDocxChildParent() {
		if (this.docxObject == null) {
			return null;
		}
		
		org.docx4j.wml.SdtBlock sdtBlock = 
			(org.docx4j.wml.SdtBlock) JAXBIntrospector.getValue(this.docxObject);
		return sdtBlock.getSdtContent();
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;

		if (this.docxObject == null) {
			;//do nothing
		} else {
			org.docx4j.wml.SdtBlock sdtBlock = 
				(org.docx4j.wml.SdtBlock) JAXBIntrospector.getValue(this.docxObject);
			theChildren = sdtBlock.getSdtContent().getEGContentBlockContent();
		}

		return theChildren;
	}

	protected void init(Object docxObject) {
		org.docx4j.wml.SdtBlock sdtBlock = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied SdtBlockML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.SdtBlock) {
				sdtBlock = (org.docx4j.wml.SdtBlock) value;
				this.isDummy = false;
			} else {
				throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
			}
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);			
		}
		
		initSdtProperties(sdtBlock);
		initChildren(sdtBlock);
	}

	private void initSdtProperties(org.docx4j.wml.SdtBlock sdtBlock) {
		this.sdtPr = null;
		if (sdtBlock != null) {
			//if not an implied SdtBlockML
			org.docx4j.wml.SdtPr pr = sdtBlock.getSdtPr();
			if (pr != null) {
				this.sdtPr = new SdtPrML(pr);
				this.sdtPr.setParent(SdtBlockML.this);
			}
		}
	}

	private void initChildren(org.docx4j.wml.SdtBlock sdtBlock) {
		this.children = null;

		if (sdtBlock == null) {
			return;
		}

		List<Object> list = sdtBlock.getSdtContent().getEGContentBlockContent();
		if (!list.isEmpty()) {
			this.children = new ArrayList<ElementML>(list.size());
			for (Object obj : list) {
				Object value = JAXBIntrospector.getValue(obj);
				
				ElementML ml = null;
				if (value instanceof org.docx4j.wml.Tbl) {
					ml = new TableML(obj);
					ml.setParent(SdtBlockML.this);
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
						ml.setParent(SdtBlockML.this);
						this.children.add(ml);
					}
					
				} else {
					ml = new ParagraphML(obj);
					ml.setParent(SdtBlockML.this);
					this.children.add(ml);
				}
			}
		}
	}// initChildren()

}// SdtBlockML class

