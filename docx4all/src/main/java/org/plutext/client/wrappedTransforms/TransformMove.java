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
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Changesets.Changeset;
import org.plutext.transforms.Transforms.T;

public class TransformMove extends TransformAbstract {
	private static Logger log = Logger.getLogger(TransformMove.class);

	public TransformMove(T t) {
		super(t);
	}

	 /* Compare the updated sdt to the original, replacing the
     * updated one with containing w:ins and w:del */
	@Override
    public String markupChanges(String original, Changeset changeset) {
        // Do nothing.
        // How best to indicate to the user that something
        // has moved?  Just in a dialog box?
		return null;
    }

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Moving SdtBlock = " + getSdt() + " - ID=" + idStr);

		Long moveToIndex = null;
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			log.error("apply(): Invalid location t.getPosition()="
					+ t.getPosition());
			moveToIndex = null;

		} else {
			// if user has locally inserted/deleted sdt's
			// we need to adjust the specified position ...
			Long pos = t.getPosition();
			moveToIndex = pos + mediator.getDivergences().getOffset(pos);

			log.debug("apply(): Location " + pos + " adjusted to "
					+ moveToIndex);
		}

		if (moveToIndex == null || moveToIndex < 0) {
			log.error("apply(): Invalid moveToIndex=" + moveToIndex);
			// TODO - throw error
			return -1;
		}

		WordMLDocument doc = 
			(WordMLDocument) mediator.getWordMLTextPane().getDocument();
		DocumentElement elem = Util.getDocumentElement(doc, idStr);
		if (elem == null) {
			// should not happen.
			log.error("apply(): DocumentElement NOT FOUND. Sdt Id=" + idStr);
			// TODO - throw error
			return -1;
		}
		
		log.debug("apply(): DocumentElement of Sdt Id=" + idStr
			+ " is "
			+ elem);

		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();

		ElementML bodyML = root.getElementML().getChild(0);
		int idx = Math.min(bodyML.getChildrenCount() - 1, moveToIndex.intValue());

		log.debug("apply(): Maximum Index="
			+ (bodyML.getChildrenCount() - 1)
			+ ". SdtBlock will be moved to idx=" + idx);

		ElementML elemMLAtMoveToIndex = bodyML.getChild(idx);
		log.debug("apply(): Currently, ElementML at idx=" + idx + " is " + elemMLAtMoveToIndex);

		if (elemMLAtMoveToIndex instanceof SdtBlockML) {
			SdtBlockML sdtML = (SdtBlockML) elemMLAtMoveToIndex;
			if (sdtML.getSdtProperties().getIdValue().equals(getId().getVal())) {
				log.debug("apply(): Need not to move."
						+ " moveToIndex == currentIndex == " + idx);
				return sequenceNumber;				
			}
		}
		
		// Semantics of move are
		// 1 remove existing element
		// 2 insert new element

		// So
		mediator.getDivergences().delete(idStr);
		mediator.getDivergences().insert(idStr, moveToIndex);

		//Move SdtBlock by first deleting the block.
		SdtBlockML copy = (SdtBlockML) elem.getElementML().clone();
		elem.getElementML().delete();
		updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());

		//Insert the deleted block into new position.
		elemMLAtMoveToIndex.addSibling(copy, false);
		
		//Record the offset range for the insertion just done.
		elem = (DocumentElement) root.getElement(idx);
		if (elem.getElementML() != elemMLAtMoveToIndex) {
			updateRefreshOffsets(mediator, 0, doc.getLength());
		} else {
			updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());			
		}

		return sequenceNumber;
	}


}// TransformMove class

