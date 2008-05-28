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
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4j.wml.Id;
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
    public long apply(ServerFrom serverFrom)
    {
        // Remove the ContentControlSnapshot representing the content control
    	
		log.debug("apply(ServerFrom): Deleting SdtBlock = " + getSdt() 
				+ " - ID=" + getId().getVal());

    	Map<Id, ContentControlSnapshot> snapshots = 
    		serverFrom.getStateDocx().getContentControlSnapshots();
    	if (snapshots.remove(getId()) == null) {
    		log.debug("apply(): Could not find SDT Id=" + getId().getVal() + " snapshot.");
	        // couldn't find!
	        // TODO - throw error
    		return -1;
    	}
    	
		apply(serverFrom.getWordMLTextPane());
		
		return sequenceNumber;
    }

	protected void apply(final WordMLTextPane editor) {
		final BigInteger id = getId().getVal();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				log.debug("apply(WordMLTextPane): Deleting SdtBlock ID=" 
						+ id + " from Editor.");
				
				Position origPos = null;
				
				try {
					editor.beginContentControlEdit();
					
					origPos = editor.getDocument().createPosition(editor.getCaretPosition());
				
					DocumentElement elem = getDocumentElement(editor, id);
					
					log.debug("apply(WordMLTextPane): SdtBlock Element=" + elem);
					log.debug("apply(WordMLTextPane): Current caret position="
						+ origPos.getOffset());
					
					if (elem != null) {
						if (elem.getStartOffset() <= origPos.getOffset()
								&& origPos.getOffset() < elem.getEndOffset()) {
							// elem is still hosting caret.
							// User may have not finished editing yet.
							// Do not apply this TransformUpdate now
							// but wait until user has finished.
							log.debug("apply(WordMLTextPane): SKIP...StdBlock element is hosting caret.");
						} else {
							log.debug("apply(WordMLTextPane): Deleting SdtBlock Element...");
							editor.setCaretPosition(elem.getStartOffset());
							editor.moveCaretPosition(elem.getEndOffset());
							editor.replaceSelection((WordMLFragment) null);
						}
					} else {
						//silently ignore
						log.warn("apply(WordMLTextPane): SdtBlock Id=" 
							+ id
							+ " NOT FOUND in editor.");
					}
				} catch (BadLocationException exc) {
					;//should not happen
				} finally {
					editor.endContentControlEdit();
					if (origPos != null) {
						editor.setCaretPosition(origPos.getOffset());
					}
				}
			}
		});
	}
	
} //TransformDelete class
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

