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
using System.Xml;
using Word = Microsoft.Office.Interop.Word;
using System.Windows.Forms;
using log4net;

namespace plutext.client.word2007
{
    class TransformInsert : TransformAbstract
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(TransformInsert));

        private string pos;
        public string Pos
        {
            get { return pos; }
            set { pos = value; }
        }

        public TransformInsert(XmlNode n)
            : base(n)
        {
            pos = n.Attributes.GetNamedItem("position", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE).Value;
        }

        public TransformInsert()
            : base()
        {
        }


        public override Int32 apply(Mediator mediator, Pkg pkg)
        {

            log.Debug(this.GetType().Name);

            // So first, find the @after existing sdt in the XmlDocument
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.PkgXmlDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            // if user has locally inserted/deleted sdt's
            // we need to adjust the specified position ...
            int adjustedPos = int.Parse(pos) + mediator.Divergences.getOffset(int.Parse(pos));

            log.Debug("Insertion location " + pos + " adjusted to " + adjustedPos);

            XmlNode refChild = pkg.PkgXmlDocument.SelectSingleNode("//w:sdt[" + adjustedPos + "]", nsmgr);

            if (refChild == null)
            {
                log.Debug("Couldn't find sdt " + id);

                //stateDocx.DeletedContentControls 

                return -1;
            }
            else
            {
                XmlNode parent = refChild.ParentNode;
                XmlNode importedNode = pkg.PkgXmlDocument.ImportNode(SDT, true);
                parent.InsertAfter(importedNode, refChild);

                pkg.StateChunks.Add(id, new StateChunk(sdt));
                mediator.Divergences.insert(id, adjustedPos);

                log.Debug("Inserted new sdt " + id + " in pkg");
                return sequenceNumber;
            }
        }

        String sdtXmlString = null;

        public void attachSdt( String xml )
        {
            sdtXmlString = xml;
        }

        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "insert");
            t.SetAttribute("position", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, pos);

            // Now attach the sdt
            XmlNode sdtNode = tf.ReadNode(new XmlTextReader(new System.IO.StringReader(sdtXmlString)));
            t.AppendChild(sdtNode);

            log.Debug(tf.OuterXml);

            return tf;
        }


    }
}
