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

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.util.DocUtil;

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public class TextSelector {
	private static Logger log = Logger.getLogger(TextSelector.class);

	protected final WordMLDocument _doc;
	protected final int _offset, _length;
	
	public TextSelector(WordMLDocument doc, int offset, int length) throws BadSelectionException {
		select(doc, offset, offset + length);
		this._doc = doc;
		this._offset = offset;
		this._length = length;
	}
	
	public int getOffset() {
		return _offset;
	}
	
	public int getLength() {
		return _length;
	}
	
	public WordMLDocument getDocument() {
		return _doc;
	}
	
	public String getText() {
		String theText = null;
		try {
			theText = _doc.getText(_offset, _length);
		} catch (BadLocationException exc) {
			;//ignore
		}
		return theText;
	}
	
	public List<ElementMLRecord> getElementMLRecords() {
		List<ElementMLRecord> theList = new ArrayList<ElementMLRecord>();
		
		DocumentElement root = (DocumentElement) _doc.getDefaultRootElement();
		
		int startIdx = root.getElementIndex(_offset);
		int endIdx = root.getElementIndex(_offset + _length - 1);
		
		for (int idx=startIdx; idx <= endIdx; idx++) {
			DocumentElement block = (DocumentElement) root.getElement(idx);
			int startOffset = Math.max(block.getStartOffset(), _offset);
			int endOffset = Math.min(block.getEndOffset(), _offset + _length);
			ElementSelector es = new ElementSelector(block, startOffset, endOffset);
			theList.addAll(es.getElementMLRecords());
		}
		
		return theList;
	}
	
	public List<DocumentElement> getDocumentElements() {
		DocumentElement root = (DocumentElement) _doc.getDefaultRootElement();
		ElementSelector es = new ElementSelector(root, _offset, _offset + _length);
		return es.getDocumentElements();
	}
	
    public boolean isFullySelected(Element elem) {
    	return (_offset <= elem.getStartOffset() 
    			&& elem.getEndOffset() <= _offset + _length);
    }

	private void select(WordMLDocument doc, int p0, int p1) throws BadSelectionException {
    	if (p0 >= p1) {
    		throw new BadSelectionException("Bad Selection", p0, p1-p0);
    	}
    	
    	DocumentElement firstLeaf = (DocumentElement) doc.getCharacterElement(p0);
    	
		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p0 = " + firstLeaf);
		}
		
		if (firstLeaf.getStartOffset() < p0 && !firstLeaf.isEditable()) {
			//cannot start with a partially selected non-editable leaf element.
			throw new BadSelectionException("Bad Start Position", p0, p1 - p0);
		}

		DocumentElement lastLeaf = (DocumentElement) doc.getCharacterElement(p1 - 1);

		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p1 = " + lastLeaf);
		}

		if (p1 < lastLeaf.getEndOffset() && !lastLeaf.isEditable()) {
			//cannot end with a partially selected non-editable leaf element
			throw new BadSelectionException("Bad End Position", p0, p1 - p0);
		}

		if (firstLeaf != lastLeaf
			&& (firstLeaf.getStartOffset() != p0
					|| lastLeaf.getEndOffset() != p1)) {
			DocumentElement root = 
				(DocumentElement) doc.getDefaultRootElement();
			List<String> path0 = DocUtil.getElementNamePath(root, p0);
			List<String> path1 = DocUtil.getElementNamePath(root, p1);

			if (path0 != null && !path0.equals(path1)) {
				throw new BadSelectionException("Bad Selection", p0, p1 - p0);
			}
		}
    }
    
}// TextSelector class



















