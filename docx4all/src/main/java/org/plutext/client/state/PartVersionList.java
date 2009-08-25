package org.plutext.client.state;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.exceptions.Docx4JException;
//import org.dom4j.Document;
//import org.dom4j.Element;
//import org.dom4j.Node;
import org.plutext.client.partWrapper.Part;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PartVersionList {
	// used to extend Part, and still could readily enough

    /*
		<pkg:part pkg:name="/customXml/item1.xml" pkg:contentType="application/xml" pkg:padding="32">
		
		     OR
		     * 
		<pkg:part pkg:name="/part-versions.xml" pkg:contentType="text/xml" xmlns:pkg="http://schemas.microsoft.com/office/2006/xmlPackage">
		    <pkg:xmlData>
		        <parts>
		            <part ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" name="/docProps/app.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.custom-properties+xml" name="/docProps/custom.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.webSettings+xml" name="/word/webSettings.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.theme+xml" name="/word/theme/theme1.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.fontTable+xml" name="/word/fontTable.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml" name="/word/styles.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml" name="/word/settings.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-package.core-properties+xml" name="/docProps/core.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml" name="/word/document.xml" version="0" />
		            <part ContentType="application/vnd.openxmlformats-package.relationships+xml" name="/word/_rels/document.xml.rels" version="0" />
		        </parts>
		    </pkg:xmlData>
     */

    	private static Logger log = Logger.getLogger(PartVersionList.class);        
    	
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
        
    	private static DocumentBuilderFactory documentFactory;
    	private static DocumentBuilder documentBuilder;
    	
    	static {
    		
    		// Crimson doesn't support setTextContent; this.writeDocument also fails.
    		// We've already worked around the problem with setTextContent,
    		// but rather than do the same for writeDocument,
    		// let's just stop using it.
    		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
    			"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    		    		
    		documentFactory = DocumentBuilderFactory.newInstance();
    		documentFactory.setNamespaceAware(true);
    		try {
    			documentBuilder = documentFactory.newDocumentBuilder();
    		} catch (ParserConfigurationException e) {
    			e.printStackTrace();
    		}
    		
    		
    	}
        

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
        
        public PartVersionList(String pvlString)
        {
            
    		try {
    			xmlDoc = documentBuilder.parse( new ByteArrayInputStream(pvlString.getBytes("UTF-8")));
    		} catch (Exception e) {
    			//throw new Docx4JException("Problems parsing InputStream", e);
    			e.printStackTrace();
    		} 
            
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
         * 
         * 20090710, StateDocx stores a copy of this PartVersionList object;
         * although it also contains Dictionary<string, Part> parts,
         * it is in this PartVersionList object that we store the version
         * numbers.
         */

        protected HashMap<String, String> versions = new HashMap<String, String>();

        /// <summary>
        /// List of parts we need to fetch; order is important.
        /// If present, document rels, then comments, footnotes,
        /// and endnotes are listed first.
        /// </summary>
        /// <param name="currentLocalPVL"></param>
        /// <returns></returns>
        public List<String> partsNewerOnServer(PartVersionList currentLocalPVL)
        {
            List<String>  relevantParts = new ArrayList<String>();
            
            Iterator it = versions.keySet().iterator();
            while (it.hasNext()) {

				String name = (String) it.next();
				String serverVersion = (String) versions.get(name);
	
				String localVersion = currentLocalPVL.versions.get(name);
	
				if (localVersion == null) {
	
					// if the partName doesn't exist, then that's because its new,
					// so we need it
					log.debug(name + " - doesn't exist locally");
					relevantParts.add(name);
	
				} else if (Integer.parseInt(localVersion) < Integer
						.parseInt(serverVersion)) {
					// server version is newer, so we need it
					log.debug(name + " - newer on server ");
					relevantParts.add(name);
				} else {
					log.debug(name + " - local version is current ");
				}
	
			}

            return orderParts(relevantParts);
        }

        private List<String> orderParts(List<String> relevantParts)
        {
            List<String> orderedParts = new ArrayList<String>();

            // Enforce order: doc rels, comments, footnotes, endnotes
            for(String pn : sequenceableParts) {
                if (relevantParts.contains(pn)) {
                    orderedParts.add(pn);
                }
            }

            // Now, the rest
            for(String pn : relevantParts) {
                if (!sequenceableParts.contains(pn)) {
                    orderedParts.add(pn);
                }
            }

            return orderedParts;
        }

        
        public void setVersion(String partname, String version) {
			versions.put(partname, version);
			log.debug("setting " + partname + ", v" + version);
		}

        public String getVersion(String partname) {
            return versions.get(partname);
        }


        // initialisation
        public void setVersions() {

        	NodeList nodes;
            if (xmlDoc.getDocumentElement().getFirstChild().getLocalName().equals("parts"))
            {
                // xmlDoc from Microsoft.Office.Core.CustomXMLPart (StateDocx)
                nodes = xmlDoc.getDocumentElement().getChildNodes();
            }
            else
            {
                // xmlDoc from pkg:part
                nodes = xmlDoc.getDocumentElement().getFirstChild().getFirstChild().getChildNodes();
            }
        	
            if (nodes != null) {
                for (int i=0; i<nodes.getLength(); i++) {
                	Node n = (Node)nodes.item(i);
                	String name = n.getAttributes().getNamedItem("name").getNodeValue();
    				versions.put(name, n.getAttributes().getNamedItem("version").getNodeValue() );    				
    				log.debug("Set version on " + name);	
                }
            }
        	            
            // If it is a part we are interested in, but not there, it is
			// version 0 of course ...
        }
	
        /// <summary>
        /// Determine whether this part is one that we update.
        /// These are our so-called "second" and "third class" citizens.
        /// </summary>
        /// <param name="name"></param>
        /// <param name="contentType"></param>
        /// <returns></returns>
        public static boolean relevant(String name, String contentType)
        {
            if (sequenceableParts.contains(name))
            {
                return true;
            }

            // Header/footer parts
            if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml")
                    || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"))
            {
                return true;
            }

            // Header/footer rels - in fact any rels except the root one,
            // (document.xml.rels handled above)s
            if (contentType.equals("application/vnd.openxmlformats-package.relationships+xml")
                    && !name.equals("/_rels/.rels"))
            {
                return true;
            }

            // TODO: styles?, numbering?

            return false;

        }
        
	
}
