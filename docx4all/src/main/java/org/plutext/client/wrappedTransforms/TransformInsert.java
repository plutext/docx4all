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
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Changesets.Changeset;
import org.plutext.transforms.Transforms.T;

public class TransformInsert extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformInsert.class);
	
	public TransformInsert(T t) {
		super(t);
	}

	/* Markup the existing sdt with one containing w:ins or w:del*/
	@Override
    public String markupChanges(String original, Changeset changeset) {
		log.debug("markupChanges(): Marking up SdtBlock = " 
			+ getSdt() 
			+ " - ID="
			+ getId().getVal().toString());
		log.debug("markupChanges(): 'original' param = " + original);
		
    	try {
    		if (original == null) {
    			this.markedUpSdt = XmlUtil.markupAsInsertion(getSdt(), changeset);
    		} else {
    			org.docx4j.wml.SdtBlock origSdt = 
    				(org.docx4j.wml.SdtBlock) XmlUtils.unmarshalString(original);
    			this.markedUpSdt = XmlUtil.markupDifference(getSdt(), origSdt, changeset);
    		}
		} catch (Exception exc) {
			log.error("markupChanges(): Exception caught during marking up:");
			exc.printStackTrace();
			this.markedUpSdt = null;
		}

		String result = null;
		if (this.markedUpSdt != null) {
			result = XmlUtils.marshaltoString(this.markedUpSdt, true);
		}
		
        log.debug("markupChanges(): Result = " + result);
        
        return result;
    }
    
    public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks) {
		String idStr = getId().getVal().toString();

		log.debug("apply(): Inserting SdtBlock = " + getSdt() + " - ID="
				+ idStr);

    	if (this.markedUpSdt == null) {
    		//Sdt has not been marked up or there was an error during marking up.
    		//See: markupChanges().
    		//Silently ignore.
    		log.error("apply(): No marked up Sdt.");
    		return -1;
    	}
    	
    	// If a Delete is followed by Insert (reinstate),
        // the server will not transmit the Delete.
        // So here we test whether the sdt is already present
        /* Testing this case requires three clients:
         * 
         * Client 1, 2, 3 all open.
         * 
         * Client 1 deletes an sdt; transmits
         * 
         * Client 2 fetches; rejects change (and optionally moves or edits); transmits
         * 
         * Client 3 fetches
         * 
         */
		WordMLDocument doc = 
			(WordMLDocument) mediator.getWordMLTextPane().getDocument();
		DocumentElement elem = Util.getDocumentElement(doc, idStr);
		if (elem != null) {
			//Treat this element as being moved.
            log.debug("apply(): Detected reinstatement.");
            log.debug("apply(): Document element=" + elem + " is deleted.");
			elem.getElementML().delete();
			updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());
			mediator.getDivergences().delete(idStr);
		}
    	    	
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

		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();

		ElementML bodyML = root.getElementML().getChild(0);
		int idx = Math.min(bodyML.getChildrenCount() - 1, insertAtIndex
				.intValue());

		log.debug("apply(): SdtBlock will be inserted at idx=" + idx);

		ElementML ml = bodyML.getChild(idx);
		
		log.debug("apply(): Currently, ElementML at idx=" + idx + " is " + ml);

		SdtBlockML markedUpML = 
			new SdtBlockML(
				(org.docx4j.wml.SdtBlock) 
				XmlUtils.deepCopy(this.markedUpSdt));
		ml.addSibling(markedUpML, false);

		elem = (DocumentElement) root.getElement(idx);
		if (elem.getElementML() != ml) {
			//Just in case.
			//hint to refresh the whole document.
			updateRefreshOffsets(mediator, 0, doc.getLength());			
		} else {
			updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());
		}

        //What goes in stateChunks is the *non-marked up* 
		//sdt that we got from the server.
        StateChunk sc = new StateChunk(getSdt());
        stateChunks.put(sc.getIdAsString(), sc);

        // But also record the marked up version
        sc.setMarkedUpSdt(XmlUtils.marshaltoString(this.markedUpSdt, true));

		mediator.getDivergences().insert(idStr, Long.valueOf(idx));
		
		return sequenceNumber;
	}

} // TransformInsert class

























