package org.plutext.client.partWrapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.plutext.client.Mediator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SequencedPart extends Part {

    /* This class represents an OpenPackaging Part, which
     * contains an ordered collection of id's. These include:
     * 
     * - rels
     * - comments
     * - endnotes, footnotes
     * 
     * Rels is a bit different, because the id's aren't ordered,
     * and nor is the "id" a number.
     * 
     * We're not interested in what _rels part it is a target of,
     * or any other semantics.
     */

    	private static Logger log = Logger.getLogger(SequencedPart.class);

        static List<String> sequenceableParts;
        public static List<String> getSequenceableParts()
        {
            return sequenceableParts; 
        }
        static
        {
            sequenceableParts = new ArrayList<String>();
            sequenceableParts.add("/word/_rels/document.xml.rels");
            sequenceableParts.add("/word/comments.xml");
            sequenceableParts.add("/word/footnotes.xml");
            sequenceableParts.add("/word/endnotes.xml");

        }

        public SequencedPart()
        {
        }
        public SequencedPart(org.w3c.dom.Document doc, String partXml)
        {
            init(doc, partXml);
            log.debug("List element: " + xmlNode.getFirstChild().getFirstChild().getLocalName() );
            sequencedElements = xmlNode.getFirstChild().getFirstChild().getChildNodes();
        }

        

        
        
        NodeList sequencedElements;


        public Node getNodeByIndex(int i)
        {
            return sequencedElements.item(i);
        }

        
}
