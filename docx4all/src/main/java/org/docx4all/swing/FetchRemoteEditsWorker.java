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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.docx4all.ui.main.WordMLEditor;
import org.plutext.client.Mediator;
import org.plutext.transforms.Changesets.Changeset;

/**
 *	@author Jojada Tirtowidjojo - 09/09/2008
 */
public class FetchRemoteEditsWorker extends SwingWorker<Void, Void> implements IProgressBarWorker {
	private final WordMLEditor wmlEditor;
	private final Mediator plutextClient;
	private final Hashtable<Integer, String> messageTable;
	
	private Cursor origCursor;	
	private Exception exc;
	private JComponent insertedEndMessage;
	
	public FetchRemoteEditsWorker(Mediator plutextClient, WordMLEditor wmlEditor) {
		this.wmlEditor = wmlEditor;
		this.plutextClient = plutextClient;
		this.messageTable = new Hashtable<Integer, String>(6);
		this.exc = null;
	}
	
    /*
     * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {
    	try {
        	WordMLTextPane editor = this.plutextClient.getWordMLTextPane();
        	setOrigCursor(editor.getCursor());
        	editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        	
    		plutextClient.startSession();
    		plutextClient.fetchUpdates(this);
    		
        } catch (Exception exc) {
        	exc.printStackTrace();
        	setException(exc);
        	setProgress(
        		FetchProgress.DONE, 
        		"There was an error while fetching remote edits. Please see logs for details.");
    	}
    	
    	return null;
    }
    
    private final synchronized void setException(Exception exc) {
    	this.exc = exc;
    }
    
    private final synchronized Exception getException() {
    	return this.exc;
    }
    
    private final synchronized void setOrigCursor(Cursor c) {
    	this.origCursor = c;
    }
    
    private final synchronized Cursor getOrigCursor() {
    	return this.origCursor;
    }
    
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() {
    	if (getException() != null) {
    		return;
    	}
    	
    	WordMLTextPane editor = this.plutextClient.getWordMLTextPane();
    	try {
    		editor.beginContentControlEdit();
    		
    		plutextClient.applyRemoteChanges(this);
    		
    		if (plutextClient.getChangeSets() != null
    			&& !plutextClient.getChangeSets().isEmpty()) {
    			createChangesetsTable(plutextClient.getChangeSets());
    		}
    		
    		//Complete the overall progress but
    		//retain the last message posted by FetchProgress.APPLYING_DONE.
    		//This is because the last message is meaningful to user.
    		//See: plutextClient.applyRemoteChanges();
    		setProgress(FetchProgress.DONE, getProgressMessage(FetchProgress.APPLYING_DONE));
//    	} catch (Exception e) {
//    		e.printStackTrace();
    	} finally {
    		plutextClient.endSession();
    		editor.endContentControlEdit();
    		editor.setCursor(getOrigCursor());
    	}
    }

    public void setProgress(FetchProgress progress, String message) {
    	setProgress(progress.value(), message);
    }
    
    public void setProgress(Integer progress, String message) {
    	//String old = this.messageTable.get(progress);
        this.messageTable.put(progress, message);
        //firePropertyChange("message", old, message);

       	setProgress(progress.intValue());
    }
    
    public String getProgressMessage(FetchProgress progress) {
    	return getProgressMessage(progress.value());
    }
    
    public String getProgressMessage(Integer progressValue) {
    	return this.messageTable.get(progressValue);
    }
    
    public JComponent getInsertedEndMessage() {
    	return this.insertedEndMessage;
    }
    
    private void createChangesetsTable(Map<String, Changeset> changesets) {
    	TableModel model = new ChangesetsTableModel(changesets);
    	JTable table = new JTable(model);
    	
    	Font font = table.getTableHeader().getFont();
    	font = font.deriveFont(Font.BOLD, 14);
    	table.getTableHeader().setFont(font);
    	
    	font = table.getFont();
    	font = font.deriveFont(14);
    	table.setFont(font);
    	
    	table.getTableHeader().setResizingAllowed(true);
    	table.setIntercellSpacing(new Dimension(5,5));
    	
    	if (model.getRowCount() > 5) {
    		JScrollPane sp = new JScrollPane(table);
    		sp.setHorizontalScrollBarPolicy(
    				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    		table.setPreferredScrollableViewportSize(new Dimension(550, 120));
    		this.insertedEndMessage = sp;
    	} else {
    		JPanel p = new JPanel();
    		p.setLayout(new BorderLayout());
    		p.add(table.getTableHeader(), BorderLayout.PAGE_START);
    		p.add(table, BorderLayout.CENTER);
    		this.insertedEndMessage = p;
    	}
    }
    
    public enum FetchProgress {
    	START_FETCHING (10),
    	REGISTERING_UPDATES (20),
    	FETCHING_REMOTE_DOC_STRUCTURE (30),
    	FETCHING_DONE (40),
    	START_APPLYING_UPDATES (50),
    	COMPARING_DOC_STRUCTURES (60),
    	APPLYING_UPDATES (70),
    	APPLYING_DONE (80),
    	LINKS (90),
    	
    	DONE (100);
    	
    	private final Integer value;
    	FetchProgress(int value) {
    		this.value = Integer.valueOf(value);
    	}
    	
    	public Integer value() {
    		return value;
    	}
    }
    
	private final static String[] COLUMN_HEADERS = new String[] {
		"#",
		"Id",
		"Author",
		"Date",
		"Message"
	};
	
	private final static int ROW_COLUMN = 0;
	private final static int ID_COLUMN = 1;
	private final static int AUTHOR_COLUMN = 2;
	private final static int DATE_COLUMN = 3;
	private final static int MESSAGE_COLUMN = 4;
	
    private class ChangesetsTableModel extends AbstractTableModel {
    	private List<Changeset> changesets;
    	ChangesetsTableModel(Map<String, Changeset> changesets) {
    		this.changesets = new ArrayList<Changeset>(changesets.values());
    		Collections.sort(this.changesets, new ChangesetComparator());
    	}
    	
        public int getRowCount() {
        	return changesets.size();
        }

        public int getColumnCount() {
        	return COLUMN_HEADERS.length;
        }

        public String getColumnName(int columnIndex) {
        	if (0 <= columnIndex && columnIndex < getColumnCount()) {
        		return COLUMN_HEADERS[columnIndex];
        	}
        	throw new IndexOutOfBoundsException("columnIndex=" + columnIndex);
        }

        public Class<?> getColumnClass(int columnIndex) {
        	return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
        	return false;
        }
        
        public Object getValueAt(int rowIndex, int columnIndex) {
        	if ((0 <= rowIndex && rowIndex < getRowCount())
        		&& (0 <= columnIndex && columnIndex < getColumnCount())) {
        		String value = null;
        		Changeset cs = changesets.get(rowIndex);
        		
        		switch (columnIndex) {
        		case ROW_COLUMN:
        			value = Integer.toString(rowIndex + 1);
        			break;
        		case ID_COLUMN: 
        			value = Long.toString(cs.getNumber());
        			break;
        		case AUTHOR_COLUMN: 
        			value = cs.getModifier();
        			break;
        		case DATE_COLUMN: 
        			value = cs.getDate();
        			break;
        		case MESSAGE_COLUMN: 
        			value = cs.getValue();
        		}
        		return value;
        	}
        	throw new IndexOutOfBoundsException(
        			"rowIndex=" 
        			+ rowIndex
        			+ ", columnIndex=" 
        			+ columnIndex);
        }

    }// ChangesetsTableModel inner class
    
    public class ChangesetComparator implements Comparator<Changeset> {
        public int compare(Changeset c1, Changeset c2) {
        	Long sn1 = Long.valueOf(c1.getNumber());
        	Long sn2 = Long.valueOf(c2.getNumber());
        	return sn1.compareTo(sn2);
        }
    }// TransformComparator class
    
}// FetchRemoteEditsWorker class



















