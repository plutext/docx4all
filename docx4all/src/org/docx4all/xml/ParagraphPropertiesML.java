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

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.document.Jc;
import org.docx4j.jaxb.document.PPr;
import org.docx4j.jaxb.document.STJc;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(ParagraphPropertiesML.class);

	private MutableAttributeSet attrs;
	
	public ParagraphPropertiesML(PPr pPr) {
		super(pPr, false);
	}
	
    public void addAttribute(Object name, Object value) {
    	this.attrs.addAttribute(name, value);
    }
    
	public void addAttribute(AttributeSet attrs) {
		this.attrs.addAttributes(attrs);
	}
	
	public MutableAttributeSet getAttributeSet() {
		return new SimpleAttributeSet(this.attrs);
	}
	
	public void save() {
		if (this.docxObject == null) {
			return;
		}
		
		//ALIGNMENT attribute
        Integer align = 
        	(Integer) this.attrs.getAttribute(StyleConstants.Alignment);
    	org.docx4j.jaxb.document.Jc jc = ObjectFactory.createJc(align);
    	if (jc != null) {
        	org.docx4j.jaxb.document.PPr pPr = 
        		(org.docx4j.jaxb.document.PPr) this.docxObject;
			pPr.setJc(jc);
		}
	}
	
	public Object clone() {
		PPr obj = null;
		if (this.docxObject != null) {
			obj = (PPr) XmlUtils.deepCopy(this.docxObject);
		}
		return new ParagraphPropertiesML(obj);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		return false;
	}
	
	public void addChild(int idx, ElementML child) {
		throw new UnsupportedOperationException("Cannot have a child.");
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {
		PPr pPr = (PPr) getDocxObject();
		if (pPr == null) {
			;//do nothing
		} else {
			pPr.setParent(docxParent);
		}
	}
	
	public List<Object> getDocxChildren() {
		return null;//do not have children
	}
		
	protected void init(Object docxObject) {
		initAttributes((PPr) docxObject);
	}
	
	private void initAttributes(PPr pPr) {
		this.attrs = new SimpleAttributeSet();
		
		if (pPr == null) {
			return;
		}
		
		//ALIGNMENT attribute
		Jc jc = pPr.getJc();
		if (jc != null) {
			if (jc.getVal() == STJc.LEFT) {
				StyleConstants.setAlignment(
						this.attrs,
						StyleConstants.ALIGN_LEFT);
			} else if (jc.getVal() == STJc.RIGHT) {
				StyleConstants.setAlignment(
						this.attrs,
						StyleConstants.ALIGN_RIGHT);
			} else if (jc.getVal() == STJc.CENTER) {
				StyleConstants.setAlignment(
						this.attrs,
						StyleConstants.ALIGN_CENTER);
			} else if (jc.getVal() == STJc.BOTH) {
				StyleConstants.setAlignment(
						this.attrs,
						StyleConstants.ALIGN_JUSTIFIED);
			}
		}
	}// initAttributes()
	
}// ParagraphPropertiesML class
























