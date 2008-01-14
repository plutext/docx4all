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
			log.debug("remove(): offset=" + offset + " length=" + length);
		}

		if (!isEnabled()) {
			super.remove(fb, offset, length);
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
    
}// DocumentFilter class



















