/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client.wrappedTransforms;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.SdtBlockML;
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;

public class TransformMove extends TransformAbstract {
	private static Logger log = Logger.getLogger(TransformMove.class);

	public TransformMove(T t) {
		super(t);
	}

	protected Long moveToIndex;

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
    	if ( stateChunks.get(getId().getVal().toString()) == null) {
    		log.error("Could not find SDT Id=" + getId().getVal() + " snapshot.");
	        // TODO - throw error
    		return -1;
    	}
    	
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			log.error("Invalid location t.getPosition()=" + t.getPosition());
			this.moveToIndex = null;

		} else {
			// if user has locally inserted/deleted sdt's
			// we need to adjust the specified position ...
			Long pos = t.getPosition();
			this.moveToIndex = pos + mediator.getDivergences().getOffset(pos);

			log.debug("Location " + pos + " adjusted to " + this.moveToIndex);
		}

		if (this.moveToIndex == null || this.moveToIndex < 0) {
			log.error("Invalid moveToIndex=" + this.moveToIndex);
			return -1;
		}

		// Semantics of move are
		// 1 remove existing element
		// 2 insert new element

		// So
		mediator.getDivergences().delete(getId().getVal().toString());
		mediator.getDivergences().insert(getId().getVal().toString(),
				this.moveToIndex);

		apply(mediator.getWordMLTextPane());

		return sequenceNumber;
	}

	protected void apply(WordMLTextPane editor) {
		int origPos = editor.getCaretPosition();
		boolean forward = true;

		log.debug("apply(WordMLTextPane): Moving SdtBlock Id=" 
				+ getId().getVal().toString() + " in Editor.");
		log.debug("apply(WordMLTextPane): Current caret position=" + origPos);

		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		try {
			editor.beginContentControlEdit();

			DocumentElement elem = 
				Util.getDocumentElement(doc, getId().getVal().toString());
			if (elem == null) {
				//should not happen.
				log.error("apply(WordMLTextPane): DocumentElement NOT FOUND. Sdt Id=" 
					+ getId().getVal().toString());
				return;
			}
			
			DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
			int idx = root.getElementIndex(elem.getStartOffset());
			if (this.moveToIndex.intValue() == idx) {
				log.debug("apply(WordMLTextPane): Need not to move."
					+ " moveToIndex == currentIndex == " 
					+ idx);
				return;
			}
			
			int start = elem.getStartOffset();
			int end = elem.getEndOffset();

			if (start <= origPos && origPos < end) {
				origPos = end;
			}

			if (end <= origPos) {
				origPos = doc.getLength() - origPos;
				forward = false;
			}

			SdtBlockML copy = (SdtBlockML) elem.getElementML().clone();
			elem.getElementML().delete();
			doc.refreshParagraphs(elem.getStartOffset(), 1);
									
			if (!forward) {
				origPos = doc.getLength() - origPos;
				forward = true;
			}

			idx = Math.min(root.getElementCount() - 1, this.moveToIndex.intValue());
			idx = Math.max(idx, 0);

			log.debug("apply(WordMLTextPane): SdtBlock will be moved to idx=" 
				+ idx
				+ " in document.");

			elem = (DocumentElement) root.getElement(idx);

			log.debug("apply(WordMLTextPane): DocumentElement at idx=" + idx
				+ " is " + elem);
			
			if (elem.getStartOffset() <= origPos) {
				origPos = doc.getLength() - origPos;
				forward = false;
			}

			elem.getElementML().addSibling(copy, false);
			doc.refreshParagraphs(elem.getStartOffset(), 1);
			
		} finally {
			if (!forward) {
				origPos = doc.getLength() - origPos;
			}

			log.debug("apply(WordMLTextPane): Resulting Structure...");
			DocUtil.displayStructure(doc);
			
			editor.endContentControlEdit();
			
			log.debug("apply(WordMLTextPane): Set caret position to " + origPos);
			editor.setCaretPosition(origPos);
		}
	}

}// TransformMove class

