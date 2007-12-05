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

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4j.document.wordprocessingml.PStyle;
import org.docx4j.document.wordprocessingml.RunProperties;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class RunPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(RunPropertiesML.class);
	
	private final RunProperties rPr;
	private final MutableAttributeSet attrs = new SimpleAttributeSet();
	
	public RunPropertiesML(RunProperties rPr) {
		this.rPr = rPr;
		this.tag = WordML.Tag.rPr;
		
		initChildren();
		initAttributes();
	}
	
	public MutableAttributeSet getAttributeSet() {
		return this.attrs;
	}
	
	private void initChildren() {
		List props = this.rPr.getProperties();
		if (!props.isEmpty()) {
			this.children = new ArrayList<ElementML>(props.size());
			for (Object o : props) {
				if (o instanceof PStyle) {
					;// TODO: Future support
				} else {
					Element elem = (Element) o;
					WordML.Tag eTag = WordML.getTag(elem.getName());
					if (eTag != null && eTag.isPropertyTag()) {
						// supported property tag;
						// see:WordML.getSupportedTags()
						PropertyML propML = new PropertyML(elem);
						propML.setParent(RunPropertiesML.this);
						this.children.add(propML);
					}// if (eTag != null)
				}// if (o instanceof PStyle)
			}// for (Object o)
		}
	}// initChildren()
	
	private void initAttributes() {
		for (ElementML child: this.children) {
			PropertyML prop = (PropertyML) child;
			if (prop.getTag() == WordML.getTag((StyleConstants) StyleConstants.Bold)) {
				StyleConstants.setBold(this.attrs, true);
			} else if (prop.getTag() == WordML.getTag((StyleConstants) StyleConstants.Italic)) {
				StyleConstants.setItalic(this.attrs, true);
			} else if (prop.getTag() == WordML.getTag((StyleConstants) StyleConstants.Underline)) {
				StyleConstants.setUnderline(this.attrs, true);
			} else if (prop.getTag() == WordML.getTag((StyleConstants) StyleConstants.FontFamily)) {
				String s = prop.getAttributeValue(WordML.Attribute.ASCII);
				StyleConstants.setFontFamily(this.attrs, s);
			} else if (prop.getTag() == WordML.getTag((StyleConstants) StyleConstants.FontSize)) {
				String s = prop.getAttributeValue(WordML.Attribute.SZ);
				StyleConstants.setFontSize(this.attrs, Integer.parseInt(s));
			}
		}
	}

}// RunPropertiesML class


























