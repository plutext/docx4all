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

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.ServerFrom;
import org.plutext.transforms.Transforms.T;

public class TransformUpdate extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformUpdate.class);

	public TransformUpdate(T t) {
		super(t);
	}

	public long apply(ServerFrom serverFrom) {

		log.debug("apply(ServerFrom): sdtBolck = " + getSdt() 
			+ " - ID=" + getSdt().getSdtPr().getId().getVal()
			+ " - TAG=" + getSdt().getSdtPr().getTag().getVal());

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
		apply(serverFrom.getWordMLTextPane(), getSdt());

		// Fourth, if we haven't thrown an exception, return the sequence number
		return sequenceNumber;
	}

	protected void apply(final WordMLTextPane editor, final SdtBlock sdt) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				log.debug("apply(WordMLTextPane, SdtBlock): Updating SDT Id=" 
						+ sdt.getSdtPr().getId() + " in Editor.");
				
				int origPos = editor.getCaretPosition();
				
				try {
					editor.beginContentControlEdit();
				
					DocumentElement elem = getDocumentElement(editor, sdt);
					if (elem != null) {
						ElementMLRecord[] recs = 
							{new ElementMLRecord(new SdtBlockML(sdt), false)};
						WordMLFragment frag = new WordMLFragment(recs);
						
						editor.setCaretPosition(elem.getStartOffset());
						editor.moveCaretPosition(elem.getEndOffset());
						editor.replaceSelection(frag);						
					} else {
						//silently ignore
						log.warn("apply(WordMLTextPane, SdtBlock): SDT Id=" 
							+ sdt.getSdtPr().getId() 
							+ " NOT FOUND in editor.");
					}
				} finally {
					editor.endContentControlEdit();
					editor.setCaretPosition(origPos);				
				}
			}
		});
	}
	
} //TransformUpdate class
























