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

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 27/11/2007
 */
public abstract class UIMenu {
	private static Logger log = Logger.getLogger(UIMenu.class);
	
	public final static String SEPARATOR_CODE = "---";
	
	public abstract String[] getMenuItemActionNames();
	public abstract String getMenuName();
	
    public javax.swing.Action getAction(String actionName) {
        ApplicationContext context = WordMLEditor.getInstance().getContext();
        javax.swing.Action action = context.getActionMap(getClass(), this).get(actionName);
        
        ResourceMap rm = context.getResourceMap(getClass());
        Icon selectedIcon = rm.getIcon(actionName + ".Action.selected.icon");
        if (selectedIcon != null) {
        	action.putValue("Selected"+javax.swing.Action.SMALL_ICON, selectedIcon);
        }
        
        if (log.isDebugEnabled()) {
        	log.debug("actionName = " + actionName + " action = " + action);
        	if (action != null) {
        		log.debug("icon = " + action.getValue(javax.swing.Action.SMALL_ICON));
        		log.debug("selectedIcon = " + selectedIcon);
        		log.debug("tooltipText = " + action.getValue(javax.swing.Action.SHORT_DESCRIPTION));
        	}
        }
        
        return action;
    }
    
    public JMenu createJMenu() {
        ApplicationContext context = WordMLEditor.getInstance().getContext();
        ResourceMap rm = context.getResourceMap(getClass());
        
		JMenu menu = new JMenu();
		menu.setName(getMenuName());
		menu.setText(rm.getString(getMenuName() + ".text"));
        
		for (String actionName : getMenuItemActionNames()) {
			if (SEPARATOR_CODE.equals(actionName)) {
				menu.add(new JSeparator());
			} else {
				menu.add(createMenuItem(actionName));
			}
		}
		
		return menu;
	}

    protected JMenuItem createMenuItem(String actionName) {
		JMenuItem theItem = new JMenuItem();
		theItem.setAction(getAction(actionName));
		return theItem;
    }
    
    
    protected static class EnableOnPositive implements PropertyChangeListener {
    	private JMenuItem _menuItem;
    	
    	EnableOnPositive(JMenuItem item) {
    		_menuItem = item;
    	}
    	
        public void propertyChange(PropertyChangeEvent evt) {
    		int newValue = ((Integer) evt.getNewValue());
    		_menuItem.setEnabled((newValue > 0));
        }
    } //EnabledOnPositive inner class
    
    protected static class EnableOnEqual implements PropertyChangeListener {
    	private JMenuItem _menuItem;
    	private Object _value;
    	
    	EnableOnEqual(JMenuItem item, Object value) {
    		_menuItem = item;
    		_value = value;
    	}
    	
        public void propertyChange(PropertyChangeEvent evt) {
        	if (evt.getNewValue().equals(_value)) {
        		_menuItem.setEnabled(true);
        	} else {
        		_menuItem.setEnabled(false);
        	}
        }
    } //EnableOnEqual inner class
    
    protected static class DisableOnEqual implements PropertyChangeListener {
    	private JMenuItem _menuItem;
    	private Object _value;
    	
    	DisableOnEqual(JMenuItem item, Object value) {
    		_menuItem = item;
    		_value = value;
    	}
    	
        public void propertyChange(PropertyChangeEvent evt) {
        	if (evt.getNewValue().equals(_value)) {
        		_menuItem.setEnabled(false);
        	} else {
        		_menuItem.setEnabled(true);
        	}
        }
    } //DisableOnEqual inner class
    
    protected static class EnableOnEqualType implements PropertyChangeListener {
    	private JMenuItem _menuItem;
    	private Class<?> _declaredType;
    	
    	EnableOnEqualType(JMenuItem item, Class<?> declaredType) {
    		_menuItem = item;
    		_declaredType = declaredType;
    	}
    	
        public void propertyChange(PropertyChangeEvent evt) {
        	if (evt.getNewValue().getClass() == _declaredType) {
        		_menuItem.setEnabled(true);
        	} else {
        		_menuItem.setEnabled(false);
        	}
        }
    } //EnableOnEqualType inner class
    

}// UIMenu class



















