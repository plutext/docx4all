package org.plutext.client.partWrapper;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.plutext.client.Mediator;

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

        public SequencedPartRels(JaxbXmlPart jxp)
        {
            init(xmlNode);

            // pkg:part/pkg:xmlData/Relationships
            foreach (XmlNode xn in xmlNode.FirstChild.FirstChild.ChildNodes)
            {

                String id = xn.Attributes.GetNamedItem("Id").Value;  
                nodesMap.Add(id, xn);

            }

            // Calculate prefixedRelsCount and suffixedRelsCount
            bool inPrefix = true;
            bool inSuffix = false;
            for (int i=1 ; i <= nodesMap.Count; i++) {

                XmlNode n = nodesMap["rId" + i];
                String type = n.Attributes.GetNamedItem("Type").Value;  
                type = type.Substring(type.LastIndexOf("/") +1);
                log.Debug("Inspecting  rId" + i + " of " + type);
                if (inPrefix
                    && (type.Equals("comments")
                            || type.Equals("image")
                            || type.Equals("hyperlink")
                            || type.Equals("header")
                            || type.Equals("footer")
                            || type.Equals("oleObject")
                            || type.Equals("fontTable")
                            || type.Equals("theme")))
                    // TODO - this code relies on that list being exhaustive!
                {
                    // This is the end of the PREFIX
                    log.Debug(".. end prefix! " );
                    inPrefix = false;
                    prefixedRelsCount = i-1;
                }

                if (inSuffix
                    || (type.Equals("fontTable")
                            || type.Equals("theme")))
                {
                    suffixedRelsCount++;
                }

            }

        }

        Dictionary<string, XmlNode> nodesMap = new Dictionary<string, XmlNode>();

        public XmlNode getNodeById(String id)
        {
            return nodesMap[id];
        }

        public XmlNode getNodeByType(String wantedType) // eg "comments"
        {
            for (int i = 1; i <= nodesMap.Count; i++)
            {

                XmlNode n = nodesMap["rId" + i];
                String type = n.Attributes.GetNamedItem("Type").Value;
                type = type.Substring(type.LastIndexOf("/") + 1);
                log.Debug("Inspecting  rId" + i + " of " + type);
                if (type.Equals(wantedType) )
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
