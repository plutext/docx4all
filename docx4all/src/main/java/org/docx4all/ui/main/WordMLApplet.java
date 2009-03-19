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

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.log4j.Logger;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.ui.menu.HyperlinkMenu;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 24/09/2008
 */
public class WordMLApplet extends JApplet {
	private static Logger log = Logger.getLogger(WordMLApplet.class);

	public void init() {
		System.out.println("Applet initialising");
		final WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.preStartup(this);
        
        setJMenuBar(editor.createMenuBar());
        add(editor.createMainPanel());
        
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
    	String s = rm.getString(Constants.APP_APPLET_OPEN_FILE_URL_PARAM_NAME);
        s = getParameter(s);
        if (s == null) {
        	;//pass
        } else if (s.endsWith(".docx")) {
        	if (s.startsWith("file://")) {
        		final String urlParam = s;
        		SwingUtilities.invokeLater(new Runnable() {
        			public void run() {
                        openLocalFile(editor, urlParam);
        			}
        		});
                
        	} else if (s.startsWith("http://")
        				|| s.startsWith("webdav://")) {
        		final String urlParam = s;
        		SwingUtilities.invokeLater(new Runnable() {
        			public void run() {
        				openRemoteFile(editor, urlParam);
        			}
        		});
                
        	} else {
            	log.error("Unknown protocol scheme. File = " + s);        		
        	}
        } else {
        	log.error("Unknown file type. File = " + s);        	
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
	
	private void openLocalFile(WordMLEditor editor, String urlParam) {
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
        
	    String localFileUrl = urlParam;
	    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
	    	if (urlParam.startsWith("file:///")) {
	    		;//pass
	    	} else {
	    		localFileUrl = "file:///" + urlParam.substring(7);
	    	}
	    }
	    
		String errMsg = null;
		try {
			FileObject fo = VFSUtils.getFileSystemManager().resolveFile(urlParam);
			if (fo.exists()) {
		        Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
		        localFileUrl = fo.getName().getURI();
				System.setProperty("javax.xml.transform.TransformerFactory", 
						XmlUtils.TRANSFORMER_FACTORY_ORIGINAL); 		        
				prefs.put(Constants.LAST_OPENED_FILE, localFileUrl);
				prefs.put(Constants.LAST_OPENED_LOCAL_FILE, localFileUrl);
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
					e.printStackTrace();
				}
				log.info("\n\n Opening " + urlParam);
				editor.createInternalFrame(fo);
			} else {
				errMsg = 
					rm.getString(
						"Application.applet.initialisation.file.not.found.message", 
						urlParam);
			}
		} catch (FileSystemException exc) {
			exc.printStackTrace();
			errMsg = 
				rm.getString(
					"Application.applet.initialisation.file.io.error.message", 
					urlParam);
		}
		
		if (errMsg != null) {
			String title = rm.getString("Application.applet.initialisation.Action.text");
			editor.showMessageDialog(title, errMsg, JOptionPane.ERROR_MESSAGE);
		}
	} //openLocalFile()
	
	private void openRemoteFile(WordMLEditor editor, String urlParam) {
		String webdavUrl = urlParam;
		if (urlParam.startsWith("http://")) {
			webdavUrl = "webdav://" + urlParam.substring(7);
		}
		
		boolean recordAsLastOpenUrl = true;
		boolean createNewIfNotFound = false;
		WordprocessingMLPackage newPackage = null;
		String callerActionName = "Application.applet.initialisation";
		HyperlinkMenu.getInstance().openWebdavDocument(
			webdavUrl, 
			recordAsLastOpenUrl, 
			createNewIfNotFound, 
			newPackage, 
			callerActionName);
	}
	
}// WordMLApplet class



















