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

import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.vfsjfilechooser.utils.VFSURIParser;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.log4j.Logger;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.util.AuthenticationUtil;
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
				prefs.put(Constants.LAST_OPENED_FILE, localFileUrl);
				prefs.put(Constants.LAST_OPENED_LOCAL_FILE, localFileUrl);
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
	} //openLocalFile()
	
	private void openRemoteFile(WordMLEditor editor, String urlParam) {
		String webdavUrl = urlParam;
		if (urlParam.startsWith("http://")) {
			webdavUrl = "webdav://" + urlParam.substring(7);
		}
		
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
        
		String temp = 
			rm.getString(Constants.VFSJFILECHOOSER_DEFAULT_WEBDAV_FOLDER_BOOKMARK_NAME);
		if (temp == null || temp.length() == 0) {
			temp = "Default Webdav Folder";
		} else {
			temp = temp.trim();
		}
		
		List<String> userCredentials = 
			AuthenticationUtil.collectUserCredentialsFromBookmark(
				new VFSURIParser(webdavUrl, false), 
				temp);
		temp = webdavUrl.substring(9); //hostname(:port) and path

		//Try each known userCredential to resolve a FileObject
		FileObject fo = null;
		for (String uc: userCredentials) {
			StringBuilder sb = new StringBuilder();
			sb.append("webdav://");
			sb.append(uc);
			sb.append("@");
			sb.append(temp);
			try {
				fo = VFSUtils.getFileSystemManager().resolveFile(sb.toString());
				if (fo.exists()) {
					break;
				} else {
					fo = null;
				}
			} catch (FileSystemException exc) {
				fo = null;
			}
		}
		
		if (fo != null) {
	        Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
			String lastFileUri = fo.getName().getURI();
			prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);

			log.info("\n\n Opening " + webdavUrl);
			editor.createInternalFrame(fo);

		} else {
			String title = rm.getString("Application.applet.initialisation.text");
			String errMsg = null;
			try {
				fo = AuthenticationUtil.userAuthenticationChallenge(editor, webdavUrl, title);
				if (fo == null) {
					//user may have cancelled the authentication challenge
					//or unsuccessfully authenticated himself.
					//Because AuthenticationUtil.userAuthenticationChallenge()
					//has displayed authentication failure message, we do
					//not need to do anything here.
				} else if (fo.exists()) {
			        Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
					String lastFileUri = fo.getName().getURI();
					prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
					if (fo.getName().getScheme().equals(("file"))) {
						prefs.put(Constants.LAST_OPENED_LOCAL_FILE, lastFileUri);
					}

					log.info("\n\n Opening " + webdavUrl);
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
				editor.showMessageDialog(title, errMsg, JOptionPane.ERROR_MESSAGE);
			}
		}
	} //openRemoteFile()
	
}// WordMLApplet class



















