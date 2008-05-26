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

import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.ServerFrom;
import org.plutext.client.state.ContentControlSnapshot;
import org.plutext.transforms.Transforms.T;


public class TransformDelete extends TransformAbstract {
	
	private static Logger log = Logger.getLogger(TransformDelete.class);	

    public TransformDelete(T t)
    {
    	super(t);
    }

    /* delete the SDT given its ID. */
    public int apply(ServerFrom serverFrom)
    {
        // Remove the ContentControlSnapshot representing the content control
    	
       	log.debug("apply(): Deleting SDT Id=" + getId());
       	
    	Map<Id, ContentControlSnapshot> snapshots = 
    		serverFrom.getStateDocx().getContentControlSnapshots();
    	if (snapshots.remove(getId()) == null) {
    		log.debug("apply(): Could not find SDT Id=" + getId() + " snapshot.");
	        // couldn't find!
	        // TODO - throw error
    		return -1;
    	}
    	
		apply(serverFrom.getWordMLTextPane(), getSdt());
		
		return sequenceNumber;
    }

	protected void apply(final WordMLTextPane editor, final SdtBlock sdt) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				log.debug("apply(WordMLTextPane, SdtBlock): Deleting SDT Id=" 
						+ sdt.getSdtPr().getId() + " from Editor.");
				
				int origPos = editor.getCaretPosition();
				
				try {
					editor.beginContentControlEdit();
				
					DocumentElement elem = getDocumentElement(editor, sdt);
					if (elem != null) {
						editor.setCaretPosition(elem.getStartOffset());
						editor.moveCaretPosition(elem.getEndOffset());
						editor.replaceSelection((WordMLFragment) null);						
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
	
} //TransformDelete class
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

