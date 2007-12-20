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

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4j.jaxb.document.Jc;
import org.docx4j.jaxb.document.ObjectFactory;
import org.docx4j.jaxb.document.PPr;
import org.docx4j.jaxb.document.STJc;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(ParagraphPropertiesML.class);

	private final PPr pPr;
	private final MutableAttributeSet attrs;
	
	public ParagraphPropertiesML(PPr pPr) {
		this.pPr = pPr;
		this.tag = WordML.Tag.pPr;
		this.attrs = new SimpleAttributeSet();
		
		initAttributes();
	}
	
	/**
	 * An implied ElementML is an ElementML that
	 * does not have a DOM element associated with it.
	 * This kind of ElementML may still have a WordML.Tag.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public boolean isImplied() {
		return this.pPr == null;
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
		ObjectFactory jaxbFactory = ElementMLFactory.getJaxbObjectFactory();
		//ALIGNMENT attribute
        Integer align = 
        	(Integer) this.attrs.getAttribute(StyleConstants.Alignment);
        if (align != null) {
    		Jc jc = jaxbFactory.createJc();
			if (align.intValue() == StyleConstants.ALIGN_LEFT) {
				jc.setVal(STJc.LEFT);
			} else if (align.intValue() == StyleConstants.ALIGN_RIGHT) {
				jc.setVal(STJc.RIGHT);
			} else if (align.intValue() == StyleConstants.ALIGN_CENTER) {
				jc.setVal(STJc.CENTER);
			} else if (align.intValue() == StyleConstants.ALIGN_JUSTIFIED) {
				jc.setVal(STJc.BOTH);
			}
			this.pPr.setJc(jc);
		}
	}
	
	private void initAttributes() {
		//ALIGNMENT attribute
		Jc jc = this.pPr.getJc();
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
	}
	
}// ParagraphPropertiesML class
























