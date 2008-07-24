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
import org.plutext.transforms.Transforms.T;

public class TransformMove extends TransformAbstract {
	private static Logger log = Logger.getLogger(TransformMove.class);

	public TransformMove(T t) {
		super(t);
	}

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Moving SdtBlock = " + getSdt() + " - ID=" + idStr);

		if (stateChunks.get(idStr) == null) {
			log.error("apply(): Could not find SDT Id=" + idStr + " snapshot.");
			// TODO - throw error
			return -1;
		}

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

		// Semantics of move are
		// 1 remove existing element
		// 2 insert new element

		// So
		mediator.getDivergences().delete(idStr);
		mediator.getDivergences().insert(idStr, moveToIndex);

		
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
			if (sdtML.getSdtProperties().getIdValue() == getId().getVal()) {
				log.debug("apply(): Need not to move."
						+ " moveToIndex == currentIndex == " + idx);
				return sequenceNumber;				
			}
		}
		
		//Move SdtBlock by first deleting the block.
		SdtBlockML copy = (SdtBlockML) elem.getElementML().clone();
		elem.getElementML().delete();

		//Record the update range in document.
		int offset = mediator.getUpdateStartOffset();
		offset = Math.min(offset, elem.getStartOffset());
		mediator.setUpdateStartOffset(offset);

		offset = mediator.getUpdateEndOffset();
		offset = Math.max(offset, elem.getEndOffset());
		mediator.setUpdateEndOffset(offset);

		//Insert the deleted block into new position.
		elemMLAtMoveToIndex.addSibling(copy, false);
		
		//Record the update range for the insertion just done.
		elem = null;
		for (int i = 0; (elem == null && i < root.getElementCount() - 1); i++) {
			elem = (DocumentElement) root.getElement(i);
			if (elem.getElementML() != elemMLAtMoveToIndex) {
				elem = null;
			}
		}

		if (elem == null) {
			//should not happen.
			//If it does happen then refresh the whole document.
			mediator.setUpdateStartOffset(0);
			mediator.setUpdateEndOffset(doc.getLength());
			
		} else {
			offset = mediator.getUpdateStartOffset();
			offset = Math.min(offset, elem.getStartOffset());
			mediator.setUpdateStartOffset(offset);

			offset = mediator.getUpdateEndOffset();
			offset = Math.max(offset, elem.getEndOffset());
			mediator.setUpdateEndOffset(offset);
		}
		
		return sequenceNumber;
	}

}// TransformMove class

