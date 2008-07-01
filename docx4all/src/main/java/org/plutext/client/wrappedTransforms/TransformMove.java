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
    class TransformMove : TransformAbstract
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(TransformMove));

        private string pos;
        public string Pos
        {
            get { return pos; }
            set { pos = value; }
        }

        public TransformMove(XmlNode n)
            : base(n)
        {
            pos = n.Attributes.GetNamedItem("position", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE).Value;
        }

        public TransformMove()
            : base()
        {
        }


        public override Int32 apply(Mediator mediator, Pkg pkg)
        {

            log.Debug(this.GetType().Name);

            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.PkgXmlDocument.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            // Semantics of move are
            // 1 remove existing element from list
            // 2 insert new element

            // So

            // First, find and remove the existing element
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

                // No need to 
                // pkg.StateChunks.Remove(id);

                // QUESTION: interaction between divergences object and existing entry??
                // - Probably have to do divergences.delete?
                mediator.Divergences.delete(id);
            }


            // Second, re-insert it at the correct location


            // if user has locally inserted/deleted sdt's
            // we need to adjust the specified position ...
            int adjustedPos = int.Parse(pos) + mediator.Divergences.getOffset(int.Parse(pos));

            log.Debug("Move location " + pos + " adjusted to " + adjustedPos);

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
                parent.InsertAfter(target, refChild); // or before??

                //pkg.StateChunks.Add(id, new StateChunk(sdt));
                mediator.Divergences.insert(id, adjustedPos);

                log.Debug("Moved sdt " + id + " in pkg");
                return sequenceNumber;
            }
        }


        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "move");
            t.SetAttribute("position", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, pos);
            t.SetAttribute("idref", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, ID);

            log.Debug(tf.OuterXml);

            return tf;
        }


    }
}
