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

/**
 *	@author Jojada Tirtowidjojo - 06/12/2007
 */
public class MenuItemStateManager implements PropertyChangeListener {
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
    	boolean newValue = ((Boolean) evt.getNewValue()).booleanValue();
    	_menuItem.setEnabled(newValue);
    }

}// MenuItemStateManager class



















