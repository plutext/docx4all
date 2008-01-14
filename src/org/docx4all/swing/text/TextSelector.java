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

import javax.swing.text.Element;

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ParagraphML;

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public class TextSelector {
	private static Logger log = Logger.getLogger(TextSelector.class);

	protected final WordMLDocument doc;
	protected final int offset, length;
	
	public TextSelector(WordMLDocument doc, int offset, int length) throws BadSelectionException {
		select(doc, offset, offset + length);
		this.doc = doc;
		this.offset = offset;
		this.length = length;
	}
	
	public List<DocumentElement> getDocumentElements() {
		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
		return getChildren(root);
	}
	
	private List<DocumentElement> getChildren(DocumentElement elem) {
		List<DocumentElement> theChildren = new ArrayList<DocumentElement>();
		
		int startIdx = elem.getElementIndex(offset);
		int endIdx = elem.getElementIndex(offset + length - 1);
		
		while (startIdx <= endIdx) {
			DocumentElement child = (DocumentElement) elem.getElement(startIdx++);
			ElementML childML = child.getElementML();
				
			if (offset <= child.getStartOffset()
				&& child.getEndOffset() <= offset + length) {
				// child is fully selected				
				if (childML instanceof ParagraphML && childML.isImplied()) {
					theChildren.addAll(getChildren(child));
				} else {
					theChildren.add(child);
				}
			} else if (!child.isLeaf()) {
				//partially selected block
				theChildren.addAll(getChildren(child));
				
			} else {
				//partially selected leaf
				theChildren.add(child);
			}
		}
		
		return theChildren;
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
			firstLeaf = null;
			throw new BadSelectionException("Bad Start Position", p0, p1 - p0);
		}

		DocumentElement lastLeaf = (DocumentElement) doc.getCharacterElement(p1 - 1);

		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p1 = " + lastLeaf);
		}

		if (p1 < lastLeaf.getEndOffset() && !lastLeaf.isEditable()) {
			lastLeaf = null;
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
    
    protected boolean isFullySelected(Element elem) {
    	return (offset <= elem.getStartOffset() 
    			&& elem.getEndOffset() <= offset + length);
    }

}// TextSelector class



















