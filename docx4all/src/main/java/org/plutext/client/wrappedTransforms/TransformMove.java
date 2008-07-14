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

import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
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
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			log.warn("Invalid location t.getPosition()=" + t.getPosition());
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

		WordMLDocument doc = (WordMLDocument) editor.getDocument();

		// Delete first
		DocumentElement elem = Util.getDocumentElement(doc, getId().getVal()
				.toString());
		if (elem != null) {
			int start = elem.getStartOffset();
			int end = elem.getEndOffset();

			if (start <= origPos && origPos < end) {
				origPos = end;
			}

			if (end <= origPos) {
				origPos = editor.getDocument().getLength() - origPos;
				forward = false;
			}

			try {
				doc.remove(start, end - start);
			} catch (BadLocationException exc) {
				;// should not happen
			}

			if (!forward) {
				origPos = doc.getLength() - origPos;
				forward = true;
			}
		}

		// Then insert at new location
		elem = (DocumentElement) doc.getDefaultRootElement();
		int idx = Math.min(elem.getElementCount() - 1, this.moveToIndex
				.intValue());
		idx = Math.max(idx, 0);

		log.debug("apply(WordMLTextPane): SdtBlock will be moved to idx=" + idx
				+ " in document.");

		elem = (DocumentElement) elem.getElement(idx);

		log.debug("apply(WordMLTextPane): DocumentElement at idx=" + idx
				+ " is " + elem);

		log.debug("apply(WordMLTextPane): Current caret position=" + origPos);

		if (elem.getStartOffset() <= origPos) {
			origPos = doc.getLength() - origPos;
			forward = false;
		}

		ElementMLRecord[] recs = { new ElementMLRecord(new SdtBlockML(sdt),
				false) };
		WordMLFragment frag = new WordMLFragment(recs);
		try {
			doc.insertFragment(elem.getStartOffset(), frag, null);
		} catch (BadLocationException exc) {
			;// should not happen
		} finally {
			if (!forward) {
				origPos = editor.getDocument().getLength() - origPos;
			}

			log
					.debug("apply(WordMLTextPane): Set caret position to "
							+ origPos);
			editor.setCaretPosition(origPos);
		}
	}

}// TransformMove class

