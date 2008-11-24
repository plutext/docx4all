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

package org.docx4all.ui.menu;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.vfsjfilechooser.utils.VFSURIParser;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.URLFileName;
import org.apache.commons.vfs.provider.webdav.WebdavClientFactory;
import org.apache.commons.vfs.provider.webdav.WebdavFileObject;
import org.apache.commons.vfs.provider.webdav.WebdavFileSystemConfigBuilder;
import org.apache.commons.vfs.provider.webdav.WebdavMethodRetryHandler;
import org.apache.log4j.Logger;
import org.docx4all.swing.UserAuthenticationDialog;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CurrentEditorBasedEnabler;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 * @author Jojada Tirtowidjojo - 21/11/2008
 */
public class HyperlinkMenu extends UIMenu {
	private static Logger log = Logger.getLogger(HyperlinkMenu.class);

	private final static HyperlinkMenu _instance = new HyperlinkMenu();

	/**
	 * The binding key used for this ViewMenu object when passed into scripting
	 * environment
	 */
	public final static String SCRIPT_BINDING_KEY = "hyperlinkMenu:org.docx4all.ui.menu.HyperlinkMenu";

	// ==========
	// MENU Names
	// ==========
	// Used as an argument to JMenu.setName().
	// Therefore it can be used in .properties file
	// to configure Hyperlink Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Hyperlink menu in the menu bar
	 */
	public final static String HYPERLINK_MENU_NAME = "hyperlinkMenu";

	// ============
	// ACTION Names
	// ============
	// The string value of each action name must be the same as
	// the method name annotated by @Action tag.
	// Action name is used to configure Menu/Button Action property
	// in .properties file and get an Action object out of
	// Swing Application Framework

	/**
	 * The action name of Link To New Document menu
	 */
	public final static String INSERT_EXTERNAL_LINK_ACTION_NAME = "insertExternalLink";

	public final static String EDIT_EXTERNAL_LINK_ACTION_NAME = "editExternalLink";
	
	public final static String INSERT_INTERNAL_LINK_ACTION_NAME = "insertInternalLink";

	public final static String EDIT_INTERNAL_LINK_ACTION_NAME = "editInternalLink";
	
	public final static String OPEN_LINKED_DOCUMENT_ACTION_NAME = "openLinkedDocument";

	private static final String[] _menuItemActionNames = {
			INSERT_EXTERNAL_LINK_ACTION_NAME,
			EDIT_EXTERNAL_LINK_ACTION_NAME,
			SEPARATOR_CODE,		
			INSERT_INTERNAL_LINK_ACTION_NAME,
			EDIT_INTERNAL_LINK_ACTION_NAME};

	public static HyperlinkMenu getInstance() {
		return _instance;
	}

	private HyperlinkMenu() {
		;// singleton
	}

	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0,
				_menuItemActionNames.length);
		return names;
	}

	public String getMenuName() {
		return HYPERLINK_MENU_NAME;
	}

	@Action
	public void insertExternalLink() {

	}

	@Action
	public void editExternalLink() {

	}

	@Action
	public void insertInternalLink() {
	}

	@Action
	public void editInternalLink() {

	}

	public void openLinkedDocument(final String sourceFilePath,	String targetFilePath) {
		final WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		final ResourceMap rm = editor.getContext().getResourceMap(getClass());

		FileObject targetFile = null;
		String errMessage = null;
		if (targetFilePath.startsWith("file://")) {
			// local file
			try {
				targetFile = VFSUtils.getFileSystemManager().resolveFile(targetFilePath);
				if (targetFile.exists()) {
					editor.createInternalFrame(targetFile);
				}
			} catch (FileSystemException exc) {
				exc.printStackTrace();
				targetFile = null;
				errMessage = 
					rm.getString("openLinkedDocument.io.error.message", targetFilePath);
			}
		} else if (targetFilePath.startsWith("http://")
				|| targetFilePath.startsWith("webdav://")) {
			// TODO: Differentiate HTTP from WEBDAV.
			// Use the same user credentials as written in sourceFilePath
			VFSURIParser parser = new VFSURIParser(sourceFilePath);
			String username = parser.getUsername();
			String password = parser.getPassword();

			// Authenticate user against destination server
			// by trying to resolve the server's root file system.
			parser = new VFSURIParser(targetFilePath);
			StringBuilder path = new StringBuilder();
			path.append(parser.getProtocol().name().toLowerCase());
			path.append("://");
			if (username != null && password != null) {
				path.append(username);
				path.append(":");
				path.append(password);
				path.append("@");
			}

			path.append(parser.getHostname());
			if (parser.getPortnumber().length() > 0) {
				path.append(":");
				path.append(parser.getPortnumber());
			}
			path.append(parser.getPath());

			try {
				targetFile = resolveFile(path.toString());
				if (targetFile != null && targetFile.exists()) {
					editor.createInternalFrame(targetFile);
				}
			} catch (FileSystemException exc) {
				exc.printStackTrace();
				targetFile = null;
				errMessage = 
					rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME + ".io.error.message", targetFilePath);
			}
		} else {
			throw new IllegalArgumentException(
					"Unsupported protocol. targetFilePath=" + targetFilePath);
		}

		if (errMessage != null) {
			String title = 
				rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME + ".Action.text");
			editor.showMessageDialog(
				title, errMessage, JOptionPane.ERROR_MESSAGE);
			
		} else if (targetFile != null) {
			final String targetUri = targetFile.getName().getURI();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					editor.tileLayout(sourceFilePath, targetUri);
				}
			});
		}
	}

	protected JMenuItem createMenuItem(String actionName) {
		JMenuItem theItem = super.createMenuItem(actionName);
		//TODO: Enable all menu items
		theItem.setEnabled(false);
		return theItem;
	}

	private FileObject resolveFile(String path) throws FileSystemException {
		int idx = path.indexOf("://");
		StringBuilder webdavPath = new StringBuilder();
		webdavPath.append("webdav://");
		webdavPath.append(path.substring(idx + 3));
		
		WebdavFileObject theFile =
			(WebdavFileObject)
				VFSUtils.getFileSystemManager().resolveFile(
					webdavPath.toString());
		int status = getAuthorisationStatus(theFile);

		if (status == 200) {
			//ok
		} else if (status == 401) {
			//unauthorised and retry
			WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
			
			UserAuthenticationDialog dialog = 
				new UserAuthenticationDialog(editor, path);
			dialog.pack();
			dialog.setLocationRelativeTo(editor.getMainFrame());
			dialog.setSize(new Dimension(400, 250));

			URLFileName urlFileName = (URLFileName) theFile.getName();
			int retry = 1;
			while (status == 401 && retry <= 3) {
				theFile = null;
				dialog.setVisible(true);

				if (dialog.getValue() == UserAuthenticationDialog.CANCEL_BUTTON_TEXT) {
					dialog.setVisible(false);
					dialog.dispose();
					retry = 4; // break
				} else {
					webdavPath = new StringBuilder();
					webdavPath.append("webdav://");
					webdavPath.append(dialog.getUsername());
					webdavPath.append(":");
					webdavPath.append(dialog.getPassword());
					webdavPath.append("@");
					webdavPath.append(urlFileName.getHostName());
					webdavPath.append(":");
					webdavPath.append(Integer.toString(urlFileName.getPort()));
					webdavPath.append(urlFileName.getPath());

					theFile =
						(WebdavFileObject)
							VFSUtils.getFileSystemManager().resolveFile(
									webdavPath.toString());
					status = getAuthorisationStatus(theFile);
					if (status == 401) {
						if (retry == 3) {
							throw new FileSystemException("Access denied");
						}
						
						ResourceMap rm = 
							editor.getContext().getResourceMap(getClass());
						String title = 
							rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME + ".Action.text");
						String message = 
							rm.getString(
								OPEN_LINKED_DOCUMENT_ACTION_NAME
								+ ".authentication.failure.message");
						editor.showMessageDialog(
							title, message.toString(), JOptionPane.INFORMATION_MESSAGE);
						retry++;
					}
				}
			} //while (status == 401 && retry <= 3);
			
			if (status == 200) {
				//ok
			} else {
				theFile = null;
			}
		} else {
			theFile = null;
		}

		return theFile;
	}

	private int getAuthorisationStatus(WebdavFileObject fo) throws FileSystemException {
		int status = 401; //unauthorised and retry.
		
		org.apache.webdav.lib.methods.OptionsMethod optionsMethod = null;
		try {
			String urlCharset = 
				WebdavFileSystemConfigBuilder.getInstance().getUrlCharset(
						fo.getFileSystem().getFileSystemOptions());
			URLFileName urlFileName = (URLFileName) fo.getName();
			optionsMethod = 
				new org.apache.webdav.lib.methods.OptionsMethod(
						urlFileName.getPathQueryEncoded(urlCharset));
		
			optionsMethod.setMethodRetryHandler(
					WebdavMethodRetryHandler.getInstance());
			optionsMethod.setFollowRedirects(true);

			char[] username = null;
			if (urlFileName.getUserName() != null) {
				username = urlFileName.getUserName().toCharArray();
			}
			char[] password = null;
			if (urlFileName.getPassword() != null) {
				password = urlFileName.getPassword().toCharArray();
			}
			
			HttpClient client = 
				WebdavClientFactory.createConnection(
						urlFileName.getHostName(),
						urlFileName.getPort(),
						username,
						password,
						fo.getFileSystem().getFileSystemOptions());
			status = client.executeMethod(optionsMethod);
		} catch (Exception exc) {
			throw new FileSystemException("Cannot get authorisation status", exc);
		} finally {
			if (optionsMethod != null) {
				optionsMethod.releaseConnection();
			}
		}
	
		return status;
	}

	private static class HyperlinkEnabler extends CurrentEditorBasedEnabler {
		HyperlinkEnabler(JMenuItem item) {
			super(item);
		}

		protected boolean isMenuEnabled(JEditorPane currentEditor) {
			if (currentEditor instanceof WordMLTextPane
					&& currentEditor.isEditable()) {
				return true;
			}

			return false;
		}
	} // HyperlinkEnabler inner class

}// HyperlinkMenu class

