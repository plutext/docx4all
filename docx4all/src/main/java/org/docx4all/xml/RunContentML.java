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

import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.Context;

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
		
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		if (this.docxObject != null 
			&& inspector.isElement(this.docxObject)) {
			Object value = JAXBIntrospector.getValue(this.docxObject);
			if (value instanceof org.docx4j.wml.Text) {
				org.docx4j.wml.Text t = (org.docx4j.wml.Text) value;
				t.setValue(textContent);
			} else if (value instanceof org.docx4j.wml.DelText) {
				org.docx4j.wml.DelText dt = (org.docx4j.wml.DelText) value;
				dt.setValue(textContent);
			}
		}
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
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		throw new UnsupportedOperationException("Cannot have a child.");
	}
	
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML.");
		}
		this.parent = parent;
	}
	
	public List<Object> getDocxChildren() {
		return null;//do not have children
	}
	
	protected void init(Object docxObject) {
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		if (docxObject == null) {
			;//implied RunContentML
			
		} else if (inspector.isElement(docxObject)) {
			Object value = JAXBIntrospector.getValue(docxObject);

			if (value instanceof org.docx4j.wml.Text) {
				String s = ((org.docx4j.wml.Text) value).getValue();
				if (s != null && s.length() > 0) {
					this.textContent = s;
				} else {
					this.textContent = Constants.TEXT_ELEMENT_EMPTY_VALUE;
				}
			} else if (value instanceof org.docx4j.wml.DelText) {
				String s = ((org.docx4j.wml.DelText) value).getValue();
				if (s != null && s.length() > 0) {
					this.textContent = s;
				} else {
					this.textContent = Constants.TEXT_ELEMENT_EMPTY_VALUE;
				}
			} else if (value instanceof org.docx4j.wml.Br) {
				// TODO: Full support of BR element
				this.textContent = Constants.NEWLINE;

			} else if (value instanceof org.docx4j.wml.R.Cr) {
				this.textContent = Constants.NEWLINE;

			} else if (value instanceof org.docx4j.wml.R.Tab) {
				this.textContent = Constants.TAB;
			
			} else if (value instanceof org.docx4j.wml.FldChar) {
				org.docx4j.wml.FldChar fldChar = 
					(org.docx4j.wml.FldChar) value;
				if (fldChar.getFldCharType() 
					== org.docx4j.wml.STFldCharType.BEGIN) {
					this.textContent = Constants.FLDCHAR_BEGIN;
				} else if (fldChar.getFldCharType()
					== org.docx4j.wml.STFldCharType.END) {
					this.textContent = Constants.FLDCHAR_END;
				} else {
					this.textContent = Constants.FLDCHAR_SEPARATE;
				}
			//} else if (value instanceof org.docx4j.wml.R.NoBreakHyphen) {
				// Unsupported yet

			//} else if (value instanceof org.docx4j.wml.R.SoftHyphen) {
				// Unsupported yet

			} else {
				// Create a dummy RunContentML for this unsupported element
				// TODO: A more informative text content in dummy RunContentML
				QName name = inspector.getElementName(docxObject);
				if (name != null) {
					this.textContent = XmlUtil.getEnclosingTagPair(name);
					this.isDummy = true;
				} else {
					//Should not happen but it could.
					this.textContent="<w:unknownTag></w:unknownTag>";
					this.isDummy = true;
					log.warn("init(): Unknown tag was detected for a JAXBElement = "
							+ XmlUtils.marshaltoString(docxObject, true));
				}
			}
		} else {
			throw new IllegalArgumentException(
					"Unsupported Docx Object = " + docxObject);
		}
		
	}// init()
	
}// RunContentML class



















