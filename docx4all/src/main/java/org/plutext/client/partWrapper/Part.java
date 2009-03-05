package org.plutext.client.partWrapper;

import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.plutext.client.Mediator;
import org.w3c.dom.Node;

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
    public static Part factory(String partXml)
    {    	
		javax.xml.parsers.DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		org.w3c.dom.Document doc = null;
		try {
			doc = dbf.newDocumentBuilder().parse(partXml);
		} catch (Exception e) {
			log.error(e);
		}
        return factory(doc);
    }

    public static Part factory(org.docx4j.openpackaging.parts.JaxbXmlPart docx4jPart)
    {
        // marshal JaxbXmlPart
        return factory(
        		org.docx4j.XmlUtils.marshaltoW3CDomDocument( 
        				docx4jPart.getJaxbElement() ));
    }
    

    public static Part factory(org.w3c.dom.Document doc)
    {
    	
    	
    	
        log.debug("documentElemnet: " + doc.getDocumentElement().getNodeName() );
        
        Node xmlNode = (Node)doc.getDocumentElement().getFirstChild();

        String name = xmlNode.getAttributes().getNamedItemNS("name", 
        		Namespaces.PKG_XML).getNodeValue();

        log.debug("part: " + name );

        if (name.equals("/word/_rels/document.xml.rels"))
        {
            return new SequencedPartRels(doc);
        }
        else if (name.equals("/word/footnotes.xml")
                    | name.equals("/word/endnotes.xml")
                    | name.equals("/word/comments.xml"))
        {
            return new SequencedPart(doc);
        } else if (name.startsWith("/customXml") )
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
                return new Part(doc);
            //}
        } else 
        {
            return new Part(doc);
        } 
    }

    public Part()
    { }

    public Part(org.w3c.dom.Document doc)
    {
        init(doc);
    }

    public void init(org.w3c.dom.Document doc)
    {
        /*
         * <pkg:part pkg:name="/_rels/.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml" pkg:padding="512">
         * 
         * <pkg:part pkg:name="/word/_rels/document.xml.rels" pkg:contentType="application/vnd.openxmlformats-package.relationships+xml"
         * 
         * <pkg:part pkg:name="/word/document.xml" pkg:contentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml">
         */
    	
    	this.jxp = jxp;

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
    	name = xmlNode.getAttributes().getNamedItemNS("name", 
        		Namespaces.PKG_XML).getNodeValue();
//        contentType = xmlNode.Attributes.GetNamedItem("contentType", Namespaces.PKG_NAMESPACE).Value;

    }
    
    JaxbXmlPart jxp;

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
//    // pkg:part/pkg:xmlData/ZZZ, where ZZZ is what we
//    // want to be able to transmit to the server;
//    // or pkg:part/pkg:binaryData/ZZZ
//    // Hmm, better if we send the xmlData|binaryData
//    // node?  No, since then we have to strip that tag.
//    private String unwrappedXml = null;
//    public String UnwrappedXml
//    {
//        get { return unwrappedXml; }
//        set { unwrappedXml = value; }
//    }
//
//
    protected String name;
      public String getName() {
    	  return name;
      }
//
//    protected string contentType;
//    public string ContentType
//    {
//        get { return contentType; }
//        set { contentType = value; }
//    }


}
