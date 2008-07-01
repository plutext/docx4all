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

        public TransformDelete(String idref)
        {
        	super();
            this.id = idref;
        }

        /* delete the SDT given its ID. */
        public override Int32 apply(Mediator mediator, Pkg pkg)
        {

            // Find the sdt in the XmlDocument
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.PkgXmlDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            XmlNode target = pkg.PkgXmlDocument.SelectSingleNode("//w:sdt[w:sdtPr/w:id/@w:val='" + id + "']", nsmgr);

            if (target == null)
            {
                log.Debug("Couldn't find sdt " + id);
                // TODO - throw error
                return -1;
            }
            else
            {
                XmlNode parent = target.ParentNode;
                parent.RemoveChild(target);

                pkg.StateChunks.Remove(id);
                mediator.Divergences.delete(id);

                log.Debug("Removed sdt " + id + " from pkg");
                return sequenceNumber;
            }
        }

        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "delete");
            t.SetAttribute("idref", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, id);

            return tf;
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
				
				int origPos = editor.getCaretPosition();
				boolean forward = true;
								
				try {
					editor.beginContentControlEdit();
					
					DocumentElement elem = getDocumentElement(editor, id);
					
					log.debug("apply(WordMLTextPane): SdtBlock Element=" + elem);
					log.debug("apply(WordMLTextPane): Current caret position=" + origPos);
					
					if (elem != null) {
						if (elem.getStartOffset() <= origPos
								&& origPos < elem.getEndOffset()) {
							// elem is still hosting caret.
							// User may have not finished editing yet.
							// Do not apply this TransformUpdate now
							// but wait until user has finished.
							log.debug("apply(WordMLTextPane): SKIP...StdBlock element is hosting caret.");
						} else {
							log.debug("apply(WordMLTextPane): Deleting SdtBlock Element...");
							
							if (elem.getEndOffset() <= origPos) {
								origPos = editor.getDocument().getLength() - origPos;
								forward = false;
							}
							
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
				} finally {
					if (!forward) {
						origPos = editor.getDocument().getLength() - origPos;
					}
					log.debug("apply(WordMLTextPane): Set caret position to " + origPos);
					editor.endContentControlEdit();
				}
			}
		});
	}
	
} //TransformDelete class
