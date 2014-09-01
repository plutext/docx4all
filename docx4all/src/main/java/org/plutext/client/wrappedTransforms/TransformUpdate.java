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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.plutext.client.Mediator;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Changesets.Changeset;
import org.plutext.transforms.Transforms.T;

public class TransformUpdate extends TransformAbstract {

	private static Logger log = LoggerFactory.getLogger(TransformUpdate.class);

	public TransformUpdate(T t) {
		super(t);
	}

	/* Compare the updated sdt to the original, replacing the
     * updated one with containing w:ins and w:del 
     */
	@Override
    public String markupChanges(String original, Changeset changeset) {
		log.debug("markupChanges(): Marking up SdtBlock = " 
			+ getSdt() 
			+ " - ID="
			+ getPlutextId() );
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
		String idStr = getPlutextId();

		log.debug("apply(): Updating SdtBlock = " + getSdt() + " - ID=" + idStr );
//				+ " - TAG=" + getVersion().getVal());

		if (stateChunks.get(idStr) == null) {
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

    	if (this.markedUpSdt == null) {
    		//Sdt has not been marked up or there was an error during marking up.
    		//See: markupChanges().
    		//Silently ignore.
    		log.error("apply(): No marked up Sdt.");
    		return -1;
    	}
    	
		SdtBlockML newSdt = new SdtBlockML(this.markedUpSdt);
		elem.getElementML().addSibling(newSdt, false);
		elem.getElementML().delete();

		updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());

        //What goes in stateChunks is the *non-marked up* 
		//sdt that we got from the server.
		StateChunk newsc = new StateChunk(getSdt());
        // But also record the marked up version
		newsc.setMarkedUpSdt(XmlUtils.marshaltoString(this.markedUpSdt, true));
		stateChunks.put(idStr, newsc);

		// Fourth, if we haven't thrown an exception, return the sequence number
		return sequenceNumber;
	}

} // TransformUpdate class

