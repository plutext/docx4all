/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client.state;

import org.apache.log4j.Logger;
import org.docx4j.wml.SdtBlock;
import java.math.BigInteger;
import org.docx4j.wml.Id;

    /* Represent the state of a chunk.
     * 
     * Intent is to be able to compare the
     * state at a time we might save it to the
     * server (eg on leaving a content control),
     * with some previously known "clean" state.
     * 
     * Because pPr (eg style, alignment) 
     * can be changed from outside
     * the content control, it is not sufficient
     * to just compare the value on exit to the
     * value on entry.
     * 
     */ 
    public class ContentControlSnapshot
    {

    	private static Logger log = Logger.getLogger(ContentControlSnapshot.class);
    	
    	Id id;
		public Id getId() {
			return id;
		}
    	
        public ContentControlSnapshot(SdtBlock cc)
        {
            this.cc = cc;
            log.debug("constructor invoked");
//            pointInTimeXml = getContentControlXMLNormalised(cc);
            pointInTimeXml = getContentControlXML(cc);
            dirty = false;

            id = cc.getSdtPr().getId();
        }

        public void refresh() {
            // log.debug("refresh() invoked " + System.Environment.StackTrace);
            
            // Typically, this is called after
            // the chunk has been saved to the server.
//            pointInTimeXml = getContentControlXMLNormalised(cc);
            pointInTimeXml = getContentControlXML(cc);
            dirty = false;
            log.debug("Refreshed " + cc.getSdtPr().getId().getVal() );
//            		+ " ContentControlSnapshot: " + RangeXml);
        }


        SdtBlock cc;
        public SdtBlock getSdtBlock() {
        	return cc;
        }

        private String pointInTimeXml = null;
		public String getPointInTimeXml() {
			return pointInTimeXml;
		}


        /* Is this chunk known to have been edited since it was last saved? */
        boolean dirty = false;
		public boolean getDirty() {
			return dirty;
		}
		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

//        private String displayName = null;
//
//        private String chunkStatus = null;


        public String getContentControlXML()
        {
        	return getContentControlXML(cc);
        	
        }
		
        /* cc.Range.WordOpenXML returns an XML document which contains all 
         * associates Parts eg style.xml etc.  But all we want is the 
         * XML for the content control itself!
        */
        public static String getContentControlXML(SdtBlock cc)
        {
        	
        	boolean suppressDeclaration = true;
        	return org.docx4j.XmlUtils.marshaltoString(cc, suppressDeclaration);

            //return "<w:sdt xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
            //    + node.InnerXml + "</w:sdt>";
        }


        /* Strip the rsidRDefault attribute, which change between the time the document is 
         * opened and the time the content control is exited.
        */
//        public static String getContentControlXMLNormalised(SdtBlock cc)
//        {
//
//            // Could do this with a regex operation on the string,
//            // but I'll recursively traverse the XML instead.
//
//
//            XmlNode n = getContentControlNode(cc);
//            stripAttribute(n);
//            //log.debug("STRIPPED = " + n.OuterXml);
//            return n.OuterXml;
//        }

        /* Recursively strip the rsidRDefault attribute. - not currently needed
         * in docx4all.
        */
//        private static void stripAttribute(XmlNode node) {
//
//            if (node.Attributes != null &&
//                node.Attributes.GetNamedItem("rsidRDefault", Namespaces.WORDML_NAMESPACE)!=null) {
//                //XmlAttributeCollection attrs = node.Attributes;
//                //foreach (XmlAttribute attr in attrs) {
//                    
//                //}
//                node.Attributes.RemoveNamedItem("rsidRDefault", Namespaces.WORDML_NAMESPACE);
//                //log.debug("removed an @w:rsidRDefault");
//
//            }
//
//            XmlNodeList children = node.ChildNodes;
//            foreach (XmlNode child in children) {
//              stripAttribute(child);
//            }
//          }


//        public static XmlNode getContentControlNode(SdtBlock cc)
//        {
//
//            // First, get the XML for the SDT
//            String chunkID = cc.ID;
//            String oXML = Globals.ThisAddIn.Application.ActiveDocument.WordOpenXML;
//            // TODO - replace that with cc.Range.WordOpenXML?
//            // The XML document will be smaller, but how expensive
//            // is the .WordOpenXML operation?
//            XmlDocument doc = new XmlDocument();
//            doc.LoadXml(oXML);
//            XmlNamespaceManager nsmgr = new XmlNamespaceManager(doc.NameTable);
//            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);
//
//            /*
//             * 
//                <w:sdt>
//                    <w:sdtPr>
//                        <w:id w:val="14270176" /> 
//
//             */
//            return doc.SelectSingleNode("//*[./w:sdtPr/w:id/@w:val='" + chunkID + "']", nsmgr);
//        }




//        public static String getDebugRunSample(SdtBlock cc)
//        {
//            String ccTmpIdentifier = cc.Range.Text;
//            String diagString = "";
//            if (ccTmpIdentifier.Length > 100)
//            {
//                diagString = ccTmpIdentifier.Substring(0, 99) + " ...";
//            }
//            else
//            {
//                diagString = ccTmpIdentifier;
//            }
//            return diagString;
//        }

    }
