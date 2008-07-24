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
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.plutext.client.Mediator;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;

public class TransformInsert extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformInsert.class);

	public TransformInsert(T t) {
		super(t);
	}

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Inserting SdtBlock = " + getSdt() + " - ID="
				+ idStr);

		// Plutext server is trying to use absolute index position for
		// locating the insert positon.
		Long insertAtIndex = null;
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			log.error("apply(): Invalid insertion location t.getPosition()="
					+ t.getPosition());
			insertAtIndex = null;

		} else {
			// if user has locally inserted/deleted sdt's
			// we need to adjust the specified position ...
			Long pos = t.getPosition();
			insertAtIndex = pos + mediator.getDivergences().getOffset(pos);

			log.debug("apply(): Insertion location " + pos + " adjusted to "
					+ insertAtIndex);
		}

		if (insertAtIndex == null || insertAtIndex < 0) {
			log.error("apply(): Invalid insertAtIndex=" + insertAtIndex);
			// TODO - throw error
			return -1;
		}

		WordMLDocument doc = (WordMLDocument) mediator.getWordMLTextPane()
				.getDocument();
		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();

		ElementML bodyML = root.getElementML().getChild(0);
		int idx = Math.min(bodyML.getChildrenCount() - 1, insertAtIndex
				.intValue());

		log.debug("apply(): SdtBlock will be inserted at idx=" + idx);

		ElementML ml = bodyML.getChild(idx);
		log.debug("apply(): Currently, ElementML at idx=" + idx + " is " + ml);

		ml.addSibling(new SdtBlockML(getSdt()), false);

		DocumentElement elem = null;
		for (int i = 0; (elem == null && i < root.getElementCount() - 1); i++) {
			elem = (DocumentElement) root.getElement(i);
			if (elem.getElementML() != ml) {
				elem = null;
			}
		}

		if (elem == null) {
			//should not happen.
			//If it does happen then refresh the whole document.
			mediator.setUpdateStartOffset(0);
			mediator.setUpdateEndOffset(doc.getLength());
			
		} else {
			int offset = mediator.getUpdateStartOffset();
			offset = Math.min(offset, elem.getStartOffset());
			mediator.setUpdateStartOffset(offset);

			offset = mediator.getUpdateEndOffset();
			offset = Math.max(offset, elem.getEndOffset());
			mediator.setUpdateEndOffset(offset);
		}

		StateChunk sc = new StateChunk(getSdt());
		stateChunks.put(sc.getIdAsString(), sc);
		mediator.getDivergences().insert(idStr, Long.valueOf(idx));
		
		return sequenceNumber;
	}

} // TransformInsert class

