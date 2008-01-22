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
import javax.swing.text.DefaultStyledDocument.ElementSpec;

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
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
    	
		if (!isEnabled()) {
			super.insertString(fb, offset, text, attrs);
			return;
		}
		
		if (text == null || text.length() == 0) {
			return;
		}
		
		WordMLDocument doc = (WordMLDocument) fb.getDocument();
		DocumentElement elem = 
			(DocumentElement) doc.getParagraphMLElement(offset, false);
		if (elem.getEndOffset() == elem.getParentElement().getEndOffset()) {
			//the last paragraph
			ElementML newPara = new ParagraphML(ObjectFactory.createPara(text));
			elem.getElementML().addSibling(newPara, false);
			
			List<ElementSpec> specs = DocUtil.getElementSpecs(newPara);
			DocUtil.insertParagraphs(doc, offset, specs);
			
		} else if (elem.getEndOffset() - elem.getStartOffset() == 1){
			//empty paragraph
			ElementML para = elem.getElementML();
			int start = elem.getStartOffset();
			
			ElementML newRun = new RunML(ObjectFactory.createR(text));
			para.addChild(newRun);
			fb.remove(elem.getStartOffset(), 1);
			
			List<ElementSpec> specs = DocUtil.getElementSpecs(para);
			insertLater(doc, start, specs);
			
		} else {
			super.insertString(fb, offset, text, attrs);
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



















