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
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;


public class TransformDelete extends TransformAbstract {
	
	private static Logger log = Logger.getLogger(TransformDelete.class);	

    public TransformDelete(T t)
    {
    	super(t);
    }

//        public TransformDelete(String idref)
//        {
//        	super();
//            this.id = idref;
//        }


        /* delete the SDT given its ID. */
	public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks)
        {

    	
		log.debug("apply(ServerFrom): Deleting SdtBlock = " + getSdt() 
				+ " - ID=" + getId().getVal());

    	if ( stateChunks.remove(getId().getVal().toString()) == null) {
    		log.debug("apply(): Could not find SDT Id=" + getId().getVal() + " snapshot.");
	        // couldn't find!
	        // TODO - throw error
    		return -1;
    	}
    	
   		apply(mediator.getWordMLTextPane());
        	
        mediator.getDivergences().delete(getId().getVal().toString());
        
		return sequenceNumber;
    }
        
	protected void apply(WordMLTextPane editor) {
		
		log.debug("apply(WordMLTextPane): Deleting SdtBlock ID=" 
				+ getId().getVal().toString() + "...");
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		int origPos = editor.getCaretPosition();
		boolean forward = true;
						
		try {
			editor.beginContentControlEdit();
			
			DocumentElement elem = Util.getDocumentElement(doc, getId().getVal().toString());
			
			if (elem == null) {
				//should not happen.
				log.error("apply(WordMLTextPane): DocumentElement NOT FOUND. Sdt Id=" 
					+ getId().getVal().toString());
				return;
			}
			
			log.debug("apply(WordMLTextPane): SdtBlock Element=" + elem);
			log.debug("apply(WordMLTextPane): Current caret position=" + origPos);
			
			if (elem.getStartOffset() <= origPos 
				&& origPos < elem.getEndOffset()) {
				origPos = elem.getEndOffset();
			}
				
			if (elem.getEndOffset() <= origPos) {
				origPos = doc.getLength() - origPos;
				forward = false;
			}
			
			elem.getElementML().delete();
			doc.refreshParagraphs(elem.getStartOffset(), 1);

		} finally {
			if (!forward) {
				origPos = doc.getLength() - origPos;
			}
			
			editor.endContentControlEdit();
			
			log.debug("apply(WordMLTextPane): Set caret position to " + origPos);
			editor.setCaretPosition(origPos);
			
		}
	}
	
} //TransformDelete class
