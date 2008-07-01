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

package org.plutext.client.wrappedTransforms;

import org.apache.log4j.Logger;
import org.plutext.transforms.Transforms.T;

using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Word = Microsoft.Office.Interop.Word;
using log4net;
//using System.Windows.Forms;


    public class TransformStyle extends TransformAbstract
    {
    	private static Logger log = Logger.getLogger(TransformStyle.class);

        public TransformStyle(T t)
        {
        	super(t);
        }
        

        public override Int32 apply(Mediator mediator, Pkg pkg)
        {
            log.Debug("TransformStyle not fully implemented!");

            /* TODO: Insert the XML for the style(s) in the pkg,
             * replacing existing definition.
             */

            return SequenceNumber;
        }

        String styleXmlString = null;

        public void attachStyle(String xml)
        {
            styleXmlString = xml;
        }

        public override XmlDocument marshal()
        {
            XmlDocument tf = createDocument();
            XmlElement t = tf.CreateElement("t", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);
            tf.AppendChild(t);

            t.SetAttribute("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE, "update");

            // Now attach the sdt
            XmlNode styleNode = tf.ReadNode(new XmlTextReader(new System.IO.StringReader(styleXmlString)));
            t.AppendChild(styleNode);

            log.Debug(tf.OuterXml);

            return tf;
        }



    }
