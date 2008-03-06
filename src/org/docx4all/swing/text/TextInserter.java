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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.DocumentFilter.FilterBypass;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 07/02/2008
 */
public class TextInserter implements TextProcessor {
	private static Logger log = Logger.getLogger(TextInserter.class);

	private final FilterBypass filterBypass;
	private final int offset;
	private final String text;
	private final AttributeSet attrs;
	
	public TextInserter(FilterBypass fb, int offset, String text, AttributeSet attrs) {
		this.filterBypass = fb;
		this.offset = offset;
		this.text = text;
		this.attrs = (attrs == null) ? SimpleAttributeSet.EMPTY : attrs;
	}

	public void doAction() throws BadLocationException {
		if (text == null || text.length() == 0) {
			return;
		}
		
		if (Constants.NEWLINE.equals(text)) {
			doInsertNewlineAction();
			return;
		}
		
		final WordMLDocument doc = (WordMLDocument) filterBypass.getDocument();
		DocumentElement elem = 
			(DocumentElement) doc.getParagraphMLElement(offset, false);
		if (elem.getEndOffset() == elem.getParentElement().getEndOffset()) {
			//insert a string in the last paragraph

			//Need not Resolve Parent attribute
			((MutableAttributeSet) attrs).removeAttribute(StyleConstants.ResolveAttribute);
			
			RunML newRun = new RunML(ObjectFactory.createR(text));
			newRun.addAttributes(attrs, true);
					
			ParagraphML newPara = new ParagraphML(ObjectFactory.createPara(null));
			newPara.addChild(newRun);
			
			ParagraphML para = (ParagraphML) elem.getElementML();
			ParagraphPropertiesML pPr =
				(ParagraphPropertiesML) para.getParagraphProperties();
			if (pPr != null) {
				pPr = (ParagraphPropertiesML) pPr.clone();
			}
			newPara.setParagraphProperties(pPr);
			
			para.addSibling(newPara, false);
			doc.refreshParagraphs(elem.getStartOffset(), 1);
			
		} else if (elem.getEndOffset() - elem.getStartOffset() == 1){
			//insert a string in an empty paragraph
			
			//Need not Resolve Parent attribute
			((MutableAttributeSet) attrs).removeAttribute(StyleConstants.ResolveAttribute);
			
			RunML newRun = new RunML(ObjectFactory.createR(text));
			newRun.addAttributes(attrs, true);
			
			ElementML para = elem.getElementML();
			para.addChild(newRun);

			doc.refreshParagraphs(elem.getStartOffset(), 1);
			
		} else {
			//insert a string in a non-empty paragraph
			int paraStart = elem.getStartOffset();
			int paraEnd = elem.getEndOffset();
			
			//Grab the resolve parent of attrs and remove from it. 
			//This is done so that normal typing case condition can be 
			//correctly verified.
			MutableAttributeSet resolveAttrs = null;
			if (attrs.getResolveParent() != null) {
				//Keep resolve parent attrs for 'abnormal' typing case condition
				resolveAttrs = new SimpleAttributeSet(attrs.getResolveParent());
				//don't need its ElementML attribute
				resolveAttrs.removeAttribute(WordMLStyleConstants.ElementMLAttribute);
				((MutableAttributeSet) attrs).removeAttribute(StyleConstants.ResolveAttribute);
			}
			
			elem = DocUtil.getInputAttributeElement(doc, offset, null);
			
			if (attrs.getAttributeCount() > 0
				&& elem != null
				&& elem.getAttributes().containsAttributes(attrs)) {
				//Normal typing
				filterBypass.insertString(offset, text, attrs);
				
			} else {
				//A new RunML needs to be created for the inserted string.
				//Remember to save text content first.
				DocUtil.saveTextContentToElementML((WordMLDocument.TextElement) elem);
				
				RunML newRun = new RunML(ObjectFactory.createR(text));
				
				//The attributes of this newRun depends on both 
				//'attrs' and 'elem' which is the input attribute element.
				MutableAttributeSet newAttrs = new SimpleAttributeSet();
				if (elem == null) {
					//No input attribute element.
					//Check the RunML element at 'offset' and
					//grab its attributes if 'offset' is not at its ends.
					elem = (DocumentElement) doc.getRunMLElement(offset);
					if (elem.getStartOffset() < offset && offset < elem.getEndOffset()) {
						newAttrs.addAttributes(elem.getAttributes().copyAttributes());
					}
				} else {
					elem = (DocumentElement) elem.getParentElement();
					newAttrs.addAttributes(elem.getAttributes().copyAttributes());
				}
				newAttrs.addAttributes(attrs);
				if (resolveAttrs != null) {
					newAttrs.addAttributes(resolveAttrs);
				}
				newAttrs.removeAttribute(WordMLStyleConstants.ElementMLAttribute);
				newRun.addAttributes(newAttrs, true);
				
				if (elem.getStartOffset() < offset && offset < elem.getEndOffset()) {
					if (elem.isEditable()) {
						//Has to be editable so that it can be split.
						int idx = offset - elem.getStartOffset();
						DocUtil.splitElementML(elem, idx);
						elem.getElementML().addSibling(newRun, true);
						
						doc.refreshParagraphs(paraStart, (paraEnd-paraStart));
					}
				} else {
					boolean after = (elem.getEndOffset() == offset);
					elem.getElementML().addSibling(newRun, after);
					
					doc.refreshParagraphs(paraStart, (paraEnd-paraStart));
				}
			}
		}
	}

    private void doInsertNewlineAction() throws BadLocationException {
		final WordMLDocument doc = (WordMLDocument) filterBypass.getDocument();

		if (log.isDebugEnabled()) {
			log.debug("doInsertNewlineAction(): offset=" + offset);
		}

		DocumentElement leafE = (DocumentElement) doc
				.getCharacterElement(offset);
		if (!leafE.isEditable() && leafE.getStartOffset() < offset
				&& offset < leafE.getEndOffset()) {
			throw new BadLocationException("Text is not editable.", offset);
		}

		DocumentElement paraE = (DocumentElement) doc.getParagraphMLElement(
				offset, false);
		if (paraE.getStartOffset() == offset
				|| offset == paraE.getEndOffset() - 1) {
			//Create a new empty paragraph
			boolean before = (paraE.getStartOffset() == offset);
			insertNewEmptyParagraph(paraE, before);
			
		} else {
			//Split paragraph
			DocUtil.splitElementML(paraE, offset - paraE.getStartOffset());
			doc.refreshParagraphs(paraE.getStartOffset(), 1);		
		}
	}

	private void insertNewEmptyParagraph(DocumentElement paraE, boolean before) {
		Style pStyleFromAttrs = getPStyle(this.attrs);
		
		ParagraphML paraML = (ParagraphML) paraE.getElementML();
		ParagraphPropertiesML pPr = (ParagraphPropertiesML) paraML
				.getParagraphProperties();
		Style pStyleFromParaE = (pPr != null) ? getPStyle(pPr.getAttributeSet()) : null;
		
		if (pStyleFromAttrs != null && pStyleFromAttrs != pStyleFromParaE) {
			pPr = ElementMLFactory.createParagraphPropertiesML(pStyleFromAttrs);
		} else if (pPr != null) {
			pPr = (ParagraphPropertiesML) pPr.clone();
		}

		ParagraphML newParaML = ElementMLFactory.createParagraphML(null, pPr,
				null);
		paraML.addSibling(newParaML, !before);
		
		//Prepare the ElementSpecs of newParaML
		List<ElementSpec> newParaSpecs = DocUtil.getElementSpecs(newParaML);
		List<ElementSpec> specList = new ArrayList<ElementSpec>(
				newParaSpecs.size() + 3);
		// Close RunML
		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
		// Close Implied ParagraphML
		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
		// Close ParagraphML
		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
		// New paragraphs
		specList.addAll(newParaSpecs);
		newParaSpecs = null;
		
		// Has to convert to array
		ElementSpec[] specsArray = new ElementSpec[specList.size()];
		specsArray = specList.toArray(specsArray);
		specList = null;
		
		try {
			WordMLDocument doc = (WordMLDocument) paraE.getDocument();
			int pos = before ? paraE.getStartOffset() : paraE.getEndOffset();

			doc.insertElementSpecs(pos, specsArray);
		} catch (BadLocationException exc) {
			//ignore
			exc.printStackTrace();
		}
	}

	private Style getPStyle(AttributeSet attrs) {
		Style theStyle = null;
		
		WordMLDocument doc = (WordMLDocument) filterBypass.getDocument();
		StyleSheet styleSheet = doc.getStyleSheet();
		if (styleSheet != null) {
			String styleId = (String) attrs
					.getAttribute(WordMLStyleConstants.PStyleAttribute);
			if (styleId != null) {
				theStyle = styleSheet.getIDStyle(styleId);
				String type = (theStyle == null) ? null : (String) theStyle
						.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
				if (!StyleSheet.PARAGRAPH_ATTR_VALUE.equals(type)) {
					theStyle = null;
				}
			}
		}
		
		return theStyle;
	}
	
}// TextInserter class



















