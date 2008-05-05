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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.docx4all.swing.NewShareDialog;
import org.docx4all.ui.main.Constants;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.PropertiesContainerML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.w3c.dom.Node;

/**
 *	@author Jojada Tirtowidjojo - 04/01/2008
 */
public class XmlUtil {

	
	/**
	 * Serialise the WordprocessingMLPackage in pkg:package format
	 * 
	 * @param wmlPackage
	 * @param out
	 */
	public final static void serialize(WordprocessingMLPackage wmlPackage, OutputStream out) {
        try {
        	// Create a org.docx4j.wml.Package object
        	org.docx4j.wml.ObjectFactory factory = new org.docx4j.wml.ObjectFactory();
        	org.docx4j.wml.Package pkg = factory.createPackage();
        	
        	// Set its parts - at present, we only handle the main document part and the style part
        	
        	// .. the main document part
        	org.docx4j.wml.Package.Part pkgPartDocument = factory.createPackagePart();
        	    	
    		MainDocumentPart documentPart = wmlPackage.getMainDocumentPart(); 
    		
        	pkgPartDocument.setName(documentPart.getPartName().getName());
        	pkgPartDocument.setContentType(documentPart.getContentType() );
    		
        	org.docx4j.wml.Package.Part.XmlData XmlDataDoc = factory.createPackagePartXmlData();
        	
    		org.docx4j.wml.Document wmlDocumentEl = (org.docx4j.wml.Document)documentPart.getJaxbElement();
    		
    		XmlDataDoc.setDocument(wmlDocumentEl);
    		pkgPartDocument.setXmlData(XmlDataDoc);
    		pkg.getPart().add(pkgPartDocument);
    				
        	// .. the style part
        	org.docx4j.wml.Package.Part pkgPartStyles = factory.createPackagePart();

        	org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart stylesPart = documentPart.getStyleDefinitionsPart();
        	
        	pkgPartStyles.setName(stylesPart.getPartName().getName());
        	pkgPartStyles.setContentType(stylesPart.getContentType() );
        	
        	org.docx4j.wml.Package.Part.XmlData XmlDataStyles = factory.createPackagePartXmlData();
        	
        	org.docx4j.wml.Styles styles = (org.docx4j.wml.Styles)stylesPart.getJaxbElement();
        	
    		XmlDataStyles.setStyles(styles);
    		pkgPartStyles.setXmlData(XmlDataStyles);
    		pkg.getPart().add(pkgPartStyles);    	        	
        	
			JAXBContext jc = Context.jc;
			Marshaller marshaller = jc.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
					new org.docx4j.jaxb.NamespacePrefixMapper() ); // Must use 'internal' for Java 6
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
			JAXBContext jc = Context.jc;
			Unmarshaller u = jc.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

			org.docx4j.wml.Package wmlPackageEl = (org.docx4j.wml.Package)u.unmarshal(
					new javax.xml.transform.stream.StreamSource(in)); 

			org.docx4j.wml.Document wmlDocument = null;
			org.docx4j.wml.Styles wmlStyles = null;
			for (org.docx4j.wml.Package.Part p : wmlPackageEl.getPart() ) {
				
				if (p.getXmlData().getDocument()!= null) {
					wmlDocument = p.getXmlData().getDocument();
				}				
				if (p.getXmlData().getStyles()!= null) {
					wmlStyles = p.getXmlData().getStyles();
				}				
			}
				
			if (wmlPackage == null) {
				wmlPackage = ObjectFactory.createDocumentPackage(wmlDocument);
			} else {
				wmlPackage.getMainDocumentPart().setJaxbElement(wmlDocument);
			}
			
			// That handled the Main Document Part; now set the Style part.
			wmlPackage.getMainDocumentPart().getStyleDefinitionsPart().setJaxbElement(wmlStyles);
			
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
	 * Filters out certain features of WordprocessingML which docx4all can’t yet handle, 
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
		List<org.docx4j.docProps.custom.Properties.Property> list = customProps.getProperty();
		
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

	public final static void removeSharedDocumentProperties(WordprocessingMLPackage wmlPackage) {
		DocPropsCustomPart docPropsCustomPart = wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		List<org.docx4j.docProps.custom.Properties.Property> list = customProps.getProperty();
		if (list != null) {
			org.docx4j.docProps.custom.Properties.Property groupingProp = null;
			org.docx4j.docProps.custom.Properties.Property checkinProp = null;
			
			for (org.docx4j.docProps.custom.Properties.Property temp: list) {
				String s = temp.getName();
				if (Constants.PLUTEXT_GROUPING_PROPERTY_NAME.equals(s)) {
					groupingProp = temp;
				} else if (Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME.equals(s)) {
					checkinProp = temp;
				}
				if (groupingProp != null && checkinProp != null) {
					break;
				}
			}
			
			if (groupingProp != null) {
				list.remove(groupingProp);
			}
			if (checkinProp != null) {
				list.remove(checkinProp);
			}
		}
	}

	public final static void setSharedDocumentProperties(
		WordprocessingMLPackage wmlPackage,
		NewShareDialog dialog) {
		
		org.docx4j.openpackaging.parts.DocPropsCustomPart docPropsCustomPart = 
			wmlPackage.getDocPropsCustomPart();
		org.docx4j.docProps.custom.Properties customProps = 
			(org.docx4j.docProps.custom.Properties) docPropsCustomPart.getJaxbElement();
		org.docx4j.docProps.custom.ObjectFactory factory = 
			new org.docx4j.docProps.custom.ObjectFactory();

		//Set plutext:Grouping property
    	org.docx4j.docProps.custom.Properties.Property tempProp =
    		XmlUtil.getCustomProperty(
    			wmlPackage, Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
    	if (tempProp == null) {
    		tempProp = factory.createPropertiesProperty();
    		tempProp.setName(Constants.PLUTEXT_GROUPING_PROPERTY_NAME);
    		tempProp.setFmtid(
    			org.docx4j.openpackaging.parts.DocPropsCustomPart.fmtidValLpwstr); // Magic string
    		tempProp.setPid(customProps.getNextId()); 
    		customProps.getProperty().add(tempProp);
    	}
    	if (dialog.isGroupOnEachParagraph()) {
    		tempProp.setLpwstr("EachBlock");
    	} else {
    		tempProp.setLpwstr("Heading1");
    	}
    	
		//Set plutext:CheckinMessageEnabled property
    	tempProp =
    		XmlUtil.getCustomProperty(
    			wmlPackage, Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
    	if (tempProp == null) {
    		tempProp = factory.createPropertiesProperty();
    		tempProp.setName(Constants.PLUTEXT_CHECKIN_MESSAGE_ENABLED_PROPERTY_NAME);
    		tempProp.setFmtid(
    			org.docx4j.openpackaging.parts.DocPropsCustomPart.fmtidValLpwstr); // Magic string
    		tempProp.setPid(customProps.getNextId()); 
    		customProps.getProperty().add(tempProp);
    	}
    	if (dialog.isPromptForCheckinComment()) {
    		tempProp.setLpwstr(Boolean.TRUE.toString());
    	} else {
    		tempProp.setLpwstr(Boolean.FALSE.toString());
    	}
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
	
	private XmlUtil() {
		;//uninstantiable
	}
}// XmlUtil class



















