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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class PropertyML extends ElementML {
	private static Logger log = Logger.getLogger(PropertyML.class);
	
	private final Element elem;
	private final Hashtable<WordML.Attribute, AttributeML> attrTable;
	
	public PropertyML(Element elem) throws RuntimeException {
		this.elem = elem;
		this.tag = WordML.getTag(elem.getName());
		this.attrTable = new Hashtable<WordML.Attribute, AttributeML>(3);
		
		if (tag == null || !tag.isPropertyTag() ) {
			StringBuffer errMsg = new StringBuffer();
			errMsg.append("Element <");
			errMsg.append(elem.getName());
			errMsg.append("> is not a supported tag");
			throw new RuntimeException(errMsg.toString());
		}
		
		initChildren();
		initAttributeTable();
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
		return this.elem == null;
	}

	public String getAttributeValue(WordML.Attribute key) {
		return getAttributeML(key).getValue();
	}
	
	private AttributeML getAttributeML(WordML.Attribute key) {
		return (AttributeML) this.attrTable.get(key);
	}
	
	private void initChildren() {
	    Iterator elementIterator = this.elem.elementIterator();
		while (elementIterator.hasNext()) {
			Element tempE = (Element) elementIterator.next();
			WordML.Tag eTag = WordML.getTag(tempE.getName());
			if (eTag != null && eTag.isPropertyTag()) {
				// supported property tag;
				// see:WordML.getSupportedTags()
				PropertyML propML = new PropertyML(elem);
				propML.setParent(PropertyML.this);
				this.children.add(propML);
			}// if (eTag != null)
		}  
	}
	
	private void initAttributeTable() {
		List attrList = this.elem.attributes();
		for (Object attrObj : attrList) {
			Attribute a = (Attribute) attrObj;
			WordML.Attribute key = WordML.getAttribute(a.getName());
			if (key != null && a.getValue() != null
					&& a.getValue().length() > 0) {
				// supported attribute and has value
				// see:WordML.getSupportedAttributes()
				this.attrTable.put(key, new AttributeML(key, a.getValue()));
			}
		}
	}
	
}// PropertyML class






















