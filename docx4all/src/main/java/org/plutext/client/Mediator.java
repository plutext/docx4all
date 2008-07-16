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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.rpc.ServiceException;

import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.wml.Tag;
import org.plutext.Context;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffResultSpan;
import org.plutext.client.state.StateChunk;
import org.plutext.client.state.StateDocx;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;
import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.plutext.client.wrappedTransforms.TransformHelper;
import org.plutext.client.wrappedTransforms.TransformUpdate;
import org.plutext.transforms.Transforms;
import org.plutext.transforms.Transforms.T;



/** This class is the real workhorse.  
 */ 
public class Mediator
{
    /* Design goals:
     * 
     * 1. Don't use content control entry and exit handlers:
     *    (i)  these are hard to get right
     *    (ii) if stuff happens at that point, user can't navigate smoothly around doc
     * 
     * 2. Pick up eg results of find/replace; any content user manages to
     *    enter outside a content control; moves.
     * 
     * 3. Efficient use of XML representation ie .WordOpenXML
     *    .. only get this once per Callback.
     * 
     * 4. Efficient use of InsertXML (just used to create new document).
     * 
     * 5. Granular control of differencing, which avoids
     *    Word's Compare which sometimes replaces the entire SDT
     *    with w:customXmlInsRangeStart&End, w:customXmlDelRangeStart&End
     * 
     * Principles (to be added to end user documentation):
     * 
     * (i) All remote changes must be fetched & applied before a user 
     *     is able to transmit his local changes
     * 
     * (ii)Any tracked changes in a cc must be resolved before the 
     *     use is allowed to commit that one
     * 
     * TODO:
     * 
     * - markup insertions!
     * - styles
     * - XSLT optimisations
     * 
     */

	private static Logger log = Logger.getLogger(Mediator.class);
    
	// TEMP values for testing purposes
	// Ultimately, username and password will be the same as used
	// for the Webdav connection.
	public static final String USERNAME = "admin";
	public static final String PASSWORD = "admin";	

    Boolean remoteUpdatesNeedApplying = false;
    // False once all are applied, even if there are conflicts.
    // Purpose: determine whether button is enabled or greyed out


//    private ContentControlEventHandler serverTo;
//    public ContentControlEventHandler getServerTo() {
//		return serverTo;
//	}

    private StateDocx stateDocx;

	public StateDocx getStateDocx() {
		return stateDocx;
	}
	
	WordMLTextPane textPane;
	public WordMLTextPane getWordMLTextPane() {
		return textPane;
	}

	public Mediator(WordMLTextPane textPane)
    {
		WordMLDocument doc = (WordMLDocument) textPane.getDocument();
		if (!DocUtil.isSharedDocument(doc)) {
			throw new IllegalArgumentException("Not a shared WordMLDocument");
		}
		
        this.textPane = textPane;
        this.stateDocx = new StateDocx(doc);
    }


	PlutextWebService ws = null;
    
/* *****************************************************
 *                   SESSION MANAGEMENT
 * ***************************************************** */
    public void startSession() throws ServiceException {
        try {
			AuthenticationUtils.startSession(USERNAME, PASSWORD);
			PlutextService_ServiceLocator locator = new PlutextService_ServiceLocator(
					AuthenticationUtils.getEngineConfiguration());
			locator
					.setPlutextServiceEndpointAddress(org.alfresco.webservice.util.WebServiceFactory
							.getEndpointAddress()
							+ "/" + locator.getPlutextServiceWSDDServiceName());
			ws = locator.getPlutextService();
			
        } catch (AuthenticationFault exc) {
        	throw new ServiceException("Authentication failure.", exc);
		}
    }
    
    public void endSession() {
    	AuthenticationUtils.endSession();
    }
    
/* ****************************************************************************************
 *          FETCH REMOTE UPDATES 
 * **************************************************************************************** */ 

    private Skeleton oldServer;
    
    public void fetchUpdates() throws RemoteException {
		log.debug(".. .. fetchUpdates, from "
				+ stateDocx.getTransforms().getTSequenceNumberHighestFetched());

		// ws = ChunkServiceOverride.getWebService();
		String[] updates = 
			ws.getTransforms(
				stateDocx.getDocID(), 
				stateDocx.getTransforms().getTSequenceNumberHighestFetched());

		log.debug(" sequence = " + updates[0]);
		if (updates.length < 2) {
			log.debug(stateDocx.getDocID() + " ERROR!!!");

		} else {
			log.debug(stateDocx.getDocID() + " transforms = " + updates[1]);
			
			String serverSkeletonStr = ws.getSkeletonDocument(stateDocx.getDocID());
			oldServer = new Skeleton(serverSkeletonStr);

			if (Integer.parseInt(updates[0]) > stateDocx.getTransforms()
					.getTSequenceNumberHighestFetched()) {
				stateDocx.getTransforms().setTSequenceNumberHighestFetched(
						Integer.parseInt(updates[0]));
				Boolean appliedTrue = false;
				Boolean localTrue = false;
				registerTransforms(updates[1], appliedTrue, localTrue);
				// Globals.ThisAddIn.Application.StatusBar = "Fetched " +
				// updates[0];

				remoteUpdatesNeedApplying = true;
			} else {
				// Globals.ThisAddIn.Application.StatusBar = "No remote
				// updates";

				// Do not do: remoteUpdatesNeedApplying = false;
				// since there may be some from the last time this was executed
			}
		}
	}

    /* Put transforms received from server into the transforms collection. */
    public void registerTransforms(
    	String transforms, 
    	Boolean setApplied, 
    	Boolean setLocal) {
    	registerTransforms(transforms, setApplied, setLocal, true);
    }
    
    public void registerTransforms(
    	String transforms, 
    	Boolean setApplied, 
    	Boolean setLocal, 
    	Boolean updateHighestFetched)
    {
        log.debug(stateDocx.getDocID() + ".. .. registerTransforms");

        // Parse the XML document, and put each transform into the transforms
		// collection
		org.plutext.transforms.Transforms transformsObj = null;
		try {
			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
			transformsObj = (org.plutext.transforms.Transforms) u
					.unmarshal(new java.io.StringReader(transforms));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (T t : transformsObj.getT()) {
			TransformAbstract ta = TransformHelper.construct(t);
            registerTransform(ta, setApplied, setLocal, updateHighestFetched);
		}
    }

    public void registerTransforms(
    		org.plutext.transforms.Transforms transformsObj, 
        	Boolean setApplied, 
        	Boolean setLocal, 
        	Boolean updateHighestFetched)
        {
            log.debug(stateDocx.getDocID() + ".. .. registerTransforms");

         

    		for (T t : transformsObj.getT()) {
    			TransformAbstract ta = TransformHelper.construct(t);
                registerTransform(ta, setApplied, setLocal, updateHighestFetched);
    		}
        }
    
    public void registerTransform(TransformAbstract t, Boolean setApplied, Boolean setLocal, 
        Boolean updateHighestFetched)
    {
        if (setApplied) { t.setApplied(true); }
        if (setLocal) { t.setLocal(true); }

        log.debug("Instance " + stateDocx.getDocID() + " -- Registering " + t.getSequenceNumber() );
        try
        {
            stateDocx.getTransforms().add(t, updateHighestFetched);
            log.debug(".. done.");
        }
        catch (Exception e)
        {
            log.debug(".. not done: " + e.getMessage() );
            // Ignore - An item with the same key has already been added.
        }

    }


/* ****************************************************************************************
 *          APPLY REMOTE UPDATES
 * **************************************************************************************** */

    Divergences divergences = null;

    public Divergences getDivergences() {
    	return divergences; 
        //set { divergences = value; }
    }

public void applyRemoteChanges()
{
    // TODO: grey out if there are no remote updates to apply


//    DateTime startTime = DateTime.Now;
//    log.debug(startTime);


    /* Create a DiffReport object, which tells us 
     * the difference between what Sdts are actually in the document,
     * and what its state should be, given the last round of server
     * updates.
     * 
     * We'll use this to adjust the insert position of new sdt's.
     * 
     * As we perform any insert/delete transform, we'll update
     * the DiffReport object. 
     * 
     * Note that if this document contains text which is not inside
     * a content control, then we won't know whether to insert
     * before that or after it.  This is an ambiguous situation, 
     * but at least the user will be able to see where the insertion
     * has occurred, since it will be (TODO) marked up.
     */

    // TODO - this is a bit expensive, and it is only necessary
    // if the updates include insertions, so look at them first

    //if (updatesIncludeInsertions)
    //{
		WordMLDocument wordMLDoc = 
			(WordMLDocument) getWordMLTextPane().getDocument();
        Skeleton actuals  = Util.createSkeleton(wordMLDoc);

        DiffEngine drift = new DiffEngine();
        drift.processDiff(this.oldServer, actuals);
        divergences = new Divergences(drift);

        /* For example

            1324568180	1324568180 (no change)
            1911345834	1911345834 (no change)
            ---	293467343 not at this location in source  <---- if user deletes
            884169107	884169107 (no change)
            528989532	528989532 (no change)
         */
 

        //}


    applyUpdates();

    remoteUpdatesNeedApplying = false;

}


/* Apply registered transforms. */
public void applyUpdates()
{
    /* Note well that it is important to the correct functioning that the 
     * updates are applied IN ORDER.  
     */
 
        log.debug(stateDocx.getDocID() + ".. .. applyUpdates");

    // loop through, fetch, apply 
    List<TransformAbstract> transformsBySeqNum = stateDocx.getTransforms().getTransformsBySeqNum();
    List<TransformAbstract> discards = new ArrayList<TransformAbstract>();
    for (TransformAbstract t : transformsBySeqNum)
    {
        // OPTIMISATION: could do the most recent only for each cc
        // (ie reverse order), except for MOVES and INSERTS, which need to 
        // be done in order.

        if (t.getApplied())  // then it shouldn't be in the list ?!
        {
            if (stateDocx.getTransforms().getTSequenceNumberHighestFetched() > t.getSequenceNumber() )
            {
                discards.add(t);
            }
            continue;
        }

        log.debug(".. applying " + t.getSequenceNumber() );

        long result = applyUpdate(t);

        log.debug(".. applied " + t.getSequenceNumber());

        if (result > 0)
        {
            // Applied, so can discard, provided highest fetched is higher
            // than this snum (otherwise it will just get fetched again!)
            if (stateDocx.getTransforms().getTSequenceNumberHighestFetched() > t.getSequenceNumber() )
            {
                discards.add(t);
            }
        }
        else
        {
            log.debug("Failed to apply transformation " + t.getSequenceNumber() );
        }


    }

    // Now remove the discards
    for (TransformAbstract ta : discards)
    {
        transformsBySeqNum.remove(ta);  
    }

}

/* On success, returns the transformation's tSequenceNumber; otherwise, 0 */
private long applyUpdate(TransformAbstract t)
{
    long result;

    log.debug("applyUpdate " + t.getClass().getName() + " - " + t.getSequenceNumber() );

    if (t instanceof org.plutext.client.wrappedTransforms.TransformInsert 
    		|| t instanceof org.plutext.client.wrappedTransforms.TransformMove
    		|| t instanceof org.plutext.client.wrappedTransforms.TransformDelete
        )
    {
        result = t.apply(this, stateDocx.getStateChunks());
        t.setApplied(true);
        log.debug(t.getSequenceNumber() + " applied (" + t.getClass().getName() + ")");
        return result;
    }
    else if (t instanceof org.plutext.client.wrappedTransforms.TransformStyle)
    {
        // TODO - Implement TransformStyle
        // that class is currently non functional.
        result = t.apply(this, stateDocx.getStateChunks());
        t.setApplied(true);
        log.debug(t.getSequenceNumber() + " UNDER CONSTRUCTION (" + t.getClass().getName() + ")");
        return result;

    }
    else if (t instanceof org.plutext.client.wrappedTransforms.TransformUpdate)
    {
        WordMLDocument wordMLDoc = (WordMLDocument) getWordMLTextPane().getDocument();
        StateChunk currentChunk = 
        	Util.getStateChunk(wordMLDoc, t.getId().getVal().toString());
        if (currentChunk == null) {
            log.debug(t.getSequenceNumber() + " trying to apply (" + t.getClass().getName() + ")");
        	log.debug("BUT No StateChunk found in WordMLDocument. t.getId()=" + t.getId().getVal());
        	return -1;
        }
        
        boolean noConflict = currentChunk.getXml().equals(
        	stateDocx.getStateChunks().get(t.getId().getVal().toString()).getXml());

        log.debug("t.getSequenceNumber()=" + t.getSequenceNumber() 
        		+ " t.isLocal()=" + t.isLocal()
				+ " noConflict=" + noConflict); 

        if ( noConflict || t.isLocal() )
        {
            sdtChangeTypes.put(t.getId().getVal().toString(), TrackedChangeType.OtherUserChange);
            hasNonConflictingChanges = true;
        }
        else
        {
            sdtChangeTypes.put(t.getId().getVal().toString(), TrackedChangeType.Conflict);
        }

        // The update we will insert is one that contains the results
        // of comparing the server's SDT to the user's local one.
        // This will allow the user to see other people's changes.
       	((TransformUpdate)t).markupChanges(currentChunk.getSdt() );
       	
        result = t.apply(this, stateDocx.getStateChunks());
        t.setApplied(true);
        
        log.debug("t.getSequenceNumber()=" + t.getSequenceNumber() 
        	+ " applied (" + t.getClass().getName() + ")");
        
        return result;

    }
    else
    {

        log.debug(" How to handle " + t.getClass().getName() );
        return -1;
    }

}




/* ****************************************************************************************
 *          ACCEPT REMOTE CHANGES
 * **************************************************************************************** */

    HashMap<String, TrackedChangeType> sdtChangeTypes = new HashMap<String, TrackedChangeType>();

    public enum TrackedChangeType
    {
        Conflict,
        OtherUserChange,
        NA
    }

    boolean hasNonConflictingChanges = false;
    
    public boolean hasNonConflictingChanges() {
    	return hasNonConflictingChanges;
    }
    
    public void setHasNonConflictingChanges(boolean b) {
    	this.hasNonConflictingChanges = b;
    }
    
    public TrackedChangeType getTrackedChangeType(String sdtBlockId) {
    	return this.sdtChangeTypes.get(sdtBlockId);
    }
    
    public TrackedChangeType removeTrackedChangeType(String sdtBlockId) {
    	return this.sdtChangeTypes.remove(sdtBlockId);
    }
    
    public List<String> getIdsOfNonConflictingChanges()
    {
    	List<String> nonConflictingChanges = new ArrayList<String>();
    	
    	Iterator<Map.Entry<String, TrackedChangeType>> it = 
    		this.sdtChangeTypes.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<String, TrackedChangeType> entry = it.next();
    		String id = entry.getKey();
    		TrackedChangeType type = entry.getValue();
    		if (type == TrackedChangeType.OtherUserChange) {
    			nonConflictingChanges.add(id);
    		}
    	}
    	
    	return nonConflictingChanges.isEmpty() ? null : nonConflictingChanges;
    }


/* ****************************************************************************************
 *          TRANSMIT LOCAL CHANGES
 * **************************************************************************************** */ 
    public void transmitLocalChanges() throws RemoteException, ClientException
    {
        // Look for local modifications
        // - commit any which are non-conflicting (send these as TRANSFORMS)
        transmitContentUpdates();

        //transmitStyleUpdates();

        //Globals.ThisAddIn.Application.StatusBar = "Local changes transmitted";
    }


    void transmitContentUpdates() throws RemoteException
    {

        log.debug(stateDocx.getDocID() + ".. .. transmitContentUpdates");

        // The list of transforms to be transmitted
        List<T> transformsToSend = new ArrayList<T>();

        /* Pre processing  - chunk as required, so that there is
         * only one paragraph in the sdt.
         * 
         * All clients (ie this Word add in, docx4all) should do this
         * if we're set to chunk on each paragraph. */

        // TODO only if chunking is required.
        //foreach (Word.ContentControl cc in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
        //{
        //    if (Chunker.containsMultipleBlocks(cc)) // TODO - && no remote changes to apply
        //    {
        //        Chunker.chunk(cc);
                // TODO: ensure you keep pkg object up to date
        //    }
        //}  

        // Indentify structural changes (ie moves, inserts, deletes)
        // If skeletons are different, there must be local changes 
        // which we need to transmit

        /* For example

            1324568180	1324568180 (no change)
            1911345834	1911345834 (no change)
            ---	293467343 not at this location in source  <---- if user deletes
            884169107	884169107 (no change)
            528989532	528989532 (no change)
         */


        // compare the inferredSkeleton to serverSkeleton
        WordMLDocument wordMLDoc = (WordMLDocument) getWordMLTextPane().getDocument();
        Skeleton inferredSkeleton = Util.createSkeleton(wordMLDoc); 

        Skeleton serverSkeleton = null;
        try {
			String serverSkeletonStr = ws.getSkeletonDocument(stateDocx
					.getDocID());
			Unmarshaller u = Context.jcTransitions.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
			org.plutext.server.transitions.Transitions serverDst = 
				(org.plutext.server.transitions.Transitions) u
					.unmarshal(new javax.xml.transform.stream.StreamSource(
							new java.io.StringReader(serverSkeletonStr)));
			serverSkeleton = new Skeleton(serverDst);
		} catch (JAXBException exc) {
			exc.printStackTrace(); //should not happen
		}

        // OK, do it
        createTransformsForStructuralChanges(
        	transformsToSend,
            inferredSkeleton, 
            serverSkeleton);


        Boolean someTransmitted = false;
        Boolean someConflicted = false;

        int insertPos;
        int i;
        
    	org.plutext.transforms.ObjectFactory transformsFactory = new org.plutext.transforms.ObjectFactory();
        
        try
        {
            String checkinComment = null;

            insertPos = -1;
            
            DocumentElement root = (DocumentElement) wordMLDoc.getDefaultRootElement();
            for (int idx=0; idx < root.getElementCount(); idx++) {
            	DocumentElement elem = (DocumentElement) root.getElement(idx);
            	ElementML ml = elem.getElementML();
            	if (ml instanceof SdtBlockML) {
                    insertPos++;
                    StateChunk chunkCurrent =
                    	new StateChunk((org.docx4j.wml.SdtBlock) ml.getDocxObject());
                    String stdId = chunkCurrent.getIdAsString();

                    StateChunk chunkOlder = stateDocx.getStateChunks().get(stdId);                
                    
                    if (chunkOlder==null) { 
                    	
                        log.debug("Couldn't find " + stdId + " .. handled already ...");

                        continue;                    	
                    	
                    } else if (chunkCurrent.getXml().equals(chunkOlder.getXml()))
                    {
                        continue;
                    }
                    
                    log.debug("textChanged:");
                    log.debug("FROM " + chunkOlder.getXml() );
                    log.debug("");
                    log.debug("TO   " + chunkCurrent.getXml() );
                    log.debug("");

                    // If we get this far, it is an update
                    // We don't need to worry about the possibility that it has
                    // changed remotely, since we checked all updates
                    // on server had been applied before entering this method.

                    if ( chunkCurrent.containsTrackedChanges())
                    {
                        // This is a conflicting update, so don't transmit ours.
                        // Keep a copy of what this user did in StateChunk
                        // (so that 

                        log.debug("Conflict! Local edit " + stdId + " not committed.");
                        someConflicted = true;
                        continue;
                    }

                    if (stateDocx.getPromptForCheckinMessage() )
                    {

                        if (checkinComment == null)
                        {

                            log.debug("Prompting for checkin message...");
                            // NB with this model, we are no longer applying a 
                            // comment to a single paragraph.  The server will stick
                            // the comment on each affected rib.

                            /*
                            formCheckin form = new formCheckin();
                            //form.Text = "Changes to '" + plutextTabbedControl.TextBoxChunkDisplayName.Text + "'";
                            form.Text = "Changes ";
                            using (form)
                            {
                                //if (form.ShowDialog(plutextTabbedControl) == DialogResult.OK)
                                if (form.ShowDialog() == DialogResult.OK)
                                {
                                    checkinComment = form.textBoxChange.Text;
                                }
                            }*/
                        }
                    }
                    else
                    {
                        checkinComment = "edited";
                    }

//                    TransformUpdate tu = new TransformUpdate();
//                    tu.attachSdt(chunkCurrent.getXml() );
//                    tu.setId( chunkCurrent.getId() );
//                    transformsToSend.add(tu);
                    
                    T t = transformsFactory.createTransformsT();
                    t.setOp("update");
                    t.setIdref( chunkCurrent.getId().getVal().longValue() );
                    t.setSdt( chunkCurrent.getSdt() );
                    transformsToSend.add(t);
                    
                    
            	}
            } //for (idx) loop

            // Ok, now send what we have
    	    Transforms transforms = transformsFactory.createTransforms();
    	    transforms.getT().addAll(transformsToSend);    	    
            boolean suppressDeclaration = true;
            boolean prettyprint = false;
            String transformsString = org.docx4j.XmlUtils.marshaltoString(transforms, suppressDeclaration, prettyprint, org.plutext.Context.jcTransforms );
            
            checkinComment = " whatever!";

            log.debug("TRANSMITTING " + transformsString );

            String[] result = ws.transform(stateDocx.getDocID(), transformsString, checkinComment);

            log.debug("Checkin also returned results" );

            i = 0;
            // We do what is necessary to in effect apply the changes immediately,
            // so there is no issue with the user making changes before it
            // is applied, and those changes getting lost
            // In strict theory, we shouldn't do this, because they'll end 
            // up in the list in the wrong order.
            // But we actually know there are no conflicting transforms with
            // lower snums, so it isn't a problem.
            // Remember the indocument controls still have the dodgy StyleSeparator, 
            // so we will have to make allowance for that later
            // (that is, until we're able to transform them away ...)
            Boolean appliedTrue = true; 
            Boolean localTrue = true;   // means it wouldn't be treated as a conflict
            Boolean updateHighestFetchedFalse = false;
            
            // Handle each result appropriately
            for (T t : transformsToSend)
            {
                log.debug(t.getIdref() + " " + t.getOp() + " result " + result[i]);

                // Primarily, we're expecting sequence numbers

                // At present, op="update" returns a transform
                // but it should be no different to what we sent
                // except that its tag is updated
                /* When registering these transforms, don't update highest fetched, 
                 * because other clients could have transmitted changes to the server
                 * while this method was running, and we wouldn't want to miss those
                 * changes. */
                if (result[i].contains("xmlns"))
                {
        			StringBuffer sb = new StringBuffer();
        			sb.append("<p:transforms xmlns:p='"); 
        			sb.append(Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE); 
        			sb.append("'>");  
        			sb.append(result[i]); 
        			sb.append("</p:transforms>");

                    //getContentControlWithId(ta.getId().getVal().toString() ).Tag = ta.getTag();
        			org.plutext.transforms.Transforms transformsObj = null;
        			try {
        				Unmarshaller u = Context.jcTransforms.createUnmarshaller();
        				u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
        				transformsObj = (org.plutext.transforms.Transforms) u
        						.unmarshal(new java.io.StringReader(sb.toString()));
        			} catch (JAXBException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        			}

        			for (T tmp : transformsObj.getT()) {
        				TransformAbstract ta = TransformHelper.construct(tmp);
        	            if (ta instanceof TransformUpdate) {
                            // Set the in-document tag to match the one we got back
                            // ?? the actual sdt or the state chunk?
                			updateLocalContentControlTag(
                				ta.getId().getVal().toString(), 
                				ta.getTag());
                			this.stateDocx.getStateChunks().put(
                				ta.getId().getVal().toString(), 
                				new StateChunk(ta.getSdt()));
        	            } else {
        	            	// Assumption is that chunking is done locally, 
        	            	// and we won't get eg an Insert back
        	            	log.error("Not handled: " + ta.getClass().getName() );
        	            }
        			}
        			
        			registerTransforms(
        					transformsObj, appliedTrue, localTrue, updateHighestFetchedFalse);
                 
                }
                else
                {
                	TransformAbstract ta = org.plutext.client.wrappedTransforms.TransformHelper.construct(t);
                    ta.setSequenceNumber(Integer.parseInt(result[i]) );
                    registerTransform(ta, appliedTrue, localTrue, updateHighestFetchedFalse);
                }



                i++;
            }

            someTransmitted = true;

            //// Need to wrap in a transforms element
            //result = "<p:transforms xmlns:p='" + Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE + "'>"
            //            + result
            //            + "</p:transforms>";

            //log.debug("Parsing " + result);



        }
        catch (Exception e)
        {
        	e.printStackTrace();
            //log.debug(e.getMessage());
            //log.debug(e.getStackTrace());
        }

        String checkinResult = null;

        if (someTransmitted)
        {
            checkinResult = "Your changes were transmitted successfully.";
            if (someConflicted)
                checkinResult += " But there were one or more conflicts.";
        }
        else
        {
            checkinResult = "Your changes were NOT transmitted.";
            if (someConflicted)
                checkinResult += " This is because there were conflicts.";
        }

        WordMLEditor wmlEditor = 
        	WordMLEditor.getInstance(WordMLEditor.class);
        String title = "Commit Local Edits";
        wmlEditor.showMessageDialog(
        	title, checkinResult, JOptionPane.INFORMATION_MESSAGE);
    }

    void createTransformsForStructuralChanges(
    	List<T> transformsToSend,
        Skeleton inferredSkeleton, 
        Skeleton serverSkeleton)
    {
    	
    	org.plutext.transforms.ObjectFactory transformsFactory = new org.plutext.transforms.ObjectFactory();

        DiffEngine de = new DiffEngine();
        de.processDiff(inferredSkeleton, serverSkeleton);

        ArrayList<DiffResultSpan> diffLines = de.getDiffLines();

        /* Detect moves
         * 
         * In order to detect moves, we have to be able to
         * identify whether a delete has a corresponding
         * insert (and vice versa).
         * 
         * These HashMap objects facilitate this. 
         * 
         */
        HashMap<String, Integer> notHereInDest = new HashMap<String, Integer>();
        HashMap<String, Integer> notHereInSource = new HashMap<String, Integer>();
        //Populate the dictionaries
        int insertPos = -1;
        int i;
        log.debug("\n\r");
        for (DiffResultSpan drs : diffLines)
        {
            switch (drs.getDiffResultSpanStatus())
            {
                case DELETE_SOURCE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        insertPos++;
                        // Must be a new local insertion
                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + " not at this location in dest");
                        String insertionId = ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine();
                        notHereInDest.put(insertionId, insertPos);
                    }

                    break;
                case NOCHANGE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        insertPos++;
                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + "\t" + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine() + " (no change)");

                        // Nothing to do
                    }

                    break;
                case ADD_DESTINATION:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        //insertPos++; // Not for a delete
                        log.debug(insertPos + ": " + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine()
                            + " not at this location in source");
                        String deletionId = ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine();
                        notHereInSource.put(deletionId, insertPos);

                    }

                    break;
            }
        }

        Divergences divergences = new Divergences(de);

        log.debug("\n\r");


        // How to make the dest (right) like the source (left)

        for (DiffResultSpan drs : diffLines)
        {
            switch (drs.getDiffResultSpanStatus())
            {
                case DELETE_SOURCE:  // Means we're doing an insertion
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        String insertionId = ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine();
                        log.debug(insertPos + ": " + insertionId
                            + " is at this location in src but not dest, so needs to be inserted");

                        Integer dicVal = notHereInSource.get(insertionId);
                        
                        if (dicVal==null) {
                        	
                            // Just a new local insertion

                            long adjPos = divergences.getTargetLocation(insertionId);
                            log.debug("Couldn't find " + insertionId + " so inserting at "
                                + adjPos);

                            divergences.insert(insertionId); // change +1 to 0

                            divergences.debugInferred();

                            StateChunk sc = 
                            	Util.getStateChunk(
                            		(WordMLDocument) getWordMLTextPane().getDocument(), 
                            		insertionId);

//                            TransformInsert ti = new TransformInsert();
//                            ti.setPos( Integer.toString(adjPos) );
//                            ti.setId( sc.getId() );
//                            ti.attachSdt(sc.getXml());
//                            transformsToSend.add(ti);
                            T t = transformsFactory.createTransformsT();
                            t.setOp("insert");
                            t.setPosition(adjPos);
                            t.setIdref( sc.getId().getVal().longValue() );
                            t.setSdt( sc.getSdt() );
                            transformsToSend.add(t);
                            
                            this.stateDocx.getStateChunks().put(sc.getIdAsString(), sc);

                            log.debug("text Inserted:");
                            log.debug("TO   " + sc.getXml());
                            log.debug("");
                        	
                        	
                        } else {                        	                        	
                        	
	                        // there is a corresponding delete, so this is really a move
	                        log.debug("   " + insertionId + " is a MOVE");
	
	                        //if (toPosition[insertionId] == divergences.currentPosition(insertionId))  //rhsPosition[insertionId])
	                        //{
	                        //    // currentPosition is the position in the inferred point-in-time 
	                        //    // server skeleton (ie as it would be with transforms 
	                        //    // generated so far applied)
	
	                        //    log.debug("Discarding <transform op=move id=" + insertionId + "  pos=" + toPosition[insertionId]);
	                        //}
	                        //else
	                        //{
	
	                        /* Semantics of move will be as follows:
	                         * 
	                         * (i) removed the identified item,
	                         * 
	                         * (ii) then insert the new item at the specified position.
	                         * 
	                         * This way, the position you specify is the position it
	                         * ends up in (ie irrespective of whether the original
	                         * position was earlier or later).  
	                         */
	
	
	
	                        // therefore:
	                        // delete first (update divergences object)
	                        divergences.delete(insertionId); // remove -1
	
	                        long adjPos = divergences.getTargetLocation(insertionId);
	
	                        log.debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);
	
	                        divergences.insert(insertionId); // change +1 to 0
	
	                        divergences.debugInferred();
	
	                        log.debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);
	
                            StateChunk sc = 
                            	Util.getStateChunk(
                            		(WordMLDocument) getWordMLTextPane().getDocument(), 
                            		insertionId);
	
//	                        TransformMove tm = new TransformMove();
//	                        tm.setPos( Integer.toString(adjPos) );
//	                        tm.setId ( sc.getId() );
//	                        //tm.attachSdt(sc.Xml);
//	                        transformsToSend.add(tm);
	                        
                            T t = transformsFactory.createTransformsT();
                            t.setOp("move");
                            t.setPosition(adjPos);
                            t.setIdref( sc.getId().getVal().longValue() );
                            //t.setSdt( sc.getSdt() );
                            transformsToSend.add(t);

	
	                        log.debug("text moved:");
	
	                        //if (rawPos + adjPos == divergences.currentPosition(insertionId))
	                        //{`
	                        //    log.debug(".. that transform could be DISCARDED.");
	                        //}
	
	                        //divergences.move(insertionId, rawPos + adjPos);
	                        //}
                        }
                    }

                    break;
                case NOCHANGE:
                    for (i = 0; i < drs.getLength(); i++)
                    {

                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + "\t" + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine() + " (no change)");

                    }

                    break;
                case ADD_DESTINATION:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        String deletionId = ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine();
                        log.debug(insertPos + ": " + deletionId
                            + " present at this location in dest but not source, so needs to be deleted");

                        Integer dicVal = notHereInDest.get(deletionId);
                        
                        if (dicVal==null) {
                            // Just a new local deletion

                            log.debug("Couldn't find " + deletionId + " so deleting");
                            divergences.delete(deletionId);

                            divergences.debugInferred();

//                            TransformDelete td = new TransformDelete(deletionId);
//                            transformsToSend.add(td);
                            
                            T t = transformsFactory.createTransformsT();
                            t.setOp("delete");
                            t.setIdref( Long.parseLong(deletionId) );
                            //t.setSdt( sc.getSdt() );
                            transformsToSend.add(t);
                            
                            this.stateDocx.getStateChunks().remove(deletionId);

                            log.debug("text deleted:");
                        	
                        	
                        } else {
                            // there is a corresponding insert, so this is really a move
                            log.debug("   " + deletionId + " is a MOVE to elsewhere (" + dicVal + ")");
                            // DO NOTHING
                        }
                    }

                    break;
            }
        }
    }

    void transmitStyleUpdates() throws RemoteException {
		log.debug(stateDocx.getDocID() + ".. .. transmitStyleUpdates");

		// TODO
		String newStyles = ""; // stateDocx.StyleMap.identifyAlteredStyles();
		if (newStyles.equals("")) {
			log.debug("styles haven't Changed ..");
			
		} else {
			log.debug("stylesChanged");
			log.debug("Committing new/updated styles" + newStyles);
			//stateDocx.TSequenceNumberHighestSeen = Int32.Parse(ws.style(stateDocx.DocID, newStyles));                    
			String[] result = { "", "" };

			// TODO - call transforms
			//result = ws.style(stateDocx.getDocID(), newStyles);

			log.debug(result[1]);

			Boolean appliedTrue = true; // Don't have to do anything more
			Boolean localTrue = true;
			registerTransforms(result[1], appliedTrue, localTrue);
			// TODO, can't use that, since it automatically updates highest fetched.

		}
	}

    private void updateLocalContentControlTag(String sdtId, Tag tag) {
    	WordMLDocument doc = (WordMLDocument) this.textPane.getDocument();
		DocumentElement elem = Util.getDocumentElement(doc, sdtId);
		
		log.debug("updateLocalContentControlTag(): elem=" + elem);
		log.debug("updateLocalContentControlTag(): tag param=" + tag.getVal());
		
		SdtBlockML ml = (SdtBlockML) elem.getElementML();
		ml.getSdtProperties().setTagValue(tag.getVal());
    }
    
}// Mediator class





























