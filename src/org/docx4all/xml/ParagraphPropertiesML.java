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
import org.docx4all.swing.text.WordMLStyleConstants;
import org.docx4j.document.wordprocessingml.PStyle;
import org.docx4j.document.wordprocessingml.ParagraphProperties;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ParagraphPropertiesML extends ElementML implements PropertiesContainerML {
	private static Logger log = Logger.getLogger(ParagraphPropertiesML.class);

	private final ParagraphProperties pPr;
	private final MutableAttributeSet attrs = new SimpleAttributeSet();
	
	public ParagraphPropertiesML(ParagraphProperties pPr) {
		this.pPr = pPr;
		this.tag = WordML.Tag.pPr;
		
		initChildren();
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

	public MutableAttributeSet getAttributeSet() {
		return this.attrs;
	}
	
	public void addChild(PropertyML prop) {
		if (this.children == null) {
			this.children = new ArrayList<ElementML>();
		}

		prop.setParent(ParagraphPropertiesML.this);
		this.children.add(prop);
		
		addAttribute(prop);
	}
	
	private void addAttribute(PropertyML prop) {
		int align = WordMLStyleConstants.getAlignment(prop);
		if (align > -1) {
			StyleConstants.setAlignment(this.attrs, align);
		}
	}
	
	private void initChildren() {
		if (this.pPr != null) {
			List props = this.pPr.getProperties();
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
							addChild(propML);
						}// if (eTag != null)
					}// if (o instanceof PStyle)
				}// for (Object o)
			}// if (!props.isEmpty())
		}// if (this.pPr != null)
	}// initChildren()
	
}// ParagraphPropertiesML class
























