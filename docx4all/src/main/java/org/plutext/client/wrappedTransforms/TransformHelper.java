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


namespace plutext.client.word2007
{
    public class TransformHelper
    {
        public static TransformAbstract construct(  XmlNode n )
        {

            string operation = n.Attributes.GetNamedItem("op", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE).Value;

            if (operation.Equals("update"))
            {
                return new TransformUpdate(n);
            }
            else if (operation.Equals("delete"))
            {
                return new TransformDelete(n);
            }
            else if (operation.Equals("insert"))
            {
                return new TransformInsert(n);
            }
            else if (operation.Equals("move"))
            {
                return new TransformMove(n);
            }
            else if (operation.Equals("style"))
            {
                return new TransformStyle(n);
            }
            else if (operation.Equals("failed"))
            {
                return new TransformFailed(n);
            }
            else throw new System.Exception();

        }
    }
}
