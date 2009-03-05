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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.ui.main.Constants;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.Parts;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.state.StateChunk;
import org.plutext.client.partWrapper.Part;
import org.plutext.client.partWrapper.SequencedPart;

public class Util {

	private static Logger log = Logger.getLogger(Util.class);

	public static String getCustomDocumentProperty(
			DocPropsCustomPart docPropsCustomPart, String propName) {

		org.docx4j.docProps.custom.Properties customProps = (org.docx4j.docProps.custom.Properties) docPropsCustomPart
				.getJaxbElement();

		for (org.docx4j.docProps.custom.Properties.Property prop : customProps
				.getProperty()) {

			if (prop.getName().equals(propName)) {
				// At the moment, you need to know what sort of value it has.
				// Could create a generic Object getValue() method.
				return prop.getLpwstr();
			}
		}
		log.error("Property '" + propName + "' not found!");
		return null;

	}

//	/** Gets the text contents of the Sdt */
//	public static String getSdtText(SdtBlock cc) {
//
//		// TODO - implement
//
//		return null;
//	}

//	public static String getChunkId(Id id) {
//		return id.getVal().toString();
//	}

	public static DocumentElement getDocumentElement(WordMLDocument doc,
			String sdtBlockId) {

		DocumentElement elem = null;

		try {
			doc.readLock();

			DocumentElement root = (DocumentElement) doc
					.getDefaultRootElement();

			for (int i = 0; i < root.getElementCount() - 1 && elem == null; i++) {
				elem = (DocumentElement) root.getElement(i);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					SdtBlockML sdtBlockML = (SdtBlockML) ml;
					if (sdtBlockId.equals(sdtBlockML.getSdtProperties()
							.getIdValue().toString())) {
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

	public static StateChunk getStateChunk(WordMLDocument doc, String sdtBlockId) {

		StateChunk theChunk = null;

		DocumentElement elem = getDocumentElement(doc, sdtBlockId);
		if (elem != null) {
			ElementML ml = elem.getElementML();
			theChunk = new StateChunk((org.docx4j.wml.SdtBlock) ml
					.getDocxObject());
		}

		return theChunk;
	}

//	public static Skeleton createSkeleton(WordMLDocument doc) {
//		Skeleton skeleton = new Skeleton();
//		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
//		for (int i = 0; i < root.getElementCount(); i++) {
//			DocumentElement elem = (DocumentElement) root.getElement(i);
//			ElementML ml = elem.getElementML();
//			if (ml instanceof SdtBlockML) {
//				SdtBlockML sdt = (SdtBlockML) ml;
//				String id = sdt.getSdtProperties().getIdValue().toString();
//				skeleton.getRibs().add(new TextLine(id));
//			}
//		}
//		return skeleton;
//	}

	public static HashMap<String, StateChunk> createStateChunks(
			WordMLDocument doc) {
		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();

		HashMap<String, StateChunk> stateChunks = new HashMap<String, StateChunk>(
				root.getElementCount());

		for (int idx = 0; idx < root.getElementCount(); idx++) {
			DocumentElement elem = (DocumentElement) root.getElement(idx);
			ElementML ml = elem.getElementML();
			if (ml instanceof SdtBlockML) {
				org.docx4j.wml.SdtBlock sdt = (org.docx4j.wml.SdtBlock) ml
						.getDocxObject();
				sdt = (org.docx4j.wml.SdtBlock) XmlUtils.deepCopy(sdt);
				StateChunk sc = new StateChunk(sdt);
				stateChunks.put(sc.getIdAsString(), sc);
			}
		}

		return stateChunks;
	}
	
    /// <summary>
    /// Extracts certain parts from the PkgXmlDocument
    /// </summary>
    /// <returns></returns>
    public static HashMap<String, org.plutext.client.partWrapper.Part> extractParts(WordMLDocument doc)
    {
        // NB at present we're only interested 
        // in certain parts

//        if (parts != null)
//        {
//            return parts;
//        }

        parts = new HashMap<String, org.plutext.client.partWrapper.Part>();

        
        WordprocessingMLPackage wmlp = ((DocumentML)doc.getDefaultRootElement()).getWordprocessingMLPackage();
        
        HashMap docx4jParts = wmlp.getParts().getParts();
		Iterator partsIterator = docx4jParts.entrySet().iterator();
	    while (partsIterator.hasNext()) {
	        Map.Entry pairs = (Map.Entry)partsIterator.next();
	        
	        if(pairs.getKey()==null) {
	        	log.warn("Skipped null key");
	        	pairs = (Map.Entry)partsIterator.next();
	        }
	        
	        PartName partName = (PartName)pairs.getKey();
	        org.docx4j.openpackaging.parts.Part docx4jPart
	        	= (org.docx4j.openpackaging.parts.Part)pairs.getValue();

            if (SequencedPart.getSequenceableParts().contains( partName.getName() ) )
            {
    	        Part p = Part.factory(docx4jPart);
                parts.Add(p.getName(), p);
                log.Debug("Added part: " + p.getName());
            }
            //else if (p.GetType().Name.Equals("PartVersionList"))
            //{
            //    partVersionList = (PartVersionList)p;
            //    log.Debug("set partVersionList");
            //}
        }

        return parts;
    }	

//	public static String getContentControlXML(SdtBlock cc) {
//
//		boolean suppressDeclaration = true;
//		return org.docx4j.XmlUtils.marshaltoString(cc, suppressDeclaration);
//
//		// return "<w:sdt
//		// xmlns:w=\
//		// "http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
//		// + node.InnerXml + "</w:sdt>";
//	}

	public final static org.docx4j.wml.Document preTransmit(Mediator mediator)
			throws Exception {
		WordMLDocument doc = (WordMLDocument) mediator.getWordMLTextPane()
				.getDocument();
		DocumentElement rootE = (DocumentElement) doc.getDefaultRootElement();
		DocumentML docML = (DocumentML) rootE.getElementML();
		org.docx4j.wml.Document docx4jDoc = (org.docx4j.wml.Document) docML
				.getDocxObject();
		String srcString = XmlUtils.marshaltoString(docx4jDoc, false);

		java.io.InputStream xslt = org.docx4j.utils.ResourceUtils
				.getResource("org/plutext/client/PreTransmit.xslt");

		Map<String, Object> xsltParameters = new HashMap<String, Object>(1);
		String chunking = 
			getCustomDocumentProperty(
				docML.getWordprocessingMLPackage().getDocPropsCustomPart(),
				Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
		Boolean chunkOnEachBlock = Boolean
				.valueOf(Constants.EACH_BLOCK_GROUPING_STRATEGY
						.equals(chunking));
		xsltParameters.put("chunkOnEachBlock", chunkOnEachBlock);
		xsltParameters.put("mediatorInstance", mediator);

		StreamSource src = new StreamSource(new StringReader(srcString));

		javax.xml.bind.util.JAXBResult result = new javax.xml.bind.util.JAXBResult(
				org.docx4j.jaxb.Context.jc);

		org.docx4j.XmlUtils.transform(src, xslt, xsltParameters, result);

		docx4jDoc = (org.docx4j.wml.Document) result.getResult();

		return docx4jDoc;
	}

}// Util class

