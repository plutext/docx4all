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
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;

/**
 *	@author Jojada Tirtowidjojo - 11/07/2009
 */
public class InlineTransparentML extends ElementML {
	private static Logger log = Logger.getLogger(InlineTransparentML.class);

	public InlineTransparentML(Object docxObject) {
		this(docxObject, false);
	}
	
	public InlineTransparentML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		return new InlineTransparentML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunML)
			&& !(child instanceof RunInsML)
			&& !(child instanceof RunDelML)
			&& !(child instanceof HyperlinkML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof RunML)
			&& !(child instanceof RunInsML)
			&& !(child instanceof RunDelML)
			&& !(child instanceof HyperlinkML)) {
			throw new IllegalArgumentException("Cannot become a child.");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		//Subject to future implementation.
		throw new UnsupportedOperationException();
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (this.docxObject == null) {
			;//do nothing
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.CTSmartTagRun) {
				theChildren = ((org.docx4j.wml.CTSmartTagRun)value).getParagraphContent();
			}
			
		} else {
			;//should not come here. See init().
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied ParagraphML
			
		} else if (inspector.isElement(docxObject)) {
			throw new IllegalArgumentException("Unsupported Docx Object = " + docxObject);	
			
		} else {
			org.docx4j.wml.CTSmartTagRun smartTag = null;
			
			Object value = JAXBIntrospector.getValue(docxObject);
			if (value instanceof org.docx4j.wml.CTSmartTagRun) {
				smartTag = (org.docx4j.wml.CTSmartTagRun) value;
				this.isDummy = false;
				
			} else {
				//Create a dummy ParagraphML for this unsupported element
				//TODO: A more informative text content in dummy ParagraphML
				QName name = inspector.getElementName(docxObject);
				String renderedText;
				if (name != null) {
					renderedText = XmlUtil.getEnclosingTagPair(name);
				} else {
					// Should not happen but it could.
					renderedText = "<w:unknownTag></w:unknownTag>";
					log.warn("init(): Unknown tag was detected for a JAXBElement = "
						+ XmlUtils.marshaltoString(docxObject, true));
				}
				smartTag = ObjectFactory.createCTSmartTagRun(renderedText);
				this.isDummy = true;
			}
			
			initChildren(smartTag);
		}
	}
	
	private void initChildren(org.docx4j.wml.CTSmartTagRun smartTag) {
		this.children = null;
		
		if (smartTag == null) {
			return;
		}
		
		List<Object> pKids = smartTag.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());

			ElementML ml = null;
			for (Object o : pKids) {
				Object value = JAXBIntrospector.getValue(o);
								
				if (value instanceof org.docx4j.wml.RunIns) {
					ml = new RunInsML(value, this.isDummy);
					ml.setParent(InlineTransparentML.this);
					this.children.add(ml);
				} else if (value instanceof org.docx4j.wml.RunDel) {
					ml = new RunDelML(value, this.isDummy);
					ml.setParent(InlineTransparentML.this);
					this.children.add(ml);
				} else if (value instanceof org.docx4j.wml.P.Hyperlink) {
					ml = new HyperlinkML(value, this.isDummy);
					ml.setParent(InlineTransparentML.this);
					this.children.add(ml);
				} else if (value instanceof org.docx4j.wml.CTSmartTagRun) {
					InlineTransparentML transparent =
						new InlineTransparentML(value, this.isDummy);
					//Current implementation is using InlineTransparentML
					//as surrogate container.
					if (transparent.getChildrenCount() > 0) {
						List<ElementML> list = 
							new ArrayList<ElementML>(
								transparent.getChildren());
						for (ElementML elem: list) {
							elem.delete();
							elem.setParent(InlineTransparentML.this);
							this.children.add(elem);
						}
					}
				} else {
					ml = new RunML(o, this.isDummy);
					ml.setParent(InlineTransparentML.this);
					this.children.add(ml);
				}
				
			}
		}
	}// initChildren()
	

}// SmartTagML class



















