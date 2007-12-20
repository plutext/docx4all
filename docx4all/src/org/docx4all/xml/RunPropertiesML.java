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
import org.docx4j.jaxb.document.BooleanDefaultTrue;
import org.docx4j.jaxb.document.ObjectFactory;
import org.docx4j.jaxb.document.RPr;
import org.docx4j.jaxb.document.Underline;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(RunPropertiesML.class);
	
	private final RPr rPr;
	private final MutableAttributeSet attrs;
	
	public RunPropertiesML(RPr rPr) {
		this.rPr = rPr;
		this.tag = WordML.Tag.rPr;
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
		return this.rPr == null;
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
		//BOLD Attribute
		BooleanDefaultTrue bdt = jaxbFactory.createBooleanDefaultTrue();
		if (StyleConstants.isBold(this.attrs)) {
			bdt.setVal(Boolean.TRUE);
		} else {
			bdt.setVal(false);
		}
		this.rPr.setB(bdt);
		
		//ITALIC Attribute
		bdt = jaxbFactory.createBooleanDefaultTrue();
		if (StyleConstants.isItalic(this.attrs)) {
			bdt.setVal(Boolean.TRUE);
		} else {
			bdt.setVal(false);
		}
		this.rPr.setI(bdt);
		
		//UNDERLINE Attribute
		if (StyleConstants.isUnderline(this.attrs)
			&& !rPrHasUnderlineSet()) {
			//As we do not support underline style
			//and color yet we do not touch the 
			//original setting of rPr underline
			List<String> val = this.rPr.getU().getVal();
			val.add("single");
			this.rPr.getU().setColor("auto");
		}
	}
	
	private void initAttributes() {
		//BOLD Attribute
		BooleanDefaultTrue bdt = this.rPr.getB();
		if (bdt != null && bdt.isVal()) {
			StyleConstants.setBold(this.attrs, Boolean.TRUE);
		}

		//ITALIC Attribute
		bdt = this.rPr.getI();
		if (bdt != null && bdt.isVal()) {
			StyleConstants.setItalic(this.attrs, Boolean.TRUE);
		}
		
		//UNDERLINE Attribute
		//TODO: To support underline style and color
		if (rPrHasUnderlineSet()) {
			StyleConstants.setUnderline(this.attrs, Boolean.TRUE);
		}
		
	}// initAttributes()

	private boolean rPrHasUnderlineSet() {
		boolean hasUnderlineSet = false;
		
		Underline u = this.rPr.getU();
		if (u != null) {
			String none = null;
			for (String s : u.getVal()) {
				if (s.equalsIgnoreCase("none")) {
					none = s;
				}
			}
			hasUnderlineSet = (none == null && !u.getVal().isEmpty());
		}
		
		return hasUnderlineSet;
	}
	
}// RunPropertiesML class


























