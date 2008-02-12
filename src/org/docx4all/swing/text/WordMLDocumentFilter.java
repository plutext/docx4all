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
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.apache.log4j.Logger;

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
		
		if (length == 0 || offset >= fb.getDocument().getLength()) {
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
				
		TextReplacer tr = new TextReplacer(fb, offset, length, text, attrs);
		tr.doAction();
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
				
		TextInserter tr = new TextInserter(fb, offset, text, attrs);
		tr.doAction();
    } //insertString()
    
}// DocumentFilter class



















