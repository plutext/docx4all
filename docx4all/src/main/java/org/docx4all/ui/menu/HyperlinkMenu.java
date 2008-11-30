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
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;

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
import org.docx4all.swing.ExternalHyperlinkDialog;
import org.docx4all.swing.UserAuthenticationDialog;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CaretUpdateEnabler;
import org.docx4all.util.DocUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.vfs.FileNameExtensionFilter;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.HyperlinkML;
import org.docx4all.xml.ParagraphML;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
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
		
		if (autoCreateLinkedDocument && linkML.getWordprocessingMLPackage() == null) {
			throw new IllegalArgumentException("Invalid HyperlinkML parameter");
		}
		
		final WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		final ResourceMap rm = editor.getContext().getResourceMap(getClass());
		String title = 
			rm.getString(OPEN_LINKED_DOCUMENT_ACTION_NAME + ".Action.text");
		String errMsg = null;
		
		String targetFilePath = HyperlinkML.encodeTarget(linkML, sourceFile, false);
		if (targetFilePath.startsWith("file://")) {
			// local file
			try {
				FileObject fo = VFSUtils.getFileSystemManager().resolveFile(targetFilePath);
				if (!fo.exists()) {
					if (autoCreateLinkedDocument) {
						boolean success = 
							createInFileSystem(fo, linkML.getWordprocessingMLPackage());
						if (!success) {
							//needs not display additional error message.
							fo = null;
						}
					} else {
						fo = null;
						errMsg = 
							rm.getString(
								OPEN_LINKED_DOCUMENT_ACTION_NAME + ".file.not.found.message", 
								targetFilePath);
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
				errMsg = 
					rm.getString(
						OPEN_LINKED_DOCUMENT_ACTION_NAME + ".io.error.message", 
						targetFilePath);
			}
			
		} else if (targetFilePath.startsWith("webdav://")) {
			try {
				FileObject fo = resolveWebdavFile(targetFilePath);
				if (fo != null && !fo.exists()) {
					if (autoCreateLinkedDocument) {
						boolean success = 
							createInFileSystem(fo, linkML.getWordprocessingMLPackage());
						if (!success) {
							fo = null;
						}
					} else {
						fo = null;
						errMsg = 
							rm.getString(
								OPEN_LINKED_DOCUMENT_ACTION_NAME + ".file.not.found.message", 
								targetFilePath);
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
				errMsg = 
					rm.getString(
						OPEN_LINKED_DOCUMENT_ACTION_NAME + ".io.error.message", 
						targetFilePath);
			}
						
		} else {
			errMsg = 
				rm.getString(
					OPEN_LINKED_DOCUMENT_ACTION_NAME + ".unsupported.protocol.message", 
					targetFilePath);
		}		
		
		if (errMsg != null) {
			editor.showMessageDialog(
					title, errMsg, JOptionPane.ERROR_MESSAGE);
		}
	}
	
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

	private FileObject resolveWebdavFile(String path) throws FileSystemException {
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
			
			WebdavClientFactory factory = new WebdavClientFactory();
			HttpClient client = 
				factory.createConnection(
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
























