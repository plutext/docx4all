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

import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.state.ContentControlSnapshot;
import org.plutext.client.state.StateDocx;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;

/** This class is the real workhorse.  It handles content control events,
 *  and initiates appropriate web service calls in response to those events.
 */
public class ServerTo {
	private static Logger log = Logger.getLogger(ServerTo.class);

	// TEMP values for testing purposes
	// Ultimately, username and password will be the same as used
	// for the Webdav connection.
	protected static final String USERNAME = "admin";
	protected static final String PASSWORD = "admin";
	protected static final String docId = "/alfresco/plutextwebdav/User Homes/jharrop/Sunday13A.docx";

	private final StateDocx stateDocx;

	private final ServerFrom serverFrom;

	public ServerTo(WordMLTextPane textPane) {
		this.stateDocx = new StateDocx(textPane);
		this.serverFrom = new ServerFrom(textPane, stateDocx);
	}
	
	public void startSession() {
		log.debug("startSession(): Session STARTS...");
		log.debug("startSession(): Fetching updates...");
		serverFrom.fetchUpdates(); // TODO - eventually put this in a background thread		
	}
	
	public void endSession() {
		log.debug("endSession(): Session ENDS....");
	}
	
	/** When this docx4all user deletes a content control */
	public void userDeletesContentControl(SdtBlock cc) {

		try {
			log.debug("DELETE started: Id=" + cc.getSdtPr().getId().getVal()
					+ " - Tag=" + cc.getSdtPr().getTag().getVal());

			// Start a new session
			AuthenticationUtils.startSession(USERNAME, PASSWORD);
			PlutextService_ServiceLocator locator = new PlutextService_ServiceLocator(
					AuthenticationUtils.getEngineConfiguration());
			locator
					.setPlutextServiceEndpointAddress(org.alfresco.webservice.util.WebServiceFactory
							.getEndpointAddress()
							+ "/" + locator.getPlutextServiceWSDDServiceName());
			PlutextWebService ws = locator.getPlutextService();

			// Check this hasn't been updated on the server
			// in the meantime
			String t = null;
			try {
				t = ws.getChunk(stateDocx.getDocID(), Util.getChunkId(cc
						.getSdtPr().getId()), cc.getSdtPr().getTag().getVal());
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			if (!t.equals("")) {
				// Yes..so markup this cc with the newer
				// changes from the server
				log
						.warn("TODO - there are shared changes to this thing you have deleted!");
				log.debug("How to abort the local delete??");
				org.plutext.client.Merge.mergeUpdate(cc, t, serverFrom);

				return;
			} else {
				log.debug("No Updates to merge - delete proceeding..");
			}

			String[] result = { "", "" };
			try {
				result = ws.deleteChunk(stateDocx.getDocID(), Util
						.getChunkId(cc.getSdtPr().getId()));
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			log.debug("Checkin Deletion also returned result[1]: '" + result[1]);
			
			// Put this in the list of transforms (and set its status
			// to applied).  Currently we
			// only really care to know that this tsn has been applied,
			// but we put a copy of the actual transformation into the
			// collection for ease of debugging, and because later, 
			// that'll help set us up for Undo.
			serverFrom.registerTransforms(result[1], true);

			stateDocx.getControlMap().remove(cc.getSdtPr().getId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// End the current session
			AuthenticationUtils.endSession();
		}

	}

	public void userEntersContentControl(SdtBlock cc) {
		log.debug("ENTER started: Id=" + cc.getSdtPr().getId().getVal()
			+ " - Tag=" + cc.getSdtPr().getTag().getVal());

		stateDocx.setCurrentCC(cc);

		log.debug("invoking applyUpdates from _Enter handler");
		serverFrom.applyUpdates(null); // anywhere in the document, but nothing forced

		log.debug(".. finished Enter (" + cc.getSdtPr().getId().getVal());

	}

	public void userExitsContentControl(SdtBlock cc) {
		log.debug("EXIT started: Id=" + cc.getSdtPr().getId().getVal()
				+ " - Tag=" + cc.getSdtPr().getTag().getVal());

		stateDocx.setCurrentCC(null);

		// TODO - if this is a content control just created in docx4all
		// (ie the user was in an existing cc, and they hit return, and
		//  we are chunking on each block), then call a new web service
		// method (yet to be written), which inserts the CC.

		try {
			ContentControlSnapshot lastKnownState = stateDocx
					.getContentControlSnapshots().get(cc.getSdtPr().getId());

			// Start a new session
			AuthenticationUtils.startSession(USERNAME, PASSWORD);
			PlutextService_ServiceLocator locator = new PlutextService_ServiceLocator(
					AuthenticationUtils.getEngineConfiguration());
			locator
					.setPlutextServiceEndpointAddress(org.alfresco.webservice.util.WebServiceFactory
							.getEndpointAddress()
							+ "/" + locator.getPlutextServiceWSDDServiceName());
			PlutextWebService ws = locator.getPlutextService();

			// Are there newer changes already on the server
			// which we need to handle?
			
			log.debug("ws.getChunk( .., " + Util.getChunkId(cc
					.getSdtPr().getId()) + " , " + cc.getSdtPr().getTag().getVal() );
			
			String t = ws.getChunk(stateDocx.getDocID(), Util.getChunkId(cc
					.getSdtPr().getId()), cc.getSdtPr().getTag().getVal());
			
			log.debug(".. returned '" + t + "'");
			
			if (!t.equals("")) {
				// Yes..so markup this cc with the newer
				// changes from the server
				log.debug("Updates to merge: " + t);
				log.warn("mergeUpdate WON'T BE IMPLEMENTED FOR A WEEK OR SO.");
				int tsn = Merge.mergeUpdate(cc, t, serverFrom);

				Object o = stateDocx.getWrappedTransforms().get(
						new Integer(tsn));
				log.debug("o: " + o.getClass().getName());
				if (o == null) {
					// Look into this
					log.warn("Couldn't find object!");
				} else if (o.getClass().getName().equals("TransformDelete")) {
					// Continue - we're set up to reinstate this chunk
				} else {
					// For TransformUpdate, we want to let the use
					// accept/reject changes before committing
					return;
				} // TODO - other types

				log.debug("    _Exit >>>");
			} else {
				log.debug("No Updates to merge (ie local is current.");
			}

			// Checkin any styles altered in this chunk
			try {
				String newStyles = stateDocx.getStylesSnapshot()
						.identifyAlteredStyles(cc);
				if (!newStyles.equals("")) {
					log.debug("Committing new/updated styles" + newStyles);
					//stateDocx.TSequenceNumberHighestSeen = Int32.Parse(ws.style(stateDocx.DocID, newStyles));                    
					String[] result = { "", "" };
					result = ws.style(stateDocx.getDocID(), newStyles);

					// Put this in the list of transforms (and set its status
					// to applied).  Currently we
					// only really care to know that this tsn has been applied,
					// but we put a copy of the actual transformation into the
					// collection for ease of debugging, and because later, 
					// that'll help set us up for Undo.
					serverFrom.registerTransforms(result[1], true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Has the content control changed?
			//                String currentXML = ContentControlSnapshot.getContentControlXMLNormalised(cc);
			String currentXML = ContentControlSnapshot.getContentControlXML(cc);

			log.debug("Current: " + currentXML);

			log.debug("Last known: " + lastKnownState.getPointInTimeXml());

			if (!currentXML.equals(lastKnownState.getPointInTimeXml())) {
				log.debug("CC OpenXML changed!");
				log.debug("FROM " + lastKnownState.getPointInTimeXml());
				log.debug("");
				log.debug("TO   " + currentXML);
				log.debug("");
				lastKnownState.setDirty(true);
			}

			if (lastKnownState.getDirty()) {
				/*                int numParas = cc.Range.Paragraphs.Count;
				 * 
				 *                  // TODO: If there is just a single paragraph, we can do the checkin
				 *                  // in a separate thread.
				 * 
				 * 
				                diagnostics("New contents detected (" + numParas + ") : " + stateChunkOnExit.RangeText);

				                if (chunkingStrategy.Equals("EachBlock") && numParas > 1 )
				 */
				// Just check in the whole SDT
				// First, get the XML for the SDT
				String chunkXml = ContentControlSnapshot
						.getContentControlXML(cc);

				String[] result = { "", "" };
				if (stateDocx.getPromptForCheckinMessage()) {

					// TODO - checkin form in docx4all

					//                        formCheckin form = new formCheckin();
					//                        form.Text = "Changes to '" + plutextTabbedControl.TextBoxChunkDisplayName.Text + "'";
					//                        using (form)
					//                        {
					//                            if (form.ShowDialog(plutextTabbedControl) == DialogResult.OK)
					//                            {
					//                                //log.warn(form.textBoxChange.Text);
					//                                result = ws.checkinWithComment(stateDocx.DocID, getChunkId(cc.ID), chunkXml, form.textBoxChange.Text);
					//                            }
					//                        }
				} else {
					result = ws.checkinWithComment(stateDocx.getDocID(), Util
							.getChunkId(cc.getSdtPr().getId()), chunkXml,
							"edited"); // Comment is simply "edited"
				}

				/*                diagnostics("Count: " + cc.Range.Paragraphs.Count);
				                if (cc.Range.Paragraphs.Count > 1 && stateDocx.ChunkingStrategy.Equals("EachBlock")) // TODO:  AND Chunk on Each Para
				                {
				 */
				log.debug("Checkin also returned result[1]: '" + result[1]);

				/* We expect that the SDT in the document is the same as the
				 * SDT on the server (and as returned in the transform), except 
				 * that the version number of the server copy has been incremented.
				 * (The version number is stored in sdtPr/@tag)
				 * 
				 * (They will be the same, since docx4all (will) contains its own 
				 * logic for splitting a multi para cc (when we're chunking on 
				 * each block) into new cc's.)
				 * 
				 * Anyway, the local sdt/sdtPr/@tag needs to be updated.
				 * 
				 * There are various ways to do this, which all amount to much
				 * the same thing.
				 * 
				 * The first thought is to programmatically update @tag directly.
				 * But if you are going to inspect the sdt received from the 
				 * server to find its @tag, you may as well use TransformUpdate.
				 * 
				 * If you did this, you could set applied = true.
				 * 
				 * The alternative is to rely on serverFrom.applyUpdates.
				 * 
				 * If you do this, you need to ensure that the local changes
				 * aren't mistaken for further changes which need to be merged.
				 * 
				 * There are 2 ways to ensure this:
				 * 
				 * (i) forceApplicationToSdtIds (which is what we do in the
				 *     Word 2007 add in), or
				 *     
				 * (ii)update the snapshot of the content control 
				 * 
				 * We do (ii), since it doesn't require us to call 
				 * serverFrom.applyUpdates(forceApplicationToSdtIds, forceApplicationToSdtIds);
				 * here.
				 * 
				 * 
				 */

				boolean applied = false;
				serverFrom.registerTransforms(result[1], applied);
				
				// Update the snapshot of this cc
				stateDocx.setCurrentCC(cc);
				stateDocx.setCurrentCC(null);
				
				// Note that exit handler does not invoke applyUpdates 

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug(".. finished Exit (" + cc.getSdtPr().getId().getVal());
	}

} //ServerTo class

























