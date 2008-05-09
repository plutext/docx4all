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

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.xml.SdtBlockML;
import org.plutext.client.ServerFrom;
import org.plutext.transforms.Transforms.T;

public class TransformUpdate extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformUpdate.class);

	public TransformUpdate(T t) {
		super(t);
	}

	public int apply(ServerFrom serverFrom) {

		log.debug(this.getClass().getName());

		// First, find the Content Control we need to update
		Boolean found = false;

		if (!found) {
			// TODO - this will happen to a client A which deleted a chunk
			// or B which has the document open, if a client C reinstates
			// a chunk.  It will happen because A and B simply get an
			// update notice

			// One way to address this is to treat it as an insert,
			// asking for the skeleton doc in order to find out where
			// exactly to insert it.

			// throw ...

		}

		// TODO - do the actual replacement in a docx4all specific way.
		apply(serverFrom.getWordMLTextPane(), serverFrom.getOffset(getSdt()));

		// Fourth, if we haven't thrown an exception, return the sequence number
		return sequenceNumber;
	}

	protected void apply(WordMLTextPane editor, int offset) {
		ElementMLRecord[] recs = {new ElementMLRecord(new SdtBlockML(getSdt()), false)};
		WordMLFragment frag = new WordMLFragment(recs);
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		DocumentElement elem = (DocumentElement) doc.getDefaultRootElement();
		int idx = elem.getElementIndex(offset);
		elem = (DocumentElement) elem.getElement(idx);
		
		editor.setCaretPosition(elem.getStartOffset());
		editor.moveCaretPosition(elem.getEndOffset());
		editor.replaceSelection(frag);
	}
}
























