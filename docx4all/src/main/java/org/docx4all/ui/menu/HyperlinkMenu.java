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
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;

import net.sf.vfsjfilechooser.utils.VFSURIParser;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4all.swing.ExternalHyperlinkDialog;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CaretUpdateEnabler;
import org.docx4all.util.AuthenticationUtil;
import org.docx4all.util.DocUtil;
import org.docx4all.util.PreferenceUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.vfs.FileNameExtensionFilter;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.HyperlinkML;
import org.docx4all.xml.ParagraphML;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 * @author Jojada Tirtowidjojo - 21/11/2008
 */
public class HyperlinkMenu extends UIMenu {
	private static Logger log = LoggerFactory.getLogger(HyperlinkMenu.class);

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
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        WordMLTextPane textpane = (WordMLTextPane) editor.getCurrentEditor();
        WordMLDocument doc = (WordMLDocument) textpane.getDocument();
        textpane.saveCaretText();
        
		HyperlinkML linkML = ElementMLFactory.createEmptyHyperlinkML();
		String temp = textpane.getSelectedText();
		if (temp != null && temp.length() > 0) {
			linkML.setDisplayText(temp);
		}
				
		ResourceMap rm = editor.getContext().getResourceMap(getClass());
    	temp = rm.getString(Constants.VFSJFILECHOOSER_DOCX_FILTER_DESC);
    	if (temp == null || temp.length() == 0) {
    		temp = "Docx Files (.docx)";
    	}
    	FileNameExtensionFilter filter = 
    		new FileNameExtensionFilter(temp, Constants.DOCX_STRING);        

		final String sourceFilePath = 
			(String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
		
		ExternalHyperlinkDialog dialog = 
			new ExternalHyperlinkDialog(editor, sourceFilePath, linkML, filter);
		dialog.pack();
		dialog.setLocationRelativeTo(editor.getMainFrame());
		dialog.setSize(new Dimension(510, 355));
		dialog.setVisible(true);

		if (dialog.getValue() == ExternalHyperlinkDialog.OK_BUTTON_TEXT
			&& linkML.getDummyTarget().lastIndexOf("/.docx") == -1) {
			//Because linkML is still detached, 
			//we keep its dummy target value.
			//When linkML has been attached/pasted
			//we'll set its target to this dummy value.
			temp = linkML.getDummyTarget();
			ElementMLRecord[] records = new ElementMLRecord[] {
					new ElementMLRecord(linkML, false)
			};
			WordMLFragment fragment = new WordMLFragment(records);
			
			int start = textpane.getSelectionStart();
			
			try {
				doc.lockWrite();
				
				textpane.replaceSelection(fragment);

				DocumentElement elem = 
					(DocumentElement) doc.getRunMLElement(start);
				final HyperlinkML hyperML = 
					((HyperlinkML) elem.getElementML().getParent());
				//hyperML has now been attached/pasted.
				//Remember to set its target value.
				hyperML.setTarget(temp);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							FileObject sourceFile = 
								VFSUtils.getFileSystemManager().resolveFile(sourceFilePath);
							openLinkedDocument(sourceFile, hyperML, true);
						} catch (FileSystemException exc) {
							;//should not happen
						}
					}
				});

			} finally {
				doc.unlockWrite();
			}
		}
	}
	
	private WordprocessingMLPackage createNewEmptyPackage(WordprocessingMLPackage source)
		throws FileSystemException {
		WordprocessingMLPackage thePack = null;
		
		boolean copyMainDocumentPart = false;
		boolean copyStyleDefPart = true;
		boolean copyDocPropsCustomPart = true;
		
		try {
			thePack = 
				XmlUtil.createNewPackage(
					source, copyMainDocumentPart, copyStyleDefPart, copyDocPropsCustomPart);
		} catch (InvalidFormatException exc) {
			throw new FileSystemException(exc);
		}
		
		return thePack;
	}
	
	private boolean createInFileSystem(FileObject fo, WordprocessingMLPackage source) 
		throws FileSystemException {
		boolean success = false;
		
		WordprocessingMLPackage newPack = createNewEmptyPackage(source);
		if (newPack != null) {
			String targetPath = fo.getName().getURI();
			FileMenu.getInstance().createInFileSystem(fo);
			success = 
				FileMenu.getInstance().save(
						newPack, 
						targetPath, 
						OPEN_LINKED_DOCUMENT_ACTION_NAME);
			if (!success) {
				fo.delete();
			}
		}
		
		return success;
	}
	
	@Action
	public void editExternalLink() {
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        WordMLTextPane textpane = (WordMLTextPane) editor.getCurrentEditor();
        textpane.saveCaretText();
        
        int caretPos = textpane.getCaretPosition();
        WordMLDocument doc = (WordMLDocument) textpane.getDocument();
		DocumentElement elem = (DocumentElement) doc.getRunMLElement(caretPos);
		if (elem != null
			&& elem.getElementML().getParent() instanceof HyperlinkML) {
			final HyperlinkML hyperML = (HyperlinkML) elem.getElementML().getParent();
			int offsWithinLink = caretPos - elem.getStartOffset();
			
			ResourceMap rm = editor.getContext().getResourceMap(getClass());
	    	String filterDesc = rm.getString(Constants.VFSJFILECHOOSER_DOCX_FILTER_DESC);
	    	if (filterDesc == null || filterDesc.length() == 0) {
	    		filterDesc = "Docx Files (.docx)";
	    	}
	    	FileNameExtensionFilter filter = 
	    		new FileNameExtensionFilter(filterDesc, Constants.DOCX_STRING);        

			final String sourceFilePath = 
				(String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
			ExternalHyperlinkDialog dialog = 
				new ExternalHyperlinkDialog(editor, sourceFilePath, hyperML, filter);
			dialog.pack();
			dialog.setLocationRelativeTo(editor.getMainFrame());
			dialog.setSize(new Dimension(510, 355));
			dialog.setVisible(true);
			
			if (dialog.getValue() == ExternalHyperlinkDialog.OK_BUTTON_TEXT
				&& hyperML.getTarget().lastIndexOf("/.docx") == -1) {
				doc.refreshParagraphs(caretPos, 1);
				if (offsWithinLink <= hyperML.getDisplayText().length()) {
					//Caret position does not change
					textpane.setCaretPosition(caretPos);
				} else {
					textpane.setCaretPosition(
						caretPos - (offsWithinLink - hyperML.getDisplayText().length()));
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							FileObject sourceFile = 
								VFSUtils.getFileSystemManager().resolveFile(sourceFilePath);
							openLinkedDocument(sourceFile, hyperML, true);
						} catch (FileSystemException exc) {
							;//should not happen
						}
					}
				});
			}
		}
	}

	@Action
	public void insertInternalLink() {
	}

	@Action
	public void editInternalLink() {

	}

	public void openLinkedDocument(FileObject sourceFile, HyperlinkML linkML) {
		openLinkedDocument(sourceFile, linkML, false);
	}
	
	public void openLinkedDocument(
		FileObject sourceFile, 
		HyperlinkML linkML,
		boolean autoCreateLinkedDocument) {

		if (autoCreateLinkedDocument
				&& linkML.getWordprocessingMLPackage() == null) {
			throw new IllegalArgumentException("Invalid HyperlinkML parameter");
		}

		final WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		final ResourceMap rm = editor.getContext().getResourceMap(getClass());
		String title = 
			rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME + ".Action.text");
		String errMsg = null;

		String targetFilePath = HyperlinkML.encodeTarget(linkML, sourceFile,
				false);
		if (targetFilePath.startsWith("file://")) {
			// local file
			try {
				FileObject fo = VFSUtils.getFileSystemManager().resolveFile(
						targetFilePath);
				if (!fo.exists()) {
					if (autoCreateLinkedDocument) {
						boolean success = createInFileSystem(fo, linkML
								.getWordprocessingMLPackage());
						if (!success) {
							// needs not display additional error message.
							fo = null;
						}
					} else {
						fo = null;
						errMsg = rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME
								+ ".file.not.found.message", targetFilePath);
					}
				}

				if (fo != null) {
					editor.createInternalFrame(fo);

					final String sourceUri = sourceFile.getName().getURI();
					final String targetUri = fo.getName().getURI();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							editor.tileLayout(sourceUri, targetUri);
						}
					});
				}
			} catch (FileSystemException exc) {
				exc.printStackTrace();
				errMsg = rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME
						+ ".io.error.message", targetFilePath);
			}

		} else if (targetFilePath.startsWith("webdav://")) {
			try {
				boolean recordAsLastOpenUrl = false;
				WordprocessingMLPackage newPackage = 
					createNewEmptyPackage(linkML.getWordprocessingMLPackage());
				if (autoCreateLinkedDocument && newPackage == null) {
					//cannot create a new WordprocessingMLPackage.
					//This is an unlikely situation.
					//Avoid creating new linked document.
					//TODO: probably display a message ?
					autoCreateLinkedDocument = false;
				}
				
				FileObject fo = 
					openWebdavDocument(
						targetFilePath, 
						recordAsLastOpenUrl, 
						autoCreateLinkedDocument, 
						newPackage, 
						OPEN_LINKED_DOCUMENT_ACTION_NAME);
				if (fo != null) {
					final String sourceUri = sourceFile.getName().getURI();
					final String targetUri = fo.getName().getURI();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							editor.tileLayout(sourceUri, targetUri);
						}
					});
				} else {
					//Does not need to display additional error message.
					//openWebdavDocument() must have displayed all necessary
					//messages.
				}
			} catch (FileSystemException exc) {
				exc.printStackTrace();
				errMsg = rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME
						+ ".io.error.message", targetFilePath);
			}

		} else {
			errMsg = rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME
					+ ".unsupported.protocol.message", targetFilePath);
		}

		if (errMsg != null) {
			editor.showMessageDialog(title, errMsg, JOptionPane.ERROR_MESSAGE);
		}
	}
		
	/**
	 * Opens a webdav document pointed by vfsWebdavUrl parameter in its own
	 * internal frame.
	 * 
	 * vfsWebdavUrl is a VFS Webdav URL that points to a webdav document.
	 * It may or may not contain user credentials information. 
	 * For example:
	 * <> webdav://dev.plutext.org/alfresco/plutextwebdav/User Homes/someone/AFile.docx
	 * <> webdav://dev.plutext.org:80/alfresco/plutextwebdav/User Homes/someone/AFile.docx
	 * <> webdav://username:password@dev.plutext.org/alfresco/plutextwebdav/User Homes/someone/AFile.docx
	 * 
	 * In the event that vfsWebdavUrl does not have user credentials or its user credentials
	 * is invalid then this method will cycle through each known user credential found in 
	 * VFSJFileChooser Bookmark in order to find an authorised user. If no such user can be 
	 * found then an authentication challenge dialog will be displayed and user has three 
	 * attempts to authenticate himself. 
	 * 
	 * @param vfsWebdavUrl a VFS Webdav Url in its friendly format.
	 * @param recordAsLastOpenUrl a boolean flag that indicates whether vfsWebdavUrl 
	 * should be recorded as the last open url.
	 * @param createNewIfNotFound a boolean flag that indicates whether a new webdav
	 * document at vfsWebdavUrl should be created if it has not existed.
	 * @param newPackage a WordprocessingMLPackage that will become the content of
	 * newly created webdav document. This parameter must be supplied when 
	 * createNewIfNotFound parameter is true.
	 * @param callerActionName an Action name that can be used as a key to get 
	 * resource properties in relation to dialog messages.
	 * @return FileObject of the opened document
	 */
	public FileObject openWebdavDocument(
		String vfsWebdavUrl, 
		boolean recordAsLastOpenUrl,
		boolean createNewIfNotFound,
		WordprocessingMLPackage newPackage,
		String callerActionName) {
		
		if (!vfsWebdavUrl.startsWith("webdav://")) {
			throw new IllegalArgumentException("Not a webdav uri");
		}
		
		if (createNewIfNotFound && newPackage == null) {
			throw new IllegalArgumentException("Invalid newPackage parameter");
		}
		
		final WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		final ResourceMap rm = editor.getContext().getResourceMap(getClass());
		
		VFSURIParser uriParser = new VFSURIParser(vfsWebdavUrl, false);
		if (uriParser.getUsername() != null
			&& uriParser.getUsername().length() > 0
			&& uriParser.getPassword() != null
			&& uriParser.getPassword().length() > 0) {
			//vfsWebdavUrl has user credentials.
			try {
				FileObject fo = VFSUtils.getFileSystemManager().resolveFile(vfsWebdavUrl);
				if (fo.exists()) {
					if (recordAsLastOpenUrl) {
						Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
						String lastFileUri = fo.getName().getURI();
						prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
						PreferenceUtil.flush(prefs);
					}

					log.info("\n\n Opening " + fo.getName().getURI());
					editor.createInternalFrame(fo);
					return fo;
				}
			} catch (FileSystemException exc) {
				;
			}
		}
		
		String temp = 
			rm.getString(Constants.VFSJFILECHOOSER_DEFAULT_WEBDAV_FOLDER_BOOKMARK_NAME);
		if (temp == null || temp.length() == 0) {
			temp = "Default Webdav Folder";
		} else {
			temp = temp.trim();
		}
		
		List<String> userCredentials = 
			org.docx4all.vfs.VFSUtil.collectUserCredentialsFromBookmark(uriParser, temp);
		
		StringBuilder sb = new StringBuilder();
		sb.append(uriParser.getHostname());
		if (uriParser.getPortnumber() != null 
			&& uriParser.getPortnumber().length() > 0) {
			sb.append(":");
			sb.append(uriParser.getPortnumber());
		}
		sb.append(uriParser.getPath());
		temp = sb.toString();//hostname[:port] and path
		vfsWebdavUrl = "webdav://" + temp;
		
		//Try each known userCredential to resolve a FileObject
		FileObject theFile = null;
		for (String uc: userCredentials) {
			sb.delete(0, sb.length());
			sb.append("webdav://");
			sb.append(uc);
			sb.append("@");
			sb.append(temp);
			try {
				theFile = VFSUtils.getFileSystemManager().resolveFile(sb.toString());
				if (theFile.exists()) {
					break;
				} else {
					theFile = null;
				}
			} catch (FileSystemException exc) {
				theFile = null;
			}
		}
		
		if (theFile != null) {
			//theFile exists
			if (recordAsLastOpenUrl) {
				Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
				String lastFileUri = theFile.getName().getURI();
				prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
				PreferenceUtil.flush(prefs);
			}

			log.info("\n\n Opening " + theFile.getName().getURI());
			editor.createInternalFrame(theFile);

		} else {
			//Cannot get theFile yet.
			//Get user to authenticate himself.
			String title = rm.getString(callerActionName + ".Action.text");
			String errMsg = null;
			Preferences prefs = Preferences.userNodeForPackage(FileMenu.class);
			try {
				theFile = AuthenticationUtil.userAuthenticationChallenge(editor, vfsWebdavUrl, title);
				if (theFile == null) {
					//user may have cancelled the authentication challenge
					//or unsuccessfully authenticated himself.
					//Because AuthenticationUtil.userAuthenticationChallenge()
					//has displayed authentication failure message, we do
					//not need to do anything here.
				} else if (theFile.exists()) {
					String lastFileUri = theFile.getName().getURI();
					if (recordAsLastOpenUrl) {
						prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
					}

					//Record lastFileUri in bookmark.
					//Use file name as bookmark entry title.
					int idx = lastFileUri.lastIndexOf("/");
					org.docx4all.vfs.VFSUtil.addBookmarkEntry(
						lastFileUri.substring(idx + 1),
						new VFSURIParser(lastFileUri, false));
					
					log.info("\n\n Opening " + lastFileUri);
					editor.createInternalFrame(theFile);
					
				} else if (createNewIfNotFound) {
					boolean success = 
						createInFileSystem(theFile, newPackage);
					if (success) {
						String lastFileUri = theFile.getName().getURI();
						if (recordAsLastOpenUrl) {
							prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
						}

						//Record lastFileUri in bookmark.
						//Use file name as bookmark entry title.
						int idx = lastFileUri.lastIndexOf("/");
						org.docx4all.vfs.VFSUtil.addBookmarkEntry(
							lastFileUri.substring(idx + 1),
							new VFSURIParser(lastFileUri, false));
						
						log.info("\n\n Opening " + lastFileUri);
						editor.createInternalFrame(theFile);
						
					} else {
						theFile = null;
						errMsg = 
							rm.getString(
								callerActionName + ".file.io.error.message", 
								vfsWebdavUrl);
					}					
				} else {
					theFile = null;
					errMsg = 
						rm.getString(
							callerActionName + ".file.not.found.message", 
							vfsWebdavUrl);
				}
			} catch (FileSystemException exc) {
				exc.printStackTrace();
				theFile = null;
				errMsg = 
					rm.getString(
						callerActionName + ".file.io.error.message", 
						vfsWebdavUrl);
			} finally {
				PreferenceUtil.flush(prefs);
			}
			
			if (errMsg != null) {
				theFile = null;
				editor.showMessageDialog(title, errMsg, JOptionPane.ERROR_MESSAGE);
			}
		}
		
		return theFile;
	} //openWebdavDocument()
	
	protected JMenuItem createMenuItem(String actionName) {
		JMenuItem theItem = super.createMenuItem(actionName);
		theItem.setEnabled(false);
		
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ToolBarStates toolbarStates = editor.getToolbarStates();
        
    	if (INSERT_EXTERNAL_LINK_ACTION_NAME.equals(actionName)) {
       		toolbarStates.addPropertyChangeListener(
   				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
      				new InsertHyperlinkEnabler(theItem));
       		
    	} else if (EDIT_EXTERNAL_LINK_ACTION_NAME.equals(actionName)) {
       		toolbarStates.addPropertyChangeListener(
       			ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
          			new EditHyperlinkEnabler(theItem));
    	}
		return theItem;
	}

	private static class InsertHyperlinkEnabler extends CaretUpdateEnabler {
		InsertHyperlinkEnabler(JMenuItem item) {
			super(item);
		}

    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
			boolean isEnabled = false;
			
			if (caretEvent != null) {
				WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
				JEditorPane source = (JEditorPane) caretEvent.getSource();
				if (source instanceof WordMLTextPane
					&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
					WordMLTextPane textpane = (WordMLTextPane) source;
					WordMLDocument doc = (WordMLDocument) textpane.getDocument();
					int start = textpane.getSelectionStart();
					int end = textpane.getSelectionEnd();
					if (start == end) {
						//no selection
						isEnabled = canInsert(doc, start);
					} else {
						isEnabled = canInsert(doc, start, end);
					}
				}
			}

			return isEnabled;
		}
    	
    	private boolean canInsert(WordMLDocument doc, int offset) {
    		boolean canInsert = true;
    		
			WordMLDocument.TextElement elem =
				DocUtil.getInputAttributeElement(doc, offset, null);
			if (elem != null
				&& elem.getStartOffset() < offset
				&& offset < elem.getEndOffset()) {
				ElementML runML = elem.getElementML().getParent();
				canInsert = 
					elem.isEditable()
					&& (runML.getParent() instanceof ParagraphML);
			}
			
			return canInsert;
    	}
    	
    	private boolean canInsert(WordMLDocument doc, int start, int end) {
    		boolean canInsert = false;
    		
    		try {
    			String s = doc.getText(start, (end-start));
    			canInsert = (s.indexOf(Constants.NEWLINE) == -1);
    		} catch (BadLocationException exc) {
    			//ignore
    		}
    		
    		return canInsert;
    	}
    	
	} //InsertHyperlinkEnabler inner class

	private static class EditHyperlinkEnabler extends CaretUpdateEnabler {
		EditHyperlinkEnabler(JMenuItem item) {
			super(item);
		}

    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
			boolean isEnabled = false;
			
			if (caretEvent != null) {
				WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
				JEditorPane source = (JEditorPane) caretEvent.getSource();
				if (source instanceof WordMLTextPane
						&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
					WordMLTextPane textpane = (WordMLTextPane) source;
					if (textpane.getSelectionStart() == textpane.getSelectionEnd()) {
						//no selection
						WordMLDocument doc = (WordMLDocument) textpane.getDocument();
										
						int offset = textpane.getCaretPosition();
						WordMLDocument.TextElement elem =
							DocUtil.getInputAttributeElement(doc, offset, null);
						if (elem != null
							&& elem.getStartOffset() < offset
							&& offset < elem.getEndOffset()) {
							ElementML runML = elem.getElementML().getParent();
							isEnabled = (runML.getParent() instanceof HyperlinkML);
						}
					}
				}
			}

			return isEnabled;
		}
	} //EditHyperlinkEnabler inner class

}// HyperlinkMenu class
























