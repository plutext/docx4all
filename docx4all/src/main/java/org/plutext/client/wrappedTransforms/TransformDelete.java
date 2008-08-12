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

public class TransformDelete extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformDelete.class);

	private org.docx4j.wml.SdtBlock markedUpSdt;
	
	public TransformDelete(T t) {
		super(t);
	}

	/* Markup the existing sdt with one containing w:ins or w:del*/
	@Override
    public String markupChanges(String sdtParam, Changeset changeset) {
		String idStr = getId().getVal().toString();
		
		log.debug("markupChanges(): THIS SdtBlock = " 
			+ getSdt()
			+ " - ID=" 
			+ idStr);
		log.debug("markupChanges(): Marking up sdtParam = " + sdtParam); 
		
		try {
			org.docx4j.wml.SdtBlock temp = 
				(org.docx4j.wml.SdtBlock) XmlUtils.unmarshalString(sdtParam);
			this.markedUpSdt = XmlUtil.markupAsDeletion(temp, null);
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

		log.debug("apply(): Deleting SdtBlock = " + getSdt()
				+ " - ID=" + idStr);

    	if (this.markedUpSdt == null) {
    		//Sdt has not been marked up or there was an error during marking up.
    		//See: markupChanges().
    		//Silently ignore.
    		log.error("apply(): No marked up Sdt.");
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

		stateChunks.remove(idStr);
		
		ElementML sdtBlockML = elem.getElementML();
		
		//Insert this.markedUpSdt into WordMLDocument
		//so that user may see the change and decide
		//to accept or reject.
		ElementML markedUpML = 
			new SdtBlockML(
				(org.docx4j.wml.SdtBlock) 
				XmlUtils.deepCopy(this.markedUpSdt));
		sdtBlockML.addSibling(markedUpML, true);
		sdtBlockML.delete();

		updateRefreshOffsets(mediator, elem.getStartOffset(), elem.getEndOffset());

        //What goes in stateChunks is the *non-marked up* sdt
		//that is deleted from WordMLDocument
        StateChunk sc = 
        	new StateChunk((org.docx4j.wml.SdtBlock) sdtBlockML.getDocxObject());
        stateChunks.put(sc.getIdAsString(), sc);

        // But also record the marked up version
        sc.setMarkedUpSdt(XmlUtils.marshaltoString(this.markedUpSdt, true));

        // Don't do this, since we are leaving it in the doc,
        // albeit marked up.
		//mediator.getDivergences().delete(idStr);
				
		return sequenceNumber;
	}

	public org.docx4j.wml.SdtBlock getMarkedUpSdt() {
		return this.markedUpSdt;
	}
	
} // TransformDelete class

