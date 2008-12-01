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

import java.util.prefs.Preferences;

import javax.swing.JApplet;
import javax.swing.JOptionPane;

import net.sf.vfsjfilechooser.accessories.bookmarks.Bookmarks;
import net.sf.vfsjfilechooser.accessories.bookmarks.TitledURLEntry;
import net.sf.vfsjfilechooser.utils.VFSURIParser;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.log4j.Logger;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.ui.menu.HyperlinkMenu;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 24/09/2008
 */
public class WordMLApplet extends JApplet {
	private static Logger log = Logger.getLogger(WordMLApplet.class);

	public void init() {
		System.out.println("Applet initialising");
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.preStartup(this);
        
        setJMenuBar(editor.createMenuBar());
        add(editor.createMainPanel());
        
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
    	String s = rm.getString(Constants.APP_APPLET_OPEN_FILE_URL_PARAM_NAME);
        s = getParameter(s);
        if (s == null) {
        	;//pass
        } else if (s.endsWith(".docx")
        			&& (s.startsWith("http://")
        					|| s.startsWith("webdav://"))) {
        	openFile(editor, s);
        } else {
        	log.error("Unknown file type or protocol");
        }
	}
	
	public void start() {
		log.info("Applet starting.");
	}
	
	public void stop() {
		log.info("Applet stopping.");		
	}
	
	public void destroy() {
		log.info("Applet being destroyed.");	
	}
	
	private void openFile(WordMLEditor editor, String urlParam) {
		if (urlParam.startsWith("http://")) {
			urlParam = "webdav://" + urlParam.substring(7);
		}
		
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
        
		String temp = 
			rm.getString(Constants.VFSJFILECHOOSER_DEFAULT_WEBDAV_FOLDER_BOOKMARK_NAME);
		if (temp == null || temp.length() == 0) {
			temp = "Default Webdav Folder";
		} else {
			temp = temp.trim();
		}
		temp = createVFSUrl(urlParam, temp);
		
		String errMsg = null;
		try {
			FileObject fo = HyperlinkMenu.getInstance().resolveWebdavFile(temp);
			if (fo == null) {
				;//user may have cancelled the authentication challenge
			} else if (fo.exists()) {
		        Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
				String lastFileUri = fo.getName().getURI();
				prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
				if (fo.getName().getScheme().equals(("file"))) {
					prefs.put(Constants.LAST_OPENED_LOCAL_FILE, lastFileUri);
				}
				log.info("\n\n Opening " + urlParam);
				editor.createInternalFrame(fo);
			} else {
				errMsg = 
					rm.getString(
						"Application.applet.file.not.found.message", 
						urlParam);
			}
		} catch (FileSystemException exc) {
			exc.printStackTrace();
			errMsg = 
				rm.getString(
					"Application.applet.file.io.error.message", 
					urlParam);
		}
		
		if (errMsg != null) {
			String title = rm.getString("Application.applet.initialisation.text");
			editor.showMessageDialog(title, errMsg, JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private String createVFSUrl(String urlParam, String defaultBookmarkEntryTitle) {
		String theVFSUrl = urlParam;
		
		VFSURIParser reference = 
			findBookmarkEntry(
				new VFSURIParser(urlParam, false), 
				defaultBookmarkEntryTitle);
		if (reference != null
			&& reference.getUsername() != null
			&& reference.getUsername().length() > 0
			&& reference.getPassword() != null
			&& reference.getPassword().length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(reference.getProtocol().getName().toLowerCase());
			sb.append("://");
			sb.append(reference.getUsername());
			sb.append(":");
			sb.append(reference.getPassword());
			sb.append("@");
			int idx = urlParam.indexOf("://") + 3;
			sb.append(urlParam.substring(idx));
			theVFSUrl = sb.toString();
		}
		
		return theVFSUrl;
	}
	
	private VFSURIParser findBookmarkEntry(VFSURIParser uri, String defaultBookmarkEntryTitle) {
		VFSURIParser theEntry = null;
		
		Bookmarks book = new Bookmarks();
		
		for (int i=0; i < book.getSize(); i++) {
			TitledURLEntry entry = book.getEntry(i);
			theEntry = new VFSURIParser(entry.getURL(), false);
			if (uri.getProtocol() == theEntry.getProtocol()
				&& uri.getHostname().equals(theEntry.getHostname())
				&& uri.getPath().startsWith(theEntry.getPath())) {
				i = book.getSize(); //break
			} else {
				theEntry = null;
			}
		}

		//if previous search failed, try to find bookmark entry
		//that matches defaultEntryTitle parameter
		if (theEntry == null) {
			for (int i=0; i < book.getSize(); i++) {
				TitledURLEntry entry = book.getEntry(i);
				if (defaultBookmarkEntryTitle.equalsIgnoreCase(entry.getTitle())) {
					i = book.getSize(); //break
					theEntry = new VFSURIParser(entry.getURL(), false);
				} else {
					entry = null;
				}
			}
		}
		
		return theEntry;
	}
	
}// WordMLApplet class



















