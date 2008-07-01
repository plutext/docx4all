/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of plutext-client-word2007.

    plutext-client-word2007 is free software: you can redistribute it and/or 
    modify it under the terms of version 3 of the GNU General Public License
    as published by the Free Software Foundation.

    plutext-client-word2007 is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with plutext-client-word2007.  If not, see 
    <http://www.gnu.org/licenses/>.
   
 */

using System;
using System.Collections.Generic;
using System.Text;
using Word = Microsoft.Office.Interop.Word;
using System.Xml;
using System.Xml.XPath;
using System.Xml.Xsl;
using log4net;
//using System.Security.Cryptography.Xml;
using System.IO;

namespace plutext.client.word2007
{

    /* Represent the state of a chunk.
     * 
     * Intent is to be able to compare the
     * state at a time we might save it to the
     * server with some previously known "clean" state.
     * 
     * We never directly look at the content
     * control itself 
     */ 
    public class StateChunk : ICloneable
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(StateChunk));

        public StateChunk(XmlNode sdt)
        {

            //log.Debug("I: " + sdt.OuterXml);

            // Canonicalise, using our custom canonicalizer
            XmlCanonicalizer canonicalizer = new XmlCanonicalizer (false, false);
            XmlDocument tmpDoc = new XmlDocument();
            XmlNamespaceManager tmpNsmgr = new XmlNamespaceManager(tmpDoc.NameTable);
            tmpNsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);
            //tmpNsmgr.AddNamespace("ns2", "http://schemas.openxmlformats.org/officeDocument/2006/relationships");
            //tmpNsmgr.AddNamespace("ns4", "http://schemas.openxmlformats.org/schemaLibrary/2006/main");
            //tmpNsmgr.AddNamespace("pkg", "http://schemas.microsoft.com/office/2006/xmlPackage");
            tmpDoc.LoadXml(sdt.OuterXml);
            Stream s = canonicalizer.Canonicalize(tmpDoc);
            byte[] bytes = new byte[s.Length];
            s.Position = 0;
            s.Read(bytes, 0, (int)s.Length);
            xml = new UTF8Encoding().GetString(bytes, 0, (int)s.Length);
            //log.Debug("C: " + xml);

            XmlNode sdtPr = sdt.FirstChild;

            XmlNamespaceManager nsmgr = new XmlNamespaceManager(sdt.OwnerDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            //log.Debug(sdtPr.OuterXml);
            //log.Debug(xml);

            XmlNode idAt = sdtPr.SelectSingleNode("w:id/@w:val", nsmgr);
            if (idAt == null)
            {
                log.Debug("ERROR: - no id?!");
            }
            id = idAt.Value;

            XmlNode tagAt = sdtPr.SelectSingleNode("w:tag/@w:val", nsmgr);
            if (tagAt == null)
            {
                log.Debug("debug: - no tag?!");

                // NB - Can't just set it here, since that won't
                // affect the xml field.
            }
            else
            {

                tag = tagAt.Value;
                log.Debug("set tag=" + tag);
            }

        }

        private String id = null;
        public String ID
        {
            get { return id; }
        }

        // The tag of the wrapped SDT
        protected string tag;
        public String Tag
        {
            get { return tag; }
        }

        private String xml = null;
        public String Xml
        {
            get { return xml; }
            set { xml = value; }
        }

        private Boolean transformUpdatesExist = false;
            // TODO - reset this to false each time thread starts

        //public Boolean TransformUpdatesExist
        //{
        //    get { return transformUpdatesExist; }
        //    set { transformUpdatesExist = value; }
        //}


        //TrackedChangeType trackedChangeType = TrackedChangeType.NA;
        //public TrackedChangeType TrackedChangeTOBE
        //{
        //    get { return trackedChangeType; }
        //    set { trackedChangeType = value; }
        //}

        public Boolean containsTrackedChanges()
        {
            if (xml.Contains("w:del")
                || xml.Contains("w:delText")
                || xml.Contains("w:ins"))
                return true;
            else
                return false;

        }

        public void acceptTrackedChanges()
        {
            transform("ChangesAccept.xslt");
        }
        public void rejectTrackedChanges()
        {
            transform("ChangesReject.xslt");
        }

        private void transform(String stylesheet)
        {
            log.Debug("In: " + xml);

            XmlDocument inDoc = new XmlDocument();
            inDoc.LoadXml(xml);
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(inDoc.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            // Stylesheet
            //XslCompiledTransform xsltTransform = new XslCompiledTransform();  
            XslTransform xslt = new XslTransform();
            xslt.Load(AppDomain.CurrentDomain.BaseDirectory + stylesheet);

            // Transform
            StringWriter stringWriter = new StringWriter();  
            XmlTextWriter transformedXml = new XmlTextWriter(stringWriter);  
     // Create a XslCompiledTransform to perform transformation  
            xslt.Transform(inDoc, null, transformedXml, null);


            // Ouptut
            xml = stringWriter.ToString();

            log.Debug("Transformed (" + stylesheet + "): " + xml);
        }

        public void removeStyleSeparator()
        {
            log.Debug("SS input: " + xml);
            transform("SdtRemoveStyleSeparator.xslt");
             // TODO - make more efficient, including by caching transform!
            log.Debug("SS fixed: " + xml);

        }


        public Object Clone()
        {
            return MemberwiseClone();
        }

        //Word.ContentControl cc;
        //public Word.ContentControl WrappedCC
        //{
        //    get { return cc; }
        //    set { cc = value; }
        //} 

        //private String rangeXml = null;
        //public String RangeXml
        //{
        //    get { return rangeXml; }
        //    set { rangeXml = value; }
        //}


        ///* Is this chunk known to have been edited since it was last saved? */
        //Boolean dirty = false;
        //public Boolean Dirty
        //{
        //    get { return dirty; }
        //    set { dirty = value; }
        //}

        //private String displayName = null;
        //public String DisplayName
        //{
        //    get { return displayName; }
        //    set { displayName = value; }
        //}

        //private String chunkStatus = null;
        //public String Status
        //{
        //    get { return chunkStatus; }
        //    set { chunkStatus = value; }
        //}


        ///* cc.Range.WordOpenXML returns an XML document which contains all 
        // * associates Parts eg style.xml etc.  But all we want is the 
        // * XML for the content control itself!
        //*/
        //public static String getContentControlXML(Word.ContentControl cc)
        //{

        //    log.Debug(".WordOpenXML: " + getContentControlNode(cc).OuterXml);
        //    return getContentControlNode(cc).OuterXml;
        //    //return "<w:sdt xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
        //    //    + node.InnerXml + "</w:sdt>";
        //}



        ///* Recursively strip the rsidRDefault attribute.
        //*/
        //private static void stripAttribute(XmlNode node) {

        //    if (node.Attributes != null &&
        //        node.Attributes.GetNamedItem("rsidRDefault", Namespaces.WORDML_NAMESPACE)!=null) {
        //        //XmlAttributeCollection attrs = node.Attributes;
        //        //foreach (XmlAttribute attr in attrs) {
                    
        //        //}
        //        node.Attributes.RemoveNamedItem("rsidRDefault", Namespaces.WORDML_NAMESPACE);
        //        //log.Debug("removed an @w:rsidRDefault");

        //    }

        //    XmlNodeList children = node.ChildNodes;
        //    foreach (XmlNode child in children) {
        //      stripAttribute(child);
        //    }
        //  }


        //public static XmlNode getContentControlNode(Word.ContentControl cc)
        //{

        //    // First, get the XML for the SDT
        //    String chunkID = cc.ID;
        //    String oXML = Globals.ThisAddIn.Application.ActiveDocument.WordOpenXML;
        //    // TODO - replace that with cc.Range.WordOpenXML?
        //    // The XML document will be smaller, but how expensive
        //    // is the .WordOpenXML operation?
        //    XmlDocument doc = new XmlDocument();
        //    doc.LoadXml(oXML);
        //    XmlNamespaceManager nsmgr = new XmlNamespaceManager(doc.NameTable);
        //    nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

        //    /*
        //     * 
        //        <w:sdt>
        //            <w:sdtPr>
        //                <w:id w:val="14270176" /> 

        //     */
        //    return doc.SelectSingleNode("//*[./w:sdtPr/w:id/@w:val='" + chunkID + "']", nsmgr);
        //}


        public static String getDebugRunSample(Word.ContentControl cc)
        {
            String ccTmpIdentifier = cc.Range.Text;
            String diagString = "";
            if (ccTmpIdentifier.Length > 100)
            {
                diagString = ccTmpIdentifier.Substring(0, 99) + " ...";
            }
            else
            {
                diagString = ccTmpIdentifier;
            }
            return diagString;
        }

        public Word.ContentControl getContentControl()
        {
            foreach (Word.ContentControl ctrl in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
            {
                if (ctrl.ID.Equals(id) )
                {
                    //diagnostics("DEBUG - Got control");
                    return ctrl;
                }
            }
            return null;
        }

    }
}
