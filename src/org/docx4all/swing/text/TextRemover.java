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

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.DocumentFilter.FilterBypass;

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.RunContentML;

/**
 *	@author Jojada Tirtowidjojo - 10/01/2008
 */
public class TextRemover extends TextSelector implements TextProcessor {
	private static Logger log = Logger.getLogger(TextRemover.class);

	private FilterBypass filterBypass;
	
	public TextRemover(FilterBypass fb, int offset, int length) 
		throws BadSelectionException {
		super((WordMLDocument) fb.getDocument(), offset, length);
		this.filterBypass = fb;
	}

	public void doAction() throws BadLocationException {
		if (log.isDebugEnabled()) {
			log.debug("doAction(): offset=" + offset + " length=" + length);
		}
		
    	List<DocumentElement> list = getDocumentElements();
    	DocumentElement tempE = list.get(0);
		
    	//Handle the first leaf element specially
    	if (tempE.isLeaf() && !isFullySelected(tempE)) {
			if (list.size() == 1) {
				// A single partially selected leaf element
				// is treated as normal string deletion
				filterBypass.remove(offset, length);
				return;
			}

			int count = offset - tempE.getStartOffset();
			String text = doc.getText(tempE.getStartOffset(), count);
			RunContentML rcml = (RunContentML) tempE.getElementML();
			rcml.setTextContent(text);

			// first leaf element has been processed.
			// Remove from list
			list.remove(0);
		}

    	// Handle the last leaf element specially
    	tempE = list.get(list.size() - 1);
    	if (tempE.isLeaf() && !isFullySelected(tempE)) {
			int count = tempE.getEndOffset() - (offset + length);
			String text = doc.getText(offset + length, count);
			RunContentML rcml = (RunContentML) tempE.getElementML();
			rcml.setTextContent(text);

			// last leaf element has been processed.
			// Remove from list
			list.remove(list.size() - 1);
		}
    	
    	DocUtil.deleteElementML(list);
    	
		DocumentElement firstPara = 
			(DocumentElement) doc.getParagraphMLElement(offset, false);
		DocumentElement lastPara = null;
		if (offset == firstPara.getEndOffset() - 1 && length == 1) {
			//deleting an implied newline character
			lastPara = 
				(DocumentElement) 
					doc.getParagraphMLElement(offset + length, false);
		} else {
			lastPara = 
				(DocumentElement)
					doc.getParagraphMLElement(offset + length - 1, false);

		}
		
		if (firstPara.getStartOffset() < offset
			&& offset + length < lastPara.getEndOffset() - 1
			&& firstPara != lastPara) {
			//Merge firstPara and lastPara if both are not fully selected.
			mergeElementML(firstPara, lastPara);
		}

    	//Time to refresh affected paragraphs.
		refreshAffectedParagraphs(firstPara, lastPara);		
	}
	
	private void refreshAffectedParagraphs(
		DocumentElement firstPara, 
		DocumentElement lastPara)
		throws BadLocationException {
		
    	int start = firstPara.getStartOffset();
    	int end = lastPara.getEndOffset();

    	List<ElementSpec> specs = new ArrayList<ElementSpec>();
    	ElementML paraML = firstPara.getElementML();
    	if (paraML.getParent() != null) {
    		specs.addAll(DocUtil.getElementSpecs(paraML));
    	}
    	
    	paraML = lastPara.getElementML();
    	if (firstPara != lastPara && paraML.getParent() != null) {
    		specs.addAll(DocUtil.getElementSpecs(paraML));
    	}
    	
    	filterBypass.remove(start, end - start);
    	
    	if (!specs.isEmpty()) {
    		insertLater(doc, start, specs);
    	}
	}
	
	private void insertLater(
		final WordMLDocument doc, 
		final int offset,
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

	private void mergeElementML(DocumentElement para1, DocumentElement para2) {		
		//Grab all lastPara's content from offset + length position
		int start = offset + length;
		int count = (para2.getEndOffset() - 1) - start;
		TextSelector ts = null;
		try {
			ts = new TextSelector(doc, start, count);
		} catch (BadSelectionException exc) {
			;//should be fine
		}
		
		//Delete the merged para2
		List<DocumentElement> elemsToMerge = ts.getDocumentElements();
		//delete the ElementMLs of elemsToMerge
		//so that they are ready for merging
		DocUtil.deleteElementML(elemsToMerge);
		//Delete the whole para2's ElementML
		para2.getElementML().delete();
		
		//Find element where merging start
		DocumentElement targetE = (DocumentElement) doc.getCharacterElement(offset - 1);
		ElementML targetML = targetE.getElementML();
		
		//elemsToMerge can only consist of RunML's DocumentElement 
		//and RunContentML's DocumentElement.
		//If there is one or more RunContentML's DocumentElement 
		//they should be at the top of the list.
		for (DocumentElement de: elemsToMerge) {
			if (!de.isLeaf() && targetE.isLeaf()) {
				targetE = (DocumentElement) targetE.getParentElement();
				targetML = targetE.getElementML();
			}
			ElementML ml = de.getElementML();
			if (!ml.isImplied()) {
				targetML.addSibling(ml, true);
				targetML = ml;
			}
		}
	}
	
}// TextRemover class



















