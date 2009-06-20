/*
 *  Copyright 2007, Plutext Pty Ltd.
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

package org.docx4all.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.vfs.FileObject;
import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.convert.in.FlatOpcXmlImporter;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.LoadFromVFSZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Document;
import org.plutext.client.Mediator;
import org.plutext.client.SdtWrapper;


/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementMLFactory {
	
	private static Logger log = Logger.getLogger(ElementMLFactory.class);		
	
	public final static DocumentML createEmptyDocumentML() {
		WordprocessingMLPackage docPackage = ObjectFactory.createEmptyDocumentPackage();
		return new DocumentML(docPackage);
	}
	
	public final static DocumentML createDocumentML(Document doc) {
		WordprocessingMLPackage docPackage = ObjectFactory.createDocumentPackage(doc);
		return new DocumentML(docPackage);
	}
	
	public final static DocumentML createDocumentML(FileObject f) throws IOException {
		return createDocumentML(f, false);
	}
	
	public final static DocumentML createDocumentML(FileObject f, boolean applyFilter) throws IOException {
		if (f == null || 
				! ( Constants.DOCX_STRING.equalsIgnoreCase(f.getName().getExtension())
						|| Constants.FLAT_OPC_STRING.equalsIgnoreCase(f.getName().getExtension() )
					)
				) {
			throw new IllegalArgumentException("Not a .docx (or .xml) file."); 
		}

		DocumentML docML = null;
		try {
			WordprocessingMLPackage wordMLPackage;
			if ( Constants.DOCX_STRING.equalsIgnoreCase(f.getName().getExtension()) ) {
				// .docx
				LoadFromVFSZipFile loader = new LoadFromVFSZipFile(true);
				wordMLPackage = 
					(WordprocessingMLPackage) 
						loader.getPackageFromFileObject(f);
			} else {
				// .xml
				
				// First get the Flat OPC package from the File Object
				InputStream is = f.getContent().getInputStream();				
					
				Unmarshaller u = Context.jcXmlPackage.createUnmarshaller();
				u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

				javax.xml.bind.JAXBElement j  = (javax.xml.bind.JAXBElement)u.unmarshal(is);
				org.docx4j.xmlPackage.Package flatOpc = (org.docx4j.xmlPackage.Package)j.getValue();
				System.out.println("unmarshalled ");

				// Now convert it to a docx4j WordML Package				
				FlatOpcXmlImporter importer = new FlatOpcXmlImporter(flatOpc);
				wordMLPackage = (WordprocessingMLPackage)importer.get();
			}
			
			if (applyFilter) {
				wordMLPackage = XmlUtil.applyFilter(wordMLPackage);
			}
			docML = new DocumentML(wordMLPackage);
		} catch (Docx4JException exc) {
			throw new IOException(exc);
		} catch (Exception exc) {
			throw new IOException(exc);
		}
		
		return docML;
	}
	
	public final static ParagraphML createEmptyParagraphML() {
		return createParagraphML(null, null, null);
	}
	
	/**
	 * Creates a ParagraphML whose children are specified in 'contents' param.
	 * 
	 * @param contents A list of RunContentML and/or RunML objects.
	 * @param newPPr A ParagraphPropertiesML given to the resulting new ParagraphML
	 * @param newRPr A RunPropertiesML given to a newly created RunML. 
	 * A RunML is newly created if 'contents' parameter contains RunContentML.
	 * @return the newly created ParagraphML
	 */
	public final static ParagraphML createParagraphML(
		List<ElementML> contents,
		ParagraphPropertiesML newPPr,
		RunPropertiesML newRPr) {
		
    	ParagraphML thePara = new ParagraphML(ObjectFactory.createP(null));
    	thePara.setParagraphProperties(newPPr);
    	
		RunML newRunML = null;

		if (contents != null) {
			for (ElementML ml : contents) {
				if (ml instanceof RunContentML) {
					// collect in one new RunML
					if (newRunML == null) {
						newRunML = new RunML(ObjectFactory.createR(null));
						newRunML.setRunProperties(newRPr);
						thePara.addChild(newRunML);
					}
					newRunML.addChild(ml);
				} else {
					if (newRunML != null) {
						newRunML = null;
					}
					thePara.addChild(ml);
				}
			}
		}
		
		return thePara;
	}
	
	/**
	 * Creates a RunML whose children are specified in 'contents' param.
	 * 
	 * @param contents
	 *            A list of RunContentML objects.
	 * @param newRPr
	 *            A RunPropertiesML given to the resulting RunML.
	 * @return the newly created RunML
	 * @throws IllegalArgumentException
	 */
	public final static RunML createRunML(
		List<ElementML> contents,
		RunPropertiesML newRPr) {
		
		RunML theRun = new RunML(ObjectFactory.createR(null));
		theRun.setRunProperties(newRPr);
    	
		if (contents != null) {
			for (ElementML ml : contents) {
				theRun.addChild(ml);
			}
		}
		
    	return theRun;
	}
	
	public final static RunPropertiesML createRunPropertiesML(AttributeSet attrs) {
		RunPropertiesML theProp = new RunPropertiesML(ObjectFactory.createRPr());
		theProp.addAttributes(attrs);
		theProp.save();
		return theProp;
	}
	
	public final static ParagraphPropertiesML createParagraphPropertiesML(AttributeSet attrs) {
		ParagraphPropertiesML theProp = new ParagraphPropertiesML(ObjectFactory.createPPr());
		theProp.addAttributes(attrs);
		theProp.save();
		return theProp;
	}
	
	public final static SdtBlockML createSdtBlockML() {
		org.docx4j.wml.SdtBlock sdtBlock = ObjectFactory.createSdtBlock();
		org.docx4j.wml.SdtPr sdtPr = ObjectFactory.createSdtPr();
		org.docx4j.wml.SdtContentBlock content = ObjectFactory.createSdtContentBlock();
		
		String id = Mediator.generateId();
		sdtPr.setTag(ObjectFactory.createTag(
				SdtWrapper.generateTag(id, "0")  ));
		sdtBlock.setSdtPr(sdtPr);
		sdtBlock.setSdtContent(content);

		SdtBlockML theBlock = new SdtBlockML(sdtBlock);
		return theBlock;
	}
	
	public final static HyperlinkML createEmptyHyperlinkML() {
		HyperlinkML theLink = new HyperlinkML(ObjectFactory.createHyperlink());
		return theLink;
	}
	
	private ElementMLFactory() {
		;//uninstantiable
	}
}// ElementMLFactory class



























