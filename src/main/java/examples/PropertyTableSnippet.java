/*******************************************************************************
 * Copyright (c) 2012 Laurent CARON
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent CARON (laurent.caron at gmail dot com) - initial API and implementation 
 *******************************************************************************/
package examples;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.mihalis.opal.propertyTable.PTProperty;
import org.mihalis.opal.propertyTable.PropertyTable;
import org.mihalis.opal.propertyTable.editor.PTCheckboxEditor;
import org.mihalis.opal.propertyTable.editor.PTColorEditor;
import org.mihalis.opal.propertyTable.editor.PTComboEditor;
import org.mihalis.opal.propertyTable.editor.PTDateEditor;
import org.mihalis.opal.propertyTable.editor.PTDimensionEditor;
import org.mihalis.opal.propertyTable.editor.PTDirectoryEditor;
import org.mihalis.opal.propertyTable.editor.PTFileEditor;
import org.mihalis.opal.propertyTable.editor.PTFloatEditor;
import org.mihalis.opal.propertyTable.editor.PTFontEditor;
import org.mihalis.opal.propertyTable.editor.PTInsetsEditor;
import org.mihalis.opal.propertyTable.editor.PTIntegerEditor;
import org.mihalis.opal.propertyTable.editor.PTPasswordEditor;
import org.mihalis.opal.propertyTable.editor.PTRectangleEditor;
import org.mihalis.opal.propertyTable.editor.PTSpinnerEditor;
import org.mihalis.opal.propertyTable.editor.PTURLEditor;
import org.mihalis.opal.utils.SWTGraphicUtil;

/**
 * This snippet demonstrates the PropertyTable widget
 * 
 */
public class PropertyTableSnippet {

        /**
         * @param args
         */
        public static void main(final String[] args) {

                Locale.setDefault(Locale.ENGLISH);

                final Display display = new Display();
                final Shell shell = new Shell(display);
                shell.setText("PropertyTable snippet");
                shell.setLayout(new FillLayout(SWT.VERTICAL));

                final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);

                final TabItem item1 = new TabItem(tabFolder, SWT.NONE);
                item1.setText("First");
                item1.setControl(buildPropertyTable(tabFolder, true, true, true));

                final TabItem item2 = new TabItem(tabFolder, SWT.NONE);
                item2.setText("Second");
                item2.setControl(buildPropertyTable(tabFolder, false, true, false));

                final TabItem item3 = new TabItem(tabFolder, SWT.NONE);
                item3.setText("Third");
                item3.setControl(buildPropertyTable(tabFolder, true, false, true));

                final TabItem item4 = new TabItem(tabFolder, SWT.NONE);
                item4.setText("Forth");
                item4.setControl(buildPropertyTable(tabFolder, true, false, false));

                shell.setSize(800, 600);
                shell.open();
                SWTGraphicUtil.centerShell(shell);

                while (!shell.isDisposed()) {
                        if (!display.readAndDispatch()) {
                                display.sleep();
                        }
                }

                display.dispose();
        }

        /**
         * Build a property table
         * 
         * @param tabFolder tabFolder that holds the property table
         * @param showButton if <code>true</code>, show buttons
         * @param showAsCategory if <code>true</code>, show property as categories.
         *            If <code>false</code>, show property as a flat list
         * @param showDescription if <code>true</code>, show description
         * @return a property table
         */
        private static PropertyTable buildPropertyTable(final TabFolder tabFolder, final boolean showButton, final boolean showAsCategory, final boolean showDescription) {
                final PropertyTable table = new PropertyTable(tabFolder, SWT.CHECK);

                if (showButton) {
                        table.showButtons();
                } else {
                        table.hideButtons();
                }

                if (showAsCategory) {
                        table.viewAsCategories();
                } else {
                        table.viewAsFlatList();
                }

                if (showDescription) {
                        table.showDescription();
                } else {
                        table.hideDescription();
                }
                table.addProperty(new PTProperty("id", "Identifier", "Description for identifier", "My id")).setCategory("General");
                table.addProperty(new PTProperty("text", "Description", "Description for the description field", "blahblah...")).setCategory("General");
                table.addProperty(new PTProperty("url", "URL:", "This is a nice <b>URL</b>", "http://www.google.com").setCategory("General")).setEditor(new PTURLEditor());
                table.addProperty(new PTProperty("password", "Password", "Enter your <i>password</i> and keep it secret...", "password")).setCategory("General").setEditor(new PTPasswordEditor());

                table.addProperty(new PTProperty("int", "An integer", "Type any integer", "123")).setCategory("Number").setEditor(new PTIntegerEditor());
                table.addProperty(new PTProperty("float", "A float", "Type any float", "123.45")).setCategory("Number").setEditor(new PTFloatEditor());
                table.addProperty(new PTProperty("spinner", "Another integer", "Use a spinner to enter an integer")).setCategory("Number").setEditor(new PTSpinnerEditor(0, 100));

                table.addProperty(new PTProperty("directory", "Directory", "Select a directory")).setCategory("Directory/File").setEditor(new PTDirectoryEditor());
                table.addProperty(new PTProperty("file", "File", "Select a file")).setCategory("Directory/File").setEditor(new PTFileEditor());

                table.addProperty(new PTProperty("comboReadOnly", "Combo (read-only)", "A simple combo with seasons")).setCategory("Combo").setEditor(new PTComboEditor(true, new Object[] {"Spring", "Summer", "Autumn", "Winter"} ) );
                table.addProperty(new PTProperty("combo", "Combo", "A combo that is not read-only")).setCategory("Combo").setEditor(new PTComboEditor("Value 1", "Value 2", "Value 3"));

                table.addProperty(new PTProperty("cb", "Checkbox", "A checkbox")).setCategory("Checkbox").setEditor(new PTCheckboxEditor()).setCategory("Checkbox");
//                table.addProperty(new PTProperty("cb", "Checkbox", "A checkboxxx")).setCategory("Checkbox").setEditor(new PTCheckboxEditor()).setCategory("Checkbox");
                table.addProperty(new PTProperty("cb2", "Checkbox (disabled)", "A disabled checkbox...")).setEditor(new PTCheckboxEditor()).setCategory("Checkbox").setEnabled(false);

                table.addProperty(new PTProperty("color", "Color", "Pick it !")).setCategory("Misc").setEditor(new PTColorEditor());
                table.addProperty(new PTProperty("font", "Font", "Pick again my friend")).setEditor(new PTFontEditor()).setCategory("Misc");
                table.addProperty(new PTProperty("dimension", "Dimension", "A dimension is composed of a width and a height")).setCategory("Misc").setEditor(new PTDimensionEditor());
                table.addProperty(new PTProperty("rectangle", "Rectangle", "A rectangle is composed of a position (x,y) and a dimension(width,height)")).setCategory("Misc").setEditor(new PTRectangleEditor());
                table.addProperty(new PTProperty("inset", "Inset", "An inset is composed of the following fields:top,left,bottom,right)")).setCategory("Misc").setEditor(new PTInsetsEditor());
                table.addProperty(new PTProperty("date", "Date", "Well, is there something more to say ?")).setCategory("Misc").setEditor(new PTDateEditor());
                
//                table.getProperties().put("cb", "true");
                for (PTProperty p : table.getPropertiesAsList()) {
                	if (p.getName().equals("cb2")) {
                		p.setValue(true);
                	}
                }
                

                return table;
        }

}