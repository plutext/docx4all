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

package org.docx4all.ui.main;

import javax.swing.JApplet;

import org.docx4all.ui.menu.FileMenu;
import org.jdesktop.application.ApplicationContext;

/**
 *	@author Jojada Tirtowidjojo - 24/09/2008
 */
public class WordMLApplet extends JApplet {
	
	public void init() {
		System.out.println("Applet initialising");
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.preStartup(this);
        
        setJMenuBar(editor.createMenuBar());
        add(editor.createMainPanel());
	}
	
	public void start() {
		System.out.println("Applet starting.");

	}
	
	public void stop() {
		System.out.println("Applet stopping.");		
	}
	
	public void destroy() {
		System.out.println("Applet being destroyed.");
		
	}
}// WordMLApplet class



















