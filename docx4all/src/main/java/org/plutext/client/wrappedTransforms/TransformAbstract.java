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

import java.math.BigInteger;

import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.Tag;
import org.plutext.client.ServerFrom;
import org.plutext.transforms.Transforms.T;


    public abstract class TransformAbstract
    {

    	private static Logger log = Logger.getLogger(TransformAbstract.class);

	protected SdtBlock sdt = null;	
	public SdtBlock getSdt() {
		return sdt;
	}

        // For debug purposes only.
        protected XmlNode tNode = null;
        public XmlNode TNode
        {
            get { return tNode; }
        }

        public TransformAbstract()
        { }


public T t = null;

    public TransformAbstract(T t)
    {
    	this.t = t;
    	
        sequenceNumber =  t.getSnum();

        sdt = t.getSdt();

        if (t.getIdref() != null)
        {
            // Case: Delete 
            
            // Convert the idref to an id object
            org.docx4j.wml.ObjectFactory factory = new org.docx4j.wml.ObjectFactory();
            id = factory.createId();
            id.setVal(BigInteger.valueOf(t.getIdref()));
            
            
            
        } else if (t.getOp().equals("style")) { 
        
            // No ID
        
        } else {
        	
            // Case: Update, Insert

            id = sdt.getSdtPr().getId();

            tag = sdt.getSdtPr().getTag();

        }

        //log.warn("Parsed SDT ID " + id);
        

    }

	protected DocumentElement getDocumentElement(WordMLTextPane editor, BigInteger sdtBlockId) {
		DocumentElement elem = null;
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		
		try {
			doc.readLock();
			
			DocumentElement root = (DocumentElement) doc.getDefaultRootElement();

			for (int i = 0; i < root.getElementCount() - 1 && elem == null; i++) {
				elem = (DocumentElement) root.getElement(i);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					SdtBlockML sdtBlockML = (SdtBlockML) ml;
					if (sdtBlockId.equals(sdtBlockML.getSdtProperties()
							.getIdValue())) {
						;// got it
					} else {
						elem = null;
					}
				} else {
					elem = null;
				}
			}
		} finally {
			doc.readUnlock();
		}
		
		return elem;
	}
	
    /* Code to apply the transform */
    	// TODO - think through method signature
    public abstract long apply(ServerFrom serverFrom);
    
	/* do the actual replacement in docx4all specific way */
	protected void apply(WordMLTextPane editor) {
		;//do nothing
	}


    protected Id id;
	public Id getId() {
		return id;
	}
	public void setId(Id id) {
		this.id = id;
	}
	
    // The tag of the wrapped SDT
    protected Tag tag;
	public Tag getTag() {
		return tag;
	}
	public void setTag(Tag tag) {
		this.tag = tag;
	}


    // Has this transform been applied to the document yet?
    Boolean applied = false;
	public Boolean getApplied() {
		return applied;
	}
	public void setApplied(Boolean applied) {
		this.applied = applied;
	}

        // Is this transform something which came from this
        // plutext client?  (If it is, we can always apply it without worrying
        // about conflicts)
        Boolean local = false;
        public Boolean Local
        {
            get { return local; }
            set { local = value; }
        }


    // The ID of the transformation.
    protected long sequenceNumber = 0;
	public long getSequenceNumber() {
		return sequenceNumber;
	}


	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}


        /* Code to apply the transform */
        public abstract Int32 apply(Mediator mediator, Pkg pkg);

        public abstract XmlDocument marshal();

        protected XmlDocument createDocument()
        {
            XmlDocument tf = new XmlDocument();
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(tf.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);
            nsmgr.AddNamespace(Namespaces.PLUTEXT_TRANSFORMS_NS_PREFIX, 
                Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);

            return tf;
        }
    }
