package org.plutext.client.partWrapper;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SequencedPartRels extends SequencedPart {

    /* This class represents an OpenPackaging rels Part
     */
	
        /*
         * Word orders the rels as follows:
         * 
FIXED_RELS_PREFIX:
  Id="rId1" Type="customXml" Target="../customXml/item1.xml" />
  Id="rId2" Type="numbering" Target="numbering.xml" />
  Id="rId3" Type="styles" Target="styles.xml" />
  Id="rId4" Type="settings" Target="settings.xml" />
  Id="rId5" Type="webSettings" Target="webSettings.xml" />
  Id="rId6" Type="footnotes" Target="footnotes.xml" />
  Id="rId7" Type="endnotes" Target="endnotes.xml" />

In order in section: 

  Id="rId8" Type="comments" Target="comments.xml" />  <---- appears once in order first encountered
  Id="rId9" Type="image" Target="media/image1.jpeg" />
  Id="rId10" Type="hyperlink" Target="http://fourthestate.com" TargetMode="External" />

End of section: 
          
  Id="rId11" Type="header" Target="header1.xml" />
  Id="rId12" Type="footer" Target="footer1.xml" />
  Id="rId13" Type="footer" Target="footer2.xml" />

In order in section: 
          
  Id="rId14" Type="hyperlink" Target="http://slashdot.org" TargetMode="External" />

FIXED_RELS_SUFFIX:
         
  Id="rId15" Type="fontTable" Target="fontTable.xml" />
  Id="rId16" Type="theme" Target="theme/theme1.xml" />
         * 
         * 
         */

    	private static Logger log = Logger.getLogger(SequencedPartRels.class);

        public SequencedPartRels(org.w3c.dom.Document doc)
        {
            init(doc);

            // pkg:part/pkg:xmlData/Relationships
            log.debug("List element: " + xmlNode.getFirstChild().getFirstChild().getLocalName() );
            
            NodeList nl = xmlNode.getFirstChild().getFirstChild().getChildNodes();            
            for (int i=0 ; i < nl.getLength() ; i++ )
            {
                String id = nl.item(i).getAttributes().getNamedItem("Id").getNodeValue();  
                nodesMap.put(id, nl.item(i) );

            }

            // Calculate prefixedRelsCount and suffixedRelsCount
            boolean inPrefix = true;
            boolean inSuffix = false;
            for (int i=1 ; i <= nodesMap.size(); i++) {

                Node n = nodesMap.get("rId" + i);
                String type = n.getAttributes().getNamedItem("Type").getNodeValue();  
                type = type.substring(type.lastIndexOf("/") +1);
                log.debug("Inspecting  rId" + i + " of " + type);
                if (inPrefix
                    && (type.equals("comments")
                            || type.equals("image")
                            || type.equals("hyperlink")
                            || type.equals("header")
                            || type.equals("footer")
                            || type.equals("oleObject")
                            || type.equals("fontTable")
                            || type.equals("theme")))
                    // TODO - this code relies on that list being exhaustive!
                {
                    // This is the end of the PREFIX
                    log.debug(".. end prefix! " );
                    inPrefix = false;
                    prefixedRelsCount = i-1;
                }

                if (inSuffix
                    || (type.equals("fontTable")
                            || type.equals("theme")))
                {
                    suffixedRelsCount++;
                }

            }

        }

        HashMap<String, Node> nodesMap = new HashMap<String, Node>();

        public Node getNodeById(String id)
        {
            return nodesMap.get(id);
        }

        public Node getNodeByType(String wantedType) // eg "comments"
        {
            for (int i = 1; i <= nodesMap.size(); i++)
            {

                Node n = nodesMap.get("rId" + i);
                String type = n.getAttributes().getNamedItem("Type").getNodeValue();
                type = type.substring(type.lastIndexOf("/") + 1);
                log.debug("Inspecting  rId" + i + " of " + type);
                if (type.equals(wantedType) )
                {
                    return n;
                }
            }
            return null;
        }

        int prefixedRelsCount = 0;
        public int getPrefixedRelsCount()
        {
            return prefixedRelsCount;
        }

        int suffixedRelsCount = 0;
        public int getSuffixedRelsCount()
        {
            return suffixedRelsCount;
        }

	
}
