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
using System.IO;
using System.Text;
using System.Xml;
using System.Xml.Serialization;
using Word = Microsoft.Office.Interop.Word;
//using System.Windows.Forms;
using log4net;

namespace plutext.client.word2007
{
    class TransformUpdate : TransformAbstract
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(TransformUpdate));

        public TransformUpdate(XmlNode n)
            : base(n)
        {

        }

        public TransformUpdate()
            : base()
        {
        }

        /* Compare the updated sdt to the original, replacing the
         * updated one with containing w:ins and w:del */
        public void markupChanges(String original)
        {
            // In this current proof of concept, we pass 
            // ParagraphDifferencer strings representing 
            // each sdt.
            // It will compare the paragraphs.
            // Currently ASSUMES there is one paragraph
            // in each SDT.

            String result = ParagraphDifferencer.diff(original, SDT.OuterXml);

            log.Debug("PD returned: " + result);

            // Have to slot the result back into SDT
            XmlDocument doc = new XmlDocument();
            doc.LoadXml(result);

            XmlNode importableNode = SDT.OwnerDocument.ImportNode(doc.DocumentElement, true);

            log.Debug("old sdt: " + SDT.OuterXml);

            XmlNode oldSdtPara = SDT.ChildNodes[1].FirstChild;
            log.Debug("old para: " + oldSdtPara.OuterXml);

            XmlNode parent = oldSdtPara.ParentNode;

            parent.ReplaceChild(importableNode, oldSdtPara);

            log.Debug("SDT now: " + SDT.OuterXml);
        }


        public override Int32 apply(Mediator mediator, Pkg pkg)
        {

            log.Debug(this.GetType().Name);

            // So first, find the @after existing sdt in the XmlDocument
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.PkgXmlDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            XmlNode refChild = pkg.PkgXmlDocument.SelectSingleNode("//w:sdt[w:sdtPr/w:id/@w:val='" + id + "']", nsmgr);

            if (refChild == null)
            {
                log.Debug("Couldn't find sdt to update " + id);
                // TODO - this will happen to a client A which deleted a chunk
                // or B which has the document open, if a client C reinstates
                // a chunk.  It will happen because A and B simply get an
                // update notice

                // One way to address this is to treat it as an insert,
                // asking for the skeleton doc in order to find out where
                // exactly to insert it.

                throw new SystemException();
            }
            else
            {
                XmlNode parent = refChild.ParentNode;
                XmlNode importedNode = pkg.PkgXmlDocument.ImportNode(SDT, true);
                parent.ReplaceChild(importedNode, refChild);

                pkg.StateChunks.Remove(id);
                pkg.StateChunks.Add(id, new StateChunk(sdt));

                log.Debug("Updated existing sdt " + id + " in pkg");
                return sequenceNumber;
            }

        }

        String sdtXmlString = null;

        public void attachSdt(String xml)
        {
            sdtXmlString = xml;
        }

        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "update");

            // Now attach the sdt
            XmlNode sdtNode = tf.ReadNode(new XmlTextReader(new System.IO.StringReader(sdtXmlString)));
            t.AppendChild(sdtNode);

            log.Debug(tf.OuterXml);

            return tf;
        }

    }
}
