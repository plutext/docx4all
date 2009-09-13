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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.DocumentML;
import org.docx4j.model.datastorage.CustomXmlDataStorageImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;
import org.docx4j.openpackaging.parts.PartName;
import org.plutext.client.CustomProperties;
import org.plutext.client.Util;
import org.plutext.client.partWrapper.Part;
import org.plutext.client.partWrapper.SequencedPart;
import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.plutext.client.wrappedTransforms.TransformComparator;
import org.w3c.dom.Document;

/* Represent the configuration of the
 * document, and certain transient
 * document-level state information.
 */
public class StateDocx {

	private static Logger log = Logger.getLogger(StateDocx.class);

	public StateDocx(WordMLDocument doc) {
    	init(doc);		
	}

	WordprocessingMLPackage wordMLPackage = null;
	
	private void init(WordMLDocument doc) {
		String fileUri = (String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
    	int idx = fileUri.indexOf("/alfresco/");
		this.docID = fileUri.substring(idx);
		
 		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
    	
  		this.wordMLPackage = 
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
		
        // Find our version list CustomXml part
        HashMap docx4jParts = wordMLPackage.getParts().getParts();
		Iterator partsIterator = docx4jParts.entrySet().iterator();
		PartName toBeRemovedPartName = null; 
	    while (partsIterator.hasNext()) {
	        Map.Entry pairs = (Map.Entry)partsIterator.next();
	        
	        if(pairs.getKey()==null) {
	        	log.warn("Skipped null key");
	        	pairs = (Map.Entry)partsIterator.next();
	        }
	        
	        PartName partName = (PartName)pairs.getKey();
	        log.debug(partName.getName() );

            if (pairs.getValue() instanceof CustomXmlDataStoragePart)             	
            {
            	CustomXmlDataStoragePart docx4jPart
	        		= (CustomXmlDataStoragePart)pairs.getValue();
            	
            	// dom4j document :-(
            	//Document customXmlDoc = ((Dom4jCustomXmlDataStorage)docx4jPart.getData()).getDom4jDocument();            	
            	Document customXmlDoc = null;
				try {
					customXmlDoc = ((CustomXmlDataStorageImpl)docx4jPart.getData()).getDocument();
            		// ElementMLFactory could set in LoadFromVFSZipFile  
				} catch (Docx4JException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
            	
            	log.debug(customXmlDoc.getDocumentElement().getLocalName() );
            	
            	if (customXmlDoc.getDocumentElement().getLocalName().equals("parts")) {
            		// ? was = PartVersionList
            		
                    partVersionList = new PartVersionList(customXmlDoc);
                    log.debug("set partVersionList");
                    
                    // now delete this part from the package,
                    // including its entry in the doc's rels.
                    toBeRemovedPartName = partName;
            	}
            	
            }
        }
	    
	    if (toBeRemovedPartName!=null) {
	    	wordMLPackage.getMainDocumentPart().getRelationshipsPart().removePart(toBeRemovedPartName);
	    	// Ensure that if we create any more parts (eg a hyperlink), there isn't
	    	// a gap in the sequence.
	    	wordMLPackage.getMainDocumentPart().getRelationshipsPart().resetIdAllocator();
	    	log.debug("Removed tmp part " + toBeRemovedPartName.getName() );
	    }
		
        parts = Util.extractParts(doc);
        
        sectPr = extractDocumentSectPr();

        // For the parts we care about, record their version info.
//        partVersionList.setVersions(parts);
        partVersionList.setVersions();

		this.transforms = new TransformsCollection();
		
		this.stateChunks = Util.createStateChunks(doc);
	}
	
	private String sectPr;
	public String getSectPr() {
		return sectPr;
	}
	
    private PartVersionList partVersionList;
    public PartVersionList getPartVersionList() {
    	return partVersionList;
    }
    
    private HashMap<String, Part> parts;
    public HashMap<String, Part> getParts() {
    	return parts;
    }
	
	
	private HashMap<String, StateChunk> stateChunks;
	public HashMap<String, StateChunk> getStateChunks() {
		return stateChunks;
	}

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

//        Styles stylemap = new Styles();
//    	public Styles getStylemap() {
//    		return stylemap;
//    	}

    	/**
    	 * NB At present, docx4all has no notion of sectPr at the ML level,
    	 * so this operates solely at the docx4j level.
    	 * @param foreignSectPr
    	 */
    	public String extractDocumentSectPr() {
    		     		
    		return Util.extractDocumentSectPr(this.wordMLPackage);
    	}

    	

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
				Collections.sort(transformsBySeqNum, new TransformComparator());
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
                    if (ta.getPlutextId().equals(id))
                        {
                            if (!ta.isLocal() || includeLocals)
                            {
                                list.add(ta);
                                log.debug("Instance  --  found a transform applicable to Sdt " + id);
                            }
                        }

                }

                return list;
            }


        } //TransformsCollection inner class


    } //StateDocx class
































