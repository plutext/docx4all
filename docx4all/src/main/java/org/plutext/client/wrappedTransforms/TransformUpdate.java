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

import java.math.BigInteger;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.diff.ParagraphDifferencer;
import org.docx4j.wml.P;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.Mediator;
import org.plutext.client.Pkg;
import org.plutext.client.Util;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;


public class TransformUpdate extends TransformAbstract {

	private static Logger log = Logger.getLogger(TransformUpdate.class);

	public TransformUpdate(T t) {
		super(t);
	}


    /* Compare the updated sdt to the original, replacing the
     * updated one with containing w:ins and w:del */
    public void markupChanges(SdtBlock local) //throws javax.xml.bind.JAXBException
    {
        // In this current proof of concept, we pass 
        // ParagraphDifferencer strings representing 
        // each sdt.
        // It will compare the paragraphs.
        // Currently ASSUMES there is one paragraph
        // in each SDT.
    	
		P pl = getParaFromSdt(sdt);
		if (pl==null) {
			log.error("Couldn't find p in sdt!");
		}
		P pr = getParaFromSdt(local);
		if (pr==null) {
			log.error("Couldn't find p in sdt!");
		}
    	
		P markedUpP;
		try {
			javax.xml.bind.util.JAXBResult result = new javax.xml.bind.util.JAXBResult(
					org.docx4j.jaxb.Context.jc);
			
			ParagraphDifferencer.diff(pl, pr, result);
			
			markedUpP = (P)result.getResult();
		} catch (JAXBException e) {
			log.error(e);
			// Oh well, we'll display it without changes marked up
			return;
		}
		
		// Now replace the paragraph in the sdt
		int pos = sdt.getSdtContent().getEGContentBlockContent().indexOf(pr);
		sdt.getSdtContent().getEGContentBlockContent().add(pos, markedUpP);
		sdt.getSdtContent().getEGContentBlockContent().remove(pos+1);
		
    }
    
    private P getParaFromSdt(SdtBlock sdt) {
    			
		for ( Object o : sdt.getSdtContent().getEGContentBlockContent() ) {
			
			if (o instanceof org.docx4j.wml.P) {
				return (P)o;
			}
		}
		return null;    	
    }
    

    public long apply(Mediator mediator, Pkg pkg)
    {


		log.debug("apply(ServerFrom): sdtBolck = " + getSdt() 
			+ " - ID=" + getSdt().getSdtPr().getId().getVal()
			+ " - TAG=" + getSdt().getSdtPr().getTag().getVal());

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

		if (!getApplied() || isLocal()) {
			apply(mediator.getWordMLTextPane());

	        //pkg.getStateChunks().remove(id);
	        //pkg.getStateChunks().put(id.getVal().toString(), new StateChunk(sdt));	
		}
		
		// Fourth, if we haven't thrown an exception, return the sequence number
		return sequenceNumber;
	}

	protected void apply(WordMLTextPane editor) {
		BigInteger id = getSdt().getSdtPr().getId().getVal();
		
		log.debug("apply(WordMLTextPane): Updating SdtBlock Id=" 
				+ id + " in Editor.");
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		int origPos = editor.getCaretPosition();
		boolean forward = true;
		
		try {
			//editor.beginContentControlEdit();
			
			DocumentElement elem = Util.getDocumentElement(doc, id.toString());
			
			log.debug("apply(WordMLTextPane): SdtBlock Element=" + elem);
			log.debug("apply(WordMLTextPane): Current caret position=" + origPos);
			log.debug("apply(WordMLTextPane): Current getApplied()=" 
				+ getApplied()
				+ " isLocal()=" + isLocal());
			
			if (elem != null) {
				if (elem.getEndOffset() <= origPos) {
					origPos = doc.getLength() - origPos;
					forward = false;
				}
				
				if (getApplied() && isLocal()) {
					SdtBlockML ml = (SdtBlockML) elem.getElementML();
					ml.getSdtProperties().setTagValue(getTag().getVal());
					doc.refreshParagraphs(elem.getStartOffset(), 0);
					
				} else if (!getApplied()){
					ElementMLRecord[] recs = { new ElementMLRecord(
							new SdtBlockML(sdt), false) };
					WordMLFragment frag = new WordMLFragment(recs);

					editor.setCaretPosition(elem.getStartOffset());
					editor.moveCaretPosition(elem.getEndOffset());
					editor.replaceSelection(frag);
					
				} else {
					log.warn("apply(WordMLTextPane): Cannot re-apply SdtBlock Id=" 
						+ id);
				}
			} else {
				//silently ignore
				log.warn("apply(WordMLTextPane): SdtBlock Id=" 
					+ id
					+ " NOT FOUND in editor.");
			}
		} finally {
			if (!forward) {
				origPos = editor.getDocument().getLength() - origPos;
			}
			log.debug("apply(WordMLTextPane): Set caret position to " + origPos);
			editor.setCaretPosition(origPos);
			//editor.endContentControlEdit();
		}
	}
} //TransformUpdate class

























