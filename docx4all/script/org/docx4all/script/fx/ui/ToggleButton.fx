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

package org.docx4all.script.fx.ui;

import javax.swing.Action;

import javafx.ui.ToggleButton as JFXToggleButton;
import javafx.ui.Image;

public class ToggleButton extends JFXToggleButton {
    attribute swingAction:<<javax.swing.Action>>;

    private operation setSwingAction(action:<<javax.swing.Action>>);
}

operation ToggleButton.setSwingAction(action:<<javax.swing.Action>>) {
    if (action <> null) {
        var actionIcon = (<<javax.swing.ImageIcon>>) action.getValue(action.SMALL_ICON);
        var actionSelectedIcon = 
            (<<javax.swing.ImageIcon>>) action.getValue("Selected{action.SMALL_ICON}");
        var actionTooltipText = (String) action.getValue(action.SHORT_DESCRIPTION);
    
        icon = Image {
            url: actionIcon.getDescription()
        };
    
        selectedIcon= Image {
            url: actionSelectedIcon.getDescription()
        };
        
        toolTipText = actionTooltipText;

        ((<<javax.swing.JToggleButton>>) getComponent()).setAction(action);
        ((<<javax.swing.JToggleButton>>) getComponent()).setHideActionText(true);
    }
}

trigger on ToggleButton.swingAction = newAction {
    setSwingAction(newAction);
}

trigger on new ToggleButton {
    setSwingAction(swingAction);
}















