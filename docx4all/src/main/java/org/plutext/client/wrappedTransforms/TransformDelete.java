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
using log4net;
//using System.Windows.Forms;


namespace plutext.client.word2007
{
    class TransformDelete : TransformAbstract
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(TransformDelete));

        public TransformDelete(XmlNode n)
            : base(n)
        {

        }

        public TransformDelete(String idref)
            : base()
        {
            this.id = idref;
        }

        /* delete the SDT given its ID. */
        public override Int32 apply(Mediator mediator, Pkg pkg)
        {

            // Find the sdt in the XmlDocument
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.PkgXmlDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            XmlNode target = pkg.PkgXmlDocument.SelectSingleNode("//w:sdt[w:sdtPr/w:id/@w:val='" + id + "']", nsmgr);

            if (target == null)
            {
                log.Debug("Couldn't find sdt " + id);
                // TODO - throw error
                return -1;
            }
            else
            {
                XmlNode parent = target.ParentNode;
                parent.RemoveChild(target);

                pkg.StateChunks.Remove(id);
                mediator.Divergences.delete(id);

                log.Debug("Removed sdt " + id + " from pkg");
                return sequenceNumber;
            }
        }

        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "delete");
            t.SetAttribute("idref", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, id);

            return tf;
        }

    }
}
