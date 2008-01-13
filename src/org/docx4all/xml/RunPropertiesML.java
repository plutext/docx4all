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
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Underline;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(RunPropertiesML.class);
	
	private MutableAttributeSet attrs;
	
	public RunPropertiesML(RPr rPr) {
		super(rPr, false);
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
		
		RPr rPr = (RPr) this.docxObject;
		
		BooleanDefaultTrue bdt = null;
		
		//BOLD Attribute
		Boolean bvalue = (Boolean) this.attrs.getAttribute(StyleConstants.Bold);
		if (bvalue != null) {
			bdt = ObjectFactory.createBooleanDefaultTrue(bvalue);
			rPr.setB(bdt);
		} else {
			rPr.setB(null);
		}
		
		//ITALIC Attribute
		bvalue = (Boolean) this.attrs.getAttribute(StyleConstants.Italic);
		if (bvalue != null) {
			bdt = ObjectFactory.createBooleanDefaultTrue(bvalue);
			rPr.setI(bdt);
		} else {
			rPr.setI(null);
		}
		
		//UNDERLINE Attribute
		bvalue = (Boolean) this.attrs.getAttribute(StyleConstants.Underline);
		if (bvalue != null) {
			if (bvalue.booleanValue()) {
				if (hasUnderlineSet(rPr)) {
					//As we do not support underline style
					//and color yet we do not touch the 
					//original setting of rPr underline
				} else {
					org.docx4j.wml.Underline u = 
						ObjectFactory.createUnderline("single", "auto");
					rPr.setU(u);
				}
			} else {
				rPr.setU(null);
			}
		} else {
			rPr.setU(null);
		}
	}
	
	public Object clone() {
		RPr obj = null;
		if (this.docxObject != null) {
			obj = (RPr) XmlUtils.deepCopy(this.docxObject);
		}
		return new RunPropertiesML(obj);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		return false;
	}
	
	public void addChild(int idx, ElementML child) {
		throw new UnsupportedOperationException("Cannot have a child.");
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML.");
		}
		this.parent = parent;
	}
	
	public void setDocxParent(Object docxParent) {
		RPr rPr = (RPr) getDocxObject();
		if (rPr == null) {
			;//do nothing
		} else {
			rPr.setParent(docxParent);
		}
	}
	
	public List<Object> getDocxChildren() {
		return null;//do not have children
	}
		
	protected void init(Object docxObject) {
		initAttributes((RPr) docxObject);
	}
	
	private void initAttributes(RPr rPr) {
		this.attrs = new SimpleAttributeSet();
		
		if (rPr == null) {
			return;
		}
		
		//BOLD Attribute
		BooleanDefaultTrue bdt = rPr.getB();
		if (bdt != null && bdt.isVal()) {
			StyleConstants.setBold(this.attrs, Boolean.TRUE);
		}

		//ITALIC Attribute
		bdt = rPr.getI();
		if (bdt != null && bdt.isVal()) {
			StyleConstants.setItalic(this.attrs, Boolean.TRUE);
		}
		
		//UNDERLINE Attribute
		//TODO: To support underline style and color
		if (hasUnderlineSet(rPr)) {
			StyleConstants.setUnderline(this.attrs, Boolean.TRUE);
		}
		
	}// initAttributes()

	private boolean hasUnderlineSet(RPr rPr) {
		boolean hasUnderlineSet = false;
		
		Underline u = rPr.getU();
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


























