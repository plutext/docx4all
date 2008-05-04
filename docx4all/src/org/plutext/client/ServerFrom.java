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

package org.plutext.client;

import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.plutext.client.state.StateDocx;
import org.plutext.client.state.ContentControlSnapshot;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;
import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.plutext.client.wrappedTransforms.TransformHelper;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.Util;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.rpc.ServiceException;

import org.plutext.transforms.Context;
import org.plutext.transforms.Transforms.T;


	/** This class is responsible for polling the server for updates
	 *  (which are received as Transform objects), 
	 *  
     */ 
    public class ServerFrom
    {
    	private static Logger log = Logger.getLogger(ServerFrom.class);
    	
        private StateDocx stateDocx;

        public ServerFrom(StateDocx stateDocx) //WordprocessingMLPackage wordMLPackage, String docID)
        {
            log.debug("Updates constructor fired"); 

            this.stateDocx = stateDocx;

        }

        public StateDocx getStateDocx() {
        	return stateDocx;
        }


        /* Find out whether there are any new transforms on the server, and
         * if there are, fetch them. 
         *
         * This is done periodically in the background in a separate thread.
         * 
         * Note that a different method actually applies the updates.  That
         * might happen at quite a different time.
         */
        void fetchUpdates() {
            //diagnostics("Local tSequence for " + myDoc.Name + " : " + tSequenceNumber);
            log.debug("Uptodate?");

			try {
				// Start a new session
				AuthenticationUtils.startSession(org.plutext.client.ServerTo.USERNAME, 
						org.plutext.client.ServerTo.PASSWORD);
				PlutextService_ServiceLocator locator = 
					new PlutextService_ServiceLocator( AuthenticationUtils.getEngineConfiguration() );
				PlutextWebService ws  = locator.getPlutextService();

				String[] updates = null;
				
			    updates = ws.getTransforms(stateDocx.getDocID(),
			                            stateDocx.getTSequenceNumberHighestFetched());

				log.debug(" sequence = " + updates[0]);
				log.debug(" transforms = " + updates[1]);

				int tsnJustFetched = Integer.parseInt(updates[0]); 
				if ( tsnJustFetched > stateDocx.getTSequenceNumberHighestFetched() )
				{
				    stateDocx.setTSequenceNumberHighestFetched(tsnJustFetched);
				    registerTransforms(updates[1], false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// End the current session
				AuthenticationUtils.endSession();				
			}
        }

        /* Put transforms received from server into the transforms collection.
         * If these transforms are the result of updates by this client, we
         * don't need to apply them, so set applied to true. */
        HashMap<Integer, TransformAbstract> registerTransforms(String transforms, Boolean setApplied)
        {

            // A list keyed by wrapped sdtPr/id, of transforms we may forcibly apply
            HashMap<Integer, TransformAbstract> additions = new HashMap<Integer, TransformAbstract>();

            // Parse the XML document, and put each transform into the transforms collection
            org.plutext.transforms.Transforms transformsObj = null;
    		try {
    			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
    			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());					
    			transformsObj = (org.plutext.transforms.Transforms)u.unmarshal( new java.io.StringReader(transforms) );
    		} catch (JAXBException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

    		for(T t : transformsObj.getT() )
            {
                TransformAbstract ta = TransformHelper.construct(t);
                if (setApplied) { 
                	ta.setApplied(true); 
                }

                log.debug("Instance " + " -- Registering " + ta.getSequenceNumber());
                try
                {
                    stateDocx.getWrappedTransforms().put( new Integer(ta.getSequenceNumber()), ta);
                    //stateDocx.TSequenceNumberHighestFetched = t.SequenceNumber;
                    log.debug(".. done.");
                }
                catch (Exception e)
                {
                    log.debug(".. not done: " + e.getMessage() );
                    // Ignore - An item with the same key has already been added.
                    // log.debug(e.Message);
                    // log.debug(t.SequenceNumber + " ... " + t.GetType().Name);
                }

                // Still want it here
                additions.put( new Integer(ta.getSequenceNumber()), ta);
                // Key is SequenceNumber, not t.ID, since TransformStyle type doesn't have an 
                // underlying SDT.  Besides, if 2 additions related to the same SDT, the
                // keys would collide.
                log.debug(ta.getSequenceNumber() + " registered in forceApplicationToSdtIds");
            }
            return additions;
        }

        /* Apply registered transforms in the known tSequence number range. */
        void applyUpdates(HashMap<Integer, TransformAbstract> forceApplicationToSdtIds)
        {
            log.debug("applyUpdates invoked.");

            if (stateDocx.Uptodate)
            {
                log.debug(".. but Uptodate - nothing to do.");
            }
            else
            {
                // loop through, fetch, apply 
                log.debug(".. applying " + stateDocx.getTSequenceNumberApplied() 
                		+ " - " + stateDocx.getTSequenceNumberHighestFetched());
                
                for (int x = (stateDocx.getTSequenceNumberApplied() + 1); 
                	 x <= stateDocx.getTSequenceNumberHighestFetched(); x++)
                {
                    // OPTIMISATION: could do the most recent only for each cc
                    // (ie reverse order), except for MOVES and INSERTS, which need to 
                    // be done in order.

                    try
                    {

                        int result = applyUpdate(stateDocx.getWrappedTransforms().get(new Integer(x)), 
                        							forceApplicationToSdtIds);
                        if (result > 0) {
                            stateDocx.setTSequenceNumberApplied(result);
                        } else {
                            log.debug("Failed to apply transformation " + x);
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("Transform[" + x + "] not found. Simultaneous commits?");
                        
                        e.printStackTrace();

                        // This shouldn't happen!  If it does, understand why...

                        // Doesn't matter if there is a more recent transform fetched which
                        // has or will be applied to the same control.

                        // If not, this is a problem.

                        // The most efficient fix is to:

                        // 1.  Send the server a list of missing tSequenceNumbers
                        // 2.  Server send back the corresponding SDT ID and version 
                        //     (what to do with deletes?)
                        // 3.  Any that we need to apply, we ask for.
                        //     (the others, put a blank entry in the dictionary
                        //      so we don't go through this again for them)
                    }

                }
            }
        }

        /* Apply registered transforms in the known tSequence number range. */
        void applyUpdates(HashMap<Integer, TransformAbstract> transforms,
                          HashMap<Integer, TransformAbstract> forceApplicationToSdtIds)
        {
            log.debug("applyUpdates(to collection) invoked.");
            // In this variant, DO NOT update stateDocx.TSequenceNumberApplied 

            // loop through, fetch, apply 
    		Iterator transformsIterator = transforms.entrySet().iterator();
    	    while (transformsIterator.hasNext()) {
    	        Map.Entry pairs = (Map.Entry)transformsIterator.next();
    	        
                TransformAbstract tr = (TransformAbstract)pairs.getValue();
                int result = applyUpdate(tr, forceApplicationToSdtIds);
                if (result > 0) {
                    log.debug("transformation applied" + tr.getSequenceNumber() );
                } else {
                    log.debug("Failed to apply transformation " + tr.getSequenceNumber() );
                }
            }
        }

        /* On success, returns the transformation's tSequenceNumber; otherwise, 0 */
        private int applyUpdate(TransformAbstract t, HashMap<Integer, TransformAbstract> forceApplicationToSdtIds)
        {
            int result;

            if (t.getApplied() ) {
                log.debug(t.getSequenceNumber() + " has been applied previously.");
                return 0;
            }
            // TODO
            // else if there is a more recent transform
            // which applies to this chunk
            // ignore this transform

            log.debug("Class: " + t.getClass().getName() );

            // Generally, we don't want to overwrite local changes
            // But when local changes are committed using the web
            // service, we do want to apply the returned transformations
            // locally. Hence this forcing mechanism.

            Boolean forceForThisID = false;
            if (forceApplicationToSdtIds != null)
            {
                try
                {
                    //TransformAbstract throwaway = forceApplicationToSdtIds[t.ID];
                    TransformAbstract throwaway 
                    	= forceApplicationToSdtIds.get(new Integer(t.getSequenceNumber() ));
                    // if that succeeds, this ID is in the force list
                    // so
                    forceForThisID = true;
                } catch (Exception e) {
                    log.debug(t.getId().getVal() + " NOT FOUND in force list.");
                }
            } else {
                log.debug("forceList was empty.");
            }

            if (forceForThisID
                || t instanceof org.plutext.client.wrappedTransforms.TransformInsert)
            {
                // its ok to overwrite
                result = t.apply(this);
                t.setApplied(true);
                log.debug(t.getSequenceNumber() + " applied (" + t.getClass().getName() + ")");
                return result;
            } else if (t instanceof org.plutext.client.wrappedTransforms.TransformStyle) {
                // TODO - Implement TransformStyle
                // that class is currently non functional.
                result = t.apply(this);
                t.setApplied(true);
                log.debug(t.getSequenceNumber() + "  UNDER CONSTRUCTION (" + t.getClass().getName() + ")");
                return result;

            }
            else
            {
                ContentControlSnapshot currentChunk = stateDocx.getContentControlSnapshots().get(t.getId());
//                String currentXML = ContentControlSnapshot.getContentControlXMLNormalised(currentChunk.WrappedCC);
                String currentXML = currentChunk.getContentControlXML();

                // We don't want to blindly apply updates to anything which has
                // changed eg pPr and effects of search/replace.
                if (currentXML.equals(currentChunk.getPointInTimeXml())) {
                    // its ok to overwrite
                    result = t.apply(this);
                    t.setApplied(true);
                    log.debug(t.getSequenceNumber() + " applied (" + t.getClass().getName() + ")");
                    return result;
                } else {
                    log.debug( " -- Updates to merge. " + t.getId().getVal() );
                    SdtBlock cc = currentChunk.getSdtBlock();
                    result = Merge.mergeUpdate(cc, t);
                    log.debug(result + " applied (merged)");
                    return result;
                }

            }
        }
  
    }
