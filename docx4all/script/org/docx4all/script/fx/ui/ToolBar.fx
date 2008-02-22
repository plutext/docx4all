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

import org.docx4all.ui.menu.FileMenu;
import org.docx4all.swing.text.StyleSheet;

import java.awt.GraphicsEnvironment;

import javafx.ui.ButtonGroup;
import javafx.ui.ComboBoxCell;
import javafx.ui.EtchedBorder;
import javafx.ui.EtchType;
import javafx.ui.FlowPanel;
import javafx.ui.RigidArea;
import javafx.ui.Orientation;
import javafx.ui.Separator;
import javafx.ui.ToolBar as JFXToolBar;
import javafx.ui.Widget;

//Java objects passed by ScriptEngine into scripting environment
var toolBarStates = toolBarStates:<<org.docx4all.ui.main.ToolBarStates>>;
var fileMenu = fileMenu:<<org.docx4all.ui.menu.FileMenu>>;
var editMenu = editMenu:<<org.docx4all.ui.menu.EditMenu>>;
var formatMenu = formatMenu:<<org.docx4all.ui.menu.FormatMenu>>;

//Global variables
var alignLeftButton = ToggleButton {
    enabledPropertyName: toolBarStates.ALIGNMENT_PROPERTY_NAME
    swingAction: formatMenu.getAction(formatMenu.ALIGN_LEFT_ACTION_NAME)
};

var alignCtrButton = ToggleButton {
    enabledPropertyName: toolBarStates.ALIGNMENT_PROPERTY_NAME
    swingAction: formatMenu.getAction(formatMenu.ALIGN_CENTER_ACTION_NAME)
};

var alignRightButton = ToggleButton {
    enabledPropertyName: toolBarStates.ALIGNMENT_PROPERTY_NAME
    swingAction: formatMenu.getAction(formatMenu.ALIGN_RIGHT_ACTION_NAME)
};

var alignJustifiedButton = ToggleButton {
    enabledPropertyName: toolBarStates.ALIGNMENT_PROPERTY_NAME
    swingAction: formatMenu.getAction(formatMenu.ALIGN_JUSTIFIED_ACTION_NAME)
};


ButtonGroup {
    buttons: [
        alignLeftButton,
        alignCtrButton,
        alignRightButton,
        alignJustifiedButton
    ]
}

TOOL_BAR_1:JFXToolBar = JFXToolBar {
    var newFileButton = Button {
        swingAction: fileMenu.getAction(fileMenu.NEW_FILE_ACTION_NAME)
    }

    var openFileButton = Button {
        swingAction: fileMenu.getAction(fileMenu.OPEN_FILE_ACTION_NAME)
    }

    var saveFileButton = Button {
        enabledPropertyName: toolBarStates.DOC_DIRTY_PROPERTY_NAME
        swingAction: fileMenu.getAction(fileMenu.SAVE_FILE_ACTION_NAME)
        enabled: false
    }
    
    var saveAllFilesButton = Button {
        enabledPropertyName: toolBarStates.ALL_DOC_DIRTY_PROPERTY_NAME
        swingAction: fileMenu.getAction(fileMenu.SAVE_ALL_FILES_ACTION_NAME)
        enabled: false
    }
    
    var printPreviewButton = Button {
        enabledPropertyName: toolBarStates.IFRAME_NUMBERS_PROPERTY_NAME
        swingAction: fileMenu.getAction(fileMenu.PRINT_PREVIEW_ACTION_NAME)
        enabled: false
    }
    
    var cutButton = Button {
        swingAction: editMenu.getAction(editMenu.CUT_ACTION_NAME)
    }
    
    var copyButton = Button {
        swingAction: editMenu.getAction(editMenu.COPY_ACTION_NAME)
    }
    
    var pasteButton = Button {
        swingAction: editMenu.getAction(editMenu.PASTE_ACTION_NAME)
    }
    
    var styleCombo = StylesComboBox {
        var styleNames = StyleSheet.getDefaultStyleSheet().getUIStyleNames()
        cells: foreach (style in styleNames)
               ComboBoxCell { text: style }
        selection: 0
        styleSheetChangePropertyName:toolBarStates.STYLE_SHEET_PROPERTY_NAME
        propertyNameToListen: toolBarStates.SELECTED_STYLE_PROPERTY_NAME              
        swingAction: formatMenu.getAction(formatMenu.APPLY_STYLE_ACTION_NAME)
    }
    
    var fontFamilyCombo = ComboBox {
        var fontNames = 
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                  getAvailableFontFamilyNames()
        var: self
        selection: select indexof font from font in fontNames
                   where font.startsWith("Monospaced")
        cells: foreach (font in fontNames)
               ComboBoxCell { text: font }
            
        propertyNameToListen: toolBarStates.FONT_FAMILY_PROPERTY_NAME
        swingAction: formatMenu.getAction(formatMenu.FONT_FAMILY_ACTION_NAME)
    }
    
    var fontSizeCombo = ComboBox {
        var fontSizes = ["8", "9", "10", "11", "12", "14", "16", "18",
                         "20", "22", "24", "26", "28", "32", "36", "40", 
                         "44", "48", "52", , "56", "64", "72"]
        var: self
        selection: 4
        cells: foreach (size in fontSizes)
               ComboBoxCell { text: size }
               
        propertyNameToListen: toolBarStates.FONT_SIZE_PROPERTY_NAME
        swingAction: formatMenu.getAction(formatMenu.FONT_SIZE_ACTION_NAME)
    }
    
    var boldButton = ToggleButton {
        enabledPropertyName: toolBarStates.FONT_BOLD_PROPERTY_NAME
        swingAction: formatMenu.getAction(formatMenu.BOLD_ACTION_NAME)
    }
    
    var italicButton = ToggleButton {
        enabledPropertyName: toolBarStates.FONT_ITALIC_PROPERTY_NAME
        swingAction: formatMenu.getAction(formatMenu.ITALIC_ACTION_NAME)
    }
    
    var underlineButton = ToggleButton {
        enabledPropertyName: toolBarStates.FONT_UNDERLINED_PROPERTY_NAME
        swingAction: formatMenu.getAction(formatMenu.UNDERLINE_ACTION_NAME)
    }
    
    borderPainted: true
    buttons: [
        newFileButton,
        openFileButton,
        saveFileButton,
        saveAllFilesButton,
        RigidArea { width: 10 },
        printPreviewButton,
        RigidArea { width: 10 },
        Separator { orientation: VERTICAL:Orientation },
        RigidArea { width: 10 },
        cutButton,
        copyButton,
        pasteButton,
        RigidArea { width: 10 },
        Separator { orientation: VERTICAL:Orientation },
        RigidArea { width: 10 },
        styleCombo,
        alignLeftButton,
        alignCtrButton,
        alignRightButton,
        alignJustifiedButton,
        RigidArea { width: 10 },
        Separator { orientation: VERTICAL:Orientation },
        RigidArea { width: 10 },
        fontFamilyCombo,
        fontSizeCombo,
        RigidArea { width: 10 },
        boldButton,
        italicButton,
        underlineButton        
    ]
};

TOOL_BAR:Widget = FlowPanel {
    alignment: LEADING
    border: EtchedBorder {
        style: LOWERED
    }
    content: [
        TOOL_BAR_1:JFXToolBar
    ]
};

return (TOOL_BAR:Widget).getComponent();

















