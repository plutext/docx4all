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

import java.util.HashMap;
import java.util.Map;

import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class WordML {

    private static final Map<String, Tag> tagMap;
    private static final Map<String, Attribute> attrMap;
    private static final Map<Object, Tag> styleTagMap;

    static {
    	tagMap = new HashMap<String, Tag>(Tag.supportedTags.length);
    	for (int i = 0; i < Tag.supportedTags.length; i++ ) {
    		tagMap.put(Tag.supportedTags[i].getTagName(), Tag.supportedTags[i]);
    		StyleContext.registerStaticAttributeKey(Tag.supportedTags[i]);
    	}
    	
    	attrMap = new HashMap<String, Attribute>(Attribute.supportedAttributes.length);
    	for (int i = 0; i < Attribute.supportedAttributes.length; i++) {
    		attrMap.put(Attribute.supportedAttributes[i].getAttributeName(), Attribute.supportedAttributes[i]);
    		StyleContext.registerStaticAttributeKey(Attribute.supportedAttributes[i]);
    	}
    	
    	styleTagMap = new HashMap<Object, Tag>(5);
    	styleTagMap.put(StyleConstants.Bold, Tag.B);
    	styleTagMap.put(StyleConstants.Italic, Tag.I);
    	styleTagMap.put(StyleConstants.Underline, Tag.U);
    	styleTagMap.put(StyleConstants.FontFamily, Tag.rFONTS);
    	styleTagMap.put(StyleConstants.FontSize, Tag.SZ);
    }


    public static Attribute[] getSupportedAttributes() {
    	Attribute[] attrs = new Attribute[Attribute.supportedAttributes.length];
		System.arraycopy(Tag.supportedTags, 0, attrs, 0, Attribute.supportedAttributes.length);
		return attrs;
	}

    public static Tag[] getSupportedTags() {
		Tag[] tags = new Tag[Tag.supportedTags.length];
		System.arraycopy(Tag.supportedTags, 0, tags, 0, Tag.supportedTags.length);
		return tags;
	}

    public static Tag getTag(String tagName) {
		return (Tag) tagMap.get(tagName);
	}

    public static Tag getTag(StyleConstants sc) {
        return (Tag) styleTagMap.get(sc);
    }

    public static Attribute getAttribute(String attName) {
    	return (Attribute) attrMap.get(attName);
    }

    public static class Tag {
    	
		// --- Supported Tag -----------------------------------
		public static final Tag DOCUMENT = new Tag("document", true, false);
		public static final Tag BODY = new Tag("body", true, true);
		public static final Tag P = new Tag("p", true, true);
		public static final Tag R = new Tag("r");
		public static final Tag T = new Tag("t");
		public static final Tag BR = new Tag("br", true, false);
		public static final Tag CR = new Tag("cr", true, false);

		// --- Property Container Tag ---------------------------
		public static final Tag pPr = new PropertyContainerTag("pPr");
		public static final Tag rPr = new PropertyContainerTag("rPr");
		
		// --- Text Formatting ---
		public static final Tag B = new PropertyTag("b");
		public static final Tag COLOR = new PropertyTag("color");
		public static final Tag I = new PropertyTag("i");
		public static final Tag rFONTS = new PropertyTag("rFonts");
		public static final Tag SPACING = new PropertyTag("spacing");
		public static final Tag SZ = new PropertyTag("sz");
		public static final Tag U = new PropertyTag("u");
		
		// --- Paragraph Border ---
		public static final Tag pBDR = new PropertyTag("pBdr");
		public static final Tag BOTTOM = new PropertyTag("bottom");
		public static final Tag LEFT = new PropertyTag("left");
		public static final Tag TOP = new PropertyTag("top");
		public static final Tag RIGHT = new PropertyTag("right");
		
		// --- Indentation ---
		public static final Tag JC = new PropertyTag("jc");
		public static final Tag IND = new PropertyTag("ind");
		
		private static final Tag[] supportedTags = { 
			DOCUMENT, BODY, P, R, T, BR, CR,
			rPr, pPr,
			B, COLOR, I, rFONTS, SPACING, SZ, U,
			pBDR, BOTTOM, LEFT, TOP, RIGHT,
			JC, IND			
		};

		static {
			// Force WordML's static initializer to be loaded.
			getTag("document");
		}

		private final boolean isBlockTag;
		private final boolean breaksFlow;
		
		protected final String name;

		Tag(String name) {
			this(name, false, false);
		}

		Tag(String name, boolean breaksFlow, boolean isBlockTag) {
			this.name = name;
			this.breaksFlow = breaksFlow;
			this.isBlockTag = isBlockTag;
		}

		public boolean isBlockTag() {
			return this.isBlockTag;
		}

		public boolean breaksFlow() {
			return this.breaksFlow;
		}

		public boolean isPropertyTag() {
			return (this instanceof PropertyTag);
		}
		
		public boolean isPropertyContainerTag() {
			return (this instanceof PropertyContainerTag);
		}
		
		public boolean isSupported() {
			return !(this instanceof UnsupportedBlockTag)
				&& !(this instanceof UnsupportedRunTag);
		}
		
		public String getTagName() {
			return name;
		}
		
		public String toString() {
			return getTagName();
		}

	}//Tag static class member

    public static final class PropertyContainerTag extends Tag {
    	PropertyContainerTag(String name) {
    		super(name);
    	}
	}// PropertyContainerTag static class member
    
    public static final class PropertyTag extends Tag {
    	PropertyTag(String name) {
    		super(name);
    	}
	}// PropertyTag static class member

    public static final class UnsupportedBlockTag extends Tag {
		public UnsupportedBlockTag(String name) {
			super(name, true, true);
		}

		public int hashCode() {
			return name.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof UnsupportedBlockTag) {
				return toString().equals(obj.toString());
			}
			return false;
		}
	}// UnsupportedBlockTag static class member
    
    public static final class UnsupportedRunTag extends Tag {
		public UnsupportedRunTag(String name) {
			super(name, true, false);
		}

		public int hashCode() {
			return name.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj instanceof UnsupportedRunTag) {
				return toString().equals(obj.toString());
			}
			return false;
		}
	}// UnsupportedRunTag static class member
    
    public static final class Attribute {
		private final String name;

		public static final Attribute ASCII = new Attribute("ascii");
		public static final Attribute BOTTOM = new Attribute("bottom");
		public static final Attribute COLOR = new Attribute("color");
		public static final Attribute LEFT = new Attribute("left");
		public static final Attribute RIGHT = new Attribute("right");
		public static final Attribute SZ = new Attribute("sz");
		public static final Attribute TOP = new Attribute("top");
		public static final Attribute TYPE = new Attribute("type");
		public static final Attribute VAL = new Attribute("val");

		private static final Attribute[] supportedAttributes = { 
			ASCII, BOTTOM, COLOR, LEFT,
			RIGHT, SZ, TOP, TYPE, VAL
		};

		Attribute(String name) {
			this.name = name;
		}

		public String getAttributeName() {
			return name;
		}
		
		public String toString() {
			return getAttributeName();
		}

	}// Attribute static class member
    

}// WordML class
