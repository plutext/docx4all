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

import org.apache.log4j.Logger;
import java.util.HashMap;

import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.docx4j.wml.SdtBlock;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.plutext.client.Util;
import org.plutext.client.CustomProperties;
import org.docx4j.wml.Id;

/* Represent the configuration of the
     * document, and certain transient
     * document-level state information.
     */ 
    public class StateDocx
    {

    	private static Logger log = Logger.getLogger(StateDocx.class);
    	
        public StateDocx(WordprocessingMLPackage wordMLPackage)
        {
            log.debug("StateDocx constructor fired");

            uptodate = true;
            
            try
            {

// TODO: implement!            	
//                foreach (SdtBlock sdt in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
//                {
//                    // Record the known state of each content control
//                    controlMap.Add(sdt);
//                }

                contentControlSnapshots = new HashMap<Id, ContentControlSnapshot>();
                
                // TODO - contentControlSnapshots.put a ContentControlSnapshot of each sdt

            	
            } 
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            tSequenceNumberAtLoadTime  
            	= Integer.parseInt(Util.getCustomDocumentProperty(
            			wordMLPackage.getDocPropsCustomPart(), CustomProperties.DOCUMENT_TRANSFORM_SEQUENCENUMBER));

            tSequenceNumberApplied = tSequenceNumberAtLoadTime;

            tSequenceNumberHighestFetched = tSequenceNumberAtLoadTime;
            
            org.docx4j.wml.Styles docxStyles = (org.docx4j.wml.Styles)wordMLPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement();                        
            stylesSnapshot = new StylesSnapshot(docxStyles );

        }

        private int tSequenceNumberAtLoadTime;

        private int tSequenceNumberApplied;
		public int getTSequenceNumberApplied() {
			return tSequenceNumberApplied;
		}
		public void setTSequenceNumberApplied(int sequenceNumberApplied) {
			tSequenceNumberApplied = sequenceNumberApplied;
		}

        private int tSequenceNumberHighestFetched; 
		public int getTSequenceNumberHighestFetched() {
			return tSequenceNumberHighestFetched;
		}
		public void setTSequenceNumberHighestFetched(int sequenceNumberHighestFetched) {
			tSequenceNumberHighestFetched = sequenceNumberHighestFetched;
		}

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


        private SdtBlock currentCC = null;
		public SdtBlock getCurrentCC() {
			return currentCC;
		}
		public void setCurrentCC(SdtBlock currentCC) {
			this.currentCC = currentCC;
			
			if (currentCC ==null) {
				return;
			}
			
			// Looks like this method is only called when
			// entering a content control?
			
				// No, as at May 9, we're also calling it
				// when exiting (at beginning and end of exit)
			
			// Therefore content control already exists in
			// document, so you can assume it is already 
			// listed in contentControlSnapshots
			
			// But could check, just to make sure?
			
			ContentControlSnapshot ccs = (ContentControlSnapshot)contentControlSnapshots.get(
												currentCC.getSdtPr().getId());
			if (ccs==null) {
				ccs = new ContentControlSnapshot(currentCC);								
			} else {
				ccs.refresh();
			}
			contentControlSnapshots.put(currentCC.getSdtPr().getId(), ccs);
		}


        private Boolean initialized = false;


        StylesSnapshot stylesSnapshot = null;
		public StylesSnapshot getStylesSnapshot() {
			return stylesSnapshot;
		}

        /* Maintain an ordered list of controls 
         * (unless docx4all already has this somewhere else?) */
        Controls controlMap = new Controls();
        public Controls getControlMap() {
        	return controlMap; 
        }

        /* Maintain a hashmap (dictionary) of ContentControlSnapshot wrapped Content Controls 
         * keyed by ID, so we can detect when one has been updated (even if not
         * entered)  */
        HashMap<Id, ContentControlSnapshot> contentControlSnapshots = null;
         
//		public void setControlMap(Controls controlMap) {
//			this.controlMap = controlMap;
//		}
        public HashMap<Id, ContentControlSnapshot> getContentControlSnapshots() {
        	return contentControlSnapshots; 
        }

        /* Maintain a collection of Transforms keyed by tSequenceNumber, so we 
         * can keep track of which ones have been applied.  */
        	// TODO - rename to wrappedTransforms
        HashMap<Integer, TransformAbstract> wrappedTransforms 
        	= new HashMap<Integer, TransformAbstract>();
		public HashMap<Integer, TransformAbstract> getWrappedTransforms() {
			return wrappedTransforms;
		}


        //Boolean uptodate = true;
        public boolean uptodate;
		public boolean getUptodate() {
			return (tSequenceNumberApplied == tSequenceNumberHighestFetched);
		}
		public void setUptodate(boolean uptodate) {
			this.uptodate = uptodate;
		}

    }
