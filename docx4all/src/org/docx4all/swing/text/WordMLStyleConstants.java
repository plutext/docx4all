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
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.docx4all.ui.main.Constants;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.PropertyML;
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

    public static int getAlignment(PropertyML prop) {
    	int theAlignment = -1;
    	if (prop.getTag() == WordML.Tag.JC) {
    		String s = prop.getAttributeValue(WordML.Attribute.VAL);
    		if (Constants.ALIGN_BOTH_VALUE.equalsIgnoreCase(s)) {
    			theAlignment = StyleConstants.ALIGN_JUSTIFIED;
    		} else if (Constants.ALIGN_CENTER_VALUE.equalsIgnoreCase(s)) {
    			theAlignment = StyleConstants.ALIGN_CENTER;
    		} else if (Constants.ALIGN_LEFT_VALUE.equalsIgnoreCase(s)) {
    			theAlignment = StyleConstants.ALIGN_LEFT;
    		} else if (Constants.ALIGN_RIGHT_VALUE.equalsIgnoreCase(s)) {
    			theAlignment = StyleConstants.ALIGN_RIGHT;
    		}
    	}
    	return theAlignment;
    }
    
    public static boolean isBold(PropertyML prop) {
    	return booleanValueOf(prop, (StyleConstants) StyleConstants.Bold);
    }
    
    public static boolean isItalic(PropertyML prop) {
    	return booleanValueOf(prop, (StyleConstants) StyleConstants.Italic);
    }
    
    public static boolean isUnderlined(PropertyML prop) {
    	return booleanValueOf(prop, (StyleConstants) StyleConstants.Underline);
    }
    
    public static String getFontFamily(PropertyML prop) {
    	String theFont = null;
    	if (prop.getTag() == WordML.Tag.rFONTS) {
    		theFont = prop.getAttributeValue(WordML.Attribute.ASCII);
    	}
    	return theFont;
    }
    
    public static String getFontSize(PropertyML prop) {
    	String theFont = null;
    	if (prop.getTag() == WordML.Tag.SZ) {
    		theFont = prop.getAttributeValue(WordML.Attribute.VAL);
    	}
    	return theFont;
    }
    
    private static boolean booleanValueOf(PropertyML prop, StyleConstants sc) {
    	boolean theValue = false;
		if (prop.getTag() == WordML.getTag(sc)) {
			String value = prop.getAttributeValue(WordML.Attribute.VAL);
			if (value == null) {
				//true by default. See:OOXML spec
				theValue = true;
			} else {
				theValue = booleanValueOf(value);
			}
		}
    	return theValue;
    }
    
    private static boolean booleanValueOf(String s) {
    	boolean theValue = false;
    	if (Constants.ON_VALUE.equalsIgnoreCase(s)) {
    		theValue = true;
    	} else if (Constants.OFF_VALUE.equalsIgnoreCase(s)) {
    		;//false;
    	} else {
    		theValue = Boolean.valueOf(s);
    	}
    	return theValue;
    }
    
    private final String name;
    
    private WordMLStyleConstants(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
    
}// WordMLStyleConstants class






















