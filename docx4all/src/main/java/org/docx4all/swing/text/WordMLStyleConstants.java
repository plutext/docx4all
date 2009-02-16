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

import org.docx4all.xml.ElementML;
import org.docx4all.xml.WordML;
import org.docx4all.xml.type.CTBorder;
import org.docx4all.xml.type.CTHeight;
import org.docx4all.xml.type.TblBorders;
import org.docx4all.xml.type.TblWidth;
import org.docx4all.xml.type.TcBorders;
import org.docx4j.wml.STLineSpacingRule;
import org.docx4j.wml.STVerticalJc;
import org.docx4j.wml.TcPrInner;

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
	
	public static final Object TblStyleAttribute = new WordMLStyleConstants("tblStyle");
	
	public static final Object PStyleAttribute = new WordMLStyleConstants("pStyle");
	
	public static final Object RStyleAttribute = new WordMLStyleConstants("rStyle");
	
	public static final Object NumPrAttribute = new WordMLStyleConstants("numPr");
	
	public static final Object BorderVisibleAttribute = new WordMLStyleConstants("borderVisible");
	
	public static final Object CTBorderAttribute = new WordMLStyleConstants("ctborder");
	
	public static final Object STLineSpacingRuleAttribute = new WordMLStyleConstants("stLineSpacingRule");
	
	public static final Object TblBordersAttribute = new WordMLStyleConstants("tblBorders");
	
	public static final Object TblWidthAttribute = new WordMLStyleConstants("tblWidth");
	public static final Object TblIndentAttribute = new WordMLStyleConstants("tblIndent");
	public static final Object TblCellSpacingAttribute = new WordMLStyleConstants("tblCellSpacing");
	
	public static final Object TrHeightAttribute = new WordMLStyleConstants("trHeight");
	public static final Object TrWAfterAttribute = new WordMLStyleConstants("trWAfter");
	public static final Object TrWBeforeAttribute = new WordMLStyleConstants("trWBefore");
	public static final Object TrGridBeforeAttribute = new WordMLStyleConstants("trGridBefore");
	public static final Object TrGridAfterAttribute = new WordMLStyleConstants("trGridAfter");
	
	public static final Object TcBordersAttribute = new WordMLStyleConstants("tcBorders");
	public static final Object TcLeftMarginAttribute = new WordMLStyleConstants("tcLeftMargin");
	public static final Object TcRightMarginAttribute = new WordMLStyleConstants("tcRightMargin");
	public static final Object TcTopMarginAttribute = new WordMLStyleConstants("tcTopMargin");
	public static final Object TcBottomMarginAttribute = new WordMLStyleConstants("tcBottomMargin");
	public static final Object TcVAlignAttribute = new WordMLStyleConstants("tcVAlign");
	public static final Object TcWidthAttribute = new WordMLStyleConstants("tcWidth");
	public static final Object TcGridSpanAttribute = new WordMLStyleConstants("tcGridSpan");
	public static final Object TcVMergeAttribute = new WordMLStyleConstants("tcVMerge");
	
    public static final Object DefaultParagraphStyleNameAttribute = 
    	new WordMLStyleConstants("defaultParagraphStyleName");
    
    /*
    private static final Object[] keys = { 
    	ElementMLAttribute, WordMLTagAttribute, DocxObjectAttribute,
    	StyleIdAttribute, StyleUINameAttribute, StyleTypeAttribute,
    	UiPriorityAttribute, QFormatAttribute,
    	TblStyleAttribute, PStyleAttribute, RStyleAttribute,
    	BorderVisibleAttribute, CTBorderAttribute, 
    	TblBordersAttribute, TcBordersAttribute,
    	TblWidthAttribute,
    	DefaultParagraphStyleNameAttribute
    };*/

    /*
    static {
		try {
			int n = keys.length;
			for (int i = 0; i < n; i++) {
				StyleContext.registerStaticAttributeKey(keys[i]);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}*/

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
     * Sets the CTBorder attribute.
     *
     * @param a the attribute set
     * @param border the CTBorder
     */
    public static void setCTBorder(MutableAttributeSet a, CTBorder border) {
        a.addAttribute(CTBorderAttribute, border);
    }

    /**
	 * Gets the CTBorder setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the CTBorder attribute
	 */
    public static CTBorder getCTBorder(AttributeSet a) {
    	return (CTBorder) a.getAttribute(CTBorderAttribute);
    }

    /**
     * Sets the STLineSpacingRule attribute.
     *
     * @param a the attribute set
     * @param rule the STLineSpacingRule
     */
    public static void setSTLineSpacingRule(MutableAttributeSet a, STLineSpacingRule rule) {
        a.addAttribute(STLineSpacingRuleAttribute, rule);
    }

    /**
	 * Gets the STLineSpacingRule setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the STLineSpacingRule attribute
	 */
    public static STLineSpacingRule getSTLineSpacingRule(AttributeSet a) {
    	return (STLineSpacingRule) a.getAttribute(STLineSpacingRuleAttribute);
    }

    /**
     * Sets the TblBorders attribute.
     *
     * @param a the attribute set
     * @param border the TblBorders
     */
    public static void setTblBorders(MutableAttributeSet a, TblBorders border) {
        a.addAttribute(TblBordersAttribute, border);
    }

    /**
	 * Gets the TblBorders setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TblBorders attribute
	 */
    public static TblBorders getTblBorders(AttributeSet a) {
    	return (TblBorders) a.getAttribute(TblBordersAttribute);
    }

    /**
     * Sets the TcBorders attribute.
     *
     * @param a the attribute set
     * @param border the TcBorders
     */
    public static void setTcBorders(MutableAttributeSet a, TcBorders border) {
        a.addAttribute(TcBordersAttribute, border);
    }

    /**
	 * Gets the TcBorders setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcBorders attribute
	 */
    public static TcBorders getTcBorders(AttributeSet a) {
    	return (TcBorders) a.getAttribute(TcBordersAttribute);
    }

    /**
     * Sets the TcLeftMargin attribute.
     *
     * @param a the attribute set
     * @param margin the left margin
     */
    public static void setTcLeftMargin(MutableAttributeSet a, TblWidth margin) {
        a.addAttribute(TcLeftMarginAttribute, margin);
    }

    /**
	 * Gets the TcLeftMargin attribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcLeftMargin attribute
	 */
    public static TblWidth getTcLeftMargin(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TcLeftMarginAttribute);
    }

    /**
     * Sets the TcRightMargin attribute.
     *
     * @param a the attribute set
     * @param margin the right margin
     */
    public static void setTcRightMargin(MutableAttributeSet a, TblWidth margin) {
        a.addAttribute(TcRightMarginAttribute, margin);
    }

    /**
	 * Gets the TcRightMargin attribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcRightMargin attribute
	 */
    public static TblWidth getTcRightMargin(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TcRightMarginAttribute);
    }

    /**
     * Sets the TcTopMargin attribute.
     *
     * @param a the attribute set
     * @param margin the top margin
     */
    public static void setTcTopMargin(MutableAttributeSet a, TblWidth margin) {
        a.addAttribute(TcTopMarginAttribute, margin);
    }

    /**
	 * Gets the TcTopMargin attribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcTopMargin attribute
	 */
    public static TblWidth getTcTopMargin(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TcTopMarginAttribute);
    }

    /**
     * Sets the TcBottomMargin attribute.
     *
     * @param a the attribute set
     * @param margin the bottom margin
     */
    public static void setTcBottomMargin(MutableAttributeSet a, TblWidth margin) {
        a.addAttribute(TcBottomMarginAttribute, margin);
    }

    /**
	 * Gets the TcBottomMargin attribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcBottomMargin attribute
	 */
    public static TblWidth getTcBottomMargin(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TcBottomMarginAttribute);
    }

    /**
     * Sets the TcVAlign attribute.
     *
     * @param a the attribute set
     * @param align the alignment
     */
    public static void setTcVAlign(MutableAttributeSet a, STVerticalJc align) {
        a.addAttribute(TcVAlignAttribute, align);
    }

    /**
	 * Gets the TcVAlign attribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcVAlign attribute
	 */
    public static STVerticalJc getTcVAlign(AttributeSet a) {
    	return (STVerticalJc) a.getAttribute(TcVAlignAttribute);
    }

    /**
     * Sets the TcWidthAttribute attribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTcWidth(MutableAttributeSet a, TblWidth value) {
        a.addAttribute(TcWidthAttribute, value);
    }

    /**
	 * Gets the TcWidthAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcWidthAttribute
	 */
    public static TblWidth getTcWidth(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TcWidthAttribute);
    }

    /**
     * Sets the TcGridSpanAttribute attribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTcGridSpan(MutableAttributeSet a, TcPrInner.GridSpan value) {
        a.addAttribute(TcGridSpanAttribute, value);
    }

    /**
	 * Gets the TcGridSpanAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcGridSpanAttribute
	 */
    public static TcPrInner.GridSpan getTcGridSpan(AttributeSet a) {
    	return (TcPrInner.GridSpan) a.getAttribute(TcGridSpanAttribute);
    }

    /**
     * Sets the TcVMergeAttribute attribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTcVMerge(MutableAttributeSet a, TcPrInner.VMerge value) {
        a.addAttribute(TcVMergeAttribute, value);
    }

    /**
	 * Gets the TcVMergeAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TcVMergeAttribute
	 */
    public static TcPrInner.VMerge getTcVMerge(AttributeSet a) {
    	return (TcPrInner.VMerge) a.getAttribute(TcVMergeAttribute);
    }

   /**
     * Sets TrHeightAttribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTrHeight(MutableAttributeSet a, CTHeight value) {
        a.addAttribute(TrHeightAttribute, value);
    }

    /**
	 * Gets the TrHeightAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TrHeightAttribute
	 */
    public static CTHeight getTrHeight(AttributeSet a) {
    	return (CTHeight) a.getAttribute(TrHeightAttribute);
    }

    /**
     * Sets TrWAfterAttribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTrWAfter(MutableAttributeSet a, TblWidth value) {
        a.addAttribute(TrWAfterAttribute, value);
    }

    /**
	 * Gets the TrWAfterAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TrWAfterAttribute attribute
	 */
    public static TblWidth getTrWAfter(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TrWAfterAttribute);
    }

    /**
     * Sets the TrWBeforeAttribute attribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTrWBefore(MutableAttributeSet a, TblWidth value) {
        a.addAttribute(TrWBeforeAttribute, value);
    }

    /**
	 * Gets the TrWBeforeAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TrWBeforeAttribute
	 */
    public static TblWidth getTrWBefore(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TrWBeforeAttribute);
    }

    /**
     * Sets the TrGridAfterAttribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTrGridAfter(MutableAttributeSet a, int value) {
        a.addAttribute(TrGridAfterAttribute, Integer.valueOf(value));
    }

    /**
	 * Gets the TrGridAfterAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TrWBeforeAttribute
	 */
    public static int getTrGridAfter(AttributeSet a) {
        Integer grid = (Integer) a.getAttribute(TrGridAfterAttribute);
        if (grid != null) {
            return grid.intValue();
        }
        return 0;
    }

    /**
     * Sets the TrGridBeforeAttribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTrGridBefore(MutableAttributeSet a, int value) {
        a.addAttribute(TrGridBeforeAttribute, Integer.valueOf(value));
    }

    /**
	 * Gets the TrGridBeforeAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TrWBeforeAttribute
	 */
    public static int getTrGridBefore(AttributeSet a) {
        Integer grid = (Integer) a.getAttribute(TrGridAfterAttribute);
        if (grid != null) {
            return grid.intValue();
        }
        return 0;
    }

   /**
     * Sets the TblWidth attribute.
     *
     * @param a the attribute set
     * @param width the TblWidth
     */
    public static void setTblWidth(MutableAttributeSet a, TblWidth width) {
        a.addAttribute(TblWidthAttribute, width);
    }

    /**
	 * Gets the TblWidth setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TblWidth attribute
	 */
    public static TblWidth getTblWidth(AttributeSet a) {
    	return (TblWidth) a.getAttribute(TblWidthAttribute);
    }

    /**
     * Gets the TblCellSpacingAttribute
     *
     * @param a the attribute set
     * @return the value, 0 if not set
     */
    public static int getTblCellSpacing(AttributeSet a) {
        Integer spacing = (Integer) a.getAttribute(TblCellSpacingAttribute);
        if (spacing != null) {
            return spacing.intValue();
        }
        return 0;
    }

    /**
     * Sets TblCellSpacingAttribute
     *
     * @param a the attribute set
     * @param i the value
     */ 
    public static void setTblCellSpacing(MutableAttributeSet a, int i) {
        a.addAttribute(TblCellSpacingAttribute, Integer.valueOf(i));
    }

    /**
     * Sets the TblIndentAttribute.
     *
     * @param a the attribute set
     * @param value the value
     */
    public static void setTblIndent(MutableAttributeSet a, int value) {
        a.addAttribute(TblIndentAttribute, Integer.valueOf(value));
    }

    /**
	 * Gets the TblIndentAttribute setting from the attribute list.
	 * 
	 * @param a the attribute set
	 * @return the TblIndentAttribute
	 */
    public static int getTblIndent(AttributeSet a) {
        Integer ind = (Integer) a.getAttribute(TblIndentAttribute);
        if (ind != null) {
            return ind.intValue();
        }
        return 0;
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
    public static org.docx4j.wml.Style getDocxStyle(AttributeSet a) {
        return (org.docx4j.wml.Style) a.getAttribute(DocxObjectAttribute);
    }

    /**
     * Sets the org.docx4j.wml.Styles.Style attribute.
     *
     * @param a the attribute set
     * @param style the org.docx4j.wml.Styles.Style
     */
    public static void setDocxStyle(MutableAttributeSet a, org.docx4j.wml.Style style) {
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






















