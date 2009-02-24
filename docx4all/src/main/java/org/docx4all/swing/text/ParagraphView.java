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
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

import org.apache.log4j.Logger;
import org.docx4all.xml.ElementML;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.wml.PPrBase;

public class ParagraphView extends BoxView {
	private static Logger log = Logger.getLogger(ParagraphView.class);

	private static short DEFAULT_INTER_PARAGRAPH_SPACE = 200;//twips
	
	public ParagraphView(Element elem) {
		super(elem, View.Y_AXIS);
		setParagraphInsets(getAttributes());
	}
	
    protected void setParagraphInsets(AttributeSet attr) {
    	if (attr == null) {
    		return;
    	}
    	
    	//According to WordprocessingML specification
    	//when determining the spacing between any two paragraphs, a consumer shall use
    	//the maximum of the interline spacing in each paragraph, 
    	//the spacing after the first paragraph and the spacing before the second
    	//paragraph to determine the net spacing between the paragraphs.
    	//Currently we do not include interline spacing in the calculation
    	//and keep the result in BOTTOM inset.
    	//TODO: Include interline spacing in calculating spacing between
    	//two paragraphs.
    	
    	//Get the spacing after this paragraph
    	Float f = (Float) attr.getAttribute(StyleConstants.SpaceBelow);
    	if (f == null) {
    		f = Float.valueOf(0.0f);
    	}

    	//Get the spacing before the next paragraph
    	AttributeSet temp = getParagraphAttr(false);
    	Float spaceBefore = null;
    	if (temp != null) {
        	spaceBefore = (Float) attr.getAttribute(StyleConstants.SpaceAbove);
    	}
    	if (spaceBefore == null) {
    		spaceBefore = Float.valueOf(0.0f);
    	}
    	
    	//BOTTOM inset is the maximum
    	f = Math.max(f.floatValue(), spaceBefore.floatValue());    	
    	short bottom = f.shortValue();
    	if (bottom == 0) {
    		bottom = (short) StyleSheet.toPixels(DEFAULT_INTER_PARAGRAPH_SPACE);
    	}

    	//TOP inset is always zero
    	short top = 0;
    	
    	//The types of indentation specified in numbering properties
    	//are firstLine, hanging, left and right indentations.
    	//Left and right indentations are calculated in this ParagraphView.class
    	//while firstLine and hanging indentations are in ImpliedParagraphView.class
    	PPrBase.Ind indByNumPr = null;
		if (attr.isDefined(WordMLStyleConstants.NumPrAttribute)) {
			PPrBase.NumPr numPr = 
				(PPrBase.NumPr) 
					attr.getAttribute(
						WordMLStyleConstants.NumPrAttribute);
			String numId = null;
			if (numPr.getNumId() != null) {
				numId = numPr.getNumId().getVal().toString();
			}
			String ilvl = null;
			if (numPr.getIlvl() != null) {
				ilvl = numPr.getIlvl().getVal().toString();
			}
			
			ElementML elemML = WordMLStyleConstants.getElementML(attr);
			WordprocessingMLPackage p = elemML.getWordprocessingMLPackage();
			// Get the Ind value
			NumberingDefinitionsPart ndp = 
				p.getMainDocumentPart().getNumberingDefinitionsPart();
			// Force initialisation of maps
			ndp.getEmulator();
			
			indByNumPr = ndp.getInd(numId, ilvl);
		}
		
    	short left = 0;
    	if (!attr.isDefined(StyleConstants.LeftIndent)
    		&& indByNumPr != null
    		&& indByNumPr.getLeft() != null) { 
    		//LeftIndent attribute defined in attr
    		//takes precedence over that in indByNumPr.
    		//Therefore, process LeftIndent in indByNumPr 
    		//only when it is not defined in attr.
			left = (short) StyleSheet.toPixels(indByNumPr.getLeft().intValue());
    	} else {
    		left = (short) StyleConstants.getLeftIndent(attr);
    	}
    	
    	short right = 0;
    	if (!attr.isDefined(StyleConstants.RightIndent)
    		&& indByNumPr != null
    		&& indByNumPr.getRight() != null) {    		
    		//RightIndent attribute defined in attr
    		//takes precedence over that in indByNumPr.
    		//Therefore, process RightIndent in indByNumPr 
    		//only when it is not defined in attr.
			right = (short) StyleSheet.toPixels(indByNumPr.getRight().intValue());
    	} else {
    		right = (short) StyleConstants.getRightIndent(attr);
    	}
    	
    	setInsets(top, left, bottom, right);
    }

    protected AttributeSet getParagraphAttr(boolean paragraphBefore) {
    	AttributeSet theAttr = null;
    	
    	WordMLDocument doc = (WordMLDocument) getDocument();
    	int start = getStartOffset();
    	int end = getEndOffset();
    	if (start > 0 && paragraphBefore) {
    		theAttr = doc.getParagraphMLElement(start - 1, true).getAttributes();
    	} else if (end < doc.getLength()) {
    		theAttr = doc.getParagraphMLElement(end, true).getAttributes();
    	}
    	
    	return theAttr;
    }

}// ParagraphView class












