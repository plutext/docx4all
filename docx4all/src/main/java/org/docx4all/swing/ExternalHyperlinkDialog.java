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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import net.sf.vfsjfilechooser.VFSJFileChooser;
import net.sf.vfsjfilechooser.VFSJFileChooser.RETURN_TYPE;
import net.sf.vfsjfilechooser.VFSJFileChooser.SELECTION_MODE;
import net.sf.vfsjfilechooser.accessories.DefaultAccessoriesPanel;
import net.sf.vfsjfilechooser.accessories.bookmarks.Bookmarks;
import net.sf.vfsjfilechooser.accessories.bookmarks.TitledURLEntry;
import net.sf.vfsjfilechooser.utils.VFSURIParser;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.FileMenu;
import org.docx4all.vfs.FileNameExtensionFilter;
import org.docx4all.xml.HyperlinkML;

/**
 *	@author Jojada Tirtowidjojo - 25/11/2008
 */
public class ExternalHyperlinkDialog extends JDialog implements PropertyChangeListener {
    public final static String OK_BUTTON_TEXT = "OK";
    public final static String CANCEL_BUTTON_TEXT = "Cancel";
    
    private JOptionPane optionPane;
    
    private JLabel displayTextLabel;
    private JTextField displayTextField;
    private JLabel tooltipLabel;
    private JTextField tooltipField;
    private JLabel documentNameLabel;
    private JTextField documentNameField;
    private JLabel directoryPathLabel;
    
    //directoryPathField holds the complete target directory
    //path in VFS url FRIENDLY format;ie: no username and password
    private JTextArea directoryPathField;
    
    //sourceFileVFSUrlPath holds the complete source file path 
    //in VFS url format
    private String sourceFileVFSUrlPath;
    private HyperlinkML hyperlinkML;
    
    //directoryUrlPath holds the complete directory url path 
    //in VFS url format
    private String directoryUrlPath;
    
    private JButton selectButton;
    
    //fileFilter is used to filter files listing in VFSJFileChooser 
    private FileNameExtensionFilter fileFilter;
    private Object value;
    
    private boolean displayTextFieldAutoUpdate;

    private final static String FILE_PREFIX = 
    	System.getProperty("os.name").toLowerCase().startsWith("windows")
    		? "file:///" 
    		: "file://";

    private final static Insets gridCellInsets = new Insets(5, 5, 5, 5);

	public ExternalHyperlinkDialog(
		WordMLEditor wmlEditor, 
		String sourceFileVFSUrlPath, 
		HyperlinkML hyperlinkML,
		FileNameExtensionFilter fileFilter) {
		super(wmlEditor.getWindowFrame(), true);
		
		this.sourceFileVFSUrlPath = sourceFileVFSUrlPath;
		this.hyperlinkML = hyperlinkML;
		this.fileFilter = fileFilter;
		this.value = CANCEL_BUTTON_TEXT;
		setTitle("External Hyperlink Setup");
		
		Object[] options = {OK_BUTTON_TEXT, CANCEL_BUTTON_TEXT};
		Object[] array = {createContentPanel(sourceFileVFSUrlPath, hyperlinkML)};
		this.optionPane = 
			new JOptionPane(
				  array,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_OPTION,
                  null,
                  options,
                  options[0]);
		
	    //Make this dialog display it.
        setContentPane(this.optionPane);

        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				ExternalHyperlinkDialog.this.optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
        
        //Register an event handler that reacts to option pane state changes.
        this.optionPane.addPropertyChangeListener(this);
	}
	
    public Object getValue() {
    	return this.value;
    }
    
	 /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible()
        	&& (e.getSource() == this.optionPane)
        	&& (JOptionPane.VALUE_PROPERTY.equals(prop) 
        		|| JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object userAnswer = this.optionPane.getValue();

            if (userAnswer == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }

            //Reset the JOptionPane's value.
            //If you don't do this, then if the user
            //presses the same button next time, no
            //property change event will be fired.
            this.optionPane.setValue(
                    JOptionPane.UNINITIALIZED_VALUE);

            setVisible(false);
            
            if (OK_BUTTON_TEXT.equals(userAnswer)) {
            	this.value = OK_BUTTON_TEXT;
            	
            	StringBuilder target = new StringBuilder();
            	if (isSourceFileDirectory(this.directoryUrlPath)) {
            		target.append(this.documentNameField.getText());
            	} else {
            		target.append(VFSUtils.getFriendlyName(this.directoryUrlPath, false));
            		target.append("/");
            		target.append(this.documentNameField.getText());
            	}
            	if (!target.toString().endsWith(".docx")) {
            		target.append(".docx");
            	}
            	
            	if (this.hyperlinkML.canSetTarget()) {
            		this.hyperlinkML.setTarget(target.toString());
            	} else {
            		this.hyperlinkML.setDummyTarget(target.toString());
            	}
            	
            	if (this.displayTextField.getText().length() == 0) {
            		this.hyperlinkML.setDisplayText(target.toString());
            	} else {
            		this.hyperlinkML.setDisplayText(this.displayTextField.getText());
            	}
            	this.hyperlinkML.setTooltip(this.tooltipField.getText());
            	
           	
            } else {
            	//User closed dialog or clicked cancel
            	this.value = CANCEL_BUTTON_TEXT;
            }
            
            //do not keep a reference to the edited hyperlinkML
            //so that this dialog can be disposed of.
            this.hyperlinkML = null;
        }
    }
    
	private JPanel createContentPanel(String sourceFileVFSUrlPath, HyperlinkML hyperlinkML) {
		JPanel thePanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		fillRow1(thePanel, c);
		fillRow2(thePanel, c);
		fillRow3(thePanel, c);
		fillRow4(thePanel, c);
		fillRow5(thePanel, c);
		
		//Display Text Field
		if (hyperlinkML.getDisplayText() != null) {
			this.displayTextField.setText(hyperlinkML.getDisplayText());
		}
		
		this.displayTextFieldAutoUpdate =
			(this.displayTextField.getText().length() == 0);
		//Set DisplayTextFieldAutoUpdate flag
		this.displayTextField.addKeyListener(new KeyAdapter() {
		    public void keyTyped(KeyEvent e) {
		    	JTextField field = (JTextField) e.getSource();
		    	if (field.getText().length() == 0) {
		    		ExternalHyperlinkDialog.this.displayTextFieldAutoUpdate = true;
		    	} else {
		    		ExternalHyperlinkDialog.this.displayTextFieldAutoUpdate = false;
		    	}
		    }
		});
		
		//Tooltip Field
		if (hyperlinkML.getTooltip() != null) {
			this.tooltipField.setText(hyperlinkML.getTooltip());
		}
		
		//Target Field is split into documentNameField and directoryPathField.
		//These two fields are in friendly format; ie: no user credentials
		String target = 
			HyperlinkML.encodeTarget(hyperlinkML, getSourceFileObject(), true);
		if (target != null) {
			int idx = target.lastIndexOf("/");
			this.documentNameField.setText(target.substring(idx+1));
			if (target.startsWith(FILE_PREFIX)) {
				target = target.substring(FILE_PREFIX.length(), idx);
			} else {
				target = target.substring(0, idx);
			}
			this.directoryPathField.setText(target);
		} else {
			//A brand new HyperlinkML may be ?
			target = VFSUtils.getFriendlyName(this.sourceFileVFSUrlPath);
	        int idx = target.lastIndexOf("/");
	        //Leave this.documentNameField blank
	        this.directoryPathField.setText(target.substring(0, idx));
		}
		
		//Install DocumentNameFieldListener so that Display Text Field
		//can be automatically updated.
		this.documentNameField.getDocument().addDocumentListener(
			new DocumentNameFieldListener());
		
		//We also need to keep a reference to target in complete VFS url format;
		//ie: include user credentials in the reference.
		target = HyperlinkML.encodeTarget(hyperlinkML, getSourceFileObject(), false);
		if (target != null) {
        	int idx = target.lastIndexOf("/");
        	this.directoryUrlPath = target.substring(0, idx);
		} else {
			//A brand new HyperlinkML may be ?
			int idx = this.sourceFileVFSUrlPath.lastIndexOf("/");
			this.directoryUrlPath = this.sourceFileVFSUrlPath.substring(0, idx);
		}
		
		return thePanel;
	}
	
	private FileObject getSourceFileObject() {
		FileObject fo = null;
		try {
			fo = VFSUtils.getFileSystemManager().resolveFile(this.sourceFileVFSUrlPath);
		} catch (FileSystemException exc) {
			;//ignore
		}
		return fo;
	}
	
	private void fillRow1(JPanel host, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        this.displayTextLabel = new JLabel("Text to display");
        host.add(this.displayTextLabel, c);
        
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.displayTextField = new JTextField(70);
        //this.displayTextField.setMinimumSize(new Dimension(100, 70));
        //this.displayTextField.setPreferredSize(new Dimension(100, 70));
        host.add(this.displayTextField, c);
	}
	
	private void fillRow2(JPanel host, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        this.tooltipLabel = new JLabel("Tooltip Text");
        host.add(this.tooltipLabel, c);
        
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.tooltipField = new JTextField(70);
        //this.tooltipField.setMinimumSize(new Dimension(100, 70));
        //this.tooltipField.setPreferredSize(new Dimension(100, 70));
        host.add(this.tooltipField, c);
	}
	
	private void fillRow3(JPanel host, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        this.documentNameLabel = new JLabel("Document Name");
        host.add(this.documentNameLabel, c);
        
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.documentNameField = new JTextField(70);
        //this.documentNameField.setMinimumSize(new Dimension(100, 70));
        //this.documentNameField.setPreferredSize(new Dimension(100, 70));
        host.add(this.documentNameField, c);
	}
	
	private void fillRow4(JPanel host, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        this.directoryPathLabel = new JLabel("Directory Full Path");
        host.add(this.directoryPathLabel, c);
        
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        host.add(Box.createHorizontalGlue(), c);
	}
	
	private void fillRow5(JPanel host, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        
		JPanel directoryPathPanel = new JPanel();
		directoryPathPanel.setBorder(BorderFactory.createEtchedBorder());
		
        this.directoryPathField = new JTextArea();
        this.directoryPathField.setEditable(false);
        this.directoryPathField.setEnabled(false);
        this.directoryPathField.setLineWrap(true);
        
		JScrollPane sp = new JScrollPane(this.directoryPathField);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setMinimumSize(new Dimension(350, 100));
		sp.setPreferredSize(new Dimension(350, 100));
		directoryPathPanel.add(sp);
        host.add(directoryPathPanel, c);
        
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = gridCellInsets;
        c.ipadx = 0;
        c.ipady = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.NONE;
        
        this.selectButton = new JButton("Select...");
        this.selectButton.addActionListener(new SelectButtonActionListener());
        this.selectButton.setSize(100, 50);
        host.add(this.selectButton, c);
	}
	
	private boolean isSourceFileDirectory(String dir) {
		int idx = sourceFileVFSUrlPath.lastIndexOf("/");
		String sourceDir = sourceFileVFSUrlPath.substring(0, idx);
		return sourceDir.equals(dir);
	}
	
	private class DocumentNameFieldListener implements DocumentListener {
	    public void insertUpdate(DocumentEvent e) {
	    	updateDisplayTextField(e.getDocument());
	    }

	    public void removeUpdate(DocumentEvent e) {
	    	updateDisplayTextField(e.getDocument());
	    }

	    public void changedUpdate(DocumentEvent e) {
	    	updateDisplayTextField(e.getDocument());
	    }
	    
		private void updateDisplayTextField(javax.swing.text.Document doc) {
			if (ExternalHyperlinkDialog.this.displayTextFieldAutoUpdate) {
				try {
					int length = doc.getLength();
					String temp = doc.getText(0, length);
					if (isSourceFileDirectory(ExternalHyperlinkDialog.this.directoryUrlPath)) {
						//Display Text Field displays Document Name Field.
						ExternalHyperlinkDialog.this.displayTextField.setText(temp);						
					} else {
						//Display Text Field displays complete document path.
						String dir = ExternalHyperlinkDialog.this.directoryPathField.getText();
						StringBuilder sb = new StringBuilder();
						sb.append(dir);
						if (!dir.endsWith("/")) {
							sb.append("/");
						}
						sb.append(temp);
						ExternalHyperlinkDialog.this.displayTextField.setText(temp);
					}
					
				} catch (BadLocationException exc) {
					;//ignore
				}
			}
		}
	} //DocumentNameFieldListener inner class
	
	private class SelectButtonActionListener implements ActionListener {
	    public void actionPerformed(ActionEvent e) {
	    	VFSJFileChooser chooser = createFileChooser();
	        RETURN_TYPE returnVal = 
	        	chooser.showOpenDialog((Component) e.getSource());
	        
	        if (returnVal == RETURN_TYPE.APPROVE) {
	        	FileObject fo = chooser.getSelectedFile();
	        	try {
	            	if (fo.getType() == FileType.FILE 
	            		|| fo.getType() == FileType.IMAGINARY) {
	            		fo = 
	            			FileMenu.getInstance().getSelectedFile(
	            				chooser, 
	            				Constants.DOCX_STRING);
	            		ExternalHyperlinkDialog.this.documentNameField.setText(
	            			fo.getName().getBaseName());
	            		fo = fo.getParent();
	            	}
	        	} catch (FileSystemException exc) {
	        		exc.printStackTrace();
	        		fo = null;
	        	}
	        	
	        	if (fo != null) {
	        		//fo is a folder.
	        		ExternalHyperlinkDialog.this.directoryUrlPath = 
	        			fo.getName().getURI();
	            	String path = 
	            		VFSUtils.getFriendlyName(
	            			ExternalHyperlinkDialog.this.directoryUrlPath);
	            	ExternalHyperlinkDialog.this.directoryPathField.setText(path);
	        	}
			}
	    }
	    
	    private VFSJFileChooser createFileChooser() {
	       	VFSJFileChooser chooser = new VFSJFileChooser();
	        chooser.setAccessory(new DefaultAccessoriesPanel(chooser));
	        chooser.setFileHidingEnabled(false);
	        chooser.setMultiSelectionEnabled(false);
	            
	        chooser.setFileSelectionMode(SELECTION_MODE.FILES_AND_DIRECTORIES);
	        
	        FileObject showedDir = null;
	   		try {
	   			String s = getCurrentDirectory();
	            showedDir = VFSUtils.getFileSystemManager().resolveFile(s);
	        } catch (FileSystemException exc) {
	        	;//ignore
	        }
	       	chooser.addChoosableFileFilter(ExternalHyperlinkDialog.this.fileFilter);
	        chooser.setCurrentDirectory(showedDir);
	            
	        return chooser;
	    }
	    
	    /**
	     * This method checks whether the current value of 
	     * ExternalHyperlinkDialog.this.directoryUrlPath has and needs
	     * user credentials.
	     * If it does not have user credentials but it needs one then
	     * VFSJFileChooser bookmark entries will be looked up.
	     * If a bookmark entry for the same host is found then
	     * the entry's user credentials will be supplied to 
	     * ExternalHyperlinkDialog.this.directoryUrlPath.
	     * 
	     * @return the value of ExternalHyperlinkDialog.this.directoryUrlPath
	     * that has been given user credentials if there is one. 
	     */
	    private String getCurrentDirectory() {
	    	String thePath = ExternalHyperlinkDialog.this.directoryUrlPath;
	    	if (thePath.startsWith("file://")) {
	    		//local file
	    	} else {
	    		int hostStartIdx = thePath.lastIndexOf("@");
	    		if (hostStartIdx == -1) {
	    			//Most likely thePath has no user credentials
	    			VFSURIParser pathParser = new VFSURIParser(thePath, false);
	    			
    				Bookmarks book = new Bookmarks();
    				VFSURIParser entryParser = null;
    				for (int i=0; i < book.getSize(); i++) {
    					TitledURLEntry entry = book.getEntry(i);
    					entryParser = new VFSURIParser(entry.getURL(), false);
    					if (pathParser.getProtocol() == entryParser.getProtocol()
    						&& pathParser.getHostname().equals(entryParser.getHostname())
    						&& pathParser.getPath().startsWith(entryParser.getPath())) {
    						i = book.getSize(); //break
    					} else {
    						entryParser = null;
    					}
    				}
    				if (entryParser != null
    					&& entryParser.getUsername() != null
    					&& entryParser.getUsername().length() > 0
    					&& entryParser.getPassword() != null
    					&& entryParser.getPassword().length() > 0) {
    					StringBuilder sb = new StringBuilder();
    					sb.append(pathParser.getProtocol().getName().toLowerCase());
    					sb.append("://");
    					sb.append(entryParser.getUsername());
    					sb.append(":");
    					sb.append(entryParser.getPassword());
    					sb.append("@");
    					int idx = thePath.indexOf("://") + 3;
    					sb.append(thePath.substring(idx));
    					thePath = sb.toString();
    				}
	    		}
	    	}
	    	return thePath;
	    }

	} //ChangeButtonActionListener inner class
	
}// ExternalHyperlinkDialog class



















