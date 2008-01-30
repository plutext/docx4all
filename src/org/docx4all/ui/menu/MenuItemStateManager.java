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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JMenuItem;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.ToolBarStates;

/**
 *	@author Jojada Tirtowidjojo - 06/12/2007
 */
public class MenuItemStateManager implements PropertyChangeListener {
	private static Logger log = Logger.getLogger(MenuItemStateManager.class);
	
	private final JMenuItem _menuItem;
	
	public MenuItemStateManager(JMenuItem item) {
		_menuItem = item;
	}
	
    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source 
     *   	and the property that has changed.
     */

    public void propertyChange(PropertyChangeEvent evt) {
    	String name = evt.getPropertyName();
    	
    	if (log.isDebugEnabled()) {
    		log.debug("propertyChange(): evt.getPropertyName()=" 
    			+ name 
    			+ " new value=" 
    			+ evt.getNewValue());
    	}
    	
    	if (ToolBarStates.DOC_DIRTY_PROPERTY_NAME.equals(name)
    		|| ToolBarStates.ALL_DOC_DIRTY_PROPERTY_NAME.equals(name)) {
        	boolean newValue = ((Boolean) evt.getNewValue()).booleanValue();
        	_menuItem.setEnabled(newValue);
        	
    	} else if (ToolBarStates.ALIGNMENT_PROPERTY_NAME.equals(name)) {
    		Integer newValue = (Integer) evt.getNewValue();
    		
    		FormatMenu fm = FormatMenu.getInstance();
    		if (fm.getAction(FormatMenu.ALIGN_LEFT_ACTION_NAME) == _menuItem.getAction()
    				&& newValue.intValue() == StyleConstants.ALIGN_LEFT) {
    			_menuItem.setEnabled(false);
    			
    		} else if (fm.getAction(FormatMenu.ALIGN_CENTER_ACTION_NAME) == _menuItem.getAction()
    					&& newValue.intValue() == StyleConstants.ALIGN_CENTER) {
    			_menuItem.setEnabled(false);
    			
    		} else if (fm.getAction(FormatMenu.ALIGN_RIGHT_ACTION_NAME) == _menuItem.getAction()
    					&& newValue.intValue() == StyleConstants.ALIGN_RIGHT) {
    			_menuItem.setEnabled(false);
   			
    		} else {
    			_menuItem.setEnabled(true);
    		}
    		
    	} else if (ToolBarStates.IFRAME_NUMBERS_PROPERTY_NAME.equals(name)) {
    		int newValue = ((Integer) evt.getNewValue());
    		_menuItem.setEnabled((newValue > 0));
    	}
    	
    }

}// MenuItemStateManager class



















