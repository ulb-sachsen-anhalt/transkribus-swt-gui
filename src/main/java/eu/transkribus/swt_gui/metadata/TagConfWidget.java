package eu.transkribus.swt_gui.metadata;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.customtags.CustomTagAttribute;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory;
import eu.transkribus.core.model.beans.customtags.CustomTagFactory.TagRegistryChangeEvent;
import eu.transkribus.swt.util.ColorChooseButton;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class TagConfWidget extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(TagConfWidget.class);
	
	Set<String> availableTagNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	CheckboxTableViewer availableTagsTv;
	SashForm availableTagsSf;
	CustomTagPropertyTableNew propsTable;
//	ColorChooseButton colorChooseBtn;
	
	TagSpecsWidget tagDefsWidget;
	TagSpecsWidgetForCollection tagDefsWidgetForCollection;

	SashForm horizontalSf;
	
	Map<String, ControlEditor> addTagToListEditors = new ConcurrentHashMap<>();
	Map<String, ControlEditor> colorEditors = new ConcurrentHashMap<>();
	
	List<CTabFolder> tabfolder = new ArrayList<>();
	CTabFolder mainTf;
	CTabFolder userTagsTf;
	CTabFolder collectionTagsTf;
	CTabItem userTagsItem;
	CTabItem collectionTagsItem;
	
	List<CTabFolder> tabfolderLeft = new ArrayList<>();
	CTabFolder mainTfLeft;
	CTabFolder userTagsTfLeft;
	CTabFolder collectionTagsTfLeft;
	CTabItem userTagsItemLeft;
	CTabItem collectionTagsItemLeft;
	
	TableViewerColumn checkboxCol;
	
	Button takeOverTagBtn;
	
	public TagConfWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new FillLayout());
		horizontalSf = new SashForm(this, SWT.HORIZONTAL);
		
//		Composite leftWidget = new Composite(horizontalSf, 0);
//		leftWidget.setLayout(new GridLayout(1, false));
		
		mainTfLeft = createTabFolder(horizontalSf);
		mainTfLeft.setLayout(new GridLayout(1, false));

		userTagsTfLeft = createTabFolder(mainTfLeft);
		userTagsItemLeft = createCTabItem(mainTfLeft, userTagsTfLeft, "User tags", null);
		userTagsItemLeft.setFont(Fonts.createBoldFont(userTagsItemLeft.getFont()));
		
		collectionTagsTf = createTabFolder(mainTfLeft);
		collectionTagsItem = createCTabItem(mainTfLeft, collectionTagsTf, "Collection tags", null);
		collectionTagsItem.setFont(Fonts.createBoldFont(collectionTagsItem.getFont()));
		
		Composite tagComp = new Composite(mainTfLeft, 0);
		tagComp.setLayout(new GridLayout(1, false));
		tagComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		availableTagsSf = new SashForm(tagComp, SWT.VERTICAL);
		availableTagsSf.setLayoutData(new GridData(GridData.FILL_BOTH));
		initTagsTable(availableTagsSf);
		initPropertyTable(availableTagsSf);
		
	    // Add an event listener to write the selected tab to stdout
		mainTfLeft.addSelectionListener(new SelectionAdapter() {
		    public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
		        //System.out.println(mainTfLeft.getSelection() + " selected");
		        mainTfLeft.getSelection().setControl(tagComp);
				mainTfLeft.setSelection(mainTfLeft.getSelection());
				for (CTabItem item : mainTfLeft.getItems()) {
					if (mainTfLeft.getSelection().equals(item)) {
						item.setFont(Fonts.createBoldFont(item.getFont()));
					}
					else {
						item.setFont(Fonts.createNormalFont(item.getFont()));
					}
				}
				
				uncheckTagsToTakeOver();
				updateAvailableTags();
				updatePropertiesForSelectedTag();
				updateTabLabels();
		    }

		private void uncheckTagsToTakeOver() {
			availableTagsTv.setAllChecked(false);
			takeOverTagBtn.setEnabled(availableTagsTv.getCheckedElements().length>0);
		}

		private void updateTabLabels() {
			// TODO Auto-generated method stub
			takeOverTagBtn.setText(isCollectionTab()? "Take over to user tags..." : "Take over to collection tags...");
			takeOverTagBtn.setToolTipText(isCollectionTab()? "Adds checked tag(s) to the list of user tags.\nThose tags can be used in every collection." : "Adds checked tag(s) to the list of collection tags.\nThose tags can then be used in Transkribus Lite.");
			
			
		}
	    });
		

		
		//Composite availableTags = initTagsTable(mainTfLeft);
		
		userTagsItemLeft.setControl(tagComp);
		mainTfLeft.setSelection(userTagsItemLeft);
		
		
//		Composite colorComp = new Composite(mainTfLeft, 0);
//		colorComp.setLayout(new GridLayout(2, false));
//		colorComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
//		Label tagColorLbl = new Label(colorComp, 0);
//		tagColorLbl.setText("Color");
//		Fonts.setBoldFont(tagColorLbl);
//		colorChooseBtn = new ColorChooseButton(colorComp, CustomTagSpec.DEFAULT_COLOR);
//		colorChooseBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button addTagSpecBtn = new Button(tagComp, 0);
		addTagSpecBtn.setText("Add tag specification");
		addTagSpecBtn.setToolTipText("Adds the tag with the configuration above to the list of available tags on the right");
		addTagSpecBtn.setImage(Images.ADD);
		addTagSpecBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addTagSpecBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tagName = getSelectedAvailableTagsName();
				if (tagName == null && availableTagsTv.getCheckedElements().length == 0)
					return;
				
				/*
				 * if no tagname selected we search for 'checked' tag names and add to tag spec
				 */
				if (availableTagsTv.getCheckedElements().length > 0) {
					for (int i = 0; i < availableTagsTv.getCheckedElements().length; i++) {
						tagName = (String) availableTagsTv.getCheckedElements()[i];
						logger.debug("checked table item " + tagName);

						try {
							logger.debug("selected tag: "+getSelectedAvailableTagsName()+", tagName: "+tagName+", curr-atts: "+getCurrentAttributes());
							CustomTag tag = CustomTagFactory.create(tagName, getCurrentAttributes(), isCollectionTab());

							CustomTagSpec tagSpec = new CustomTagSpec(tag);
							tagSpec.setColor(getTagColorString(tag.getTagName(), isCollectionTab()));
							
							logger.debug("tagSpec: "+tagSpec);
							Storage.getInstance().addCustomTagSpec(tagSpec);

						} catch (Exception ex) {
							DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding tag definiton", ex.getMessage(), ex);
						}

					}
					availableTagsTv.setAllChecked(false);
				}
				else {
					try {
						if (tagName != null) {
							logger.debug("selected tag: "+getSelectedAvailableTagsName()+", tagName: "+tagName+", curr-atts: "+getCurrentAttributes());
							CustomTag tag = CustomTagFactory.create(tagName, getCurrentAttributes(), isCollectionTab());
	
							CustomTagSpec tagSpec = new CustomTagSpec(tag);
							tagSpec.setColor(getTagColorString(tag.getTagName(), isCollectionTab()));
							
							logger.debug("tagSpec: "+tagSpec);
							Storage.getInstance().addCustomTagSpec(tagSpec);
						}

					} catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding tag definiton", ex.getMessage(), ex);
					}
				}				
			}
		});
		

		
		
		mainTf = createTabFolder(horizontalSf);

		userTagsTf = createTabFolder(mainTf);
		userTagsItem = createCTabItem(mainTf, userTagsTf, "Tag list for fast assignment", null);
		userTagsItem.setFont(Fonts.createBoldFont(userTagsItem.getFont()));
//		initServerTf();

//		collectionTagsTf = createTabFolder(mainTf);//		documentTf.setLayout(new FillLayout());
//		collectionTagsItem = createCTabItem(mainTf, collectionTagsTf, "Collection specific (for WebUI)", null);
//		collectionTagsItem.setFont(Fonts.createBoldFont(collectionTagsItem.getFont()));
		
		tagDefsWidget = new TagSpecsWidget(mainTf, 0, true);	
		userTagsItem.setControl(tagDefsWidget);
		
		/*
		 * TODO: add these two lines once we support defining tags for collections
		 * Use case: only a few tag definitions should be tagged in the webUI
		 * At the moment this would be confusing
		 */
//		tagDefsWidgetForCollection = new TagSpecsWidgetForCollection(mainTf, 0);
//		collectionTagsItem.setControl(tagDefsWidgetForCollection);

		// listener:
		SWTUtil.onSelectionEvent(mainTf, e -> {
			updateTabItemStyles();
		});
		
		mainTf.setSelection(userTagsItem);
		
		updateTabItemStyles();
		
		horizontalSf.setWeights(new int[] { 35, 65 });
		//availableTagsSf.setWeights(new int[] { 70, 30 });
		
		updateAvailableTags();
		
		Observer tagsChangedObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if (arg instanceof TagRegistryChangeEvent) {
					logger.debug("TagRegistryChangeEvent: "+arg);
					updateAvailableTags();

				}				
			}
		};
		CustomTagFactory.addObserver(tagsChangedObserver);
		
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				CustomTagFactory.deleteObserver(tagsChangedObserver);
			}
		});
	}
	
	private Composite initTagsTable(Composite parent) {
		Composite tagsTableContainer = new Composite(parent, SWT.NONE);
		tagsTableContainer.setLayout(new GridLayout(1, false));
		
		Label headerLbl = new Label(tagsTableContainer, 0);
		headerLbl.setText("Available Tags");
		Fonts.setBoldFont(headerLbl);
		headerLbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		tagsTableContainer.setLayout(new GridLayout(1, false));
		tagsTableContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		
		Composite btnsContainer = new Composite(tagsTableContainer, 0);
		btnsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnsContainer.setLayout(new GridLayout(4, false));

		Button createTagBtn = new Button(btnsContainer, SWT.PUSH);
		createTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		createTagBtn.setText("Create new tag...");
		createTagBtn.setImage(Images.ADD);
		createTagBtn.setToolTipText("Adds a new tag to the list of registered tags\nThose tags can then be added to the tag definitions for usage in the user interface");
		
		createTagBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				CreateTagNameDialog d = new CreateTagNameDialog(getShell(), "Specify new tag name", true);				
				if (d.open() == Window.OK) {
					String name = d.getName();
					boolean isEmptyTag = d.isEmptyTag();
					try {
						CustomTagFactory.addToRegistry(CustomTagFactory.create(name), null, isEmptyTag, false, isCollectionTab(), true);
						if (isCollectionTab()) {
							//if 'collection specific tab' -> store in DB
							Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
						}
						//save local user settings
						else {
							saveTagDefs();
						}

					} catch (Exception e1) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error creating tag", e1.getMessage(), e1);
					}
				}
			}
		});
		
		Button deleteTagDefBtn = new Button(btnsContainer, SWT.PUSH);
		deleteTagDefBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		deleteTagDefBtn.setText("Delete tag definition");
		deleteTagDefBtn.setImage(Images.DELETE);
		deleteTagDefBtn.setToolTipText("Deletes the selected tag from the list of available tags");
		deleteTagDefBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				String tn = getSelectedAvailableTagsName();
				if (tn != null) {
					try {
						logger.debug("deleting tag: "+tn);
						logger.debug("is Collection tag " + isCollectionTab());
						CustomTagFactory.removeFromRegistry(tn, isCollectionTab());
						updateAvailableTags();
						if (isCollectionTab()) {
							//if 'collection specific tab' -> store in DB
							Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
						}
						//save local user settings
						else {
							saveTagDefs();
						}
					} catch (IOException ex) {
						DialogUtil.showErrorMessageBox(getShell(), "Cannot remove tag", ex.getMessage());
					}
				}
			}
		});
		
		takeOverTagBtn = new Button(btnsContainer, SWT.PUSH);
		takeOverTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		takeOverTagBtn.setText(isCollectionTab()? "Take over to user tags..." : "Take over to collection tags...");
		takeOverTagBtn.setImage(Images.ADD);
		takeOverTagBtn.setToolTipText(isCollectionTab()? "Adds checked tag(s) to the list of user tags.\nThose tags can be used in every collection." : "Adds checked tag(s) to the list of collection tags.\nThose tags can then be used in Transkribus Lite.");
		takeOverTagBtn.setEnabled(false);
		
		takeOverTagBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < availableTagsTv.getCheckedElements().length; i++) {
					String item = (String) availableTagsTv.getCheckedElements()[i];
					logger.debug("checked table item " + item);

					CustomTag ct = CustomTagFactory.getCustomTag(item, isCollectionTab());
					try {
						CustomTagFactory.addToRegistry(ct, CustomTagFactory.getTagColor(item), CustomTagFactory.isEmptyTag(item), true, !isCollectionTab(), true);
					} catch (NoSuchMethodException | SecurityException | IllegalAccessException
							| InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
				}
				if (availableTagsTv.getCheckedElements().length > 0) {
					if (!isCollectionTab()) {
						//if NOT 'collection specific tab' (tag is taken from user defined to collection defined) -> store in DB
						Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
					}
					//save local user settings
					else {
						saveTagDefs();
					}
				}
				availableTagsTv.setAllChecked(false);

			}
		});
		
//		Button saveBtn = new Button(btnsContainer, SWT.PUSH);
//		saveBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//		saveBtn.setImage(Images.DISK);
//		saveBtn.setToolTipText("Save the tag definitions to the local config.properties file s.t. they are recovered next time around");
//		saveBtn.addSelectionListener(new SelectionAdapter() {
//			@Override public void widgetSelected(SelectionEvent e) {
//				String tagNamesProp = CustomTagFactory.constructTagDefPropertyForConfigFile();
//				logger.debug("storing tagNamesProp: "+tagNamesProp);
//				TrpConfig.getTrpSettings().setTagNames(tagNamesProp);
//			}
//		});
		
		Table table = new Table(tagsTableContainer, SWT.CHECK | SWT.NO_FOCUS | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);

		availableTagsTv = new CheckboxTableViewer(table);
		availableTagsTv.getTable().setToolTipText("List of tags - italic tags are predefined and cannot be removed");
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 150;
		availableTagsTv.getTable().setLayoutData(gd);
		availableTagsTv.setContentProvider(new ArrayContentProvider());
		availableTagsTv.getTable().setHeaderVisible(false);
		availableTagsTv.getTable().setLinesVisible(true);
		
		availableTagsTv.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override public void selectionChanged(SelectionChangedEvent event) {
				logger.debug("selection changed: chekced??");
				deleteTagDefBtn.setEnabled(getSelectedAvailableTagsName() != null);
				takeOverTagBtn.setEnabled(availableTagsTv.getCheckedElements().length>0);
				updatePropertiesForSelectedTag();
			}
		});
		
		//add checkbox to select tags for the user interface
		checkboxCol = new TableViewerColumn(availableTagsTv, SWT.CHECK);
		checkboxCol.getColumn().setText("Select for WebUI");
		checkboxCol.getColumn().setResizable(true);
		checkboxCol.getColumn().setWidth(30);
		
		checkboxCol.setLabelProvider(new ColumnLabelProvider(){
			@Override public String getText(Object element) {
				return "";
			}
        });
		
		TableViewerColumn nameCol = new TableViewerColumn(availableTagsTv, SWT.NONE);
		nameCol.getColumn().setText("Name");
		nameCol.getColumn().setResizable(true);
		nameCol.getColumn().setWidth(150);
		ColumnLabelProvider nameColLP = new ColumnLabelProvider() {
			@Override public String getText(Object element) {
				return (String) element;
			}
			@Override public Font getFont(Object element) {
				CustomTag t = CustomTagFactory.getTagObjectFromRegistry((String)element, userTagsItem.isShowing());
				if (t != null && !t.isDeleteable()) {
					return Fonts.createItalicFont(availableTagsTv.getTable().getFont());
				}
				
				return null;
			}
		};
		nameCol.setLabelProvider(nameColLP);
		
		if (true) {
			TableViewerColumn colorCol = new TableViewerColumn(availableTagsTv, SWT.NONE);
			colorCol.getColumn().setText("Color");
			colorCol.getColumn().setResizable(true);
			colorCol.getColumn().setWidth(50);
			colorCol.setLabelProvider(new CellLabelProvider() {
				@Override public void update(ViewerCell cell) {
					TableItem item = (TableItem) cell.getItem();
					String tagName = (String) cell.getElement();
					
					TableEditor editor = new TableEditor(item.getParent());				
	                editor.grabHorizontal  = true;
	                editor.grabVertical = true;
	                editor.horizontalAlignment = SWT.LEFT;
	                editor.verticalAlignment = SWT.TOP;
	                
	                Color tagColor = getTagColor(tagName, isCollectionTab());
	                
	                ColorChooseButton colorCtrl = new ColorChooseButton((Composite) cell.getViewerRow().getControl(), tagColor.getRGB()) {
	                	@Override protected void onColorChanged(RGB rgb) {
	                		if (CustomTagFactory.setTagColor(tagName, Colors.toHex(rgb), isCollectionTab())) {
	                			availableTagsTv.refresh();	
	                			
	                			/*
	                			 * with this call we save the changed color to the config.settings so that the user will see these changes also after the 
	                			 * next login - without this call the colors are always the default ones 
	                			 */
	    						if (isCollectionTab()) {
	    							//if 'collection specific tab' -> store in DB
	    							Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
	    						}
	    						//save local user settings
	    						else {
	    							saveTagDefs();
	    						}
	                		}
	                	}
	                };
	                colorCtrl.setEditorEnabled(true);

	                editor.setEditor(colorCtrl , item, cell.getColumnIndex());
	                editor.layout();
	                
	                TaggingWidgetUtils.replaceEditor(colorEditors, tagName, editor);
				}
			});
		}
		
		TableViewerColumn descCol = new TableViewerColumn(availableTagsTv, SWT.NONE);
		descCol.getColumn().setText("");
		descCol.getColumn().setResizable(true);
		descCol.getColumn().setWidth(100);
		ColumnLabelProvider isEmptyTagColLP = new ColumnLabelProvider() {
			@Override public String getText(Object element) {
//				return (String) element;
				boolean isEmpty = CustomTagFactory.isEmptyTag((String) element);
				return isEmpty ? "Empty tag" : "";
			}
		};
		descCol.setLabelProvider(isEmptyTagColLP);	

		if (false) { // add btns for table rows
		TableViewerColumn addButtonCol = new TableViewerColumn(availableTagsTv, SWT.NONE);
		addButtonCol.getColumn().setText("");
		addButtonCol.getColumn().setResizable(false);
		addButtonCol.getColumn().setWidth(100);
		
		class AddTagToListSelectionAdapter extends SelectionAdapter {
			String tagName;
			public AddTagToListSelectionAdapter(String tagName) {
				this.tagName = tagName;
			}
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!StringUtils.isEmpty(tagName)) {
					try {
						logger.info("selected tag: "+getSelectedAvailableTagsName()+", tagName: "+tagName+", curr-atts: "+getCurrentAttributes());
						CustomTag tag = null;
						if (tagName.equals(getSelectedAvailableTagsName())) {
							tag = CustomTagFactory.create(tagName, getCurrentAttributes());
						}
						else {
//							CustomTag t = CustomTagFactory.create(tagName, 0, sel.getUnicodeText().length(), attributes);	
							tag = CustomTagFactory.create(tagName);
						}
						logger.info("tag = "+tag);
						
						CustomTagSpec tagDef = new CustomTagSpec(tag);
						Storage.getInstance().addCustomTagSpec(tagDef);
					} catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding tag definiton", ex.getMessage(), ex);
					}
				}
			}
		};
		
		CellLabelProvider addButtonColLabelProvider = new CellLabelProvider() {
			@Override public void update(final ViewerCell cell) {
				String tagName = (String) cell.getElement();				
				final TableItem item = (TableItem) cell.getItem();
				TableEditor editor = new TableEditor(item.getParent());
				
//				boolean createDelBtn = false;
//				TagAddRemoveComposite c = new TagAddRemoveComposite((Composite) cell.getViewerRow().getControl(), SWT.NONE, true, createDelBtn);
//				if (c.getAddButton() != null)
//					c.getAddButton().addSelectionListener(addTagToListSelectionListener);
//				c.pack();
				
				Button addButton = new Button((Composite) cell.getViewerRow().getControl(), SWT.PUSH);
//		        addButton.setImage(Images.ADD);
		        addButton.setImage(Images.getOrLoad("/icons/add_12.png"));
		        addButton.setToolTipText("Add tag (including properties below) to list of tags for collection");
		        addButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
		        addButton.addSelectionListener(new AddTagToListSelectionAdapter(tagName));
		        Control c = addButton;
		        
                Point size = c.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				editor.minimumWidth = size.x;
				editor.horizontalAlignment = SWT.LEFT;
                editor.setEditor(c , item, cell.getColumnIndex());
                editor.layout();
                
                TaggingWidgetUtils.replaceEditor(addTagToListEditors, tagName, editor);
			}
		};
		addButtonCol.setLabelProvider(addButtonColLabelProvider);
		}
		
		availableTagsTv.refresh(true);
		availableTagsTv.getTable().pack();

		tagsTableContainer.layout(true);
		return tagsTableContainer;
	}
	
	public static Color getTagColor(String tagName, int defaultColor) {
		String tagColorStr = CustomTagFactory.getTagColor(tagName);
		Color c = Colors.decode2(tagColorStr);

		if (c == null) {
			c = Colors.getSystemColor(defaultColor); // default tag color
		}
		
		return c;
	}
	
	public static Color getTagColor(String tagName, int defaultColor, boolean collectionColor) {
		String tagColorStr;
		if (collectionColor) {
			tagColorStr = CustomTagFactory.getCollectionTagColor(tagName);
		}
		else {
			tagColorStr = CustomTagFactory.getTagColor(tagName);
		}
		
		Color c = Colors.decode2(tagColorStr);

		if (c == null) {
			c = Colors.getSystemColor(defaultColor); // default tag color
		}
		
		return c;
	}
	
	public static String getTagColorString(String tagName, boolean collectionColor) {
		String tagColorStr;
		if (collectionColor) {
			tagColorStr = CustomTagFactory.getCollectionTagColor(tagName);
		}
		else {
			tagColorStr = CustomTagFactory.getTagColor(tagName);
		}
		
		return tagColorStr;
	}
	
	public static Color getTagColor(String tagName) {
		return getTagColor(tagName, SWT.COLOR_GRAY);
	}
	
	
	public static Color getTagColor(String tagName, boolean collectionColor) {
		return getTagColor(tagName, SWT.COLOR_GRAY, collectionColor);
	}
	
	private void initPropertyTable(Composite parent) {
		Composite propsContainer = new Composite(parent, ExpandableComposite.COMPACT);
		propsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		propsContainer.setLayout(new GridLayout(2, false));
		
		Label headerLbl = new Label(propsContainer, 0);
		headerLbl.setText("Properties");
		Fonts.setBoldFont(headerLbl);
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Button addPropertyBtn = new Button(propsContainer, SWT.PUSH);
		addPropertyBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		addPropertyBtn.setText("Add property...");
		addPropertyBtn.setImage(Images.ADD);
		addPropertyBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				final String tn = getSelectedAvailableTagsName();
				if (tn == null)
					return;
				
				CreateTagNameDialog d = new CreateTagNameDialog(getShell(), "Specify property for '"+tn+"' tag", false);				
				if (d.open() == Window.OK) {
					try {
						String name = d.getName();
						CustomTagAttribute att = new CustomTagAttribute(name);
						
						CustomTag t = CustomTagFactory.getTagObjectFromRegistry(tn, isCollectionTab());
						logger.debug("tag object: "+t);
						
						if (t.hasAttribute(att.getName())) {
							DialogUtil.showErrorMessageBox(getShell(), "Cannot add property", "Property already exists!");
							return;
						}
						
						if (t != null) {
							t.setAttribute(att.getName(), null, true);
							boolean isEmptyTag = CustomTagFactory.isEmptyTag(t.getTagName());
							CustomTagFactory.addToRegistry(t, CustomTagFactory.getTagColor(t.getTagName()), isEmptyTag, true);
						}

						updatePropertiesForSelectedTag();
						
						if (isCollectionTab()) {
							//if 'collection specific tab' -> store in DB
							Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
						}
						//save local user settings
						else {
							saveTagDefs();
						}
					}
					catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding property", ex.getMessage(), ex);
					}
				}
			}
		});
		
		Button deletePropertyButton = new Button(propsContainer, SWT.PUSH);
		deletePropertyButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		deletePropertyButton.setText("Delete selected property");
		deletePropertyButton.setImage(Images.DELETE);
		deletePropertyButton.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				String tn = getSelectedAvailableTagsName();
				if (tn == null) {
					return;
				}
				
				CustomTagAttribute selectedProperty = getSelectedProperty();
				logger.debug("delete selected property: "+selectedProperty);
				
				if (!StringUtils.isEmpty(tn) && selectedProperty != null) {
					try {
						CustomTag t = CustomTagFactory.getTagObjectFromRegistry(tn, !userTagsItem.isShowing());
						//logger.debug("tag found: "+t.getTagName());
						if (t != null) {
							t.deleteCustomAttribute(selectedProperty.getName());
							boolean isEmptyTag = CustomTagFactory.isEmptyTag(t.getTagName());
							//add to registry to update the attributes there as well
							CustomTagFactory.addToRegistry(t, CustomTagFactory.getTagColor(t.getTagName()), isEmptyTag, true);
						}
						
						updatePropertiesForSelectedTag();
						if (isCollectionTab()) {
							//if 'collection specific tab' -> store in DB
							Storage.getInstance().updateCustomTagSpecsForCurrentCollectionInDB();
						}
						//save local user settings
						else {
							saveTagDefs();
						}
					} catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding property to tag "+tn, ex.getMessage(), ex);
					}
				}
			}
		});
		
		propsTable = new CustomTagPropertyTableNew(propsContainer, 0, false);
		propsTable.getTableViewer().getTable().setHeaderVisible(false);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2);
//		gd.heightHint = 200;
		propsTable.setLayoutData(gd);
		
//		initPropertyTable();

		layout();
	}

	public String getSelectedAvailableTagsName() {
		if (availableTagsTv.getSelection().isEmpty())
			return null;
		else
			return (String) ((IStructuredSelection) availableTagsTv.getSelection()).getFirstElement();	
	}
	
	public CustomTagAttribute getSelectedProperty() {
		return propsTable.getSelectedProperty();
	}
	
	public boolean isAvailableTagSelected() {
		return getSelectedAvailableTagsName() != null;
	}
	
	private void updateAvailableTags() {
		if (SWTUtil.isDisposed(this) || SWTUtil.isDisposed(availableTagsSf)) {
			return;
		}
		
		logger.debug("updating available tags");
		
		availableTagNames.clear();
		
		List<CustomTag> ctList = isCollectionTab()? CustomTagFactory.getRegisteredCollectionTags() : CustomTagFactory.getRegisteredCustomTags();
		
		for (CustomTag t : ctList) {
			logger.trace("update of av. tags, tn = "+t.getTagName()+" showInTagWidget: "+t.showInTagWidget());
			if (t.showInTagWidget()) {
				availableTagNames.add(t.getTagName());
			}
		}
		
		Display.getDefault().asyncExec(new Runnable() {
		@Override public void run() {
//			updateAvailableTags();
			if (!availableTagsTv.getTable().isDisposed()){
				availableTagsTv.setInput(availableTagNames);
				availableTagsTv.refresh(true);
			}
			
			TaggingWidgetUtils.updateEditors(colorEditors, availableTagNames);
			TaggingWidgetUtils.updateEditors(addTagToListEditors, availableTagNames);
			}
		});
	}
	
	private void updatePropertiesForSelectedTag() {
		String tn = getSelectedAvailableTagsName();
		if (tn == null) {
			propsTable.setInput(null);
			propsTable.update();
			return;
		}
			
		try {
			logger.debug("collection tags item selected? " + isCollectionTab());
			CustomTag tag = CustomTagFactory.getTagObjectFromRegistry(tn, isCollectionTab());
			if (tag == null)
				throw new Exception("could not retrieve tag from registry: "+tn+" - should not happen here!");
			
			logger.debug("tag from object registry: "+tag);
			logger.debug("tag atts: "+tag.getAttributeNames());
			
			CustomTag protoTag = tag.copy();
			logger.debug("protoTag copy: "+protoTag);
			logger.debug("protoTag atts: "+protoTag.getAttributeNames());
			
			propsTable.setInput(protoTag, isCollectionTab());
			propsTable.selectFirstAttribute();
			
			// set color label:
//			String colorStr = CustomTagFactory.getTagColor(tag.getTagName());
//			if (colorStr != null) {
//				colorChooseBtn.setRGB(Colors.toRGB(colorStr));
//			} else {
//				colorChooseBtn.setRGB(ColorChooseButton.DEFAULT_COLOR);
//			}			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}
	
	public Map<String, Object> getCurrentAttributes() {
		Map<String, Object> props = new HashMap<>();
		CustomTag st = propsTable.getSelectedTag();
		if (st != null) {
			return st.getAttributeNamesValuesMap();
		}
	
		return props;
	}
	
	private void saveTagDefs() {
		Storage.getInstance().saveTagDefinitions();
	}
	
	private void updateTabItemStyles() {
		SWTUtil.setBoldFontForSelectedCTabItem(mainTf);
		SWTUtil.setBoldFontForSelectedCTabItem(mainTfLeft);
	}
	
	private CTabFolder createTabFolder(Composite parent) {
		CTabFolder tf = new CTabFolder(parent, SWT.BORDER | SWT.FLAT);
		tf.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		tf.setBorderVisible(true);
		tf.setSelectionBackground(Colors.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		tabfolder.add(tf);

		return tf;
	}
	
	private CTabItem createCTabItem(CTabFolder tabFolder, Control control, String Text, List<CTabItem> list) {
		CTabItem ti = new CTabItem(tabFolder, SWT.NONE);
		ti.setText(Text);
		ti.setControl(control);

		if (list != null)
			list.add(ti);

		return ti;
	}
	
	void setDefaultSelection() {
		SWTUtil.setSelection(mainTf, userTagsItem);
		SWTUtil.setSelection(mainTfLeft, userTagsItemLeft);
		SWTUtil.setSelection(userTagsTf, userTagsItem);
		SWTUtil.setSelection(collectionTagsTf, collectionTagsItem);
	}
	
	void addTabFolderSelectionListener() {
		SelectionListener sl = new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if (!(e.item instanceof CTabItem))
					return;

				CTabItem item = (CTabItem) e.item;

				//updateSelectedOnTabFolder(item.getParent());
			}
		};

		for (CTabFolder tf : tabfolder) {
			tf.addSelectionListener(sl);
		}

		//updateTabItemStyles();
	}
	
	boolean isCollectionTab() {
		return (mainTfLeft == null || mainTfLeft.getSelection() == null)? false : mainTfLeft.getSelection().equals(collectionTagsItem);
	}

}
