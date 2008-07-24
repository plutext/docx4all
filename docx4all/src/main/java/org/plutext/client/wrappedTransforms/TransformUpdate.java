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

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.diff.ParagraphDifferencer;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;

public class TransformUpdate extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformUpdate.class);

	public TransformUpdate(T t) {
		super(t);
	}

	/*
	 * Compare the updated sdt to the original, replacing the updated one with
	 * containing w:ins and w:del
	 */
	public void markupChanges(SdtBlock local) {
		SdtContentBlock markedUpContent;
		try {
			javax.xml.bind.util.JAXBResult result = new javax.xml.bind.util.JAXBResult(
					org.docx4j.jaxb.Context.jc);

			ParagraphDifferencer.diff(getSdt().getSdtContent(), local
					.getSdtContent(), result);

			markedUpContent = (SdtContentBlock) result.getResult();
		} catch (JAXBException e) {
			log.error("markupChanges(): JAXBException caught.", e);
			// Oh well, we'll display it without changes marked up
			return;
		}

		// Now replace the content of sdt
		getSdt().setSdtContent(markedUpContent);
	}

	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Updating SdtBlock = " + getSdt() + " - ID=" + idStr
				+ " - TAG=" + getTag().getVal());

		if (stateChunks.get(idStr) == null) {
			log.error("apply(): Could not find SDT Id=" + idStr + " snapshot.");
			// TODO - throw error
			return -1;
		}

		WordMLDocument doc = (WordMLDocument) mediator.getWordMLTextPane()
				.getDocument();
		DocumentElement elem = Util.getDocumentElement(doc, idStr);
		if (elem == null) {
			// should not happen.
			log.error("apply(): DocumentElement NOT FOUND. Sdt Id=" + idStr);
			// TODO - throw error
			return -1;
		}

		SdtBlockML newSdt = new SdtBlockML(getSdt());
		elem.getElementML().addSibling(newSdt, false);
		elem.getElementML().delete();

		int offset = mediator.getUpdateStartOffset();
		offset = Math.min(offset, elem.getStartOffset());
		mediator.setUpdateStartOffset(offset);

		offset = mediator.getUpdateEndOffset();
		offset = Math.max(offset, elem.getEndOffset());
		mediator.setUpdateEndOffset(offset);

		stateChunks.put(idStr, new StateChunk(getSdt()));

		// Fourth, if we haven't thrown an exception, return the sequence number
		return sequenceNumber;
	}

} // TransformUpdate class

