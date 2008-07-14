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

import java.math.BigInteger;
import java.util.HashMap;

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

public class TransformInsert extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformInsert.class);

	public TransformInsert(T t) {
		super(t);
		insertAtIndex = null;
	}

	protected Long insertAtIndex;

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		// Plutext server is trying to use absolute index position for
		// locating the insert positon.
		// TODO: The following code is subject to change.
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			log.warn("Invalid insertion location t.getPosition()="
					+ t.getPosition());
			this.insertAtIndex = null;

		} else {
			// if user has locally inserted/deleted sdt's
			// we need to adjust the specified position ...
			Long pos = t.getPosition();
			this.insertAtIndex = pos + mediator.getDivergences().getOffset(pos);

			log.debug("Insertion location " + pos + " adjusted to "
					+ insertAtIndex);
		}

		if (this.insertAtIndex == null || this.insertAtIndex < 0) {
			log.warn("Invalid insertAtIndex=" + this.insertAtIndex);
			return -1;
		}

		apply(mediator.getWordMLTextPane());

		StateChunk sc = new StateChunk(sdt);
		stateChunks.put(sc.getIdAsString(), sc);
		mediator.getDivergences().insert(id.getVal().toString(),
					insertAtIndex);

		return sequenceNumber;
	}

	protected void apply(WordMLTextPane editor) {
		BigInteger id = getSdt().getSdtPr().getId().getVal();
		
		log.debug("apply(WordMLTextPane): Inserting SdtBlock Id=" 
				+ id + " in Editor.");
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		if (Util.getDocumentElement(doc, id.toString()) != null) {
			log.error("apply(WordMLTextPane): SdtBlock Id=" + id
					+ " already exists in editor");
			return;
		}

		int origPos = editor.getCaretPosition();
		boolean forward = true;

		try {
			// editor.beginContentControlEdit();

			DocumentElement elem = (DocumentElement) doc
					.getDefaultRootElement();

			int idx = Math.min(elem.getElementCount() - 1, this.insertAtIndex
					.intValue());
			idx = Math.max(idx, 0);

			log
					.debug("apply(WordMLTextPane): SdtBlock will be inserted at idx="
							+ idx + " in document.");

			elem = (DocumentElement) elem.getElement(idx);

			log.debug("apply(WordMLTextPane): DocumentElement at idx=" + idx
					+ " is " + elem);

			log.debug("apply(WordMLTextPane): Current caret position="
					+ origPos);

			if (elem.getStartOffset() <= origPos) {
				origPos = doc.getLength() - origPos;
				forward = false;
			}

			ElementMLRecord[] recs = { new ElementMLRecord(new SdtBlockML(sdt),
					false) };
			WordMLFragment frag = new WordMLFragment(recs);

			editor.setCaretPosition(elem.getStartOffset());
			editor.replaceSelection(frag);

		} finally {
			if (!forward) {
				origPos = editor.getDocument().getLength() - origPos;
			}

			log
					.debug("apply(WordMLTextPane): Set caret position to "
							+ origPos);
			editor.setCaretPosition(origPos);

			// editor.endContentControlEdit();
		}
	}

} // TransformInsert class

