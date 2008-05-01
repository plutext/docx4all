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
    
    public static final Object DocxObjectAttribute = new WordMLStyleConstants("docxObject");
    
    public static final Object StyleIdAttribute = new WordMLStyleConstants("styleId");
    
    public static final Object StyleUINameAttribute = new WordMLStyleConstants("styleUIName");
    
	public static final Object StyleTypeAttribute = new WordMLStyleConstants("styleType");
	
	public static final Object UiPriorityAttribute = new WordMLStyleConstants("uiPriority");
	
	public static final Object QFormatAttribute = new WordMLStyleConstants("qformat");
	
	public static final Object PStyleAttribute = new WordMLStyleConstants("pStyle");
	
	public static final Object RStyleAttribute = new WordMLStyleConstants("rStyle");
	
	public static final Object BorderVisibleAttribute = new WordMLStyleConstants("borderVisible");
	
    public static final Object DefaultParagraphStyleNameAttribute = 
    	new WordMLStyleConstants("defaultParagraphStyleName");
    
    private static final Object[] keys = { 
    	ElementMLAttribute, WordMLTagAttribute, DocxObjectAttribute,
    	StyleIdAttribute, StyleUINameAttribute, StyleTypeAttribute,
    	UiPriorityAttribute, QFormatAttribute,
    	PStyleAttribute, RStyleAttribute,
    	BorderVisibleAttribute,
    	DefaultParagraphStyleNameAttribute
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
     * Sets the BorderVisible attribute.
     *
     * @param a the attribute set
     * @param b visible or invisible
     */
    public static void setBorderVisible(MutableAttributeSet a, boolean b) {
        a.addAttribute(BorderVisibleAttribute, Boolean.valueOf(b));
    }

    /**
	 * Gets the BorderVisible setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return true or false value of BorderVisible attribute
	 */
    public static boolean getBorderVisible(AttributeSet a) {
        Boolean val = (Boolean) a.getAttribute(BorderVisibleAttribute);
        return (val != null) ? val.booleanValue() : false;
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
	 * Gets the org.docx4j.wml.Styles.Style object setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the org.docx4j.wml.Styles.Style object, null if none
	 */
    public static org.docx4j.wml.Styles.Style getDocxStyle(AttributeSet a) {
        return (org.docx4j.wml.Styles.Style) a.getAttribute(DocxObjectAttribute);
    }

    /**
     * Sets the org.docx4j.wml.Styles.Style attribute.
     *
     * @param a the attribute set
     * @param style the org.docx4j.wml.Styles.Style
     */
    public static void setDocxStyle(MutableAttributeSet a, org.docx4j.wml.Styles.Style style) {
        a.addAttribute(DocxObjectAttribute, style);
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






















