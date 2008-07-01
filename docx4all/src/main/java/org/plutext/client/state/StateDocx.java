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

package org.plutext.client.state;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.DocumentML;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;

import org.plutext.client.CustomProperties;
import org.plutext.client.Pkg;
import org.plutext.client.Util;
import org.plutext.client.wrappedTransforms.TransformAbstract;

/* Represent the configuration of the
 * document, and certain transient
 * document-level state information.
 */
public class StateDocx {

	private static Logger log = Logger.getLogger(StateDocx.class);

	public StateDocx(WordMLTextPane textPane) {
		WordMLDocument doc = (WordMLDocument) textPane.getDocument();
		if (!DocUtil.isSharedDocument(doc)) {
			throw new IllegalArgumentException("Invalid WordMLDocument");
		}
		
		String fileUri = (String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
    	int idx = fileUri.indexOf("/alfresco/");
    	if (idx <= 0) {
    		//temporary checking
    		//TODO: Has to check whether fileUri's protocol is webdav
    		//and its context is correct.
    		throw new IllegalArgumentException("Invalid WordMLDocument.FILE_PATH_PROPERTY value");
    	}
    	
		this.docID = fileUri.substring(idx);
    	init(doc);		
	}

	WordprocessingMLPackage wordMLPackage = null;
	
	private void init(WordMLDocument doc) {
  		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
    	
  		wordMLPackage = 
    		((DocumentML) root.getElementML()).getWordprocessingMLPackage();

    			
		if (Util.getCustomDocumentProperty(
				wordMLPackage.getDocPropsCustomPart(),
				CustomProperties.PROMPT_FOR_CHECKIN_MESSAGE).equals("true")) {
			this.promptForCheckinMessage = Boolean.TRUE;
			
		} else if (Util.getCustomDocumentProperty(
				wordMLPackage.getDocPropsCustomPart(),
				CustomProperties.PROMPT_FOR_CHECKIN_MESSAGE).equals("false")) {
			this.promptForCheckinMessage = Boolean.FALSE;
			
		} else {
			log.warn(CustomProperties.PROMPT_FOR_CHECKIN_MESSAGE + "unknown");
			this.promptForCheckinMessage = Boolean.FALSE;
		}
		
		// Expected values: EachBlock, Heading1
		this.chunkingStrategy = Util.getCustomDocumentProperty(
				wordMLPackage.getDocPropsCustomPart(),
				CustomProperties.CHUNKING_STRATEGY);

	}
	
	

        private String skeletonSnapshot;


        // Set from App_DocumentOpen
        private String docID = null;
    	public String getDocID() {
    		return docID;
    	}
    	public void setDocID(String docID) {
    		this.docID = docID;
    	}



        // These three variables are read from document properties
        private Boolean promptForCheckinMessage;
    	public Boolean getPromptForCheckinMessage() {
    		return promptForCheckinMessage;
    	}
    	public void setPromptForCheckinMessage(Boolean promptForCheckinMessage) {
    		this.promptForCheckinMessage = promptForCheckinMessage;
    	}

        private String chunkingStrategy;
    	public String getChunkingStrategy() {
    		return chunkingStrategy;
    	}
    	public void setChunkingStrategy(String chunkingStrategy) {
    		this.chunkingStrategy = chunkingStrategy;
    	}




        private Boolean initialized = false;

    	public Boolean getInitialized() {
    		return initialized;
    	}

    	public void setInitialized(Boolean initialized) {
    		this.initialized = initialized;
    	}

        Pkg pkg = null;
    	public Pkg getPkg() {
    		return pkg;
    	}
    	public void setPkg(Pkg pkg) {
    		this.pkg = pkg;
    	}


//        Styles stylemap = new Styles();
//    	public Styles getStylemap() {
//    		return stylemap;
//    	}



        /* Maintain a collection of Transforms keyed by tSequenceNumber, so we 
         * can keep track of which ones have been applied.  */
        TransformsCollection transforms;
    	public TransformsCollection getTransforms() {
    		return transforms;
    	}




        public class TransformsCollection
        {
        	
        	private int tSequenceNumberAtLoadTime = Integer.parseInt(Util
    				.getCustomDocumentProperty(wordMLPackage
    						.getDocPropsCustomPart(),
    						CustomProperties.DOCUMENT_TRANSFORM_SEQUENCENUMBER));

        	private int tSequenceNumberHighestFetched = this.tSequenceNumberAtLoadTime;
        	
        	
			public int getTSequenceNumberHighestFetched() {
				return tSequenceNumberHighestFetched;
			}
			public void setTSequenceNumberHighestFetched(int sequenceNumberHighestFetched) {
				tSequenceNumberHighestFetched = sequenceNumberHighestFetched;
			}


            /* Maintain a collection of Transforms keyed by tSequenceNumber, so we 
             * can keep track of which ones have been applied. 
             * 
               Key is SequenceNumber, not t.ID, since TransformStyle type doesn't have an 
               underlying SDT.  Besides, if 2 additions related to the same SDT, the
               keys would collide.
             */
            ArrayList<TransformAbstract> transformsBySeqNum = new ArrayList<TransformAbstract>();
			public ArrayList<TransformAbstract> getTransformsBySeqNum() {
				return transformsBySeqNum;
			}

            public void add(TransformAbstract t, Boolean updateHighestFetched)
            {
                // Check it is not already present before adding
                // This list should be pretty short, so there is
                // little harm in just iterating through it
                for (TransformAbstract ta : transformsBySeqNum)
                {
                    if (ta.getSequenceNumber() == t.getSequenceNumber())
                    {
                        log.debug(t.getSequenceNumber() + " already registered.  Ignoring.");
                        return;
                    }
                }

                transformsBySeqNum.add(t);

                if (updateHighestFetched && t.getSequenceNumber() > tSequenceNumberHighestFetched)
                {
                    // Only update highest fetched if this addition is 
                    // a result of fetch updates. (If it is a result
                    // of local transmits, there is a possibility that
                    // someone else may have generated an snum (ie while
                    // our transmitLocalChanges was running), and we 
                    // wouldn't want to miss that

                    tSequenceNumberHighestFetched = Integer.parseInt(Long.toString(t.getSequenceNumber()));
                    
                    
                }
            }

            public ArrayList<TransformAbstract> getTransformsBySdtId(String id, Boolean includeLocals)
            {
                ArrayList<TransformAbstract> list = new ArrayList<TransformAbstract>();

                for (TransformAbstract ta : transformsBySeqNum)
                {
                    if (ta.getId().getVal().toString().equals(id))
                        {
                            if (!ta.Local || includeLocals)
                            {
                                list.add(ta);
                                log.debug("Instance  --  found a transform applicable to Sdt " + id);
                            }
                        }

                }

                return list;
            }


        }


    }
