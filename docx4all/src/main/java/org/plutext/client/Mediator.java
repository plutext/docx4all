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

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.rpc.ServiceException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.util.AuthenticationDetails;
import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4all.swing.CheckinCommentDialog;
import org.docx4all.swing.FetchRemoteEditsWorker;
import org.docx4all.swing.TransmitLocalEditsWorker;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.FetchRemoteEditsWorker.FetchProgress;
import org.docx4all.swing.TransmitLocalEditsWorker.TransmitProgress;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.DocUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.vfs.WebdavUri;
import org.docx4all.xml.BodyML;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.jaxb.NamespacePrefixMappings;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.CommentsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.EndnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.FootnotesPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.wml.Tag;
import org.plutext.Context;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffResultSpan;
import org.plutext.client.partWrapper.SequencedPart;
import org.plutext.client.partWrapper.SequencedPartRels;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

	private static XPathExpression[] xpaths; 
	private static XPathExpression xpathRelTest; 
	
	static {
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath xPath = factory.newXPath();
		
		xPath.setNamespaceContext(new NamespacePrefixMappings());
				
		
		xpaths = new XPathExpression[4];
	    try {
			xpaths[0] =  xPath.compile(".//@r:embed | .//@r:link | .//@r:id | ./descendant::w:commentReference[1]");
			xpaths[1] =  xPath.compile(".//w:commentReference/@w:id | .//w:commentRangeStart/@w:id | .//w:commentRangeEnd/@w:id");
			xpaths[2] =  xPath.compile(".//w:footnoteReference/@w:id");
			xpaths[3] =  xPath.compile(".//w:endnoteReference/@w:id");
			
            // linked images, hyperlinks (and w:object/v:imagedata, w:object/o:OLEObject)			
			xpathRelTest = xPath.compile(" .//@r:link | .//@r:id ");
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		
	}

	/***************************************************************************
	 * SESSION MANAGEMENT 
	 ******************************************************
	 */
	
    AuthenticationDetails authDetails; 
	
	public void startSession() throws ServiceException {
		
		log.info("starting session");
		
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

            log.debug(uri.getUsername() );
            log.debug( java.net.URLDecoder.decode(uri.getUsername(), "UTF-8") );
            
            // Ensure a tenant user name appears as eg tester@public
            // rather than tester%40public
            
            AuthenticationUtils.startSession(java.net.URLDecoder.decode(uri.getUsername(),"UTF-8"), uri.getPassword());
            /*
             * NB - that stores the session in ThreadLocal<AuthenticationDetails> authenticationDetails.
             * 
             * The practical effect of this is that all web service calls must be from the
             * same thread! ie you can't do some in a background thread, and others in
             * the event dispatching thread.
             * 
             * A workaround for this is to store the AuthenticationDetails here, so that
             * they can be set on other threads as necessary.
             */
            authDetails = AuthenticationUtils.getAuthenticationDetails();
		      
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
					String sdtId = sdt.getSdtProperties().getPlutextId();
					TextLine rib = new TextLine(sdtId);
					currentClientSkeleleton.getRibs().add(rib);
				}
			}

		} catch (AuthenticationFault exc) {
			log.error(exc);
			throw new ServiceException("Service Connection failure.", exc);
		} catch ( Exception e) {
			log.error(e);
        	throw new ServiceException("Service Connection failure.", e);			
		}
	}

	public void endSession() {
		log.info("Ending session.");		
		
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

	public void fetchUpdates(FetchRemoteEditsWorker worker) throws RemoteException {
		
		worker.setProgress(FetchProgress.START_FETCHING, "Fetch updates");
		
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
                worker.setProgress(FetchProgress.REGISTERING_UPDATES, "Registering updates");
                
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
				
				if (needToFetchSkel || oldServer == null) {
                    worker.setProgress(
                    	FetchProgress.FETCHING_REMOTE_DOC_STRUCTURE, 
                    	"Fetching remote document structure");

					String serverSkeletonStr = ws.getSkeletonDocument(stateDocx
							.getDocID());
					oldServer = new Skeleton(serverSkeletonStr);
					
				}
				worker.setProgress(FetchProgress.FETCHING_DONE, "About to apply remote edits to local document");
			} else {
				worker.setProgress(FetchProgress.FETCHING_DONE, "No remote updates");
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
	
	public Map<String, Changeset> getChangeSets() {
		return changeSets;
	}
	
	/***************************************************************************
	 * APPLY REMOTE UPDATES
	 * ****************************************************************************************
	 */

	Divergences divergences = null;

	public Divergences getDivergences() {
		return divergences;
		// set { divergences = value; }
	}

	public void applyRemoteChanges(FetchRemoteEditsWorker worker) {
		// TODO: grey out if there are no remote updates to apply

		//Too close to the next message. Therefore, comment this.
		//worker.setProgress(FetchProgress.START_APPLYING_UPDATES, "Start to apply remote edits");

		if (this.oldServer == null 
			|| this.changeSets == null
			|| this.changeSets.isEmpty()) {
			//applyRemoteChanges() is preceded with fetchUpdates().
			//If fetchUpdates() ends up with error or does not
			//fetch any new transform then nothing to be applied
			//in this method.
			worker.setProgress(FetchProgress.APPLYING_DONE, "No remote updates");
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
		
		worker.setProgress(
			FetchProgress.COMPARING_DOC_STRUCTURES, 
			"Making allowances for local differences");
		DiffEngine drift = new DiffEngine();
		drift.processDiff(this.oldServer, this.currentClientSkeleleton);
		divergences = new Divergences(drift);

		/*
		 * For example
		 * 
		 * 1324568180 1324568180 (no change)
		 * 1911345834 1911345834 (no change)
		 * ---        293467343   not at this location in source <---- if user deletes
		 * 884169107  884169107  (no change) 528989532 528989532 (no change)
		 */

		// }

		worker.setProgress(FetchProgress.APPLYING_UPDATES, "Applying updates");
		
	    fetchParts[PART_RELS] = false;
	    fetchParts[PART_COMMENTS] = false;
	    fetchParts[PART_FOOTNOTES] = false;
	    fetchParts[PART_ENDNOTES] = false;
	    // applyUpdates may tell us otherwise ...

	    changedChunks = new HashMap<String, String>();		
		
		applyUpdates(worker);
		
	    // update references to other parts
	    //
	    // if any of the transforms we just applied insert | update another part, 
	    if (fetchParts[PART_RELS]
	        | fetchParts[PART_COMMENTS]
	        | fetchParts[PART_FOOTNOTES]
	        | fetchParts[PART_ENDNOTES])
	    {
	        try {
	        	//log.error("Related parts need updating..");
	        	updateRelatedParts(worker);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
		
		this.oldServer = null;
	}
	
	static Templates xsltReferenceMap;		
	
	/// <summary>
	/// If any of the transforms we just applied insert/update another part, then we need to do
	/// two things:
	/// 1. update that part (ie re-create it, out of the local version and the one on the server)
	/// 2. renumber the id's in the document (in document order) and the composite part, so they match
	/// </summary>
	/// <param name="pkg"></param>
	/// <param name="currentStateChunks"></param>
	/// <param name="bw"></param>
	public void updateRelatedParts(FetchRemoteEditsWorker worker) throws Exception
	{
	    worker.setProgress(FetchProgress.LINKS, "Links");

	    // 1. update that part

	    // array of part names we need
	    // how many to get?
	    int max = 0;
	    for (int i = 0; i < 4; i++)
	    {
	        if (fetchParts[i])
	        {
	            max++;
	        }
	    }
	    String[] partNames = new String[max];
	    String[] parts = new String[max];
	    int j = 0;
	    HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
	    for (int i = 0; i < 4; i++)
	    {
	        if (fetchParts[i])
	        {
	        	log.debug("Will fetch " + fetchParts[i]);
	            partNames[j] = SequencedPart.getSequenceableParts().get(i);
	            mapping.put(j, i);
	            j++;
	        }
	    }


	    // invoke web service - returns the part and its version number.
	    log.debug("invoking web service");
	    //this.startSession();
	    
	    // Since this method updateRelatedParts is performed
	    // in the event dispatching thread, as opposed to
	    // Swing Worker's background thread, we need to 
	    // let this thread know the Auth details..
	    AuthenticationUtils.setAuthenticationDetails(authDetails);
	    
	    String[][] weirdParts = ws.getParts(stateDocx.getDocID(), partNames);

	    log.debug("number of weird parts: " + weirdParts.length);

	    SequencedPart[] serverSequencedParts = new SequencedPart[max];
	    for (int i = 0; i < max; i++)
	    {
	        String[] itemField = weirdParts[i];

	        log.debug("weirdParts[" + i + "] length = " + itemField.length );

	        if (itemField[1].equals(""))
	        {
	            // Part doesn't exist on server
	            // This should not happen.

	            log.error(i + " : " + SequencedPart.getSequenceableParts().get(mapping.get(i)) 
	            		+ " doesn't exist on server!  INVESTIGATE");

	        }
	        else
	        {

	            SequencedPart sp = (SequencedPart)org.plutext.client.partWrapper.Part.factory(
	            		itemField[1] );
	            serverSequencedParts[i] = sp;
	            log.debug("Set serverSequencedParts[" + i );
	                        

	            // In anticipation of success in what follows, 
	            // set the version of this part in StateDocx 
	            stateDocx.getPartVersionList().setVersion( sp.getName(), itemField[0]);
	            //sp.Version = itemField[0];

	            // and update our record of the part in StateDocx
	            // (since any change from this is something we want to transmit)
	            stateDocx.getParts().put(sp.getName(), sp);

	        }
	    }

	    // .. and the corresponding local parts.
	    // We want to work with the ones corresponding to the current state of the
	    // document, not its last recorded state (which would be stateDocx)
	    HashMap<String, org.plutext.client.partWrapper.Part> localParts 
	    	= Util.extractParts( getWordMLDocument() );
	    
	    SequencedPart[] localSequencedParts = new SequencedPart[max];
	    for (int i = 0; i < max; i++)
	    {
	        String partName = serverSequencedParts[i].getName();

	        if (partName.equals("/word/_rels/document.xml.rels"))
	        {
	            //localSequencedParts[i] = (SequencedPartRels)stateDocx.Parts[partName];
	            localSequencedParts[i] = (SequencedPartRels)localParts.get(partName);
	            log.debug("Set localSequencedParts[" + i + " as rels");
	            
	        }
	        else
	        {
	                //localSequencedParts[i] = (SequencedPart)stateDocx.Parts[partName];
                localSequencedParts[i] = (SequencedPart)localParts.get(partName);
                if ( localSequencedParts[i]==null )
	            {

	                // So, the part doesn't yet exist locally.
	                // However, if the local document does not already contain
	                // a comment, footnote or endnote,
	                // we will need to add it.

	                // Rather than fetch it again,
	                // We can just say:
	                localSequencedParts[i] = serverSequencedParts[i];
	                // (in which case the numbers are going to be aligned, and
	                //  much of the following code would be unnecessary in that case;
	                //  however, it does take care of adding the part for us...)
		            log.debug("Set localSequencedParts[" + i + " using server part. ");	            	
	            } else {
		            log.debug("Set localSequencedParts[" + i + " ");	            	
	            }
	        }
	        if ( localSequencedParts[i]==null ) {
	        	log.error("but it is null!!");
	        }
	      }



	    // construct composite part(s)

	    // so here's the tricky bit
	    /* for each of the parts we are dealing with,
	    // we need to go through the document, and make a list of the correct
	    // references:  
	     *  - Sdt's which we didn't update, will reference the existing part
	     *  - Sdt's which we inserted/updated will reference the part we just fetched.
	    */

	    // Go through the document ..
	    
	    // Trick here is to run a single transform which gives us
	    // the data we want
	    
	    WordMLDocument doc = getWordMLDocument();
 		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();    	
 		WordprocessingMLPackage wmlp = 
    		((DocumentML) root.getElementML()).getWordprocessingMLPackage();
		MainDocumentPart mdp = wmlp.getMainDocumentPart();
		
		org.w3c.dom.Document mdpW3C = XmlUtils.marshaltoW3CDomDocument( mdp.getJaxbElement() );
		
		if (xsltReferenceMap==null) {
			Source xsltSource  = new StreamSource( org.docx4j.utils.ResourceUtils.getResource("org/plutext/client/ReferenceMap.xslt"));
			xsltReferenceMap = XmlUtils.getTransformerTemplate(xsltSource);			
		}
		javax.xml.transform.dom.DOMResult domResult = new javax.xml.transform.dom.DOMResult();
		XmlUtils.transform(mdpW3C, xsltReferenceMap, null, domResult);
		
//		javax.xml.transform.stream.StreamResult debugStream =
//			new javax.xml.transform.stream.StreamResult(System.out);		
//		XmlUtils.transform(mdpW3C, xslt, null, debugStream);
		
	    /* Something like:
	     * 
	     * 
	            <ReferenceMap>
	                <sdt>
	                    <id>368753226</id>
	                    <rels />
	                    <comments />
	                    <footnotes />
	                    <endnotes />
	                </sdt>
	                <sdt>
	                    <id>770713813</id>
	                    <rels>
	                        <idref>rId4</idref>
	                    </rels>
	                    <comments />
	                    <footnotes />
	                    <endnotes />
	                </sdt>
	                <sdt>
	                    <id>1413643190</id>
	                    <rels>
	                        <idref>rId5</idref>
	                        <idref>rId6</idref>
	                        <idref>rId7</idref>
	                    </rels>
	                    <comments />
	                    <footnotes />
	                    <endnotes />
	                </sdt>
	     */

		//log.debug(domResult.getNode().getNodeName() ); --> #document		
		NodeList sdts = domResult.getNode().getFirstChild().getChildNodes();
		
		
	    // nb: std's aren't sdt's as such - see the example XML above.

	    ArrayList[] constructedContent = new ArrayList[max]; // or XmlNodeList[] ?

	    // for each of the parts we are dealing with, 
	    // get referenced objects from local | remote part as appropriate.
	    // We do this to get the reference content (as opposed to the ids of
	    // that referenced content, which we will renumber in due course).
	    for (int i = 0; i < max; i++)
	    {
	        String partName = serverSequencedParts[i].getName(); // as good a way as any to get the part name
	        log.debug("Constructing content for Part: " + i + " .. " + partName);
	        
	        constructedContent[i] = new ArrayList();

	        if (partName.equals("/word/footnotes.xml")
	                || partName.equals("/word/endnotes.xml"))
	        {
	            // footnotes & endnotes 0 & 1 are artificial;
	            // - add these; it doesn't matter whether we use the server or local copy
	            // We don't actually read these entries again, we just 
	            // have to fill the slots so later references to 
	            // (constructedContent[i])[k + 2] work!
	            log.debug("Adding  artificial entries.." );
	            constructedContent[i].add(
	                serverSequencedParts[i].getNodeByIndex(0));
	            constructedContent[i].add(
	                serverSequencedParts[i].getNodeByIndex(1));
	        }	        

	        //foreach (XmlNode sdt in sdts)
            for (int nli=0 ; nli < sdts.getLength() ; nli++ )
            {
            	
            	Node sdt = sdts.item(nli);
	            //log.debug(sdt.getNodeName());
	            //log.debug(sdt.getFirstChild().getNodeName());
            	
	            String sdtId = sdt.getFirstChild().getFirstChild().getNodeValue();
	            	// getFirstChild().getNodeValue() is the value of the #text child
	            //log.debug(sdtId);

                Object dummy = changedChunks.get(sdtId);
                if (dummy == null) {
                	
	                log.debug("id: " + sdtId + " - references local SequencedPart");

	                NodeList idrefs = sdt.getChildNodes().item(1 + mapping.get(i)).getChildNodes();
	                //foreach (XmlNode idref in sdt.ChildNodes[1 + mapping[i]])
	                for (int nl2i=0 ; nl2i < idrefs.getLength() ; nl2i++ )
	                {
	                	
	                	Node idref = idrefs.item(nl2i); 
	                
	                    // An Sdt which we didn't update, will reference the existing part
	                    if ( localSequencedParts[i] instanceof SequencedPartRels )
	                    {
	                        constructedContent[i].add(
	                            ((SequencedPartRels)localSequencedParts[i]).getNodeById(
	                                idref.getFirstChild().getNodeValue()).cloneNode(true));
	                    }
	                    else
	                    {
	                        constructedContent[i].add(
	                            localSequencedParts[i].getNodeByIndex(
	                            		Integer.parseInt(idref.getFirstChild().getNodeValue())
	                                ));
	                        log.debug("Added to constructedContent[" + i);
	                    }
	                }
                } else {

	                // An Sdt which we inserted/updated will reference the part we just fetched
	                // (assuming it contains rel idrefs).
	                // If we didn't just insert/update the sdt, we branch to KeyNotFoundException

	                log.debug("id: " + sdtId + " - references serverSequencedPart");
	                NodeList idrefs = sdt.getChildNodes().item(1 + mapping.get(i)).getChildNodes();
	                //foreach (XmlNode idref in sdt.ChildNodes[1 + mapping.get(i)])
	                for (int nl2i=0 ; nl2i < idrefs.getLength() ; nl2i++ )
	                {
	                	
	                	Node idref = idrefs.item(nl2i); 
	                    if ( serverSequencedParts[i] instanceof SequencedPartRels )
	                    {
	                        constructedContent[i].add(
	                            ((SequencedPartRels)serverSequencedParts[i]).getNodeById(
	                                idref.getFirstChild().getNodeValue()).cloneNode(true));
	                        // Clone, so that if there 2 references to the same image,
	                        // we get distinct copies of the rel node, which we can 
	                        // number as we choose.  Without the distinct copies,
	                        // when we number the images in the document sequentially,
	                        // there is no corresponding rel for the second image.
	                    }
	                    else
	                    {
	                        constructedContent[i].add(
	                            serverSequencedParts[i].getNodeByIndex(
	                                Integer.parseInt(idref.getFirstChild().getNodeValue())
	                                ));
	                        log.debug("Added to constructedContent[" + i);
	                    }
	                }

	            }
	        }
	    }

	    // ok, now we have the correct references in ArrayList[] constructedContent
	    // All we have to do is, for each of the relevant sequences (ie comments, rels etc):
	    // 2. renumber the id's in (i) document order and (ii) list, 
	    // and (iii) build an actual part

	    
	    // Use org.w3c.dom.Document mdpW3C	    
	    // .. here we want to be manipulating the 'live' document
	    // so when we are finished, we'll need to unmarshall that,
	    // then do whatever is necessary for docx4all to use the
	    // unmarshalled thing
	    

 // NB same order as Pkg.sequencableParts

	    // NB XPath spec says: the location path //para[1] does not mean the same as the 
	    // location path /descendant::para[1].
	    // The latter selects the first descendant para element; the former selects all descendant para 
	    // elements that are the first para children of their parents.


	    // Any @r:embed should only be temporary, since such images
	    // will be replaced with @r:link on transmit.

	    // Note: @r:id will match images, hyperlinks, object related stuff,
	    // and header/footerReference

	    Node rel_comment = null;
	    
	    
	    // We need the real underlying parts, so we can update them
	    // at the end of each loop
        HashMap<PartName, org.docx4j.openpackaging.parts.Part> docx4jParts 
        	= wmlp.getParts().getParts();
	    // The Parts list doesn't include rels parts,
	    // but we need "/word/_rels/document.xml.rels"
        // so add it
        RelationshipsPart relsPart = wmlp.getMainDocumentPart().getRelationshipsPart();
        docx4jParts.put(relsPart.getPartName(), relsPart);

	    // for each of the parts we are dealing with, 
	    for (int i = 0; i < max; i++)
	    {
	        String partName = serverSequencedParts[i].getName(); // as good a way as any to get the part name
	        // Get a NodeList of the id's in the document
	        // .. for this we need an XPath expression particular
	        // to that type ...
	        XPathExpression xpath = xpaths[mapping.get(i)];
	        log.debug("Renumbering for XPath: " + xpath.toString() + " .. (" + partName);
	        
	        // Note that for each iteration, we are intending to
	        // update the underlying main document part, 
	        // which we intend to have already been updated in the
	        // previous iteration. If this doesn't work, then
	        // first workaround is to explicitly replace mdpW3C
	        // at the end of each loop
	        NodeList nodeList = (NodeList)xpath.evaluate(mdpW3C, XPathConstants.NODESET);
	        
	        Boolean correctOffsetForCommentReference = false;

	        // Renumber
	        for (int k = 0; k < nodeList.getLength(); k++)
	        {
	            log.debug("Node k: " + k);

	            if (partName.equals("/word/_rels/document.xml.rels"))
	            {
	                // First, a sanity check
	                if (((SequencedPartRels)(serverSequencedParts[i])).getPrefixedRelsCount()
	                        != ((SequencedPartRels)(localSequencedParts[i])).getPrefixedRelsCount())
	                {
	                    log.error("Invalid assumption - prefixed rels have changed!");

	                    // dump the 2 parts..	                    
	                    log.debug( XmlUtils.w3CDomNodeToString(serverSequencedParts[i].getXmlNode() ));
	                    log.debug( XmlUtils.w3CDomNodeToString(localSequencedParts[i].getXmlNode() ));
	                }
	                // Hope the sanity check was ok; if it wasn't, its better to use the local _rels
	                // since its for that that we actually have the matching parts
	                // id is a 1-based index.
	                int idNum = k + 1 + ((SequencedPartRels)(localSequencedParts[i])).getPrefixedRelsCount();

	                log.debug(nodeList.item(k).getLocalName());
	                if (nodeList.item(k).getLocalName().equals("commentReference"))
	                // Name = w:commentReference, LocalName = commentReference
	                {
	                    // Special case - this is the first spot at which we
	                    // encounter a commentReference, so its the location in the rels part
	                    // at which we need to insert a reference to the comments part.

	                    // NB this commentReference is in nodeList, 
	                    // but (given our PkgToReferenceMap.xslt), it is
	                    // NOT in constructedcontent

	                    // Subsequent refs are incremented by 1 to take account of this
	                    // rel_comment_step = 1;
	                    // - no need for that: because it has a slot in
	                    // nodeList, it will be taken into account automatically.
	                    
	                    // But since the comment reference is in the NodeList,
	                    // but not constructedContent, subsequent iterations
	                    // will need to take this into account
	                    correctOffsetForCommentReference = true;

	                    // Renumber in the document
	                    // - not in this case!

	                    // But we will need a rel to comments with the correct id.
	                    // This isn't in constructed content (and nor are
	                    // any of the rels in FIXED_PARTS_PREFIX or FIXED_PARTS_SUFFIX),
	                    // so just note its id for now. We can add it at the end
	                    // of the rels part (because although we are taking care 
	                    // to get all the id's correct, they don't have to be written
	                    // in order)

	                    String rel_comment_id_new = "rId" + idNum;
	                    
	                    // WRONG! String rel_comment_id_old = nodeList[k].Attributes.GetNamedItem(Namespaces.WORDML_NAMESPACE, "id").Value;
	                    // log.Debug("rel_comment_id_old: " + rel_comment_id_old);

	                    rel_comment = ((SequencedPartRels)(localSequencedParts[i])).getNodeByType("comments");
	                    if (rel_comment == null)
	                    {
	                        // Comments part does not exist locally
	                        rel_comment = ((SequencedPartRels)(serverSequencedParts[i])).getNodeByType("comments").cloneNode(true);
	                    }
	                    else
	                    {
	                        // We know it exists, so let's use a clone of it
	                        rel_comment = ((SequencedPartRels)(localSequencedParts[i])).getNodeByType("comments").cloneNode(true);
	                    }
	                    rel_comment.getAttributes().getNamedItem("Id").setNodeValue(rel_comment_id_new);
	                }
	                else
	                {
	                    log.debug("Setting rId" + idNum );
	                    // Renumber in the document
	                    nodeList.item(k).setNodeValue( "rId" + idNum);

	                    // Number the constructed content the same
	                    Node n;
	                    if (!correctOffsetForCommentReference)
	                    {
	                        // Up to the point in the nodelist where
	                        // we encountered the single comment reference
	                        n = (Node)(constructedContent[i].get(k));
	                    }
	                    else
	                    {
	                        // After the comment reference
	                        n = (Node)(constructedContent[i].get(k-1));
	                    }
	                    n.getAttributes().getNamedItem("Id").setNodeValue("rId" + idNum);  // No Namespaces.WORDML_NAMESPACE

	                }
	            }
	            else if (partName.equals("/word/footnotes.xml")
	                || partName.equals("/word/endnotes.xml"))
	            {

	                // footnotes & endnotes 0 & 1 are artificial;
	                //    the first one in the document is #2 ...

	                // Renumber in the document
	                nodeList.item(k).setNodeValue( Integer.toString(k + 2));
	                /* The existing value should already have been changed to match
	                 * the value on the server, so any change is something to be
	                 * transmitted.
	                 * 
	                 * That is, if we renumber here (ie actually change the number 
	                 * to something different), we will need to transmit the change.
	                 * 
	                 * However, such a change will be detected in the transmit 
	                 * code (since this will be different to the statechunk we'll
	                 * be comparing it to), so nothing extra is required here.
	                 */ 

	                // Number the constructed content the same
	                Node n = (Node)constructedContent[i].get(k + 2);
	                n.getAttributes().getNamedItemNS(Namespaces.WORDML_NAMESPACE, "id").setNodeValue( Integer.toString(k + 2) );
	            }
	            else  // comments, and hmm, what might else this catch all catch?
	            {
	                // Each comment has 3 nodes:
	                /*
	                 * <w:commentRangeStart w:id="0" />
				        <w:r>
					        <w:t>Here</w:t>
				        </w:r>
				        <w:commentRangeEnd w:id="0" />
				        <w:r>
					        <w:rPr>
						        <w:rStyle w:val="CommentReference" />
					        </w:rPr>
					        <w:commentReference w:id="0" />
				        </w:r>
	                 */ 

	                // Renumber in the document - 3 times  
	                // FIXME: what if its not a comment??
	                int cid = (int)(k / 3);
	                log.debug("Comment @id: " + cid);
	                nodeList.item(k).setNodeValue( Integer.toString(cid) );

	                // Number the constructed content the same - once
	                if (cid == (k / 3))
	                {
	                    Node n = (Node)constructedContent[i].get(cid);
	                    n.getAttributes().getNamedItemNS(Namespaces.WORDML_NAMESPACE, "id").setNodeValue(
	                    		Integer.toString(cid));
	                }

	            }

	        }

	        // Build the part
	        // .. we have a list of nodes, some of which are foreign
	        // We need to attach them 
	        Node parent = localSequencedParts[i].getXmlNode();
	        Node listParent = parent.getFirstChild().getFirstChild();

	        if (partName.equals("/word/_rels/document.xml.rels"))
	        {
	            int localPrefixedRelsCount = ((SequencedPartRels)(localSequencedParts[i])).getPrefixedRelsCount();
	            int localSuffixedRelsCount = ((SequencedPartRels)(localSequencedParts[i])).getSuffixedRelsCount();
	            log.debug("localPrefixedRelsCount = " + localPrefixedRelsCount);
	            log.debug("localSuffixedRelsCount = " + localSuffixedRelsCount);

	            //log.debug(listParent.OuterXml);

	            // Sanity check - as good to do it here as anywhere
	            if (((SequencedPartRels)(serverSequencedParts[i])).getSuffixedRelsCount()
	                    != localSuffixedRelsCount)
	            {
	                log.error("Invalid assumption - suffixed rels have changed!");

	                //  dump the 2 parts?
	            }

	            // Keep FIXED_RELS_PREFIX and FIXED_RELS_SUFFIX,
	            // but Remove the other children 
	            // These *aren't stored in order*, so we can't just do:
	            // XmlNode deletion = listParent.ChildNodes[i2 - 1];
	            int relCount = listParent.getChildNodes().getLength();
	            for (int i2 = relCount; i2 > 0; i2--)
	            {

	                // extract number from rIdnn
	                String idtmp = listParent.getChildNodes().item(i2 - 1).getAttributes().getNamedItem("Id").getNodeValue();
	                int relId =  Integer.parseInt(idtmp.substring(3));

	                log.debug(idtmp + " ( " + listParent.getChildNodes().item(i2 - 1).getAttributes().getNamedItem("Target").getNodeValue());
	                if (relId <= localPrefixedRelsCount)
	                {
	                    // Its one of the FIXED_RELS_PREFIX,
	                    // so just keep it
	                    log.debug(relId + "<=" + localPrefixedRelsCount + "---> keeping");

	                }
	                else if (relId >
	                    (relCount - localSuffixedRelsCount))
	                {
	                    // Its one of the FIXED_RELS_SUFFIX,
	                    // so renumber (which is all we need to do with this)
	                    log.debug(relId + ">" + relCount + " - " + localSuffixedRelsCount);
	                    int offset = relId - (relCount - localSuffixedRelsCount);
	                    log.debug(localPrefixedRelsCount + " + " + (constructedContent[i]).size() + " + " + offset);

	                    int newnum = localPrefixedRelsCount
	                        + (constructedContent[i]).size()
	                        + offset;
	                    if (rel_comment != null)
	                    {
	                        newnum++;
	                    }

	                    listParent.getChildNodes().item(i2 - 1).getAttributes().getNamedItem("Id").setNodeValue(
	                        "rId" + newnum );
	                    log.debug("---> renumbered as " + newnum);

	                }
	                else
	                {
	                    // its one of the others, so delete it
	                    Node deletion = listParent.getChildNodes().item(i2 - 1);
	                    listParent.removeChild(deletion);
	                    log.debug("---> deleted");

	                }
	            }

	            if (rel_comment != null)
	            {
	                // We need a reference to the comments part;
	                // we can do it at the end;
	                // we have already given it its correct Id.
	                Node importedNode = parent.getOwnerDocument().importNode(rel_comment, true);  // pkgB.PkgXmlDocument.ImportNode(n, true);
	                listParent.appendChild(importedNode);
	            }
	        }
	        else
	        {
	            // Remove the children 
	            for (int i2 = listParent.getChildNodes().getLength(); i2 > 0; i2--)
	            {
	                log.debug(i2);
	                Node deletion = listParent.getChildNodes().item(i2 - 1);
	                listParent.removeChild(deletion);
	            }
	        }

	        for (int j2 = 0; j2 < (constructedContent[i]).size(); j2++)
	        {
	            log.debug(j2);
	            Node n = (Node)constructedContent[i].get(j2);
	            Node importedNode = parent.getOwnerDocument().importNode(n, true);  // pkgB.PkgXmlDocument.ImportNode(n, true);
	            listParent.appendChild(importedNode);
	        }
	        //log.Debug("RESULT: " + parent.OuterXml);

	        
	        // .. Get the part; (where the local part exists, the local SequencedPart
	        // object wraps it, so we can either get it that way, or go back to
	        // using the underlying package.  If the local part doesn't exist
	        // though, we'd have to go back to the underlying package to create
	        // it. So we may as well operate at that level right from the start...)
	        
	        org.docx4j.openpackaging.parts.Part p = docx4jParts.get( new PartName(partName) );
	        JaxbXmlPart jPart;
	        if (p == null ) {
	        	// This is a new part ie one which is present on the server,
	        	// but not available locally.
	        	// So we need to add it to the package. 
	        	
	        		log.error(partName + " does not exist .. creating");
	        		PartName pn = new PartName(partName);
	        	       if (partName.equals("/word/_rels/document.xml.rels")) {
	        	    	   jPart = new RelationshipsPart( pn );
	        	        } else if (partName.equals("/word/comments.xml")) {	
	        	        	jPart = new CommentsPart( pn );
	        	        } else if (partName.equals("/word/footnotes.xml")) {	        	        	
	        	        	jPart = new FootnotesPart( pn );	        	        	
	        	        } else if (partName.equals("/word/endnotes.xml")) {
	        	        	jPart = new EndnotesPart( pn );
	        	        } else {
	        	        	log.warn("TODO: handle " + partName);
	        	        	jPart = null;
	        	        }
	        	       
	        	       docx4jParts.put(pn, jPart);
	        	       log.debug("Added new part " + partName);
	        	       
	        	    // (But it should already be
	   	        	// present in the document rels, courtesy of some other
	   	        	// iteration through this loop)
	        	       
	        	    // What about [Content_Types].xml?
	        		
	        } else {
		        // It is safe to assume we are dealing with a JAXB part, since
		        // each of the sequenceableParts are that:
		        // 
		        // 		/word/_rels/document.xml.rels
		        // 		/word/comments.xml
		        //		/word/footnotes.xml
		        //		/word/endnotes.xml	 
		        		        
		        jPart = (JaxbXmlPart)p;
		        jPart.unmarshal((Element)listParent);  
	        }
	    }
	    
	    // In the loop above, we updated each of the sequencable parts.
	    // But we still have to do the main document part itself:
	    mdp.unmarshal( mdpW3C.getDocumentElement() );
	    
        // Now we need for docx4all to update its model of the document. 
	    //  see WordMLEditor.synchEditorView? That seems to 
        // replace just the mdp; but perhaps everything else follows
        // from that?
	    // See WordMLDocument.applyFilter
		org.docx4j.wml.Document wmlDoc = 
			(org.docx4j.wml.Document)
				mdp.getJaxbElement();
		doc.replaceBodyML(new BodyML(wmlDoc.getBody()));
		
//		Need: ?
//    	editorView.validate();
//    	editorView.repaint();

	    
        // CONSIDER also, for docx4all and Word client - when/how does the stateDocx copy get updated???	        	        
	    
	}
	

	/* Apply registered transforms. */
	public void applyUpdates(FetchRemoteEditsWorker worker) {
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
		int total = transformsBySeqNum.size();
		int i = 1;
		long changeset = transformsBySeqNum.get(0).getChangesetNumber();
		
		for (TransformAbstract t : transformsBySeqNum) {
			// OPTIMISATION: could do the most recent only for each cc
			// (ie reverse order), except for MOVES and INSERTS, which need to
			// be done in order.

            worker.setProgress(
            	FetchProgress.APPLYING_UPDATES, 
            	"Update " + (i++) + " of " + total);
			if (t.getApplied()) // then it shouldn't be in the list ?!
				// ? (unless it was injected by 
		        // transmitLocalChanges, or hasn't previously been discarded)
			{
				if (stateDocx.getTransforms()
						.getTSequenceNumberHighestFetched() > t
						.getSequenceNumber()) {
					discards.add(t);
				}
				continue;
			}

			// docx4all specific
			if (changeset != t.getChangesetNumber()) {
				changeset = t.getChangesetNumber();
				refreshLocalDocument();
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

		refreshLocalDocument();
		
		if (cantOverwrite) {
			worker.setProgress(
				FetchProgress.APPLYING_DONE, 
				"You need to accept/reject revisions before all remote changes can be applied.  Please do so, then hit the button again.");
		} else {
			worker.setProgress(FetchProgress.APPLYING_DONE, "Changesets applied");
		}
	}

	/* On success, returns the transformation's tSequenceNumber; otherwise, 0 */
	private long applyUpdate(TransformAbstract t) {
		long resultCode;

		log.debug("applyUpdate " + t.getClass().getName() + " - "
				+ t.getSequenceNumber());

		String plutextId = t.getPlutextId();

		StateChunk currentChunk = 
			Util.getStateChunk(getWordMLDocument(), plutextId);
		
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
	        try {
				scanSdtForIdref(t);
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	        changedChunks.put( t.getPlutextId(), t.getPlutextId() );  
	        	// TODO, what if it is already there?	        
			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");
			
	        // Word Add-In 2009 02 05 - Add it to currentStateChunks, so it is there
	        // for updateRelatedParts
	        //currentStateChunks.put(t.getId(), stateDocx.getStateChunks().get(t.getId()));
			// TODO We do the above in the Word Add-In, but how to do it here
			// (since there is no currentStateChunks as such)?
			// And is it even necessary??

			if (resultCode >= 0) {
				this.sdtChangeTypes.put(plutextId,
						TrackedChangeType.OtherUserChange);
			}

			return resultCode;
			
		} else if (t instanceof TransformDelete) {
			StateChunk stateDocxSC = stateDocx.getStateChunks().get(plutextId);
			if (currentChunk == null) {
				// It is missing from current StateChunks, and the
				// reason for this is an insert and a delete transform
				// both being received this round

				// In this case we have already put it in stateDocx.StateChunks,
				// so we can do:
				t.markupChanges(stateDocxSC.getXml(), changeset);

			} else {
				boolean conflict = isConflict(currentChunk, stateDocxSC);
				
	            // The update we will insert is one that contains the results
	            // of comparing the server's SDT to the user's local one.
	            // This will allow the user to see other people's changes.				
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
						this.sdtChangeTypes.put(plutextId,
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
				this.sdtChangeTypes.put(plutextId,
						TrackedChangeType.OtherUserChange);
				this.sdtIdUndead.put(plutextId, plutextId);
			}
			
	        // Word Add-In 2009 02 05 - Add it to currentStateChunks, so it is there
	        // for updateRelatedParts
//	        try
//	        {
//	            currentStateChunks.Remove(t.getId());
//	        }
//	        catch (KeyNotFoundException knf) { }	
			// TODO We do the above in the Word Add-In, but how to do it here
			// (since there is no currentStateChunks as such)?
			// And is it even necessary??
	        

			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");
			return resultCode;

		} else if (t instanceof TransformMove) {
			resultCode = t.apply(this, stateDocx.getStateChunks());
			t.setApplied(true);
			if (resultCode >= 0) {
				this.sdtChangeTypes.put(plutextId,
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

			StateChunk stateDocxSC = stateDocx.getStateChunks().get(plutextId);
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

	            // The update we will insert is one that contains the results
	            // of comparing the server's SDT to the user's local one.
	            // This will allow the user to see other people's changes.				
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
						this.sdtChangeTypes.put(plutextId,
								TrackedChangeType.Conflict);
						return CANT_OVERWRITE;

					} else {
						t.markupChanges(currentChunk.getXml(), changeset);

						// We could warn the user here that their stuff has been
						// redlined as a deletion.
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
	        try {
				scanSdtForIdref(t);
			} catch (XPathExpressionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        changedChunks.put( t.getPlutextId(), t.getPlutextId() );  
	        	// TODO, what if it is already there?

			log.debug(t.getSequenceNumber() + " applied ("
					+ t.getClass().getName() + ")");

			if (conflict) {
				this.sdtChangeTypes.put(plutextId, TrackedChangeType.Conflict);
				log.debug("set state to CONFLICTED");
			} else {
				this.sdtChangeTypes.put(plutextId,
						TrackedChangeType.OtherUserChange);
			}
			
	        // Word Add-In 2009 02 05 - Add it to currentStateChunks, so it is there
	        // for updateRelatedParts
//	        currentStateChunks.put(t.getId(), stateDocx.StateChunks[t.getId()]);			
			// TODO We do the above in the Word Add-In, but how to do it here
			// (since there is no currentStateChunks as such)?
			// And is it even necessary??

			return resultCode;

		} else {
			log.debug(" How to handle " + t.getClass().getName());
			return -1;
		}
	}

	/// <summary>
	/// Determine whether the user has changed this sdt, so that changes fetched from
	/// the server have to be merged (and this marked as TrackedChangeType.Conflict).
	/// If the only change is Word's renumbering of rel references, that doesn't
	/// count.
	/// </summary>
	/// <param name="currentStateChunk"></param>
	/// <param name="stateDocxSC"></param>
	/// <returns></returns>	
	boolean isConflict(StateChunk currentStateChunk, StateChunk stateDocxSC) {

		boolean conflict = !(currentStateChunk.getXml().equals(stateDocxSC
				.getXml()));
		
	    if (!conflict)
	    {
	        return false;
	    }
	    
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
		} 
		
		log.debug("Still different!");
		log.debug("stateDocx marked up: "
				+ stateDocxSC.getMarkedUpSdt());
		log.debug("current : " + currentStateChunk.getXml());
		
	    // If all that has happened is that Word has renumbered the rel id's,
	    // we don't flag that
		// TODO - do we need a docx4all equivalent?
//	    if (currentStateChunk.RelReferencesDropped.equals(stateDocxSC.RelReferencesDropped))
//	    {
//	        log.debug("Match with RelReferencesDropped.");
//	        return false;
//	    }
		
		return true;

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
	
	
	public boolean [] fetchParts = new boolean[4];

	public static int PART_RELS = 0;
	public static int PART_COMMENTS = 1;
	public static int PART_FOOTNOTES = 2;
	public static int PART_ENDNOTES = 3;

	/// <summary>
	/// Sdt's which are altered through the application of a transform, 
	/// during this round of apply updates.
	/// </summary>
	HashMap<String, String> changedChunks;

	void scanSdtForIdref(TransformAbstract t) throws XPathExpressionException
	{
	    // Look at this Insert | Update, to see
	    // whether it contains any of these 
	    // id's which Word renumbers in document order
	    NodeList nodeList;

	    Node sdt = XmlUtils.marshaltoW3CDomDocument(t.getSdt());

	    if (!fetchParts[PART_COMMENTS])
	    {
	        // <w:commentReference w:id="1" />
	    	
			// xpaths[1] =  xPath.compile(".//w:commentReference/@w:id | .//w:commentRangeStart/@w:id | .//w:commentRangeEnd/@w:id");
	    	
	        //nodeList = sdt.SelectNodes("//w:commentReference", nsmgr);
	    	nodeList = (NodeList)xpaths[1].evaluate(sdt, XPathConstants.NODESET);
	        if (nodeList.getLength() > 0)
	        {
	            fetchParts[PART_COMMENTS] = true;
	            log.debug("Detected comment");
	            // TODO: iff this is the first comment, we need to adjust the rels part.
	            // But for now:
	            fetchParts[PART_RELS] = true;
	        }
	    }

	    if (!fetchParts[PART_FOOTNOTES])
	    {
	        // <w:footnoteReference w:id="3" />
	    	
			// xpaths[2] =  xPath.compile(".//w:footnoteReference/@w:id");
	    	nodeList = (NodeList)xpaths[2].evaluate(sdt, XPathConstants.NODESET);
	        if (nodeList.getLength() > 0)
	        {
	            fetchParts[PART_FOOTNOTES] = true;
	            log.debug("Detected footnote");
	            // TODO: iff this is the first footnote, we need to adjust the rels part.
	            // But for now:
	            fetchParts[PART_RELS] = true;
	        }
	    }


	    if (!fetchParts[PART_ENDNOTES])
	    {
	        // <w:endnoteReference w:id="2" />
	    	
			// xpaths[3] =  xPath.compile(".//w:endnoteReference/@w:id");
	    	nodeList = (NodeList)xpaths[3].evaluate(sdt, XPathConstants.NODESET);
	        if (nodeList.getLength() > 0)
	        {
	            fetchParts[PART_ENDNOTES] = true;
	            log.debug("Detected endnote");
	            // TODO: iff this is the first endnote, we need to adjust the rels part.
	            // But for now:
	            fetchParts[PART_RELS] = true;
	        }
	    }				

	    if (!fetchParts[PART_RELS] )
	    {
	        // Only perform this test, if we don't already require this part
	    	
			// xpathRelTest = xPath.compile(" .//@r:link | .//@r:id ");
	    	nodeList = (NodeList)xpathRelTest.evaluate(sdt, XPathConstants.NODESET);

	            /* We only expect @r:link, since all
	             * @r:embed would have been converted when the 
	             * sdt containing the image was transmitted
	             * to the server by the other client. */

	        if (nodeList.getLength() > 0)
	        {
	            fetchParts[PART_RELS] = true;
	            log.debug("Detected @r");
	        }

	    }

	}

	/* ****************************************************************************************
	*          ACCEPT REMOTE CHANGES
	* **************************************************************************************** */

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

	public TrackedChangeType getTrackedChangeType(String plutextId) {
		return this.sdtChangeTypes.get(plutextId);
	}

	public TrackedChangeType removeTrackedChangeType(String plutextId) {
		return this.sdtChangeTypes.remove(plutextId);
	}

	public List<String> getIdsOfNonConflictingChanges() {
		List<String> nonConflictingChanges = new ArrayList<String>();

		Iterator<Map.Entry<String, TrackedChangeType>> it = this.sdtChangeTypes
				.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, TrackedChangeType> entry = it.next();
			String plutextId = entry.getKey();
			TrackedChangeType type = entry.getValue();
			if (type == TrackedChangeType.OtherUserChange) {
				nonConflictingChanges.add(plutextId);
			}
		}

		return nonConflictingChanges.isEmpty() ? null : nonConflictingChanges;
	}

	/***************************************************************************
	 * TRANSMIT LOCAL CHANGES
	 * ****************************************************************************************
	 */
	public boolean transmitLocalChanges(TransmitLocalEditsWorker worker) throws RemoteException, ClientException {
		// Look for local modifications
		// - commit any which are non-conflicting (send these as TRANSFORMS)
		boolean success = transmitContentUpdates(worker);

		// transmitStyleUpdates();

		return success;
	}

    boolean transmitContentUpdates(TransmitLocalEditsWorker worker) throws RemoteException {
		log.debug(stateDocx.getDocID() + ".. .. transmitContentUpdates");
	
		// The list of transforms to be transmitted
		List<T> transformsToSend = new ArrayList<T>();
		
		// See TransmitLocalEditsWorker.preTransmit()
		
		// TODO When an image is added, making that External needs to
		// be done here. See Word Add-In line ~ 2019
		// But this work can be defered until such time as it is
		// possible to add a new image in docx4all ..
		// and it would be best if the code used for adding
		// an image took care of making it external		
	    // (which includes actually saving them on the server)
				
//	    foreach (DetachedImagePart dip in detachedImages)
//	    {
//	        log.debug( ws.injectPart(stateDocx.DocID, 
//	            dip.Name, "0", dip.ContentType, dip.Data) );
//	    }
	
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
		
		worker.setProgress(
			TransmitProgress.INSPECTING_LOCAL_DOC_STRUCTURE, 
			"Inspecting local document structure");
		
	    WordMLDocument doc = getWordMLDocument();
	    
	    List<String> bornAgain = new ArrayList<String>();
	    
	    //Build an iterator for iterating applied TransformDelete(s).
	    //See:applyUpdate(TransformAbstract) method
	    Iterator<Map.Entry<String, String>> it = 
	    	this.sdtIdUndead.entrySet().iterator();
	    while (it.hasNext()) {
	    	Map.Entry<String, String> entry = it.next(); 
	    	StateChunk currentChunk = Util.getStateChunk(doc, entry.getKey());
	    	log.debug("Inspecting undead: " + entry.getKey() );
	    	if (currentChunk == null) {
	    		//The applied TransformDelete must have been accepted.
	            log.debug(".. really kill.");
	            // All we do here is remove it from sdtIdUndead
	            bornAgain.add(entry.getKey());
	            
	    	} else if (currentChunk.containsTrackedChanges()) {
	            // Remove it from inferredSkeleton and currentStateChunks
	            // but keep in document.
	            log.debug(".. still in limbo.");
	            if (!this.currentClientSkeleleton.removeRib(
	            	  new TextLine(entry.getKey())) ) {
	            	log.error("Couldn't find '" + entry.getKey() + "' to remove!");
	            }
	            
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
         * 
         * Note that we need to ensure that we are working against
         * the latest server skeleton, so that the positions we
         * send for moves and inserts correspond to the server's state.
         */
	
        worker.setProgress(
        	TransmitProgress.FETCHING_REMOTE_DOC_STRUCTURE, 
        	"Fetching remote document structure");
        
	    String serverSkeletonStr = ws.getSkeletonDocument(stateDocx.getDocID());
	    log.debug(serverSkeletonStr);

	    Skeleton serverSkeleton = new Skeleton(serverSkeletonStr);
	    
	    // TODO - Add to docx4all 2009 03 05
//	    boolean structuralTransformsPending = !serverSkeleton.init(
//	            stateDocx.getTransforms().getTSequenceNumberHighestFetched() );
//
//
//	        /* When we detect a difference, we need to know whether
//	         * this is a local change, or a remote one which we haven't
//	         * applied yet.
//	         * 
//	         * Given that we have to work with the latest server skeleton,
//	         * we have to rely on it to be able to tell us that.
//	         * (If we could use an older one, then that ambiguity 
//	         *  would go away.  And we could keep the old one around
//	         *  for this purpose, but code which used the old one to
//	         *  resolve such ambiguities would probably be a little
//	         *  harder to understand - though with the advantage that
//	         *  maybe we could continue - TODO think this through ..)
//	         * 
//	         * So, if there are any pending remote moves/inserts/deletes,
//	         * require the user to apply these before transmitting:
//	         */
//	        if (structuralTransformsPending)
//	        {
//	            worker.setProgress(0, "Please fetch remote updates, then try again.");
//	            return false;
//	        }
	    
	
	    // OK, compare the inferredSkeleton to serverSkeleton
        worker.setProgress(
        	TransmitProgress.IDENTIFYING_STRUCTURAL_CHANGES,
        	"Identifying structural changes in document");
        
	    createTransformsForStructuralChanges(
	    	transformsToSend,
	        this.currentClientSkeleleton, 
	        serverSkeleton);
	
	    Boolean someTransmitted = false;
        // Whether an sdt on the server is newer than the local version
	    Boolean someConflicted = false;
	
        // Whether the local sdt contains tracked changes
        // (which must be resolved before it can be transmitted)
        Boolean someTrackedConflicts = false;

		org.plutext.transforms.ObjectFactory transformsFactory = 
			new org.plutext.transforms.ObjectFactory();

		WordMLDocument wordMLDoc = getWordMLDocument();
		DocumentElement root = (DocumentElement) wordMLDoc
				.getDefaultRootElement();

		try {
			worker.setProgress(
					TransmitProgress.IDENTIFYING_UPDATED_TEXT, 
        		"Identifying updated text");
        
			for (int idx = 0; idx < root.getElementCount(); idx++) {
				DocumentElement elem = (DocumentElement) root.getElement(idx);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					org.docx4j.wml.SdtBlock sdt = 
						(org.docx4j.wml.SdtBlock) ml.getDocxObject();
					StateChunk chunkCurrent = new StateChunk(sdt);

					// TODO
//		            if (chunkCurrent.IsNew)
//		            {
//		                log.debug(chunkCurrent.getIdAsString() + " IsNew, so ignoring");
//		                continue;
//		            }					
					
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
                    // However, we need to worry about the possibility that it has
                    // changed remotely, since we HAVE NOT checked all updates
                    // on server had been applied 

                    // 2 possible approaches:

                    // 1: optimistic checkin, which the server is
                    // supposed to reject [need to look at 409 conflict
                    // bit again - workaround for case where version 
                    // changed already in given changeset].  User 
                    // then has to manually fetch updates.

                    // This was the approach until 2008 09 08

                    // 2:  we have the server skeleton document;
					// That tells us whether the server
                    // version is newer:
                    // <ns3:rib ns3:version="2" ns3:id="1773260365">
					Long localVersionNumber = Long.valueOf(chunkCurrent.getVersionAsLong() );
					if (serverSkeleton.getVersion(sdtId) == null) {
                        // Shouldn't need to worry about the std not being
                        // present in the skeleton, since:
                        // 1. we don't get this far if the object has been 
                        //    deleted on the server
                        // 2. this loop has already exited for new objects on the client 

                        // so this is an error.

                        log.error("Couldn't find key " + sdtId);						
                        
					} else if (localVersionNumber.longValue() < serverSkeleton.getVersion(sdtId).longValue()) {
                        log.debug("Conflict (old local version) ! Local edit " + sdtId + " not committed.");
                        someConflicted = true;
                        continue;
					}
					
                    if ( chunkCurrent.containsTrackedChanges()) {
                        // This is a conflicting update, so don't transmit ours.
                        // Keep a copy of what this user did in StateChunk
                        // (so that 
                        log.debug("Unresolved tracked change! Local edit " + sdtId + " not committed.");
                        someTrackedConflicts = true;
                        continue;
                    }
			
					// TransformUpdate tu = new TransformUpdate();
					// tu.attachSdt(chunkCurrent.getXml() );
					// tu.setId( chunkCurrent.getId() );
					// transformsToSend.add(tu);

					T t = transformsFactory.createTransformsT();
					t.setOp("update");
					t.setIdref(chunkCurrent.getIdAsLong() );
					t.setSdt(chunkCurrent.getSdt());
					transformsToSend.add(t);
				}
			}// for (idx) loop
			
	        try {
				transmitOtherUpdates(); // TODO - move this, since its a separate ws call.			
			} catch (Exception e1) {
				log.error(e1);
				e1.printStackTrace();
				throw e1;
			}
	    
			if (transformsToSend.isEmpty()) {
				boolean success = false;
				if (someConflicted) {
					String message = 
						"Done - Conflict warning: Fetch updates then accept/reject changes before trying again.";
					worker.setProgress(TransmitProgress.DONE, message);					
				} else if (someTrackedConflicts) {
					String message =
						"Done - Conflict warning: Accept/Reject changes before trying again.";
					worker.setProgress(TransmitProgress.DONE, message);
				} else {
					worker.setProgress(TransmitProgress.DONE, "Nothing to send.");
					success = true;
				}
				return success;
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
			worker.setProgress(
            	TransmitProgress.TRANSMITTING_MESSAGE, 
            	"Preparing and transmitting message");
            
			Transforms transforms = transformsFactory.createTransforms();
			transforms.getT().addAll(transformsToSend);
			boolean suppressDeclaration = true;
			boolean prettyprint = false;
			String transformsString = 
				org.docx4j.XmlUtils.marshaltoString(
					transforms, 
					suppressDeclaration, 
					prettyprint,
					org.plutext.Context.jcTransforms);

			log.debug("TRANSMITTING " + transformsString);

			String[] result = 
				ws.transform(
					stateDocx.getDocID(), 
					transformsString,
					checkinComment);

			worker.setProgress(
				TransmitProgress.INTERPRETING_TRANSMISSION_RESULT, 
        		"Response received. Interpreting...");
        
			log.debug("Checkin also returned results");

	        /* Design choice:
	         * 
	         * Either you chunk locally, in which case, you don't have to apply
	         * transforms which are local in origin, 
	         * 
	         * .. or you leave it to the 
	         * server to do the chunking, in which case you do have to apply
	         * the resulting transforms.
	         * 
	         * You have to do one or the other to apply the changes immediately,
	         * so there is no issue with the user making changes before it
	         * is applied, and those changes getting lost
	         * 
	         * I've opted to chunk locally.  
	         * 
	         * If one was to leave it to the server to do the chunking, then
	         * apply the resulting transforms, you'd have to make sure you
	         * had the corresponding server skeleton doc, so any insertions 
	         * were in the correct place.
	         * 
	         */
	        // In strict theory, we shouldn't do this, because they'll end 
	        // up in the list in the wrong order.
	        // But we actually know there are no conflicting transforms with
	        // lower snums, so it isn't a problem.
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
						Unmarshaller u = 
							Context.jcTransforms.createUnmarshaller();
						u.setEventHandler(
							new org.docx4j.jaxb.JaxbValidationEventHandler());
						transformsObj = 
							(org.plutext.transforms.Transforms) 
							u.unmarshal(new java.io.StringReader(sb.toString()));
					} catch (JAXBException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (T tmp : transformsObj.getT()) {
						TransformAbstract ta = TransformHelper.construct(tmp);
						if (ta instanceof TransformUpdate) {
							// Set the in-document tag to match the one we got back
							// ?? the actual sdt or the state chunk?
							updateLocalContentControlTag(ta.getPlutextId(), ta.getTag());
							this.stateDocx.getStateChunks().put(
								ta.getPlutextId(),
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
			
			someTransmitted = true;
			
		} catch (Exception exc) {
			exc.printStackTrace();
			someTransmitted = false;
		}
		
		boolean success = false;
        String checkinResult = null;
        
        if (someConflicted) {
        	checkinResult = 
        		"Done - Conflict warning: Fetch updates then accept/reject changes before trying again.";
        	
        } else if (someTrackedConflicts) {
        	checkinResult =
        		"Done - Conflict warning: Accept/Reject changes before trying again."; 
        	
        } else if (someTransmitted) {
        	checkinResult =
        		"Done - Your changes were transmitted successfully.";
        	success = true;
        	
        } else {
        	checkinResult = "Your changes were NOT transmitted.";
        }
    	
        worker.setProgress(TransmitProgress.DONE, checkinResult);
    	return success;
	}

	void createTransformsForStructuralChanges(
		List<T> transformsToSend,
		Skeleton inferredSkeleton, 
		Skeleton serverSkeleton) {
		
		org.plutext.transforms.ObjectFactory transformsFactory 
			= new org.plutext.transforms.ObjectFactory();

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
						t.setIdref(sc.getIdAsLong() );
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
						t.setIdref(sc.getIdAsLong() );
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
	
	void transmitOtherUpdates() throws RemoteException
	{
	    HashMap<String, org.plutext.client.partWrapper.Part> knownParts = stateDocx.getParts();

	    HashMap<String, org.plutext.client.partWrapper.Part> discoveredParts 
	    	= Util.extractParts( getWordMLDocument() );

	    Map.Entry pairs;
	    org.plutext.client.partWrapper.Part knownPart;
	    org.plutext.client.partWrapper.Part discoveredPart;
	    
	    // DELETED PART - are there any KnownParts which have now gone?
		Iterator knownPartsIterator = knownParts.entrySet().iterator();
	    while (knownPartsIterator.hasNext()) {
	        pairs = (Map.Entry)knownPartsIterator.next();
	        
	        if(pairs.getKey()==null) {
	        	log.warn("Skipped null key");
	        	pairs = (Map.Entry)knownPartsIterator.next();
	        }
	    
	        knownPart 
	        	= (org.plutext.client.partWrapper.Part)pairs.getValue(); 
	        // do we know about it?
	        
	        discoveredPart 
	        	= discoveredParts.get(knownPart.getName() );

            // So we do know about it
            // That's fine - the foreach below will see whether it has
			// changed?
            
            if (discoveredPart==null) {	        {
	            // This part has been deleted
	            log.warn(knownPart.getName() + " no longer present locally; delete it on server?");
	            // TODO removePart(PartName)
	        }
	    }


	    // INSERTED/UPDATED parts
		Iterator discoveredPartsIterator = discoveredParts.entrySet().iterator();
	    while (discoveredPartsIterator.hasNext()) {
	        pairs = (Map.Entry)discoveredPartsIterator.next();
	        
	        if(pairs.getKey()==null) {
	        	log.warn("Skipped null key");
	        	pairs = (Map.Entry)knownPartsIterator.next();
	        }
	    //foreach (KeyValuePair<String, Part> kvp in discoveredParts)
	    //{
	        discoveredPart = (org.plutext.client.partWrapper.Part)pairs.getValue(); 
	        log.error("Considering " + discoveredPart.getName() );
	        // do we know about it?
            knownPart = knownParts.get(discoveredPart.getName());
            
            if (knownPart==null) {
            	
	            // This must be a new part, so version is 0.
	            String resultingVersion = ws.injectPart(stateDocx.getDocID(), 
	            		discoveredPart.getName(), 
	                "0", discoveredPart.getContentType(), discoveredPart.getUnwrappedXml());

	            // expect that to be 1?  well, no: the first version on the server will be numbered 0.
	            if (!resultingVersion.equals("0"))
	            {
	                log.error("expected this be to version 0 ?!");
	            }
	            stateDocx.getPartVersionList().setVersion(discoveredPart.getName(), 
	            		resultingVersion);

	            // and update our record of the part in StateDocx
	            // (since any change from this new baseline is something we will want to transmit)
	            stateDocx.getParts().put(discoveredPart.getName(), discoveredPart);

	            // note that _rels of this which is a target will get handled 
	            // automatically, because we will have detected a change to that part as well,
	            // and sent it ...            
            
            } else {

	            // So we do know about it
	            // - has it changed?
	            //if (knownPart.Xml.Equals(discoveredPart.Xml))
            	// docx4all uses unwrapped here
            	if (knownPart.getXmlNode().equals(discoveredPart.getXmlNode()) )
	            {
            		// that's a DOM Level 3 feature - if Eclipse says it is undefined,
            		// go into Build Path > Order and Export > and make sure
            		// your (>JDK 5) system library precedes anything else
            		// which defines DOM APIs
            		// (TODO - why do we have XML APIs 1.0 beta 2 ??)
            		
	                log.debug("No changes detected in: " + knownPart.getName() );
	            }
	            else
	            {
	                // Similar to what we do when we send an update to an SDT,
	                // we send this with our current version number.
	                // All being well, the server will respond with a new version
	                // number.

	                String localVersion = stateDocx.getPartVersionList().getVersion(
	                    discoveredPart.getName() );

	                String resultingVersion = ws.injectPart(stateDocx.getDocID(), 
	                    discoveredPart.getName(), localVersion, 
	                    discoveredPart.getContentType(), discoveredPart.getUnwrappedXml() );
	                stateDocx.getPartVersionList().setVersion(discoveredPart.getName(), 
	                		resultingVersion);

	                // and update our record of the part in StateDocx
	                // (since any change from this new baseline is something we will want to transmit)
	                knownParts.put(discoveredPart.getName(), discoveredPart );
	            }

	        }
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

	/* ****************************************************************************************
	 *          VERSION HISTORY
	 * **************************************************************************************** */
	public WordprocessingMLPackage getVersionHistory(String sdtId) throws RemoteException {
		WordprocessingMLPackage theHistory = null;
		
		String historyString = ws.reportVersionHistory(this.stateDocx.getDocID(), sdtId);
		log.debug("getVersionHistory(): historyString = " + historyString);
		
		try {
			JAXBContext jc = org.docx4j.jaxb.Context.jcXmlPackage;

			Unmarshaller u = jc.createUnmarshaller();
						
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

			StreamSource src = new StreamSource(new StringReader(historyString));
			Object o = u.unmarshal(src); 
			org.docx4j.xmlPackage.Package xmlPackage 
				= (org.docx4j.xmlPackage.Package)((JAXBElement<?>)o).getValue();
					
			org.docx4j.convert.in.FlatOpcXmlImporter inWorker = 
				new org.docx4j.convert.in.FlatOpcXmlImporter(xmlPackage);
			
			theHistory = (WordprocessingMLPackage) inWorker.get();
			
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
		return theHistory;
    }

	/* ****************************************************************************************
	 *          REPORT RECENT CHANGES
	 * **************************************************************************************** */
	public WordprocessingMLPackage getRecentChangesReport() throws RemoteException {
		WordprocessingMLPackage theReport = null;
		
		String reportString = ws.reportRecentChanges(this.stateDocx.getDocID());
	    log.debug("Recent changes: " + reportString);
	    
		try {
			JAXBContext jc = org.docx4j.jaxb.Context.jcXmlPackage;

			Unmarshaller u = jc.createUnmarshaller();
						
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

			StreamSource src = new StreamSource(new StringReader(reportString));
			Object o = u.unmarshal(src); 
			org.docx4j.xmlPackage.Package xmlPackage 
				= (org.docx4j.xmlPackage.Package)((JAXBElement<?>)o).getValue();
					
			org.docx4j.convert.in.FlatOpcXmlImporter inWorker = 
				new org.docx4j.convert.in.FlatOpcXmlImporter(xmlPackage);
			
			theReport = (WordprocessingMLPackage) inWorker.get();
			theReport = XmlUtil.export(theReport);
			
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
		return theReport;
	}
	
	private void updateLocalContentControlTag(String sdtId, Tag tag) {
		WordMLDocument doc = getWordMLDocument();
		DocumentElement elem = Util.getDocumentElement(doc, sdtId);

		log.debug("updateLocalContentControlTag(): elem=" + elem);
		log.debug("updateLocalContentControlTag(): tag param=" + tag.getVal());

		SdtBlockML ml = (SdtBlockML) elem.getElementML();
		ml.getSdtProperties().setTagValue(tag.getVal());
	}

	private void refreshLocalDocument() {
    	WordMLDocument doc = getWordMLDocument();
		int start = getUpdateStartOffset();
		int end = getUpdateEndOffset();
		if (start <= end) {
			doc.refreshParagraphs(start, end);
			setUpdateStartOffset(doc.getLength());
			setUpdateEndOffset(0);
		}
	}
	
	private boolean isUndead(String sdtId) {
		return sdtIdUndead.containsKey(sdtId);
	}
	
	private static final java.util.Random RANDOM = new java.util.Random();
	
    public static final String generateId() {
    	java.math.BigInteger id =
    		java.math.BigInteger.valueOf(Math.abs(RANDOM.nextInt()));
    	return id.toString();
    }
	
	public static boolean isDeletedPermanently(Mediator mediator, String sdtId, String textContents) {
		if (!mediator.isUndead(sdtId)) {
            // This is not a candidate for removal
            return false;
        }

        // Remove it, if the w:del changes have been 
        // accepted (ie the sdt contains only whitespace)
        // So far, we can look at the text contents, but
        // we don't know whether they are in a normal run,
        // or a w:del (or w:ins) element
        if (textContents.trim().length() == 0)
        {
            log.debug("Extension detected only whitespace in : " + sdtId);
            return true;
        }

        return false;
	}
	
	
}// Mediator class

