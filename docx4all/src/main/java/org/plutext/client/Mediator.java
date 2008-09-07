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

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.rpc.ServiceException;

import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4all.swing.CheckinCommentDialog;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.DocUtil;
import org.docx4all.vfs.WebdavUri;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.wml.Tag;
import org.plutext.Context;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffResultSpan;
import org.plutext.client.state.StateChunk;
import org.plutext.client.state.StateDocx;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;
import org.plutext.client.wrappedTransforms.TransformAbstract;
import org.plutext.client.wrappedTransforms.TransformDelete;
import org.plutext.client.wrappedTransforms.TransformHelper;
import org.plutext.client.wrappedTransforms.TransformInsert;
import org.plutext.client.wrappedTransforms.TransformMove;
import org.plutext.client.wrappedTransforms.TransformStyle;
import org.plutext.client.wrappedTransforms.TransformUpdate;
import org.plutext.transforms.Transforms;
import org.plutext.transforms.Changesets.Changeset;
import org.plutext.transforms.Transforms.T;

/**
 * This class is the real workhorse.
 */
public class Mediator {
	/*
	 * Design goals:
	 * 
	 * 1. Don't use content control entry and exit handlers: (i) these are hard
	 * to get right (ii) if stuff happens at that point, user can't navigate
	 * smoothly around doc
	 * 
	 * 2. Pick up eg results of find/replace; any content user manages to enter
	 * outside a content control; moves.
	 * 
	 * 3. Efficient use of XML representation ie .WordOpenXML .. only get this
	 * once per Callback.
	 * 
	 * 4. Efficient use of InsertXML (just used to create new document).
	 * 
	 * 5. Granular control of differencing, which avoids Word's Compare which
	 * sometimes replaces the entire SDT with w:customXmlInsRangeStart&End,
	 * w:customXmlDelRangeStart&End
	 * 
	 * Principles (to be added to end user documentation):
	 * 
	 * (i) All remote changes must be fetched & applied before a user is able to
	 * transmit his local changes
	 * 
	 * (ii)Any tracked changes in a cc must be resolved before the use is
	 * allowed to commit that one
	 * 
	 * TODO:
	 *  - markup insertions! - styles - XSLT optimisations
	 * 
	 */

	private static Logger log = Logger.getLogger(Mediator.class);

	private static final Long CANT_OVERWRITE = Long.valueOf(0);

	private StateDocx stateDocx;

	public StateDocx getStateDocx() {
		return stateDocx;
	}

	private WordMLTextPane textPane;

	public WordMLTextPane getWordMLTextPane() {
		return textPane;
	}

	private WordMLDocument getWordMLDocument() {
		return (WordMLDocument) textPane.getDocument();
	}
	
	public Mediator(WordMLTextPane textPane) {
		WordMLDocument doc = (WordMLDocument) textPane.getDocument();
		if (!DocUtil.isSharedDocument(doc)) {
			throw new IllegalArgumentException("Not a shared WordMLDocument");
		}

		this.textPane = textPane;
		this.stateDocx = new StateDocx(doc);
	}

	private PlutextWebService ws = null;

	private Skeleton currentClientSkeleleton = null;

	/***************************************************************************
	 * SESSION MANAGEMENT 
	 ******************************************************
	 */
	public void startSession() throws ServiceException {
        try {
			WordMLDocument doc = getWordMLDocument();
			WebdavUri uri =
				new WebdavUri(
				(String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY));
			
		    StringBuilder endPointAddress = new StringBuilder("http://");
		    endPointAddress.append(uri.getHost());
		    if (uri.getPort() != null) {
		    	endPointAddress.append(":");
		    	endPointAddress.append(uri.getPort());
		    }
		    endPointAddress.append("/alfresco/api");
		    			
            org.alfresco.webservice.util.WebServiceFactory.setEndpointAddress(
            		endPointAddress.toString());
            
            AuthenticationUtils.startSession(uri.getUsername(), uri.getPassword());
		      
            PlutextService_ServiceLocator locator = 
            	new PlutextService_ServiceLocator(
            			AuthenticationUtils.getEngineConfiguration());
            
            locator.setPlutextServiceEndpointAddress(
            	endPointAddress + "/" + locator.getPlutextServiceWSDDServiceName());

            ws = locator.getPlutextService();

			this.updateStartOffset = doc.getLength();
			this.updateEndOffset = 0;

			DocumentElement root = (DocumentElement) doc
					.getDefaultRootElement();
			currentClientSkeleleton = new Skeleton();
			for (int idx = 0; idx < root.getElementCount(); idx++) {
				DocumentElement elem = (DocumentElement) root.getElement(idx);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					SdtBlockML sdt = (SdtBlockML) ml;
					String sdtId = sdt.getSdtProperties().getIdValue()
							.toString();
					TextLine rib = new TextLine(sdtId);
					currentClientSkeleleton.getRibs().add(rib);
				}
			}

		} catch (AuthenticationFault exc) {
			throw new ServiceException("Service Connection failure.", exc);
		} catch ( org.alfresco.webservice.util.WebServiceException wse) {
        	throw new ServiceException("Service Connection failure.", wse);			
		}
	}

	public void endSession() {
		AuthenticationUtils.endSession();
		currentClientSkeleleton = null;
		ws = null;
		changeSets = null;
	}

	/***************************************************************************
	 * FETCH REMOTE UPDATES
	 * ****************************************************************************************
	 */

	private Skeleton oldServer;

	public void fetchUpdates() throws RemoteException {
		log.debug(".. .. fetchUpdates, from "
				+ stateDocx.getTransforms().getTSequenceNumberHighestFetched());

		// ws = ChunkServiceOverride.getWebService();
		String[] updates = ws.getTransforms(stateDocx.getDocID(), stateDocx
				.getTransforms().getTSequenceNumberHighestFetched());

		log.debug(" sequence = " + updates[0]);
		if (updates.length < 2) {
			log.error(stateDocx.getDocID() + " ERROR!!!");

		} else {
			log.debug(stateDocx.getDocID() + " transforms = " + updates[1]);

            /* eg
             * 
             * <ns6:updates >
             *      <ns6:transforms>
             *          <ns6:t ns6:op="update" ns6:changeset="1" ns6:snum="1" ns6:tstamp="1218269730469">
             *              <w:sdt><w:sdtPr><w:id w:val="759551861"/><w:tag w:val="1"/></w:sdtPr><w:sdtContent><w:p><w:r><w:t>So now .. </w:t></w:r></w:p></w:sdtContent></w:sdt>
             *          </ns6:t>
             *      </ns6:transforms>
             *      <ns6:changesets>
             *          <ns6:changeset ns6:modifier="jharrop" ns6:number="1">edited</ns6:changeset>
             *      </ns6:changesets>
             * </ns6:updates> 
             * 
             */ 
			
            Boolean needToFetchSkel = false;
            
			if (Integer.parseInt(updates[0]) > stateDocx.getTransforms()
					.getTSequenceNumberHighestFetched()) {
				stateDocx.getTransforms().setTSequenceNumberHighestFetched(
						Integer.parseInt(updates[0]));
				Boolean appliedFalse = false;
				Boolean localFalse = false;
				Boolean updateHighestFetchedTrue = true;
				needToFetchSkel = 
					registerUpdates(
						updates[1], 
						appliedFalse, 
						localFalse, 
						updateHighestFetchedTrue);
			} else {
				// Globals.ThisAddIn.Application.StatusBar = "No remote
				// updates";
			}
			
			if (needToFetchSkel || oldServer == null) {
				String serverSkeletonStr = ws.getSkeletonDocument(stateDocx
						.getDocID());
				oldServer = new Skeleton(serverSkeletonStr);
			}

		}
	}
	
    /**
     * Put transforms received from server into the transforms collection.
     * 
     * @return true if there is a structural change due to TransformInsert, 
     *              TransformDelete, or TransformMove;
     *         false, otherwise.
     */
    public boolean registerUpdates(
    	String updates, 
    	Boolean setApplied, 
    	Boolean setLocal, 
    	Boolean updateHighestFetched)
    {
        log.debug(stateDocx.getDocID() + ".. .. registerTransforms");

        // Parse the XML document, and put each transform into the transforms
		// collection
        org.plutext.transforms.Updates updatesObj = null;
		try {
			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
			updatesObj = (org.plutext.transforms.Updates) u
					.unmarshal(new java.io.StringReader(updates));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		boolean result = 
			registerTransforms(
				updatesObj.getTransforms(), 
				setApplied, 
				setLocal, 
				updateHighestFetched);
		
        // Changesets
		List<Changeset> changesetlist = updatesObj.getChangesets().getChangeset();
		this.changeSets = new HashMap<String, Changeset>(changesetlist.size());
		for (Changeset c: changesetlist) {
			this.changeSets.put(Long.toString(c.getNumber()), c);
		}
		
        log.debug("Changesets = " 
        	+ XmlUtils.marshaltoString(
        			updatesObj.getChangesets(), 
        			true, 
        			false, 
        			Context.jcTransforms));

        /* eg <ns6:changesets xmlns:ns6="http://www.plutext.org/transforms">
         *          <ns6:changeset ns6:date="2008-08-09T19:45:10.666+10:00" 
         *                         ns6:modifier="jharrop" 
         *                         ns6:number="1">
         *                          edited
         *          </ns6:changeset>
         *  </ns6:changesets> 
         */ 
        return result;
    }

    public boolean registerTransforms(
        String transforms, 
        Boolean setApplied, 
        Boolean setLocal, 
        Boolean updateHighestFetched) {
    	
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

    	return registerTransforms(transformsObj, setApplied, setLocal, updateHighestFetched);
    }

    /**
     * Returns true if there is a structural change due to TransformInsert, 
     *              TransformDelete, or TransformMove;
     *         false, otherwise.
     */
	public boolean registerTransforms(
			org.plutext.transforms.Transforms transformsObj,
			Boolean setApplied, Boolean setLocal, Boolean updateHighestFetched) {
		log.debug(stateDocx.getDocID() + ".. .. registerTransforms");

		boolean result = false;
		for (T t : transformsObj.getT()) {
			TransformAbstract ta = TransformHelper.construct(t);
            // Check for structural change, which will mean we
            // need to refresh our copy of the server skeleton
			result = 
				ta instanceof TransformInsert
				|| ta instanceof TransformDelete
				|| ta instanceof TransformMove;

			registerTransform(ta, setApplied, setLocal, updateHighestFetched);
		}
		return result;
	}

	public void registerTransform(TransformAbstract t, Boolean setApplied,
			Boolean setLocal, Boolean updateHighestFetched) {
		if (setApplied) {
			t.setApplied(true);
		}
		if (setLocal) {
			t.setLocal(true);
		}

		log.debug("Instance " + stateDocx.getDocID() + " -- Registering "
				+ t.getSequenceNumber() + ": " + t.getClass().getSimpleName());
		try {
			stateDocx.getTransforms().add(t, updateHighestFetched);
			log.debug(".. done.");
		} catch (Exception e) {
			log.debug(".. not done: " + e.getMessage());
			// Ignore - An item with the same key has already been added.
		}

	}

	private Map<String, Changeset> changeSets = null;
	
	/***************************************************************************
	 * APPLY REMOTE UPDATES
	 * ****************************************************************************************
	 */

	Divergences divergences = null;

	public Divergences getDivergences() {
		return divergences;
		// set { divergences = value; }
	}

	public void applyRemoteChanges() throws ClientException {
		// TODO: grey out if there are no remote updates to apply

		// DateTime startTime = DateTime.Now;
		// log.debug(startTime);

		if (this.oldServer == null || this.changeSets == null) {
			//applyRemoteChanges() is preceded with fetchUpdates().
			//If fetchUpdates() ends up with error or does not
			//fetch any new transform then nothing to be applied
			//in this method.
			return;
		}
		
		/*
		 * Create a DiffReport object, which tells us the difference between
		 * what Sdts are actually in the document, and what its state should be,
		 * given the last round of server updates.
		 * 
		 * We'll use this to adjust the insert position of new sdt's.
		 * 
		 * As we perform any insert/delete transform, we'll update the
		 * DiffReport object.
		 * 
		 * Note that if this document contains text which is not inside a
		 * content control, then we won't know whether to insert before that or
		 * after it. This is an ambiguous situation, but at least the user will
		 * be able to see where the insertion has occurred, since it will be
		 * (TODO) marked up.
		 */

		// TODO - this is a bit expensive, and it is only necessary
		// if the updates include insertions, so look at them first
		// if (updatesIncludeInsertions)
		// {
		DiffEngine drift = new DiffEngine();
		drift.processDiff(this.oldServer, this.currentClientSkeleleton);
		divergences = new Divergences(drift);

		/*
		 * For example
		 * 
		 * 1324568180 1324568180 (no change) 1911345834 1911345834 (no change)
		 * --- 293467343 not at this location in source <---- if user deletes
		 * 884169107 884169107 (no change) 528989532 528989532 (no change)
		 */

		// }

		applyUpdates();

		this.oldServer = null;
	}

	/* Apply registered transforms. */
	public void applyUpdates() throws ClientException {
		/*
		 * Note well that it is important to the correct functioning that the
		 * updates are applied IN ORDER.
		 */

		log.debug(stateDocx.getDocID() + ".. .. applyUpdates");

		List<TransformAbstract> transformsBySeqNum = 
			stateDocx.getTransforms().getTransformsBySeqNum();
		List<TransformAbstract> discards = new ArrayList<TransformAbstract>();
		
		boolean cantOverwrite = false;
		// loop through and apply
		for (TransformAbstract t : transformsBySeqNum) {
			// OPTIMISATION: could do the most recent only for each cc
			// (ie reverse order), except for MOVES and INSERTS, which need to
			// be done in order.

			if (t.getApplied()) // then it shouldn't be in the list ?!
			{
				if (stateDocx.getTransforms()
						.getTSequenceNumberHighestFetched() > t
						.getSequenceNumber()) {
					discards.add(t);
				}
				continue;
			}

			log.debug(".. applying " + t.getSequenceNumber());

			long resultCode = applyUpdate(t);

			log.debug(".. applied " + t.getSequenceNumber());

			if (resultCode == CANT_OVERWRITE) {
				cantOverwrite = true;

			} else if (resultCode > 0) {
				// Applied, so can discard, provided highest fetched is higher
				// than this snum (otherwise it will just get fetched again!)
				if (stateDocx.getTransforms()
						.getTSequenceNumberHighestFetched() > t
						.getSequenceNumber()) {
					discards.add(t);
				}
			} else {
				log.debug("Failed to apply transformation "
						+ t.getSequenceNumber());
			}

		}

		// Now remove the discards
		for (TransformAbstract ta : discards) {
			transformsBySeqNum.remove(ta);
		}

		if (cantOverwrite) {
			throw new ClientException("Cannot overwrite.");
		}
	}

	/* On success, returns the transformation's tSequenceNumber; otherwise, 0 */
	private long applyUpdate(TransformAbstract t) {
		long resultCode;

		log.debug("applyUpdate " + t.getClass().getName() + " - "
				+ t.getSequenceNumber());

		String idStr = t.getId().getVal().toString();

		StateChunk currentChunk = 
			Util.getStateChunk(getWordMLDocument(), idStr);
		boolean virgin = (currentChunk == null);

		Changeset changeset =
			this.changeSets.get(Long.toString(t.getChangesetNumber()));
        
		if (virgin && (t instanceof TransformInsert)) {
			// Server reduces Delete > Insert (reinstate)
			// to Insert, so it is possible for this client to
			// already have this control. This case is
			// handled like a TransformUpdate.

			// Mark it up
			t.markupChanges(null, changeset);

			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);
			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");

			if (resultCode >= 0) {
				this.sdtChangeTypes.put(idStr,
						TrackedChangeType.OtherUserChange);
			}

			return resultCode;
			
		} else if (t instanceof TransformDelete) {
			StateChunk stateDocxSC = stateDocx.getStateChunks().get(idStr);
			if (currentChunk == null) {
				// It is missing from current StateChunks, and the
				// reason for this is an insert and a delete transform
				// both being received this round

				// In this case we have already put it in stateDocx.StateChunks,
				// so we can do:
				t.markupChanges(stateDocxSC.getXml(), changeset);

			} else {
				boolean conflict = isConflict(currentChunk, stateDocxSC);
				if (conflict) {
					if (currentChunk.containsTrackedChanges()) {
						/*
						 * We can't just automatically accept the user's
						 * changes, String userSdtAcceptedXml =
						 * currentStateChunks[t.ID].acceptTrackedChanges(); then
						 * mark it all up as a deletion:
						 * ((TransformUpdate)t).markupChanges(userSdtAcceptedXml);
						 * since the second time through, automatically
						 * accepting would remove it entirely!
						 * 
						 * So instead ...
						 */

						// You need to accept/reject revisions before all
						// remote changes can be applied
						this.sdtChangeTypes.put(idStr,
								TrackedChangeType.Conflict);
						return CANT_OVERWRITE;
						
					} else {
						t.markupChanges(currentChunk.getXml(), changeset);
					}
					
				} else if (matchedOnMarkedUpVersion(currentChunk, stateDocxSC)) {
					// Compare it to non-marked up
					t.markupChanges(stateDocxSC.getXml(), changeset);
					
				} else {
					// Easy - they are the same
					t.markupChanges(currentChunk.getXml(), changeset);
				}
			} // if (currentChunk == null)

			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);

			if (resultCode >= 0) {
				this.sdtChangeTypes.put(idStr,
						TrackedChangeType.OtherUserChange);
				this.sdtIdUndead.put(idStr, idStr);
			}

			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");
			return resultCode;

		} else if (t instanceof TransformMove) {
			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);
			if (resultCode >= 0) {
				this.sdtChangeTypes.put(idStr,
						TrackedChangeType.OtherUserChange);
			}

			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");
			return resultCode;

		} else if (t instanceof TransformStyle) {
			// TODO - Implement TransformStyle
			// that class is currently non functional.
			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);
			// if (resultCode >= 0) {
			// this.sdtChangeTypes.put(idStr,
			// TrackedChangeType.OtherUserChange);
			// }
			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");
			return resultCode;

		} else if ((t instanceof TransformUpdate)
				|| (t instanceof TransformInsert)) {

			StateChunk stateDocxSC = stateDocx.getStateChunks().get(idStr);
			boolean conflict = false;

			if (currentChunk == null) {
				// t must not be a TransformInsert because
				// the condition of (currentChunk == null && t instanceof
				// TransformInsert)
				// is handled above.

				// t's Sdt missing from current StateChunks, and the
				// reason for this is an insert and a delete transform
				// both being received this round

				// So user hasn't seen it before, so
				// handle this like we handle a TransformInsert.
				((TransformUpdate) t).markupChanges(null, changeset);

			} else {
				conflict = isConflict(currentChunk, stateDocxSC);

				if (conflict) {
					if (currentChunk.containsTrackedChanges()) {
						/*
						 * We can't just automatically accept the user's
						 * changes, String userSdtAcceptedXml =
						 * currentStateChunks[t.ID].acceptTrackedChanges(); then
						 * mark it all up as a deletion:
						 * ((TransformUpdate)t).markupChanges(userSdtAcceptedXml);
						 * since the second time through, automatically
						 * accepting would remove it entirely!
						 * 
						 * So instead ...
						 */

						// You need to accept/reject revisions before all
						// remote changes can be applied
						this.sdtChangeTypes.put(idStr,
								TrackedChangeType.Conflict);
						return CANT_OVERWRITE;

					} else {
						t.markupChanges(currentChunk.getXml(), changeset);

						// We could warn the user here that their stuff has been
						// redlined
						// as a deletion.
					}
				} else if (matchedOnMarkedUpVersion(currentChunk, stateDocxSC)) {
					// Compare it to non-marked up
					t.markupChanges(stateDocxSC.getXml(), changeset);
				} else {
					// Easy
					t.markupChanges(currentChunk.getXml(), changeset);
				}
			}

			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);

			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");

			if (conflict) {
				this.sdtChangeTypes.put(idStr, TrackedChangeType.Conflict);
				log.debug("set state to CONFLICTED");
			} else {
				this.sdtChangeTypes.put(idStr,
						TrackedChangeType.OtherUserChange);
			}

			return resultCode;

		} else {
			log.debug(" How to handle " + t.getClass().getName());
			return -1;
		}
	}

	boolean isConflict(StateChunk currentStateChunk, StateChunk stateDocxSC) {

		boolean conflict = !(currentStateChunk.getXml().equals(stateDocxSC
				.getXml()));

		if (conflict) {
			log.debug("different!");
			log.debug("stateDocx : " + stateDocxSC.getXml());
			log.debug("current : " + currentStateChunk.getXml());

			// The StateChunks in stateDocx _never_ have markup
			// applied to them (because nothing on the server
			// can ever have markup in it).
			// But currentStateChunks might have markup in them,
			// if the user hasn't accepted changes.
			// So, we do store the marked up string in stateDocx
			// stateChunks, so we can perform the following test.

			if (currentStateChunk.getXml().equals(stateDocxSC.getMarkedUpSdt())) {
				log.debug("Match on marked up versions");
				return false;
			} else {
				log.debug("Still different!");
				log.debug("stateDocx marked up: "
						+ stateDocxSC.getMarkedUpSdt());
				log.debug("current : " + currentStateChunk.getXml());
				return true;
			}
		} else {
			return false;
		}

	}

	private boolean matchedOnMarkedUpVersion(StateChunk currentStateChunk,
			StateChunk stateDocxSC) {

		boolean matched = currentStateChunk.getXml().equals(stateDocxSC.getMarkedUpSdt());

		log.debug("matchedOnMarkedUpVersion(): currentStateChunk = " 
			+ currentStateChunk.getXml());
		log.debug("matchedOnMarkedUpVersion(): stateDocxSC.getMarkedUpSdt() = "
			+ stateDocxSC.getMarkedUpSdt());
		log.debug("matchedOnMarkedUpVersion(): matched = " + matched);
		
		return matched;
	}

	private int updateStartOffset;

	public int getUpdateStartOffset() {
		return this.updateStartOffset;
	}

	public void setUpdateStartOffset(int i) {
		this.updateStartOffset = i;
	}

	private int updateEndOffset;

	public int getUpdateEndOffset() {
		return this.updateEndOffset;
	}

	public void setUpdateEndOffset(int i) {
		this.updateEndOffset = i;
	}

	HashMap<String, TrackedChangeType> sdtChangeTypes = new HashMap<String, TrackedChangeType>();

	HashMap<String, String> sdtIdUndead = new HashMap<String, String>();

	public enum TrackedChangeType {
		Conflict, OtherUserChange, NA
	}

	public boolean hasNonConflictingChanges() {
		return this.sdtChangeTypes
				.containsValue(TrackedChangeType.OtherUserChange);
	}

	public TrackedChangeType getTrackedChangeType(String sdtBlockId) {
		return this.sdtChangeTypes.get(sdtBlockId);
	}

	public TrackedChangeType removeTrackedChangeType(String sdtBlockId) {
		return this.sdtChangeTypes.remove(sdtBlockId);
	}

	public List<String> getIdsOfNonConflictingChanges() {
		List<String> nonConflictingChanges = new ArrayList<String>();

		Iterator<Map.Entry<String, TrackedChangeType>> it = this.sdtChangeTypes
				.entrySet().iterator();
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

	/***************************************************************************
	 * TRANSMIT LOCAL CHANGES
	 * ****************************************************************************************
	 */
	public void transmitLocalChanges() throws RemoteException, ClientException {
		// Look for local modifications
		// - commit any which are non-conflicting (send these as TRANSFORMS)
		transmitContentUpdates();

		// transmitStyleUpdates();

		// Globals.ThisAddIn.Application.StatusBar = "Local changes
		// transmitted";
	}

    void transmitContentUpdates() throws RemoteException, ClientException {
	
		log.debug(stateDocx.getDocID() + ".. .. transmitContentUpdates");
	
		// The list of transforms to be transmitted
		List<T> transformsToSend = new ArrayList<T>();
	
	    /*
	     * Handle remote deleted chunks.
	     * 
	     * These are marked up locally as deleted.
	     * 
	     * If the user accepts the deletions, we are left with an
	     * empty content control (contains only whitespace), 
	     * which needs to be removed.
	     * 
	     * If the user rejected the deletions, the control will be
	     * sent as an insert.
	     * 
	     * If the control still contains markup, we have an interesting
	     * problem.  The problem is that we don't want to transmit it
	     * as an insertion, but the position of other local moves &
	     * inserts will still be calculated counting this one, which the 
	     * server doesn't know about.
	     * 
	     * Choices:
	     * 1. refuse to transmit any moves/inserts, until changes handled
	     * 2. exclude it from local Skeleton, so it isn't counted in
	     * moves/inserts
	     * 3. (NO: accept changes and transmit it as an insert)
	     * 
	     * Go with option 2.
	     * 
	     * [Consider the situation when *applying* a later round of remote 
	     *  changes.  I think that is ok, since the position of 
	     *  moves and inserts already adjusts to accommodate local
	     *  differences.]
	     * 
	     * How to implement?
	     * 
	     * We need to be able to reliably identify an 'undead' chunk,
	     * so that:
	     * (i) can differentiate from a new sdt containing only
	     *     whitespace,
	     * (ii)can differentiate from some other sdt containing 
	     *     markup, which DOES need to be included in the Skeleton
	     * 
	     * So we need a dictionary of the undead, which we process here.
	     */
	    WordMLDocument doc = getWordMLDocument();
	    
	    List<String> bornAgain = new ArrayList<String>();
	    
	    //Build an iterator for iterating applied TransformDelete(s).
	    //See:applyUpdate(TransformAbstract) method
	    Iterator<Map.Entry<String, String>> it = 
	    	this.sdtIdUndead.entrySet().iterator();
	    while (it.hasNext()) {
	    	Map.Entry<String, String> entry = it.next(); 
	    	StateChunk currentChunk = Util.getStateChunk(doc, entry.getKey());
	    	if (currentChunk == null) {
	    		//The applied TransformDelete must have been accepted.
	            log.debug(".. really kill.");
	            // All we do here is remove it from sdtIdUndead
	            bornAgain.add(entry.getKey());
	            
	    	} else if (currentChunk.containsTrackedChanges()) {
	            // Remove it from inferredSkeleton and currentStateChunks
	            // but keep in document.
	            log.debug(".. still in limbo.");
	            this.currentClientSkeleleton.removeRib(
	            	new TextLine(entry.getKey()));
	            
	        } else {
	            // The delete was  rejected
	            // (in which case this content control lives again)
	
	            log.debug(".. reincarnating.");
	            // Simply remove from sdtIdUndead
	            bornAgain.add(entry.getKey());
	
	            // .. so it will be treated like any other
	            // new sdt (though the server will recognise
	            // this one has existed before)
	    	}
	    }// while (it.hasNext())
	    
	    for (String s: bornAgain) {
	        this.sdtIdUndead.remove(s);
	    }
	
	    // Identify structural changes (ie moves, inserts, deletes)
	    // If skeletons are different, there must be local changes 
	    // which we need to transmit
	
	    /* For example
	
	        1324568180  1324568180 (no change)
	        1911345834  1911345834 (no change)
	        --- 293467343 not at this location in source  <---- if user deletes
	        884169107   884169107 (no change)
	        528989532   528989532 (no change)
	     */
	
	    String serverSkeletonStr = ws.getSkeletonDocument(stateDocx.getDocID());
	    Skeleton serverSkeleton = new Skeleton(serverSkeletonStr);
	
	    // OK, compare the inferredSkeleton to serverSkeleton
	    createTransformsForStructuralChanges(
	    	transformsToSend,
	        this.currentClientSkeleleton, 
	        serverSkeleton);
	
	    // Hmm, what if something is only detected as moved, because a
	    // recent remote change has not been applied?  Then we will
	    // be moving it back to where it was before!
	    // One way around this would be to require that all remote moves
	    // have been applied, before any local changes can be transmitted.
	
	    Boolean someConflicted = false;
	
		org.plutext.transforms.ObjectFactory transformsFactory = 
			new org.plutext.transforms.ObjectFactory();

		WordMLDocument wordMLDoc = getWordMLDocument();
		DocumentElement root = (DocumentElement) wordMLDoc
				.getDefaultRootElement();

		for (int idx = 0; idx < root.getElementCount(); idx++) {
			DocumentElement elem = (DocumentElement) root.getElement(idx);
			ElementML ml = elem.getElementML();
			if (ml instanceof SdtBlockML) {
				org.docx4j.wml.SdtBlock sdt = (org.docx4j.wml.SdtBlock) ml
						.getDocxObject();
				StateChunk chunkCurrent = new StateChunk(sdt);
				String sdtId = chunkCurrent.getIdAsString();

				StateChunk chunkOlder = stateDocx.getStateChunks().get(sdtId);

				if (chunkOlder == null) {
					log.debug("Couldn't find " + sdtId + " .. Shouldn't happen!?");
					continue;
					
				} else if (chunkCurrent.getXml().equals(chunkOlder.getXml())
							|| chunkCurrent.getXml().equals(chunkOlder.getMarkedUpSdt())) {
					continue;
				}

				log.debug("textChanged:");
				log.debug("FROM " + chunkOlder.getXml());
				log.debug("");
				log.debug("TO   " + chunkCurrent.getXml());
				log.debug("");
				
	            // If we get this far, it is an update
	            // We don't need to worry about the possibility that it has
	            // changed remotely, since we checked all updates
	            // on server had been applied before entering this method.
	
	            if (chunkCurrent.containsTrackedChanges())
	            {
	                // This is a conflicting update, so don't transmit ours.
	                // Keep a copy of what this user did in StateChunk
	                // (so that 
	
	                log.debug("Conflict! Local edit " + sdtId + " not committed.");
	                someConflicted = true;
	                continue;
	            }
			
				// TransformUpdate tu = new TransformUpdate();
				// tu.attachSdt(chunkCurrent.getXml() );
				// tu.setId( chunkCurrent.getId() );
				// transformsToSend.add(tu);

				T t = transformsFactory.createTransformsT();
				t.setOp("update");
				t.setIdref(chunkCurrent.getId().getVal().longValue());
				t.setSdt(chunkCurrent.getSdt());
				transformsToSend.add(t);
			}
		}// for (idx) loop
	    
	    if (transformsToSend.isEmpty()) {
	    	log.debug("Nothing to send.");
	    	
			if (someConflicted) {
				throw new ClientException("There was one or more conflicts.");
			}
	    	return;
	    }
	    
		String checkinComment = null;
        if (stateDocx.getPromptForCheckinMessage()) {
        	java.awt.Frame frame = 
        		(java.awt.Frame)
        			SwingUtilities.getWindowAncestor(getWordMLTextPane());
			CheckinCommentDialog d = new CheckinCommentDialog(frame);
			d.pack();
			d.setLocationRelativeTo(frame);
			d.setVisible(true);

			checkinComment = d.getTextComment();
        } else {
            checkinComment = "edited";
        }

		// Ok, now send what we have
		Transforms transforms = transformsFactory.createTransforms();
		transforms.getT().addAll(transformsToSend);
		boolean suppressDeclaration = true;
		boolean prettyprint = false;
		String transformsString = org.docx4j.XmlUtils.marshaltoString(
				transforms, suppressDeclaration, prettyprint,
				org.plutext.Context.jcTransforms);

		log.debug("TRANSMITTING " + transformsString);

		String[] result = ws.transform(stateDocx.getDocID(), transformsString,
				checkinComment);

		log.debug("Checkin also returned results");

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
		Boolean localTrue = true; // means it wouldn't be treated as a conflict
		Boolean updateHighestFetchedFalse = false;

		// Handle each result appropriately
		int i = 0;
		for (T t : transformsToSend) {
			log.debug(t.getIdref() + " " + t.getOp() + " result " + result[i]);

			// Primarily, we're expecting sequence numbers

			// At present, op="update" returns a transform
			// but it should be no different to what we sent
			// except that its tag is updated
			/*
			 * When registering these transforms, don't update highest fetched,
			 * because other clients could have transmitted changes to the
			 * server while this method was running, and we wouldn't want to
			 * miss those changes.
			 */
			
			if (result[i].contains("xmlns")) {
				StringBuffer sb = new StringBuffer();
				sb.append("<p:transforms xmlns:p='");
				sb.append(Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
				sb.append("'>");
				sb.append(result[i]);
				sb.append("</p:transforms>");

				// getContentControlWithId(ta.getId().getVal().toString() ).Tag
				// = ta.getTag();
				org.plutext.transforms.Transforms transformsObj = null;
				try {
					Unmarshaller u = Context.jcTransforms.createUnmarshaller();
					u
							.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
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
						updateLocalContentControlTag(ta.getId().getVal()
								.toString(), ta.getTag());
						this.stateDocx.getStateChunks().put(
								ta.getId().getVal().toString(),
								new StateChunk(ta.getSdt()));
					} else {
						// Assumption is that chunking is done locally,
						// and we won't get eg an Insert back
						log.error("Not handled: " + ta.getClass().getName());
					}
				}

				registerTransforms(transformsObj, appliedTrue, localTrue,
						updateHighestFetchedFalse);

			} else if (Integer.parseInt(result[i]) > 0) {
				TransformAbstract ta = 
					org.plutext.client.wrappedTransforms.TransformHelper.construct(t);
				ta.setSequenceNumber(Integer.parseInt(result[i]));
				registerTransform(ta, appliedTrue, localTrue,
						updateHighestFetchedFalse);
			} else {
                // If result was 0, the server has decided
                // this transform is redundant, and thus discarded
                // (and not allocated a sequence number).
                // This happens with a Move to the same position.

                // Do nothing.
			}

			i++;
		}

		if (someConflicted) {
			throw new ClientException("There was one or more conflicts.");
		}
	
	}

	void createTransformsForStructuralChanges(
		List<T> transformsToSend,
		Skeleton inferredSkeleton, 
		Skeleton serverSkeleton) {
		
		org.plutext.transforms.ObjectFactory transformsFactory = new org.plutext.transforms.ObjectFactory();

		DiffEngine de = new DiffEngine();
		de.processDiff(inferredSkeleton, serverSkeleton);

		ArrayList<DiffResultSpan> diffLines = de.getDiffLines();

		/*
		 * Detect moves
		 * 
		 * In order to detect moves, we have to be able to identify whether a
		 * delete has a corresponding insert (and vice versa).
		 * 
		 * These HashMap objects facilitate this.
		 * 
		 */
		HashMap<String, Integer> notHereInDest = new HashMap<String, Integer>();
		HashMap<String, Integer> notHereInSource = new HashMap<String, Integer>();
		// Populate the dictionaries
		int insertPos = -1;
		int i;
		log.debug("\n\r");
		for (DiffResultSpan drs : diffLines) {
			switch (drs.getDiffResultSpanStatus()) {
			case DELETE_SOURCE:
				for (i = 0; i < drs.getLength(); i++) {
					insertPos++;
					// Must be a new local insertion
					log.debug(insertPos
							+ ": "
							+ ((TextLine) inferredSkeleton.getByIndex(drs
									.getSourceIndex()
									+ i)).getLine()
							+ " not at this location in dest");
					String insertionId = ((TextLine) inferredSkeleton
							.getByIndex(drs.getSourceIndex() + i)).getLine();
					notHereInDest.put(insertionId, insertPos);
				}

				break;
			case NOCHANGE:
				for (i = 0; i < drs.getLength(); i++) {
					insertPos++;
					log.debug(insertPos
							+ ": "
							+ ((TextLine) inferredSkeleton.getByIndex(drs
									.getSourceIndex()
									+ i)).getLine()
							+ "\t"
							+ ((TextLine) serverSkeleton.getByIndex(drs
									.getDestIndex()
									+ i)).getLine() + " (no change)");

					// Nothing to do
				}

				break;
			case ADD_DESTINATION:
				for (i = 0; i < drs.getLength(); i++) {
					// insertPos++; // Not for a delete
					log.debug(insertPos
							+ ": "
							+ ((TextLine) serverSkeleton.getByIndex(drs
									.getDestIndex()
									+ i)).getLine()
							+ " not at this location in source");
					String deletionId = ((TextLine) serverSkeleton
							.getByIndex(drs.getDestIndex() + i)).getLine();
					notHereInSource.put(deletionId, insertPos);

				}

				break;
			}
		}

		Divergences divergences = new Divergences(de);

		log.debug("\n\r");

		// How to make the dest (right) like the source (left)

		for (DiffResultSpan drs : diffLines) {
			switch (drs.getDiffResultSpanStatus()) {
			case DELETE_SOURCE: // Means we're doing an insertion
				for (i = 0; i < drs.getLength(); i++) {
					String insertionId = ((TextLine) inferredSkeleton
							.getByIndex(drs.getSourceIndex() + i)).getLine();
					log
							.debug(insertPos
									+ ": "
									+ insertionId
									+ " is at this location in src but not dest, so needs to be inserted");

					Integer dicVal = notHereInSource.get(insertionId);

					if (dicVal == null) {

						// Just a new local insertion

						long adjPos = divergences
								.getTargetLocation(insertionId);
						log.debug("Couldn't find " + insertionId
								+ " so inserting at " + adjPos);

						divergences.insert(insertionId); // change +1 to 0

						divergences.debugInferred();

						WordMLDocument doc = getWordMLDocument();
						StateChunk sc = Util.getStateChunk(doc, insertionId);
						//Mediator.cs needs to call sc.setNew(true)
						//because when pasting MS-Word UI preserves 
						//pre-existing tag value and Mediator.cs has
						//to create a 'new' StateChunk whose tag value is 0 (zero)
						//sc.setNew(true);
						
						// TransformInsert ti = new TransformInsert();
						// ti.setPos( Integer.toString(adjPos) );
						// ti.setId( sc.getId() );
						// ti.attachSdt(sc.getXml());
						// transformsToSend.add(ti);

						T t = transformsFactory.createTransformsT();
						t.setOp("insert");
						t.setPosition(adjPos);
						t.setIdref(sc.getId().getVal().longValue());
						t.setSdt(sc.getSdt());
						transformsToSend.add(t);

						this.stateDocx.getStateChunks().put(sc.getIdAsString(),
								sc);

						log.debug("text Inserted:");
						log.debug("TO   " + sc.getXml());
						log.debug("");

					} else {

						// there is a corresponding delete, so this is really a
						// move
						log.debug("   " + insertionId + " is a MOVE");

						// if (toPosition[insertionId] ==
						// divergences.currentPosition(insertionId))
						// //rhsPosition[insertionId])
						// {
						// // currentPosition is the position in the inferred
						// point-in-time
						// // server skeleton (ie as it would be with transforms
						// // generated so far applied)

						// log.debug("Discarding <transform op=move id=" +
						// insertionId + " pos=" + toPosition[insertionId]);
						// }
						// else
						// {

						/*
						 * Semantics of move will be as follows:
						 * 
						 * (i) removed the identified item,
						 * 
						 * (ii) then insert the new item at the specified
						 * position.
						 * 
						 * This way, the position you specify is the position it
						 * ends up in (ie irrespective of whether the original
						 * position was earlier or later).
						 */

						// therefore:
						// delete first (update divergences object)
						divergences.delete(insertionId); // remove -1

						long adjPos = divergences
								.getTargetLocation(insertionId);

						log.debug("<transform op=move id=" + insertionId
								+ "  pos=" + adjPos);

						divergences.insert(insertionId); // change +1 to 0

						divergences.debugInferred();

						log.debug("<transform op=move id=" + insertionId
								+ "  pos=" + adjPos);

						WordMLDocument doc = getWordMLDocument();
						StateChunk sc = Util.getStateChunk(doc, insertionId);

						// TransformMove tm = new TransformMove();
						// tm.setPos( Integer.toString(adjPos) );
						// tm.setId ( sc.getId() );
						// //tm.attachSdt(sc.Xml);
						// transformsToSend.add(tm);

						T t = transformsFactory.createTransformsT();
						t.setOp("move");
						t.setPosition(adjPos);
						t.setIdref(sc.getId().getVal().longValue());
						// t.setSdt( sc.getSdt() );
						transformsToSend.add(t);

						log.debug("text moved:");

						// if (rawPos + adjPos ==
						// divergences.currentPosition(insertionId))
						// {`
						// log.debug(".. that transform could be DISCARDED.");
						// }

						// divergences.move(insertionId, rawPos + adjPos);
						// }
					}
				}

				break;
			case NOCHANGE:
				for (i = 0; i < drs.getLength(); i++) {

					log.debug(insertPos
							+ ": "
							+ ((TextLine) inferredSkeleton.getByIndex(drs
									.getSourceIndex()
									+ i)).getLine()
							+ "\t"
							+ ((TextLine) serverSkeleton.getByIndex(drs
									.getDestIndex()
									+ i)).getLine() + " (no change)");

				}

				break;
			case ADD_DESTINATION:
				for (i = 0; i < drs.getLength(); i++) {
					String deletionId = ((TextLine) serverSkeleton
							.getByIndex(drs.getDestIndex() + i)).getLine();
					log
							.debug(insertPos
									+ ": "
									+ deletionId
									+ " present at this location in dest but not source, so needs to be deleted");

					Integer dicVal = notHereInDest.get(deletionId);

					if (dicVal == null) {
						// Just a new local deletion

						log.debug("Couldn't find " + deletionId
								+ " so deleting");
						divergences.delete(deletionId);

						divergences.debugInferred();

						// TransformDelete td = new TransformDelete(deletionId);
						// transformsToSend.add(td);

						T t = transformsFactory.createTransformsT();
						t.setOp("delete");
						t.setIdref(Long.parseLong(deletionId));
						// t.setSdt( sc.getSdt() );
						transformsToSend.add(t);

						this.stateDocx.getStateChunks().remove(deletionId);

						log.debug("text deleted:");

					} else {
						// there is a corresponding insert, so this is really a
						// move
						log.debug("   " + deletionId
								+ " is a MOVE to elsewhere (" + dicVal + ")");
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
			// stateDocx.TSequenceNumberHighestSeen =
			// Int32.Parse(ws.style(stateDocx.DocID, newStyles));
			String[] result = { "", "" };

			// TODO - call transforms
			// result = ws.style(stateDocx.getDocID(), newStyles);

			log.debug(result[1]);

			Boolean appliedTrue = true; // Don't have to do anything more
			Boolean localTrue = true;
			registerTransforms(result[1], appliedTrue, localTrue, false);
			// TODO, can't use that, since it automatically updates highest
			// fetched.

		}
	}

	private void updateLocalContentControlTag(String sdtId, Tag tag) {
		WordMLDocument doc = getWordMLDocument();
		DocumentElement elem = Util.getDocumentElement(doc, sdtId);

		log.debug("updateLocalContentControlTag(): elem=" + elem);
		log.debug("updateLocalContentControlTag(): tag param=" + tag.getVal());

		SdtBlockML ml = (SdtBlockML) elem.getElementML();
		ml.getSdtProperties().setTagValue(tag.getVal());
	}

	
}// Mediator class

