/*
 *  Copyright 2007, Plutext Pty Ltd.
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

package org.docx4all.swing;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

/**
 *	@author Jojada Tirtowidjojo - 05/12/2008
 */
public class PDFViewer extends com.sun.pdfview.PDFViewer {
	private static Logger log = Logger.getLogger(PDFViewer.class);
	
    public PDFViewer(boolean useThumbs) {
    	super(useThumbs);
    }
    
    public Icon getIcon(String name) {
        Icon icon = null;
        URL url = null;
        try {
            url = com.sun.pdfview.PDFViewer.class.getResource(name);

            icon = new ImageIcon(url);
            if (icon == null) {
                log.error("Couldn't find " + url);
            }
        } catch (Exception e) {
            log.error(
            	"Couldn't find " 
            	+ com.sun.pdfview.PDFViewer.class.getName() 
            	+ "/" 
            	+ name);
            e.printStackTrace();
        }
        return icon;
    }

    public void doQuit() {
        doClose();
        dispose();
    }

}// PDFViewer class



















