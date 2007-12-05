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

package org.docx4all.swing.text;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleContext;

import org.docx4all.xml.ElementML;
import org.docx4all.xml.WordML;

public class WordMLStyleConstants {
    /**
     * Attribute name used to refer to ElementML attribute
     */
    public static final Object ElementMLAttribute = new WordMLStyleConstants("elementML");

    public static final Object WordMLTagAttribute = new WordMLStyleConstants("wordMLTag");
    
    private static final Object[] keys = { 
    	ElementMLAttribute, WordMLTagAttribute
    };

    static {
		try {
			int n = keys.length;
			for (int i = 0; i < n; i++) {
				StyleContext.registerStaticAttributeKey(keys[i]);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

    /**
	 * Gets the ElementML setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the ElementML, null if none
	 */
    public static ElementML getElementML(AttributeSet a) {
        return (ElementML) a.getAttribute(ElementMLAttribute);
    }

    /**
     * Sets the ElementML attribute.
     *
     * @param a the attribute set
     * @param elem the ElementML
     */
    public static void setElementML(MutableAttributeSet a, ElementML elem) {
        a.addAttribute(ElementMLAttribute, elem);
    }

    /**
	 * Gets the WordML.Tag setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the WordML.Tag, null if none
	 */
    public static WordML.Tag getWordMLTag(AttributeSet a) {
        return (WordML.Tag) a.getAttribute(WordMLTagAttribute);
    }

    /**
     * Sets the WordML.Tag attribute.
     *
     * @param a the attribute set
     * @param tag the WordML.Tag
     */
    public static void setWordMLTag(MutableAttributeSet a, WordML.Tag tag) {
        a.addAttribute(WordMLTagAttribute, tag);
    }

    private final String name;
    
    private WordMLStyleConstants(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
    
}// WordMLStyleConstants class






















