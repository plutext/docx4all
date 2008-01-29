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

import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.RunML;

/**
 *	@author Jojada Tirtowidjojo - 10/12/2007
 */
public class WordMLDocumentFilter extends DocumentFilter {
	private static Logger log = Logger.getLogger(WordMLDocumentFilter.class);
	private boolean enabled = true;
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}
	
    public void remove(FilterBypass fb, int offset, int length)
			throws BadLocationException {

		if (log.isDebugEnabled()) {
			log.debug("remove(): offset=" + offset 
				+ " length=" + length
				+ " doc.getLength()=" + fb.getDocument().getLength());
		}

		if (!isEnabled()) {
			super.remove(fb, offset, length);
			return;
		}
		
		if (length == 0 || offset >= fb.getDocument().getLength() - 1) {
    		//Cannot delete the end of document
			return;
		}

		try {
			TextRemover tr = new TextRemover(fb, offset, length);
			tr.doAction();
		} catch (BadSelectionException exc) {
			throw new BadLocationException("Unselectable range: offset="
					+ offset + " length=" + length, offset);
		}
	}
    
    public void replace(FilterBypass fb, int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {
    	
		if (log.isDebugEnabled()) {
			log.debug("replace(): offset=" + offset 
				+ " length=" + length
				+ " text=" + text
				+ " attrs=" + attrs
				+ " doc.getLength()=" + fb.getDocument().getLength());
		}

		if (!isEnabled()) {
			super.replace(fb, offset, length, text, attrs);
			return;
		}
		
		if (length > 0 && offset < fb.getDocument().getLength() - 1) {
			remove(fb, offset, length);
			insertStringLater(fb, offset, text, attrs);
		} else {
			insertString(fb, offset, text, attrs);
		}
    }

    public void insertString(FilterBypass fb, int offset, String text,
            AttributeSet attrs) throws BadLocationException {
    	
		if (log.isDebugEnabled()) {
			log.debug("insertString(): offset=" + offset 
				+ " text=" + text
				+ " attrs=" + attrs
				+ " doc.getLength()=" + fb.getDocument().getLength());
		}

		if (!isEnabled()) {
			super.insertString(fb, offset, text, attrs);
			return;
		}
		
		if (text == null || text.length() == 0) {
			return;
		}
		
		final WordMLDocument doc = (WordMLDocument) fb.getDocument();
		DocumentElement elem = 
			(DocumentElement) doc.getParagraphMLElement(offset, false);
		if (elem.getEndOffset() == elem.getParentElement().getEndOffset()) {
			//the last paragraph
			RunML newRun = new RunML(ObjectFactory.createR(text));
			newRun.addAttributes(attrs, true);
			
			ParagraphML newPara = new ParagraphML(ObjectFactory.createPara(null));
			newPara.addChild(newRun);
			
			ParagraphML para = (ParagraphML) elem.getElementML();
			ParagraphPropertiesML pPr =
				(ParagraphPropertiesML) para.getParagraphProperties();
			newPara.setParagraphProperties(pPr);
			
			para.addSibling(newPara, false);
			
			List<ElementSpec> specs = DocUtil.getElementSpecs(newPara);
			DocUtil.insertParagraphs(doc, offset, specs);
			
		} else if (elem.getEndOffset() - elem.getStartOffset() == 1){
			//empty paragraph
			RunML newRun = new RunML(ObjectFactory.createR(text));
			newRun.addAttributes(attrs, true);
			
			ElementML para = elem.getElementML();
			para.addChild(newRun);

			int start = elem.getStartOffset();
			fb.remove(start, 1);
			
			List<ElementSpec> specs = DocUtil.getElementSpecs(para);
			insertLater(doc, start, specs);
			
		} else if (attrs != null && attrs.getAttributeCount() > 0){
			ElementML para = elem.getElementML();
			int paraStart = elem.getStartOffset();
			int paraEnd = elem.getEndOffset();
			
			elem = (DocumentElement) doc.getParagraphMLElement(offset, true);
			if (elem.getStartOffset() == offset) {
				elem = (DocumentElement) doc.getRunMLElement(offset);
			} else {
				elem = (DocumentElement) doc.getRunMLElement(offset - 1);
			}
			
			if (elem.getAttributes().containsAttributes(attrs)) {
				super.insertString(fb, offset, text, attrs);
			} else {
				//Needs to save text content first
				WordMLDocument.TextElement textE = 
					(WordMLDocument.TextElement)
						elem.getElement(elem.getElementIndex(offset));
				DocUtil.saveTextContentToElementML(textE);
				
				MutableAttributeSet newAttrs = 
					new SimpleAttributeSet(elem.getAttributes().copyAttributes());
				newAttrs.removeAttribute(WordMLStyleConstants.ElementMLAttribute);
				newAttrs.addAttributes(attrs);
				RunML newRun = new RunML(ObjectFactory.createR(text));
				newRun.addAttributes(newAttrs, true);
				
				if (elem.getStartOffset() == offset) {
					elem.getElementML().addSibling(newRun, false);
				} else if (elem.getEndOffset() == offset) {
					elem.getElementML().addSibling(newRun, true);
				} else {
					int idx = offset - elem.getStartOffset();
					DocUtil.splitElementML(elem, idx);
					elem.getElementML().addSibling(newRun, true);
				}
				
				//Refresh the affected paragraph
				fb.remove(paraStart, (paraEnd-paraStart));
				
				List<ElementSpec> specs = DocUtil.getElementSpecs(para);
				insertLater(doc, paraStart, specs);
			}
		} else {
			super.insertString(fb, offset, text, attrs);
		}
		
		if (log.isDebugEnabled()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					log.debug("insertString(): Resulting structure...");
					DocUtil.displayStructure(doc);
				}
			});
		}
    }
    
	private void insertLater(final WordMLDocument doc, final int offset,
			final List<ElementSpec> specs) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					DocUtil.insertParagraphs(doc, offset, specs);
				} catch (BadLocationException exc) {
					;// ignore
				}
			}
		});
	}

    private void insertStringLater(
    	final FilterBypass fb, 
    	final int offset, 
    	final String text,
        final AttributeSet attrs) throws BadLocationException {
    	
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					insertString(fb, offset, text, attrs);
				} catch (BadLocationException exc) {
					;// ignore
				}
			}
		});
    }
    
}// DocumentFilter class



















