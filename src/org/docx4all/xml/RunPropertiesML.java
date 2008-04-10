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

import java.math.BigInteger;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.StyleSheet;
import org.docx4all.swing.text.WordMLStyleConstants;
import org.docx4j.XmlUtils;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;

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
    
	public void addAttributes(AttributeSet attrs) {
		this.attrs.addAttributes(attrs);
	}
	
	public MutableAttributeSet getAttributeSet() {
		return new SimpleAttributeSet(this.attrs);
	}
	
    public void removeAttributes(AttributeSet attributes) {
    	attrs.removeAttributes(attributes);
	}

    public void removeAttribute(Object name) {
    	attrs.removeAttribute(name);
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
				if (StyleSheet.hasUnderlineSet(rPr)) {
					//As we do not support underline style
					//and color yet we do not touch the 
					//original setting of rPr underline
				} else {
					org.docx4j.wml.U u = 
						ObjectFactory.createUnderline("single", "auto");
					rPr.setU(u);
				}
			} else {
				rPr.setU(null);
			}
		} else {
			rPr.setU(null);
		}
		
		//FONT FAMILY Attribute
		String strValue = StyleConstants.getFontFamily(this.attrs);
		RFonts rfonts = rPr.getRFonts();
		if (rfonts != null) {
			//Just set the asscii value.
			//Do not touch other attributes.
			rfonts.setAscii(strValue);
		} else if (strValue != null) {
			rfonts = ObjectFactory.createRPrRFonts(strValue);
			rfonts.setParent(rPr);
			rPr.setRFonts(rfonts);
		}
		
		//FONT SIZE Attribute
		Integer intValue = (Integer) this.attrs.getAttribute(StyleConstants.FontSize);
		HpsMeasure sz = rPr.getSz();
		if (sz != null) {
			if (intValue == null) {
				rPr.setSz(null);
			} else {
				BigInteger val = new BigInteger(intValue.toString());
				sz.setVal(val);
			}
		} else if (intValue != null) {
			sz = ObjectFactory.createHpsMeasure(intValue);
			sz.setParent(rPr);
			rPr.setSz(sz);
		}
		
    	//RStyle
    	if (this.attrs.isDefined(WordMLStyleConstants.RStyleAttribute)) {
    		String rStyle =
    			(String) this.attrs.getAttribute(WordMLStyleConstants.RStyleAttribute);
    		if (rPr.getRStyle() == null) {
    			rPr.setRStyle(ObjectFactory.createRStyle(rStyle));
    		} else {
    			rPr.getRStyle().setVal(rStyle);
    		}
    	} else {
    		rPr.setRStyle(null);
    	}
    	
	} //save()
	
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
		this.attrs = new SimpleAttributeSet();
		
		if (docxObject != null) {
			StyleSheet.addAttributes(this.attrs, (RPr) docxObject);
		}
	}
	
}// RunPropertiesML class


























