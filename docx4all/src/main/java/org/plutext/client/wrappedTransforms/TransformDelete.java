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
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;

public class TransformDelete extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformDelete.class);

	public TransformDelete(T t) {
		super(t);
	}

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Deleting SdtBlock = " + getSdt()
				+ " - ID=" + idStr);

		if (stateChunks.remove(idStr) == null) {
			log.error("apply(): Could not find SDT Id=" + idStr + " snapshot.");
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

		elem.getElementML().delete();

		int offset = mediator.getUpdateStartOffset();
		offset = Math.min(offset, elem.getStartOffset());
		mediator.setUpdateStartOffset(offset);

		offset = mediator.getUpdateEndOffset();
		offset = Math.max(offset, elem.getEndOffset());
		mediator.setUpdateEndOffset(offset);

		mediator.getDivergences().delete(idStr);

		return sequenceNumber;
	}

} // TransformDelete class

