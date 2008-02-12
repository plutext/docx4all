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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.DocumentFilter.FilterBypass;

import org.apache.log4j.Logger;

/**
 *	@author Jojada Tirtowidjojo - 12/02/2008
 */
public class TextReplacer implements TextProcessor {
	private static Logger log = Logger.getLogger(TextReplacer.class);

	private final FilterBypass filterBypass;
	private final int offset;
	private final int length;
	private final String text;
	private final AttributeSet attrs;
	
	public TextReplacer(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) {
		this.filterBypass = fb;
		this.offset = offset;
		this.length = length;
		this.text = text;
		this.attrs = (attrs == null) ? SimpleAttributeSet.EMPTY : attrs;
	}

	public void doAction() throws BadLocationException {
		final WordMLDocument doc = (WordMLDocument) filterBypass.getDocument();
		
		if (length > 0 && offset < doc.getLength()) {
			try {
				TextRemover tr = new TextRemover(filterBypass, offset, length);
				tr.doAction();
			} catch (BadSelectionException exc) {
				throw new BadLocationException("Unselectable range: offset="
						+ offset + " length=" + length, offset);
			}
		}

		if (text != null && text.length() > 0) {
			TextInserter tr = new TextInserter(filterBypass, offset, text, attrs);
			tr.doAction();
		}
	}
	
}// TextReplacer class



















