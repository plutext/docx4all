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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import net.sf.vfsjfilechooser.VFSJFileChooser;
import net.sf.vfsjfilechooser.acessories.DefaultAccessoriesPanel;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.VFS;
import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.util.DocUtil;
import org.docx4all.util.SwingUtil;
import org.docx4all.vfs.FileNameExtensionFilter;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.SaveToVFSZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 27/11/2007
 */
public class FileMenu extends UIMenu {
	private static Logger log = Logger.getLogger(FileMenu.class);
	
	private final static FileMenu _instance = new FileMenu();
	
	/**
	 * The binding key used for this FileMenu object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "fileMenu:org.docx4all.ui.menu.FileMenu";
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure File Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts File menu in the menu bar
	 */
	public final static String FILE_MENU_NAME = "fileMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Spring Application Framework
	
	/**
	 * The action name of New file menu
	 */
	public final static String NEW_FILE_ACTION_NAME = "newFile";
	
	/**
	 * The action name of Open file menu
	 */
	public final static String OPEN_FILE_ACTION_NAME = "openFile";
	
	/**
	 * The action name of Save file menu
	 */
	public final static String SAVE_FILE_ACTION_NAME = "saveFile";
	
	/**
	 * The action name of Save As file menu
	 */
	public final static String SAVE_AS_DOCX_ACTION_NAME = "saveAsDocx";
	
	/**
	 * The action name of Save As Html menu
	 */
	public final static String SAVE_AS_HTML_ACTION_NAME = "saveAsHtml";
	
	/**
	 * The action name of Save As file menu
	 */
	public final static String SAVE_ALL_FILES_ACTION_NAME = "saveAllFiles";
	
	/**
	 * The action name of Print Preview menu
	 */
	public final static String PRINT_PREVIEW_ACTION_NAME = "printPreview";
	
	/**
	 * The action name of Close menu
	 */
	public final static String CLOSE_FILE_ACTION_NAME = "closeFile";
	
	/**
	 * The action name of Close All menu
	 */
	public final static String CLOSE_ALL_FILES_ACTION_NAME = "closeAllFiles";
	
	/**
	 * The action name of Exit menu
	 */
	public final static String EXIT_ACTION_NAME = "exit";
	
	private static final String[] _menuItemActionNames = {
		NEW_FILE_ACTION_NAME,
		OPEN_FILE_ACTION_NAME,
		SAVE_FILE_ACTION_NAME,
		SAVE_AS_DOCX_ACTION_NAME,
		SAVE_AS_HTML_ACTION_NAME,
		SAVE_ALL_FILES_ACTION_NAME,
		SEPARATOR_CODE,
		PRINT_PREVIEW_ACTION_NAME,
		SEPARATOR_CODE,
		CLOSE_FILE_ACTION_NAME,
		CLOSE_ALL_FILES_ACTION_NAME,
		SEPARATOR_CODE,
		EXIT_ACTION_NAME
	};
	
	public static FileMenu getInstance() {
		return _instance;
	}
	
	private short _untitledFileNumber = 0;
	
	private FileMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return FILE_MENU_NAME;
	}
	
    protected JMenuItem createMenuItem(String actionName) {
    	JMenuItem theItem = super.createMenuItem(actionName);
    	
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ToolBarStates toolbarStates = editor.getToolbarStates();
        
    	if (SAVE_FILE_ACTION_NAME.equals(actionName)
    		|| SAVE_AS_DOCX_ACTION_NAME.equals(actionName)
    		|| SAVE_AS_HTML_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.DOC_DIRTY_PROPERTY_NAME, 
    				new EnableOnEqual(theItem, Boolean.TRUE));
    		
    	} else if (SAVE_ALL_FILES_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.ANY_DOC_DIRTY_PROPERTY_NAME, 
    				new EnableOnEqual(theItem, Boolean.TRUE));
    		
    	} else if (PRINT_PREVIEW_ACTION_NAME.equals(actionName)
    		|| CLOSE_FILE_ACTION_NAME.equals(actionName)
    		|| CLOSE_ALL_FILES_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.IFRAME_NUMBERS_PROPERTY_NAME, 
    				new EnableOnPositive(theItem));
    	}
    	
    	return theItem;
    }
    
	@Action public void newFile() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		FileObject fo = null;
		try {
			StringBuffer path = new StringBuffer("tmp://");
			path.append(System.getProperty("user.home"));
			path.append("/.docx4all/tmp/");
			path.append(editor.getUntitledFileName());
			path.append(++_untitledFileNumber);
			path.append(".docx");
			fo = VFS.getManager().resolveFile(path.toString());
			
		} catch (FileSystemException exc) {
			exc.printStackTrace();
	    	ResourceMap rm = editor.getContext().getResourceMap(getClass());
	        String title = 
	        	rm.getString(FileMenu.NEW_FILE_ACTION_NAME + ".Action.text");
	        String message =
	        	rm.getString(FileMenu.NEW_FILE_ACTION_NAME + ".Action.errorMessage");
	    	editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		editor.createInternalFrame(fo);
	}
	
    @Action public void openFile(ActionEvent actionEvent) {
        Preferences prefs = Preferences.userNodeForPackage( getClass() );
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        
        String lastFileUri = prefs.get(Constants.LAST_OPENED_FILE, Constants.EMPTY_STRING);
        FileObject dir = null;
        if (lastFileUri.length() > 0) {
        	try {
        		dir = VFS.getManager().resolveFile(lastFileUri).getParent();
        	} catch (FileSystemException exc) {
        		dir = null;
        	}
        }
        
    	ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
        VFSJFileChooser chooser = createFileChooser(rm, dir, Constants.DOCX_STRING);
        
        int returnVal = chooser.showOpenDialog((Component) actionEvent.getSource());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
			FileObject file = getSelectedFile(chooser, Constants.DOCX_STRING);
			if (file != null) {
				lastFileUri = file.getName().getURI();
				prefs.put(Constants.LAST_OPENED_FILE, lastFileUri);
				log.info("\n\n Opening " + VFSUtils.getFriendlyName(lastFileUri));
				editor.createInternalFrame(file);
			}
		}
    }

    @Action public void saveFile(ActionEvent actionEvent) {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        if (editor.getToolbarStates().isDocumentDirty()) {
        	JInternalFrame iframe = editor.getCurrentInternalFrame();
        	if (iframe.getTitle().startsWith(editor.getUntitledFileName())) {
        		saveAsDocx(actionEvent);
        	} else {
				boolean success = save(iframe, null, SAVE_FILE_ACTION_NAME);
				if (success) {
					editor.getToolbarStates().setDocumentDirty(iframe, false);
				}
			}
		}
    }

    @Action public void saveAsHtml(ActionEvent actionEvent) {
    	saveAsFile(SAVE_AS_HTML_ACTION_NAME, actionEvent, Constants.HTML_STRING);
    }
    
    @Action public void saveAsDocx(ActionEvent actionEvent) {
    	saveAsFile(SAVE_AS_DOCX_ACTION_NAME, actionEvent, Constants.DOCX_STRING);
    }
    
    private void saveAsFile(String callerActionName, ActionEvent actionEvent, String fileType) {
        Preferences prefs = Preferences.userNodeForPackage( getClass() );
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
    	ResourceMap rm = editor.getContext().getResourceMap(getClass());
       
        JInternalFrame iframe = editor.getCurrentInternalFrame();
        String filePath = 
			(String) iframe.getClientProperty(WordMLDocument.FILE_PATH_PROPERTY);
        
        FileObject file = null;
        FileObject dir = null;
        try {
        	file = VFS.getManager().resolveFile(filePath);
        	dir = file.getParent();
        } catch (FileSystemException exc) {
        	;//ignore
        }

        VFSJFileChooser chooser = createFileChooser(rm, dir, fileType);
        
        int returnVal = chooser.showSaveDialog((Component) actionEvent.getSource());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
			FileObject selectedFile = getSelectedFile(chooser, fileType);
			if (selectedFile != null) {
				String selectedFilePath = selectedFile.getName().getURI();
				if (log.isDebugEnabled()) {
					log.debug("saveAsFile(): selectedFile = " 
						+ VFSUtils.getFriendlyName(selectedFilePath));
				}
				
				prefs.put(Constants.LAST_OPENED_FILE, selectedFilePath);
				
				boolean selectedFileExists = false;
				try {
					selectedFileExists = selectedFile.exists();
				} catch (FileSystemException exc) {
					exc.printStackTrace();//ignore
					log.warn("Couldn't check file existence. File = " 
						+ selectedFilePath);
				}
				
				boolean canSave = true;
				if (selectedFileExists
					&& !selectedFilePath.equalsIgnoreCase(filePath)) {
		            String title = 
		            	rm.getString(callerActionName + ".Action.text");
		            String message =
		            	selectedFilePath 
		            	+ "\n"
		            	+ rm.getString(callerActionName + ".Action.confirmMessage");
		            int answer = 
		            	editor.showConfirmDialog(
		            		title, 
		            		message, 
		            		JOptionPane.YES_NO_OPTION, 
		            		JOptionPane.QUESTION_MESSAGE);
		            canSave = (answer == JOptionPane.YES_OPTION);
				}// if (file.exists())
				
				if (canSave) {
					boolean success = save(iframe, filePath, callerActionName);
					if (success) {
			       		if (Constants.DOCX_STRING.equals(fileType)) {
			       			//If saving as .docx then update the document dirty flag 
			       			//of toolbar states as well as internal frame title
			       			editor.getToolbarStates().setDocumentDirty(iframe, false);
			       			editor.updateInternalFrame(file, selectedFile);
			       		} else {
			       			//Because document dirty flag is not cleared
			       			//and internal frame title is not changed,
			       			//we present a success message.
				            String title = 
				            	rm.getString(callerActionName + ".Action.text");
				            String message =
				            	VFSUtils.getFriendlyName(filePath)
				            	+ "\n"
				            	+ rm.getString(callerActionName + ".Action.successMessage");
			            	editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
			       		}
					}
				}
			} else {
				log.error("saveAsFile(): selectedFile = NULL"); 
	            String title = 
	            	rm.getString(callerActionName + ".Action.text");
				String message = 
					rm.getString(callerActionName + ".Action.errorMessage");
				editor.showMessageDialog(title, message, JOptionPane.ERROR_MESSAGE);	
			}
			
		}//if (returnVal == JFileChooser.APPROVE_OPTION)
    }// saveAsFile()
    
    /**
     * If user types in a filename JFileChooser does not append the file extension
     * displayed by its FileFilter to it.
     * This method appends desiredFileType parameter as file extension
     * if JFileChooser's selected file does not have it.
     * 
     * @param chooser JFileChooser instance
     * @param desiredFileType File extension
     * @return a File whose extension is desiredFileType.
     */
    private FileObject getSelectedFile(VFSJFileChooser chooser, String desiredFileType) {
    	FileObject theFile = chooser.getSelectedFile();
		
    	StringBuffer uri = new StringBuffer(theFile.getName().getURI());
    	if (desiredFileType.equalsIgnoreCase(theFile.getName().getExtension())) {
			//user may type in the file extension in the JFileChooser dialog.
			//Therefore worth checking file extension case insensitively
    		int dot = uri.lastIndexOf(Constants.DOT);
    		uri = new StringBuffer(uri.substring(0, dot));
    	}
		uri.append(Constants.DOT);
		uri.append(desiredFileType);
		
		try {
			theFile = VFS.getManager().resolveFile(uri.toString());
		} catch (FileSystemException exc) {
			exc.printStackTrace();
			theFile = null;
		}
		
		return theFile;
    }
    
    @Action public void saveAllFiles() {
        WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        for (JInternalFrame iframe : wmlEditor.getAllInternalFrames()) {
			if (wmlEditor.getToolbarStates().isDocumentDirty(iframe)
					&& save(iframe, null, SAVE_ALL_FILES_ACTION_NAME)) {
				wmlEditor.getToolbarStates().setDocumentDirty(iframe, false);
			}
		}
    }
    
    @Action public void printPreview() {
        WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        JEditorPane editor = wmlEditor.getCurrentEditor();
   	
    	WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
    	kit.saveCaretText();
    	
    	Document doc = editor.getDocument();
		String filePath = 
			(String) doc.getProperty(WordMLDocument.FILE_PATH_PROPERTY);
        DocumentElement elem = (DocumentElement) doc.getDefaultRootElement();
		DocumentML rootML = (DocumentML) elem.getElementML();

		//Do not include the last paragraph when saving or printing.
		//we'll put it back when the job is done.
		elem = (DocumentElement) elem.getElement(elem.getElementCount() - 1);
		ElementML paraML = elem.getElementML();
		ElementML bodyML = paraML.getParent();
		paraML.delete();
		
		try {
			WordprocessingMLPackage wordMLPackage = rootML
					.getWordprocessingMLPackage();
			// Create temporary .pdf file.
			String tmpName = 
				filePath.substring(
					filePath.lastIndexOf(File.separator),
					filePath.lastIndexOf(Constants.DOT));
			File tmpFile = File.createTempFile(tmpName + ".tmp", ".pdf");
			// Delete the temporary file when program exits.
			tmpFile.deleteOnExit();
			
			OutputStream os = new java.io.FileOutputStream(tmpFile);

			// Could write to a ByteBuffer and avoid the temp file if:
			// 1. com.sun.pdfview.PDFViewer had an appropriate open method
			// 2. We knew how big to make the buffer
			// java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(15000);
			// //15kb
			// OutputStream os = newOutputStream(buf);

			wordMLPackage.pdf(os);

			os.close();

			com.sun.pdfview.PDFViewer pv = new com.sun.pdfview.PDFViewer(true);
			// pv.openFile(buf, "some name"); // requires modified
			// com.sun.pdfview.PDFViewer
			pv.openFile(tmpFile);
			
		} catch (Exception exc) {
			exc.printStackTrace();
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
            String title = 
            	rm.getString(PRINT_PREVIEW_ACTION_NAME + ".Action.text");
			String message = 
				rm.getString(PRINT_PREVIEW_ACTION_NAME + ".Action.errorMessage")
				+ "\n" 
				+ VFSUtils.getFriendlyName(filePath);
			wmlEditor.showMessageDialog(title, message, JOptionPane.ERROR_MESSAGE);

		} finally {
	        //Remember to put 'paraML' as last paragraph
	        bodyML.addChild(paraML);
		}
		
    } //printPreview()
    
    @Action public void closeFile() {
        WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        wmlEditor.closeInternalFrame(wmlEditor.getCurrentInternalFrame());
    }
    
    @Action public void closeAllFiles() {
        WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        wmlEditor.closeAllInternalFrames();
    }
    
    @Action public void exit() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.exit();
    }
    
    private VFSJFileChooser createFileChooser(
    	ResourceMap resourceMap, 
    	FileObject showedDir,
    	String filteredFileExtension) {
        
    	VFSJFileChooser chooser = new VFSJFileChooser();
        chooser.setAccessory(new DefaultAccessoriesPanel(chooser));
        chooser.setFileHidingEnabled(false);
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(VFSJFileChooser.FILES_ONLY);
        
        String desc = null;
        if (Constants.HTML_STRING.equals(filteredFileExtension)) {
        	desc = resourceMap.getString(Constants.HTML_FILTER_DESC);
        	if (desc == null || desc.length() == 0) {
        		desc = "Html Files (.html)";
        	}
        } else {
        	desc = resourceMap.getString(Constants.DOCX_FILTER_DESC);
        	if (desc == null || desc.length() == 0) {
        		desc = "Docx Files (.docx)";
        	}
        }
    	
    	FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, filteredFileExtension);        
    	chooser.addChoosableFileFilter(filter);
        chooser.setCurrentDirectory(showedDir);
        
        return chooser;
    }
    
    /**
     * Saves editor documents to a file.
     * 
     * Internal frame may have two editors for presenting two different views
     * to user namely editor view and source view. WordMLTextPane is used for 
     * editor view and JEditorPane for source view.
     * The contents of these two editors are synchronized when user switches
     * from one view to the other. Therefore, there will be ONLY ONE editor 
     * that is dirty and has to be saved by this method.
     * 
     * @param iframe
     * @param saveAsFilePath
     * @param callerActionName
     * @return
     */
    public boolean save(JInternalFrame iframe, String saveAsFilePath, String callerActionName) {
    	boolean success = true;
    	
		if (saveAsFilePath == null) {
			saveAsFilePath = 
				(String) iframe.getClientProperty(WordMLDocument.FILE_PATH_PROPERTY);
		}
		
    	JEditorPane editor = SwingUtil.getSourceEditor(iframe);
    	if (editor != null
        	&& !((Boolean) editor.getClientProperty(Constants.SYNCHRONIZED_FLAG)).booleanValue()) {
    		//signifies that Source View is not synchronised with Editor View yet.
    		//Therefore, it is dirty and has to be saved.
    		
    		EditorKit kit = editor.getEditorKit();
    		Document doc = editor.getDocument();
			WordprocessingMLPackage wmlPackage =
				(WordprocessingMLPackage) doc.getProperty(
						WordMLDocument.WML_PACKAGE_PROPERTY);
			
    		DocUtil.write(kit, doc, wmlPackage);
			success = save(wmlPackage, saveAsFilePath, callerActionName);

			if (success) {
				if (saveAsFilePath.endsWith(Constants.DOCX_STRING)) {
					doc.putProperty(WordMLDocument.FILE_PATH_PROPERTY,
							saveAsFilePath);
					iframe.putClientProperty(WordMLDocument.FILE_PATH_PROPERTY,
							saveAsFilePath);
				}
			}

			if (log.isDebugEnabled()) {
				log.debug("save(): filePath=" + VFSUtils.getFriendlyName(saveAsFilePath));
				DocUtil.displayXml(doc);
			}
			return success;
    	}
    	
    	editor = SwingUtil.getWordMLTextPane(iframe);
    	if (editor != null) {
        	WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
        	kit.saveCaretText();
        	
        	Document doc = editor.getDocument();
            DocumentElement elem = (DocumentElement) doc.getDefaultRootElement();
    		DocumentML rootML = (DocumentML) elem.getElementML();

    		//Do not include the last paragraph when saving.
    		//After saving we put it back.
    		elem = (DocumentElement) elem.getElement(elem.getElementCount() - 1);
    		ElementML paraML = elem.getElementML();
    		ElementML bodyML = paraML.getParent();
    		paraML.delete();
    		
    		success = save(rootML.getWordprocessingMLPackage(), saveAsFilePath, callerActionName);
    		
	        //Remember to put 'paraML' as last paragraph
	        bodyML.addChild(paraML);
	        
	        if (success) {
	        	if (saveAsFilePath.endsWith(Constants.DOCX_STRING)) {
	        		doc.putProperty(WordMLDocument.FILE_PATH_PROPERTY, saveAsFilePath);
	        		iframe.putClientProperty(WordMLDocument.FILE_PATH_PROPERTY, saveAsFilePath);
	        	}
	        }
	        
	        if (log.isDebugEnabled()) {
				log.debug("save(): filePath=" + VFSUtils.getFriendlyName(saveAsFilePath));
	        	DocUtil.displayXml(doc);
	        }    		
    	}

    	return success;
    }
    
    private boolean save(WordprocessingMLPackage wmlPackage, String saveAsFilePath, String callerActionName) {
    	boolean success = true;
        try {
       		if (saveAsFilePath.endsWith(Constants.DOCX_STRING)) {
       			SaveToVFSZipFile saver = new SaveToVFSZipFile(wmlPackage);
       			saver.save(saveAsFilePath);
       			
       		} else if (saveAsFilePath.endsWith(Constants.HTML_STRING)) {
       			FileObject fo = VFS.getManager().resolveFile(saveAsFilePath);
       			OutputStream fos = fo.getContent().getOutputStream();
       			javax.xml.transform.stream.StreamResult result = 
					new javax.xml.transform.stream.StreamResult(fos);
       			wmlPackage.html(result);
				try {
					//just in case
					fos.close();
				} catch (IOException exc) {
					;//ignore
				}
			} else {
       			throw new Docx4JException(
       				"Invalid filepath = " + VFSUtils.getFriendlyName(saveAsFilePath));
			}
        } catch (Exception exc) {
        	exc.printStackTrace();
        	
        	success = false;
            WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);            
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());

            String title = 
            	rm.getString(callerActionName + ".Action.text");
			String message = 
				rm.getString(callerActionName + ".Action.errorMessage")
				+ "\n" 
				+ VFSUtils.getFriendlyName(saveAsFilePath);
			wmlEditor.showMessageDialog(title, message, JOptionPane.ERROR_MESSAGE);	
        }
        
        return success;
    }

}// FileMenu class



















