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

package org.docx4all.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.PropertiesContainerML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4j.XmlUtils;
import org.docx4j.convert.out.flatOpcXml.FlatOpcXmlCreator;
import org.docx4j.diff.ParagraphDifferencer;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.contenttype.ContentTypeManagerImpl;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtContentBlock;
import org.plutext.transforms.Changesets.Changeset;
import org.w3c.dom.Node;

/**
 *	@author Jojada Tirtowidjojo - 04/01/2008
 */
public class XmlUtil {

    private final static SimpleDateFormat RFC3339_FORMAT = 
    	new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
    
	protected static Logger log = Logger.getLogger(XmlUtil.class);
		
    
	/**
	 * Serialise the WordprocessingMLPackage in pkg:package format
	 * 
	 * @param wmlPackage
	 * @param out
	 */
	public final static void serialize(WordprocessingMLPackage wmlPackage, OutputStream out) {
        try {

    		FlatOpcXmlCreator worker = new FlatOpcXmlCreator(wmlPackage);
    		org.docx4j.xmlPackage.Package pkg = worker.get();
        	
    		JAXBContext jc = Context.jcXmlPackage;
    		Marshaller marshaller=jc.createMarshaller();        	
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
			// Use the character set expected by JEditorPane
			//marshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-10646-UCS-2");
			marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-16");
			
			// Suppress the XML declaration
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
			
			try { 
				marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", 
						new org.docx4j.jaxb.NamespacePrefixMapper() ); 

				// Reference implementation appears to be present (in endorsed dir?)
				log.info("using com.sun.xml.bind.namespacePrefixMapper");
				
			} catch (javax.xml.bind.PropertyException cnfe) {
				
				log.error(cnfe);

				log.info("attempting to use com.sun.xml.INTERNAL.bind.namespacePrefixMapper");
				
				marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
						new org.docx4j.jaxb.NamespacePrefixMapper() ); // Must use 'internal' for Java 6
				
			}
			
			
			/* Setting the property as above is all you need to do for the code
			 * to compile in Eclipse (or using Ant on the Mac with Apple's Java 6 preview).
			 * 
			 * However if you try to compile it using javac (eg via Ant), you may get: 
			 * 
			 * [javac] /home/dev/workspace/docx4all/src/org/docx4all/util/XmlUtil.java:108: cannot access com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper
			 * [javac] class file for com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper not found
			 * 
			 * To get around this, see
			 * Step 4 in http://pragmaticintegration.blogspot.com/2007/11/moving-jaxb-20-applications-built-by.html
			 * 
			 * The workaround is to add bootclasspathref="jre.libs" includeJavaRuntime="yes" to the javac task,
			 * which I have done.
			 * 
			 */
			
			marshaller.marshal(pkg, out);			

        } catch (Exception exc) {
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

	
	/**
	 * Deserialise the inputstream from pkg:package format
	 * into a WordprocessingMLPackage. 
	 * 
	 * @param wmlPackage
	 * @param in
	 */
	public final static WordprocessingMLPackage deserialize(
		WordprocessingMLPackage wmlPackage, InputStream in) {
		
		// NB at present we only handle main document part and style part.
				
		try {
			JAXBContext jc = Context.jcXmlPackage;
			Unmarshaller u = jc.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());
						
			javax.xml.bind.JAXBElement je = (javax.xml.bind.JAXBElement)u.unmarshal(
					new javax.xml.transform.stream.StreamSource(in));
			
			org.docx4j.xmlPackage.Package wmlPackageEl = (org.docx4j.xmlPackage.Package)je.getValue(); 
			org.docx4j.convert.in.FlatOpcXmlImporter xmlPackage = new org.docx4j.convert.in.FlatOpcXmlImporter( wmlPackageEl); 

			ContentTypeManager ctm = new ContentTypeManagerImpl();
			
			Part tmpDocPart = xmlPackage.getRawPart(ctm,  "/word/document.xml");
			Part tmpStylesPart = xmlPackage.getRawPart(ctm,  "/word/styles.xml");
			
//			org.docx4j.wml.Document wmlDocument = null;
//			org.docx4j.wml.Styles wmlStyles = null;
//			for (org.docx4j.wml.Package.Part p : wmlPackageEl.getPart() ) {
//				
//				if (p.getXmlData().getDocument()!= null) {
//					wmlDocument = p.getXmlData().getDocument();
//				}				
//				if (p.getXmlData().getStyles()!= null) {
//					wmlStyles = p.getXmlData().getStyles();
//				}				
//			}
				
			if (wmlPackage == null) {
				wmlPackage = ObjectFactory.createDocumentPackage(
						(org.docx4j.wml.Document)((JaxbXmlPart)tmpDocPart).getJaxbElement());
			} else {
				wmlPackage.getMainDocumentPart().setJaxbElement(
						((JaxbXmlPart)tmpDocPart).getJaxbElement());
			}
			
			// That handled the Main Document Part; now set the Style part.
			wmlPackage.getMainDocumentPart().getStyleDefinitionsPart().setJaxbElement( 
					((JaxbXmlPart)tmpStylesPart).getJaxbElement() );
			
		} catch (Exception exc) {
			exc.printStackTrace();
			throw new RuntimeException(exc);
		}
		
		return wmlPackage;
	}

	public final static String getEnclosingTagPair(QName qname) {
		return getEnclosingTagPair(qname.getPrefix(), qname.getLocalPart());
	}
	
	public final static String getEnclosingTagPair(Node node) {
		return getEnclosingTagPair(node.getPrefix(), node.getLocalName());
	}
	
	private final static String getEnclosingTagPair(String prefix, String localName) {
		if (prefix == null) {
			prefix = "";
		} else if (prefix.length() > 0) {
			prefix = prefix + ":";
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<");
		sb.append(prefix);
		sb.append(localName);
		sb.append(">");
		sb.append("</");
		sb.append(prefix);
		sb.append(localName);
		sb.append(">");
		
		return sb.toString();
	}
	
	/** 
	 * 
	 * Filters out certain features of WordprocessingML which docx4all cannot yet handle, 
	 * into something it can. Examples include proofErr, hyperlink, and lastRenderedPageBreak.
	 * 
	 * @param wmlPackage
	 */
	public final static WordprocessingMLPackage applyFilter(WordprocessingMLPackage wmlPackage) {
		try {
			// Apply the filter
			WordprocessingMLPackage.FilterSettings filterSettings = 
				new WordprocessingMLPackage.FilterSettings();
			filterSettings.setRemoveProofErrors(true);
			filterSettings.setRemoveContentControls(false);
			filterSettings.setRemoveRsids(true);
			filterSettings.setTidyForDocx4all(true);
			wmlPackage.filter(filterSettings);
								
		} catch (Exception exc) {
			exc.printStackTrace();
			throw new RuntimeException(exc);
		}
		return wmlPackage;
	}
	
	public final static org.docx4j.docProps.custom.Properties.Property
		getCustomProperty(WordprocessingMLPackage wmlPackage, String propertyName) {
		
		org.docx4j.docProps.custom.Properties.Property theProp = null;
		
		DocPropsCustomPart docPropsCustomPart = wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		List<org.docx4j.docProps.custom.Properties.Property> list = 
			(customProps != null) ? customProps.getProperty() : null;
		
		if (list != null) {
			for (org.docx4j.docProps.custom.Properties.Property temp: list) {
				String s = temp.getName();
				if (propertyName.equals(s)) {
					theProp = temp;
					break;
				}
			}
		}
		
		return theProp;
	}
	
	public final static boolean isSharedDocumentPackage(WordprocessingMLPackage wmlPackage) {
		org.docx4j.docProps.custom.Properties.Property groupingProp =
			getCustomProperty(
					wmlPackage, 
					Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
		org.docx4j.docProps.custom.Properties.Property checkinProp =
			getCustomProperty(
					wmlPackage, 
					Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
		return (groupingProp != null && checkinProp != null);
	}
	
	public final static WordprocessingMLPackage createNewPackage(
		WordprocessingMLPackage source,
		boolean copyMainDocumentPart,
		boolean copyStyleDefPart,
		boolean copyDocPropsCustomPart) 
		throws InvalidFormatException {
		
		// Create a package
		WordprocessingMLPackage thePack = new WordprocessingMLPackage();

		//Check for source's document part
		org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart
			srcDocumentPart = source.getMainDocumentPart();
		
		if (srcDocumentPart == null) {
			log.warn("createNewPackage(): Main document part missing!");

		} else {
			// Create main document part
			org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart 
				documentPart = new org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart();
			Object jaxbElement;
			if (copyMainDocumentPart) {
				jaxbElement = org.docx4j.XmlUtils.deepCopy(srcDocumentPart.getJaxbElement());
			} else if (isSharedDocumentPackage(source)) {
				jaxbElement = ObjectFactory.createEmptySharedDocument();
			} else {
				jaxbElement = ObjectFactory.createEmptyDocument();
			}
			documentPart.setJaxbElement(jaxbElement);
			thePack.addTargetPart(documentPart);
			
			//Check for source's styles part
			JaxbXmlPart srcStylesPart = srcDocumentPart.getStyleDefinitionsPart();
			if (srcStylesPart == null) {
				log.warn("createNewPackage(): Style definitions part missing!");
			} else  {
				// Create a styles part
				org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart stylesPart = 
					new org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart();
				if (copyStyleDefPart) {
					jaxbElement = org.docx4j.XmlUtils.deepCopy(srcStylesPart.getJaxbElement());
					stylesPart.setJaxbElement(jaxbElement);
					// Add the styles part to the main document part relationships
					// (creating it if necessary)
					documentPart.addTargetPart(stylesPart); // NB - add it to main doc part, not package!
				} else {
					try {
						stylesPart.unmarshalDefaultStyles();
						// Add the styles part to the main document part relationships
						// (creating it if necessary)
						documentPart.addTargetPart(stylesPart); // NB - add it to main doc part, not package!
					} catch (JAXBException exc) {
						;//ignore
					}
				}
			}
			
			//Check for document custom properties
			DocPropsCustomPart docPropsCustomPart = thePack.getDocPropsCustomPart();
			if (copyDocPropsCustomPart) {
				DocPropsCustomPart srcDocPropsCustomPart = source.getDocPropsCustomPart();
				jaxbElement = org.docx4j.XmlUtils.deepCopy(srcDocPropsCustomPart.getJaxbElement(), Context.jcDocPropsCustom);
				docPropsCustomPart.setJaxbElement(jaxbElement);
			}
			
		}
		
		return thePack;
		
	}
	
	public final static void setPlutextGroupingProperty(
		WordprocessingMLPackage wmlPackage,
		String value) {
		
		org.docx4j.openpackaging.parts.DocPropsCustomPart docPropsCustomPart = 
			wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		org.docx4j.docProps.custom.ObjectFactory factory = 
			new org.docx4j.docProps.custom.ObjectFactory();

		//Set plutext:Grouping property
    	org.docx4j.docProps.custom.Properties.Property groupingProp =
    		XmlUtil.getCustomProperty(
    			wmlPackage, Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
    	if (groupingProp == null) {
    		groupingProp = factory.createPropertiesProperty();
    		groupingProp.setName(Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
    		groupingProp.setFmtid(
    			org.docx4j.openpackaging.parts.DocPropsCustomPart.fmtidValLpwstr); // Magic string
    		groupingProp.setPid(customProps.getNextId()); 
    		customProps.getProperty().add(groupingProp);
    	}
		groupingProp.setLpwstr(value);
	}
	
	public final static void setPlutextCheckinMessageEnabledProperty(
		WordprocessingMLPackage wmlPackage,
		boolean enabled) {
		
		org.docx4j.openpackaging.parts.DocPropsCustomPart docPropsCustomPart = 
			wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		org.docx4j.docProps.custom.ObjectFactory factory = 
			new org.docx4j.docProps.custom.ObjectFactory();

		org.docx4j.docProps.custom.Properties.Property checkinProp =
    		XmlUtil.getCustomProperty(
    			wmlPackage, Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
    	if (checkinProp == null) {
    		checkinProp = factory.createPropertiesProperty();
    		checkinProp.setName(Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
    		checkinProp.setFmtid(
    			org.docx4j.openpackaging.parts.DocPropsCustomPart.fmtidValLpwstr); // Magic string
    		checkinProp.setPid(customProps.getNextId()); 
    		customProps.getProperty().add(checkinProp);
    	}
    	if (enabled) {
    		checkinProp.setLpwstr(Boolean.TRUE.toString());
    	} else {
    		checkinProp.setLpwstr(Boolean.FALSE.toString());
    	}
	}
	
	public final static void removePlutextProperty(
		WordprocessingMLPackage wmlPackage,
		String propertyName) {
		List<String> names = new ArrayList<String>(1);
		names.add(propertyName);
		removePlutextProperties(wmlPackage, names);
	}
	
	public final static void removePlutextProperties(
		WordprocessingMLPackage wmlPackage,
		List<String> propertyNames) {
		
		DocPropsCustomPart docPropsCustomPart = wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		List<org.docx4j.docProps.custom.Properties.Property> list = customProps.getProperty();
		if (list != null) {
			List<Integer> indexes = new ArrayList<Integer>();
			for (int i=0; i < list.size(); i++) {
				org.docx4j.docProps.custom.Properties.Property prop = list.get(i);
				if (propertyNames.contains(prop.getName())) {
					indexes.add(Integer.valueOf(i));
				}					
			}
			
			for (int i = indexes.size()-1; 0 <= i; i--) {
				list.remove(indexes.get(i).intValue());
			}
		}
	}

	public final static void removeSharedDocumentProperties(WordprocessingMLPackage wmlPackage) {
		List<String> names = new ArrayList<String>(2);
		names.add(Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
		names.add(Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
		removePlutextProperties(wmlPackage, names);
	}
	
	/**
	 * Empty the children of parent argument
	 * 
	 * @param parent the element whose children are to be deleted.
	 * @return The deleted children
	 */
	public final static List<ElementML> deleteChildren(ElementML parent) {
		List<ElementML> children = new ArrayList<ElementML>(parent.getChildren());
		for (ElementML elem: children) {
			elem.delete();
		}
		return children;
	}
	
	public final static RunContentML getFirstRunContentML(ElementML root) {
		RunContentML theElem = null;
		
		if (root.getChildrenCount() > 0) {
			ElementML lastChild = root.getChild(0);
			if (lastChild instanceof RunContentML) {
				theElem = (RunContentML) lastChild;
			} else {
				theElem = getFirstRunContentML(lastChild);
			}
		} else if (root instanceof RunContentML) {
			theElem = (RunContentML) root;
		}
		
		return theElem;
	}
	
	public final static RunContentML getLastRunContentML(ElementML root) {
		RunContentML theElem = null;
		
		if (root.getChildrenCount() > 0) {
			ElementML lastChild = root.getChild(root.getChildrenCount() - 1);
			if (lastChild instanceof RunContentML) {
				theElem = (RunContentML) lastChild;
			} else {
				theElem = getLastRunContentML(lastChild);
			}
		} else if (root instanceof RunContentML) {
			theElem = (RunContentML) root;
		}
		
		return theElem;
	}
	
	public final static int getIteratedIndex(ElementML root, ElementML target) {
		int theIdx = -1;
		
		ElementMLIterator it = new ElementMLIterator(root);
		int i = -1;
		while (it.hasNext() && theIdx == -1) {
			i++;
			ElementML elem = it.next();
			if (elem == target) {
				theIdx = i;
			}
		}
		
		return theIdx;
	}
	
	public final static ElementML getElementMLAtIteratedIndex(ElementML root, int idx) {
		ElementML theElem = null;
		
		ElementMLIterator it = new ElementMLIterator(root);
		int i = -1;
		while (it.hasNext() && i < idx) {
			i++;
			theElem = it.next();
		}
		
		if (i != idx) {
			theElem = null;
		}
		
		return theElem;
	}
	
	public final static void deleteIteration(ElementML root, int startIdx, int endIdx) {
		List<ElementML> list = new ArrayList<ElementML>();
		
		ElementMLIterator it = new ElementMLIterator(root);
		int i = -1;
		while (it.hasNext() && i < endIdx) {
			ElementML ml = it.next();
			i++;
			if (startIdx <= i) {
				list.add(ml);
			}
		}
		
		if (!list.isEmpty()) {
			for (ElementML ml: list) {
				ml.delete();
			}
		}
	}
	
	public final static void setAttributes(
		ElementML elem, 
		AttributeSet paragraphAttrs, 
		AttributeSet runAttrs,
		boolean replace) {
		
		ElementMLIterator it = new ElementMLIterator(elem);
		while (it.hasNext()) {
			ElementML ml = it.next();
			if (runAttrs != null && (ml instanceof RunML)) {
				PropertiesContainerML prop = ((RunML) ml).getRunProperties();
				if (replace) {
					prop.removeAttributes(prop.getAttributeSet());
				}
				prop.addAttributes(runAttrs);
				
			} else if (paragraphAttrs != null && (ml instanceof ParagraphML)) {
				PropertiesContainerML prop = ((ParagraphML) ml).getParagraphProperties();
				if (replace) {
					prop.removeAttributes(prop.getAttributeSet());
				}
				prop.addAttributes(paragraphAttrs);
			}
		}
	}
	
	static Templates xsltApplyRemoteChanges;	
	
	public final static void applyRemoteRevisions(
			javax.xml.transform.Source src, javax.xml.transform.Result result) {

		try {
			if (xsltApplyRemoteChanges == null) {
				Source xsltSource = new StreamSource(
						org.docx4j.utils.ResourceUtils
								.getResource("org/docx4all/util/ApplyRemoteChanges.xslt"));
				xsltApplyRemoteChanges = XmlUtils
						.getTransformerTemplate(xsltSource);
			}
			org.docx4j.XmlUtils.transform(src, xsltApplyRemoteChanges, null,
					result);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	static Templates xsltDiscardRemoteChanges;

	public final static void discardRemoteRevisions(
			javax.xml.transform.Source src, javax.xml.transform.Result result) {

		try {
			if (xsltDiscardRemoteChanges == null) {
				Source xsltSource = new StreamSource(
						org.docx4j.utils.ResourceUtils
								.getResource("org/docx4all/util/DiscardRemoteChanges.xslt"));
				xsltDiscardRemoteChanges = XmlUtils
						.getTransformerTemplate(xsltSource);
			}
			org.docx4j.XmlUtils.transform(src, xsltDiscardRemoteChanges, null,
					result);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	static Templates xsltExport;

	public final static WordprocessingMLPackage export(
			WordprocessingMLPackage source) {
		WordprocessingMLPackage theResult = null;

		try {
			boolean copyMainDocumentPart = true;
			boolean copyStyleDefPart = true;
			boolean copyDocPropsCustomPart = false;
			theResult = createNewPackage(source, copyMainDocumentPart,
					copyStyleDefPart, copyDocPropsCustomPart);
			if (xsltExport == null) {
				Source xsltSource = new StreamSource(
						org.docx4j.utils.ResourceUtils
								.getResource("org/docx4all/util/Export.xslt"));
				xsltExport = XmlUtils.getTransformerTemplate(xsltSource);
			}
			theResult.transform(xsltExport, null);

		} catch (Exception exc) {
			exc.printStackTrace();
			theResult = null;
		}

		return theResult;
	}

   /* Split a control containing n paragraphs
     * into n controls.  
     * 
     * The ID of the first control remains the same.
     * 
     * Returns a list of SdtBlock(s) that represents 
     * all new resulting content controls.
     */
    public final static List<SdtBlock> chunk(SdtBlock cc)
    {
    	SdtBlock copy = (SdtBlock) XmlUtils.deepCopy(cc);
    	
    	List<SdtBlock> theChunks = new ArrayList<SdtBlock>();
		theChunks.add(copy);
    	
    	List<Object> children = copy.getSdtContent().getEGContentBlockContent();
    	if (children.size() > 1) {
        	List<Object> childrenToChunk = new ArrayList<Object>();
    		for (int i=1; i < children.size(); i++) {
    			Object o = children.get(i);
    			if (o instanceof org.docx4j.wml.P
    				|| (o instanceof org.docx4j.wml.Tbl)) {
    				childrenToChunk.add(o);
    			} else {
    				//TODO: Consider what to do with these
    			}
    		}
    		
    		for (Object o: childrenToChunk) {
    			children.remove(o);
    			SdtBlock newChunk = createSdtBlock();
    			newChunk.getSdtContent().getEGContentBlockContent().add(o);
    			theChunks.add(newChunk);
    		}
    	}
    	
    	return theChunks;
    }

    private final static SdtBlock createSdtBlock() {
		org.docx4j.wml.SdtBlock sdtBlock = ObjectFactory.createSdtBlock();
		org.docx4j.wml.SdtPr sdtPr = ObjectFactory.createSdtPr();
		org.docx4j.wml.SdtContentBlock content = ObjectFactory.createSdtContentBlock();
		
		sdtPr.setId();
		sdtPr.setTag(ObjectFactory.createTag("0"));
		sdtBlock.setSdtPr(sdtPr);
		sdtBlock.setSdtContent(content);

		return sdtBlock;
    }

    public final static boolean containsTrackedChanges(Object jaxbObject) {
		String s = org.docx4j.XmlUtils.marshaltoString(jaxbObject, false);
		return (s.indexOf("</w:ins>") >= 0 
				|| s.indexOf("</w:del>") >= 0);
    }
    
    public final static org.docx4j.wml.SdtBlock markupDifference(
		org.docx4j.wml.SdtBlock leftSdt, 
		org.docx4j.wml.SdtBlock rightSdt,
		Changeset changeset) throws Exception {

		org.docx4j.wml.SdtBlock theSdt = ObjectFactory.createSdtBlock();
		theSdt.setSdtPr((org.docx4j.wml.SdtPr) XmlUtils.deepCopy(leftSdt
				.getSdtPr()));

		// javax.xml.bind.util.JAXBResult result =
		// new javax.xml.bind.util.JAXBResult(
		// org.docx4j.jaxb.Context.jc);

		java.io.StringWriter sw = new java.io.StringWriter();
		javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(
				sw);

		// Calendar changeDate = Calendar.getInstance();
		// changeDate.setTime(RFC3339_FORMAT.parse(changeset.getDate()));
		Calendar changeDate = null;

		ParagraphDifferencer pd = new ParagraphDifferencer();
		pd.diff(leftSdt.getSdtContent(), rightSdt
				.getSdtContent(), result, changeset.getModifier(), changeDate,
				null, null);

		// SdtContentBlock markedUpContent = (SdtContentBlock)
		// result.getResult();

		String contentStr = sw.toString();
		log.info("Transform: " + contentStr);
		SdtContentBlock markedUpContent = (SdtContentBlock) org.docx4j.XmlUtils
				.unmarshalString(contentStr);
		log.debug("..unmarshalled ");

		// Now put into resulting sdt.
		theSdt.setSdtContent(markedUpContent);
		return theSdt;
	}
        
	public final static org.docx4j.wml.SdtBlock markupAsDeletion(
		org.docx4j.wml.SdtBlock sdt,
		Changeset changeset) throws Exception {
		
		Map<String, Object> params = new HashMap<String, Object>(1);
		params.put("author", changeset.getModifier());
		
		return markupAsDeletion(sdt, params);
	}

	public final static org.docx4j.wml.SdtBlock markupAsDeletion(
		org.docx4j.wml.SdtBlock sdt,
		Map<String, Object> xsltParameters) throws Exception {
		String xml = XmlUtils.marshaltoString(sdt, false);
		return markupAsDeletion(xml, xsltParameters);
	}

	static Templates xsltMarkupAsDeletion;	
	
	public final static org.docx4j.wml.SdtBlock markupAsDeletion(
		String sdtXmlString,
		Map<String, Object> xsltParameters) throws Exception {
		
		StreamSource src = new StreamSource(new StringReader(sdtXmlString));

		javax.xml.bind.util.JAXBResult result = 
			new javax.xml.bind.util.JAXBResult(
				org.docx4j.jaxb.Context.jc);
		
		if (xsltMarkupAsDeletion == null) {
			Source xsltSource = new StreamSource(org.docx4j.utils.ResourceUtils
					.getResource("org/docx4all/util/MarkupAsDeletion.xslt"));
			xsltMarkupAsDeletion = XmlUtils.getTransformerTemplate(xsltSource);
		}
		org.docx4j.XmlUtils.transform(src, xsltMarkupAsDeletion,
				xsltParameters, result);

		org.docx4j.wml.SdtBlock newSdt = (org.docx4j.wml.SdtBlock) result.getResult();
		
		return newSdt;
	}

	public final static org.docx4j.wml.SdtBlock markupAsInsertion(
		org.docx4j.wml.SdtBlock sdt,
		Changeset changeset) throws Exception {
			
		Map<String, Object> params = new HashMap<String, Object>(1);
		params.put("author", changeset.getModifier());
			
		return markupAsInsertion(sdt, params);
	}

	public final static org.docx4j.wml.SdtBlock markupAsInsertion(
		org.docx4j.wml.SdtBlock sdt,
		Map<String, Object> xsltParameters) throws Exception {
		
		String xml = XmlUtils.marshaltoString(sdt, false);
		return markupAsInsertion(xml, xsltParameters);
	}

	static Templates xsltMarkupAsInsertion;	
	
	public final static org.docx4j.wml.SdtBlock markupAsInsertion(
		String sdtXmlString,
		Map<String, Object> xsltParameters) throws Exception {
		
		StreamSource src = new StreamSource(new StringReader(sdtXmlString));

		javax.xml.bind.util.JAXBResult result = new javax.xml.bind.util.JAXBResult(
				org.docx4j.jaxb.Context.jc);

		if (xsltMarkupAsInsertion == null) {
			Source xsltSource = new StreamSource(org.docx4j.utils.ResourceUtils
					.getResource("org/docx4all/util/MarkupAsInsertion.xslt"));
			xsltMarkupAsInsertion = XmlUtils.getTransformerTemplate(xsltSource);
		}
		org.docx4j.XmlUtils.transform(src, xsltMarkupAsInsertion,
				xsltParameters, result);

		org.docx4j.wml.SdtBlock newSdt = (org.docx4j.wml.SdtBlock) result.getResult();
		
		return newSdt;		
	}
	
	private XmlUtil() {
		;//uninstantiable
	}
}// XmlUtil class



















