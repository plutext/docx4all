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

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public class TextSelector {
	private static Logger log = Logger.getLogger(TextSelector.class);

	private final WordMLDocument doc;
	private final int offset, length;
	
	public TextSelector(WordMLDocument doc, int offset, int length) throws BadSelectionException {
		select(doc, offset, offset + length);
		this.doc = doc;
		this.offset = offset;
		this.length = length;
	}
	

    private void select(WordMLDocument doc, int p0, int p1) throws BadSelectionException {
    	if (p0 >= p1) {
    		throw new BadSelectionException("Bad Selection", p0, p1-p0);
    	}
    	
    	DocumentElement elem = (DocumentElement) doc.getCharacterElement(p0);
    	
		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p0 = " + elem);
		}
		
		if (elem.getStartOffset() < p0 && !elem.isEditable()) {
			throw new BadSelectionException("Bad Start Position", p0, p1 - p0);
		}

		elem = (DocumentElement) doc.getCharacterElement(p1);

		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p1 = " + elem);
		}

		if (elem.getStartOffset() < p1 && !elem.isEditable()) {
			throw new BadSelectionException("Bad End Position", p0, p1 - p0);
		}

		elem = (DocumentElement) doc.getDefaultRootElement();
		List<String> path0 = DocUtil.getElementNamePath(elem, p0);
		List<String> path1 = DocUtil.getElementNamePath(elem, p1);

		if (path0 != null && !path0.equals(path1)) {
			throw new BadSelectionException("Bad Selection", p0, p1 - p0);
		}
    }
    
}// TextSelector class



















