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

import org.docx4all.ui.main.ToolBarStates;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;

import javafx.ui.Button as JFXButton;
import javafx.ui.Image;

public class Button extends JFXButton {
    attribute enabledPropertyName: String?;
    attribute swingAction: <<javax.swing.Action>>?;

    private operation setSwingAction(action:<<javax.swing.Action>>);
    private attribute stateListener: <<java.beans.PropertyChangeListener>>;
}

operation Button.setSwingAction(action:<<javax.swing.Action>>) {
    if (action <> null) {
        var actionIcon = (<<javax.swing.ImageIcon>>) action.getValue(action.SMALL_ICON);
        var actionTooltipText = (String) action.getValue(action.SHORT_DESCRIPTION);
    
        icon = Image {
            url: actionIcon.getDescription()
        };
    
        toolTipText = actionTooltipText;

        ((<<javax.swing.JButton>>) getComponent()).setAction(action);
        ((<<javax.swing.JButton>>) getComponent()).setHideActionText(true);
    }
}

trigger on Button.swingAction = newAction {
    setSwingAction(newAction);
}

trigger on Button.enabledPropertyName[oldName] = newName {
    //Java objects passed by ScriptEngine into scripting environment
    var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
    
    if (oldName <> null) {
        toolBarStates.removePropertyChangeListener((String) oldName, stateListener);
    }
    if (newName <> null) {
        toolBarStates.addPropertyChangeListener((String) newName, stateListener);
    }
}

trigger on (new Button) {
    setSwingAction(swingAction);

    var self = this;
    this.stateListener =  new PropertyChangeListener {
        operation propertyChange(evt:PropertyChangeEvent) {
            self.enabled = ((Boolean) evt.getNewValue()).booleanValue();
        }
    };// stateListener
    
    if (enabledPropertyName <> null) {
        //Java objects passed by ScriptEngine into scripting environment
        var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
        toolBarStates.addPropertyChangeListener((String) enabledPropertyName, this.stateListener);
    }
}




















