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

package org.plutext.client;

import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.plutext.Context;
import org.plutext.server.transitions.Transitions;



public class Skeleton implements IDiffList
{
	private static Logger log = Logger.getLogger(Skeleton.class);
	
    public Skeleton(Transitions t)
    {
        /*
         * <dst:transitions>
         *   <dst:ribs>
         *      <dst:rib id="54989358" />
         *      <dst:rib id="1447653797" />
         *        :
         * </dst:transitions>
         * 
         * */

    	for ( Transitions.Ribs.Rib r : t.getRibs().getRib() ) {
    		
    		ribs.add( new TextLine( Long.toString(r.getId() ) ) );
    		
    	}

    }

    
    public Skeleton() {
    	
    }
    

    // Version number
    int version;

    // Ordered list of ribs
    ArrayList ribs = new ArrayList();
    public ArrayList getRibs() {
		return ribs;
	}
	public void setRibs(ArrayList ribs) {
		this.ribs = ribs;
	}
    

    private static void TextDiff(Skeleton source, Skeleton dest)
    {

        try
        {
            double time = 0;
            DiffEngine de = new DiffEngine();
            time = de.ProcessDiff(source, dest, DiffEngineLevel.Medium);

            System.Collections.ArrayList rep = de.DiffLines;

            //log.Debug(de.Results(source, dest, rep));
        }
        catch (Exception ex)
        {
            string tmp = string.Format("{0}{1}{1}***STACK***{1}{2}",
                ex.Message,
                Environment.NewLine,
                ex.StackTrace);
            log.Debug(tmp);
            return;
        }
    }

    //#region IDiffList Members

    public int Count()
    {
        return ribs.Count;
    }

    public IComparable GetByIndex(int index)
    {
        return (TextLine)ribs[index];
    }

    //#endregion

    public class TextLine implements IComparable
    {
        public String line;
		public String getLine() {
			return line;
		}
		
        public int _hash;

        public TextLine(String str)
        {
            line = str.Replace("\t", "    ");
            _hash = str.GetHashCode();
        }
        //#region IComparable Members

        public int CompareTo(object obj)
        {
            return _hash.CompareTo(((TextLine)obj)._hash);
        }


        //#endregion
    }

}
