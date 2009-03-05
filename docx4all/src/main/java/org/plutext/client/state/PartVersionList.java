package org.plutext.client.state;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.plutext.client.partWrapper.Part;

public class PartVersionList {
	// used to extend Part, and still could readily enough

    /*
     * <pkg:part pkg:name="/customXml/item1.xml" pkg:contentType="application/xml" pkg:padding="32">
<pkg:xmlData><parts>
				<part name="/docProps/app.xml" version="0"/>
				<part name="/docProps/custom.xml" version="0"/>
				<part name="/word/webSettings.xml" version="0"/>
				<part name="/word/theme/theme1.xml" version="0"/>
				<part name="/word/fontTable.xml" version="0"/>
				<part name="/word/styles.xml" version="0"/>
				<part name="/word/settings.xml" version="0"/>
				<part name="/docProps/core.xml" version="0"/>
				<part name="/word/document.xml" version="1"/>
			</parts></pkg:xmlData></pkg:part></pkg:package>
     * 
     */

    	private static Logger log = Logger.getLogger(PartVersionList.class);        

        public PartVersionList()
        { }

        public PartVersionList(Document xmlDoc)
        {
        	// NB, this class uses dom4j document, since
        	// that is what is used in the underlying part.
        	
            this.xmlDoc = xmlDoc;
            //this.xmlNode = xmlNode;
            //log.Debug(xmlNode.OuterXml);
            //init(xmlNode);

            // We need to be able to remove this part from the
            // document (and especially the reference to it
            // in document.xml.rels), so that it isn't transmitted and
            // persisted. That is done in StateDocx


            //name = xmlNode.Attributes.GetNamedItem("name", Namespaces.PKG_NAMESPACE).Value;
        }

        protected Document xmlDoc;

        //protected XmlNode xmlNode;
        //protected string name;

        /*
         * Best to store a record of part version numbers in a suitable
         * Dictionary in StateDocx, rather than in the parts themselves,
         * since the parts in the document can be newer than the parts
         * in StateDocx, and we don't have persistent objects corresponding
         * to the current parts in the document.
         */

        HashMap<String, String> versions = new HashMap<String, String>();

        public void setVersion(String partname, String version)
        {
            versions.put(partname, version);
            log.debug("setting " + partname + ", v" + version);
        }

        public String getVersion(String partname)
        {
            return versions.get(partname);
        }


        // initialisation
        public void setVersions(HashMap<String, Part> parts)
        {
            //NodeList nodes = ((Node)xmlDoc.content().get(0))..content();
        	
        	Element root = xmlDoc.getRootElement();
        	
        	Iterator elementIterator = root.elementIterator();
        	while (elementIterator.hasNext() ) {
        		
        		Element element = (Element)elementIterator.next();
				// log.Debug(n.LocalName);
				String name = element.attributeValue("name");
	
				versions.put(name, element.attributeValue("version"));
	
				log.debug("Set version on " + name);	
        		
        	}
        	

            
            // If it is a part we are interested in, but not there, it is
			// version 0 of course ...
        }
	
	
}
