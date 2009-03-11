package org.plutext.client.partWrapper;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.plutext.client.Mediator;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class Part {
	
	
	private static Logger log = Logger.getLogger(Part.class);

	/*
	 * We need to be able to create this Part wrapper from:
	 * 
	 * 1.  an string of xml for serverSequencedParts, since this is 
	 * 	   what we get from the server.
	 * 
	 * 2.  a JaxbXmlPart, since this is what we have when
	 *     we extractParts from the local document
	 *     
	 * In the Word Add-In, we operate at the XML DOM level.
	 * 
	 * In docx4all, we could operate at the XML DOM level, or
	 * potentially at the JAXB level.
	 * The problem with operating at the JAXB level is that
	 * we have:
	 *     List<Comments.Comment>
	 *     List<CTFtnEdn> footnote;
	 *     List<Relationship> relationship;
	 * Whilst I can handle all these as List<Object>, I need to 
	 * be able to use comment.setId(BigInteger), rel.setId(String)
	 * methods, which I think I could do via reflection.
	 * 
	 * But all things considered, its better to keep the
	 * code similar to the Word Add-In, so that means
	 * XML DOM level. 
	 */
	
	
	// Handle XML String we get from the server
//    public static Part factory(String partXml)
//    {    	
//		javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		dbf.setNamespaceAware(true);
//		org.w3c.dom.Document doc = null;
//		try {
//			doc = dbf.newDocumentBuilder().parse(partXml);
//		} catch (Exception e) {
//			log.error(e);
//		}
//        return factoryWorker(doc);
//    }

    public static Part factory(org.docx4j.openpackaging.parts.JaxbXmlPart docx4jPart)
    {
        // marshal JaxbXmlPart
    	// to a string, so we can capture string representation
    	String partXml = org.docx4j.XmlUtils.marshaltoString(docx4jPart.getJaxbElement(), true); // suppressDeclaration
    	
    	return factory(partXml, docx4jPart.getPartName().getName() );
    	
//        return factoryWorker(
//        		org.docx4j.XmlUtils.marshaltoW3CDomDocument( 
//        				docx4jPart.getJaxbElement() ));
    }
    
	// TODO - Handle XML String we get from the server!
    // Unlike the JAXB XML, this is wrapped in a part element
    // Need to handle both
    
    
    
    public static Part factory(String partXml, String partName)
    {   
    	log.debug(partName + " : " + partXml);
    	
		javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		org.w3c.dom.Document doc = null;
		try {
			doc = dbf.newDocumentBuilder().parse(
					new InputSource(
							new StringReader(partXml)));
		} catch (Exception e) {
			log.error(e);
		}
    	
        log.debug("documentElemnet: " + doc.getDocumentElement().getNodeName() );
        
        Node xmlNode = (Node)doc.getDocumentElement().getFirstChild();

//        String name = xmlNode.getAttributes().getNamedItemNS("name", 
//        		Namespaces.PKG_XML).getNodeValue();

        log.debug("part: " + partName );

        Part part = null;
        if (partName.equals("/word/_rels/document.xml.rels"))
        {
            part = new SequencedPartRels(doc, partXml);
        }
        else if (partName.equals("/word/footnotes.xml")
                    | partName.equals("/word/endnotes.xml")
                    | partName.equals("/word/comments.xml"))
        {
        	part = new SequencedPart(doc, partXml);
        } else if (partName.startsWith("/customXml") )
        {
//            XmlNode n = xmlNode.FirstChild.FirstChild;
//            log.Debug("customXml contains: " + n.Name);

            // 2009 03 03, this is now gone by the time this runs

            //if (n.Name.Equals("parts"))
            //{
            //    log.Debug("Got it! " );
            //    return new PartVersionList(xmlNode);
            //}
            //else
            //{
        	part = new Part(doc, partXml);
            //}
        } else 
        {
        	part = new Part(doc, partXml);
        } 
        part.name = partName;
        return part;
    }

    public Part()
    { }

    public Part(org.w3c.dom.Document doc, String partXml)
    {
        init(doc, partXml);
    }

    public void init(org.w3c.dom.Document doc, String partXml)
    {
        /*
         * <pkg:part pkg:name="/_rels/.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml" pkg:padding="512">
         * 
         * <pkg:part pkg:name="/word/_rels/document.xml.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml"
         * 
         * <pkg:part pkg:name="/word/document.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml">
         */
    	
    	//this.jxp = jxp;

        // Record XML for the purposes of detecting changes we 
        // may have to transmit.  Note that although fetching updates
        // can create a Part object out of XML sent by the
        // server, the Part objects examined in transmission
        // are each generated by Word.  (And in any case, the
        // ones sent by the server are never marshalled/unmarshalled
        // as at 2008 12 18)
    	
    	xmlNode = (Node)doc.getDocumentElement().getFirstChild();
    	
    	
//        xml = xmlNode.OuterXml;
//
//        // pkg:part/pkg:xmlData/ZZZ, where ZZZ is what we
//        // want to be able to transmit to the server
//        unwrappedXml = xmlNode.FirstChild.FirstChild.OuterXml;
//
//        this.xmlNode = xmlNode;
//
    	unwrappedXml = partXml;
    	
    	
//        contentType = xmlNode.getAttributes().getNamedItemNS("contentType", 
//        		Namespaces.PKG_XML).getNodeValue();

    }
    
    //JaxbXmlPart jxp;

//    // Reference to original node.
//    // Not of any use if the constructor took a string rather than the node!
    protected Node xmlNode;
    public Node getXmlNode() {
    	return xmlNode;    	
    }
//
//    private String xml = null;
//    public String Xml
//    {
//        get { return xml; }
//        set { xml = value; }
//    }
//
    
    // pkg:part/pkg:xmlData/ZZZ, where ZZZ is what we
    // want to be able to transmit to the server;
    // or pkg:part/pkg:binaryData/ZZZ
    // Hmm, better if we send the xmlData|binaryData
    // node?  No, since then we have to strip that tag.
    private String unwrappedXml = null;
    public String getUnwrappedXml() {
    	return unwrappedXml;
    }

    protected String name;
      public String getName() {
    	  return name;
      }

    // TODO - 
    protected String contentType;
    public String getContentType() {
        return contentType; 
    }


}
