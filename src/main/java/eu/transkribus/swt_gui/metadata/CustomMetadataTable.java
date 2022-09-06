package eu.transkribus.swt_gui.metadata;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.JAXBPageTranscript;
import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpPage;
import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.pagecontent.PcGtsType;
import eu.transkribus.core.model.beans.pagecontent.PropertyType;
import eu.transkribus.core.model.beans.pagecontent.TranskribusMetadataType;
import eu.transkribus.core.util.MonitorUtil;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt.util.TableViewerUtils;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.dialogs.CopyPageMdConfDialog;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CustomMetadataTable extends Composite {
	private static final Logger logger = LoggerFactory.getLogger(CustomMetadataTable.class);

	HashMap<String, HashMap<String, String>> customMetadata = new HashMap<String, HashMap<String, String>>();
	String usedMdSet = null;
	
	// show pages with no transcribed lines in gray
	public static final Color GRAY = Colors.getSystemColor(SWT.COLOR_GRAY);
	static final Color BLACK = Colors.getSystemColor(SWT.COLOR_BLACK);

	TableViewer availableTagsTv;
	SashForm availableTagsSf;
	TableViewer propsTable;
	SashForm horizontalSf;
	
	Combo shapeTypeCombo;

	private Button createTagBtn, deleteTagDefBtn, copyMdOtherPages;

	private Button addPropertyBtn, deletePropertyButton, deleteAllValuesButton, copyProperty2OtherPages;

	public CustomMetadataTable(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new FillLayout());
		horizontalSf = new SashForm(this, SWT.HORIZONTAL);

		Composite leftWidget = new Composite(horizontalSf, 0);
		leftWidget.setLayout(new FillLayout());

		availableTagsSf = new SashForm(leftWidget, SWT.VERTICAL);
		
		//availableTagsSf.setLayoutData(new GridData(GridData.FILL));
		initTagsTable(availableTagsSf);
		initPropertyTable(availableTagsSf);
		
		availableTagsSf.setWeights(new int[] {1, 4});

		updateMetadataNamesInDropDown();
		selectFirstMdSetInDropDown();

		Storage.getInstance().addListener(new IStorageListener() {
			public void handleTranscriptLoadEvent(TranscriptLoadEvent arg) {
				logger.debug("transcript load event in customMetadataTable");
				logger.debug("page index------- " + Storage.getInstance().getPageIndex());
				if (Storage.getInstance().hasTranscript() && Storage.getInstance().getTranscript().getPageData().getMetadata() != null) {
					onTranscriptLoad(Storage.getInstance().getTranscript().getPageData().getMetadata().getTranskribusMetadata());
				}
			}

//			public boolean handleBeforeTranscriptSaveEvent(BeforeTranscriptSaveEvent tse) {
//				logger.debug("transcript before save event in customMetadataTable");
//				updatePropertiesForSelectedTag();
//				TranskribusMetadataType value = onTranscriptSave(
//						Storage.getInstance().getTranscript().getPageData().getMetadata().getTranskribusMetadata());
//				if (value != null) {
//					logger.debug("value != null" + value);
//					Storage.getInstance().getTranscript().getPageData().getMetadata().setTranskribusMetadata(value);
//					return true;
//				}
//				return false;
//			}
		});

		availableTagsSf.pack(true);
		availableTagsSf.redraw();
		// updatePropertiesForSelectedTag();

	}

	private Composite initTagsTable(Composite parent) {
		Composite tagsTableContainer = new Composite(parent, 0);
//		tagsTableContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		tagsTableContainer.setLayout(new GridLayout(1, false));

		Label headerLbl = new Label(tagsTableContainer, 0);
		headerLbl.setText("Metadata");
		Fonts.setBoldFont(headerLbl);
		headerLbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		tagsTableContainer.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gd.heightHint = 100;
		tagsTableContainer.setLayoutData(gd);

		Composite btnsContainer = new Composite(tagsTableContainer, 0);
		btnsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnsContainer.setLayout(new GridLayout(3, false));
		//btnsContainer.setSize(parent.getBounds().width, 100);

		createTagBtn = new Button(btnsContainer, SWT.PUSH);
		createTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		createTagBtn.setText("Create new md set...");
		createTagBtn.setImage(Images.ADD);
		createTagBtn.setToolTipText("Creates a new metadata set for which the user can add key/value pairs");
		createTagBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreateTagNameDialog d = new CreateTagNameDialog(getShell(), "Specify new md set name", false);
				if (d.open() == Window.OK) {
					String name = d.getName();
					
					addNewMdSet(name);
				}
			}
		});

		deleteTagDefBtn = new Button(btnsContainer, SWT.PUSH);
		deleteTagDefBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		deleteTagDefBtn.setText("Delete md set");
		deleteTagDefBtn.setImage(Images.DELETE);
		deleteTagDefBtn.setToolTipText("Deletes the selected md set with all key/value pairs");
		deleteTagDefBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tn = getSelectedAvailableTagsName();
				if (tn != null) {
					logger.debug("deleting tag: " + tn);
					customMetadata.remove(tn);
					shapeTypeCombo.remove(tn);
					printComboValues();
					//updateMetadataNamesInDropDown();
					selectFirstMdSetInDropDown();
					saveCustomMetadataInConfig();
					//tagsTableContainer.layout(true);
				}
			}
		});

		copyMdOtherPages = new Button(btnsContainer, SWT.PUSH);
		copyMdOtherPages.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		copyMdOtherPages.setText("Copy page md to other pages");
		copyMdOtherPages.setImage(Images.PAGE_COPY);
		copyMdOtherPages.setToolTipText("Copy the metadata values to some other pages");
		copyMdOtherPages.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// toDo implement this method
				String tn = getUsedMdSet();
				if (tn == null || !arePropValuesSet()) {
					logger.debug("nothing to copy: no metadata inserted");
					DialogUtil.createAndShowBalloonToolTip(tagsTableContainer.getShell(), 0, "Test", "nothing to copy: no metadata inserted", tagsTableContainer.getLocation().x, tagsTableContainer.getLocation().y, true);
					return;
				}
				else {
					//dialog mit Seitenauswahl - siehe Canvas tools
					logger.debug("copy the metadata values to the other selected pages ");
					
					boolean updateSingleProperty = false;
					boolean deleteSingleProperty = false;
					updatePageMd(updateSingleProperty, deleteSingleProperty, null);
				}
			}
		});
		
		//initTable(tagsTableContainer);
		initDropDown(btnsContainer);
		tagsTableContainer.layout(true);
		return tagsTableContainer;
	}
	
	protected void addNewMdSet(String name) {
		logger.debug("new md set name is: " + name);
		if (name == null) {
			//default md set
			name = "USER_DEFINED_PAGE_MD";
		}
		saveCustomMetadataInConfig();
		
		logger.debug("indes of name: " + shapeTypeCombo.indexOf(name));
		if (shapeTypeCombo.indexOf(name) == -1) {
			shapeTypeCombo.add(name);
			customMetadata.put(name, null);
		}
		setUsedMdSet(name);
		
//		printCustomMetadaKeys();
		logger.debug("debug index of: " + shapeTypeCombo.indexOf(name));
		shapeTypeCombo.select(shapeTypeCombo.indexOf(name));
		updatePropertiesForSelectedTag();
		
	}

	private void printCustomMetadaKeys() {
		for (String key : customMetadata.keySet()) {
			logger.debug("key: " + key);
			logger.debug("debug: " + shapeTypeCombo.indexOf(key));
		}
		
	}
	
	
	private void printComboValues() {
		for (String key : shapeTypeCombo.getItems()) {
			logger.debug("key: " + key);
			logger.debug("debug: " + shapeTypeCombo.indexOf(key));
		}
		
	}

	private void initDropDown(Composite container) {
		
		Label shapeTypeLabel = new Label(container, 0);
		shapeTypeLabel.setText("Selected metadata set: ");
		Fonts.setBoldFont(shapeTypeLabel);
		shapeTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1);
		gd.heightHint = 100;

		shapeTypeCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		shapeTypeCombo.setLayoutData(gd);
		shapeTypeCombo.setItems(customMetadata.keySet().toArray(new String[0]));
		
		
		logger.debug("length of combo entries: " + shapeTypeCombo.getItems().length);
		if (!(shapeTypeCombo.getItems().length > 0)) {
			addNewMdSet(getUsedMdSet());
		}

		shapeTypeCombo.addSelectionListener(new SelectionListener() {
//			@Override
//			public void selectionChanged(SelectionChangedEvent event) {
//
//				//logger.debug("selection changed: chekced??" + usedMdSet);
//				if (usedMdSet.contentEquals("") || (getSelectedAvailableTagsName() != null
//						&& usedMdSet.contentEquals(getSelectedAvailableTagsName()))) {
//					// if (usedMdSet.contentEquals("")) {
//					updateVisibilityOfButtons();
//			
//					updatePropertiesForSelectedTag();
//				}
//
//			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				//logger.debug("selection changed: chekced??" + usedMdSet);
//				if (usedMdSet.contentEquals("") || (getSelectedAvailableTagsName() != null
//						&& usedMdSet.contentEquals(getSelectedAvailableTagsName()))) {
					// if (usedMdSet.contentEquals("")) {
				logger.debug("selection changed: new md set " + shapeTypeCombo.getItem(shapeTypeCombo.getSelectionIndex()));
				setUsedMdSet(shapeTypeCombo.getItem(shapeTypeCombo.getSelectionIndex()));
				updateVisibilityOfButtons();
				updatePropertiesForSelectedTag();
//				}
				
			}
		});
		
		shapeTypeCombo.layout();

//		TableViewerColumn nameCol = new TableViewerColumn(availableTagsTv, SWT.NONE);
//		nameCol.getColumn().setText("Name");
//		nameCol.getColumn().setResizable(true);
//		nameCol.getColumn().setWidth(400);
//		ColumnLabelProvider nameColLP = new ColumnLabelProvider() {
//			@Override
//			public String getText(Object element) {
//				return (String) element;
//			}
//
//			@Override
//			public Color getForeground(Object element) {
////				logger.debug(" text in label provider: " + (String) element);
////				logger.debug(" = usedMdSet: " + usedMdSet);
//				// if (usedMdSet.contentEquals("") && !((String) element == usedMdSet)) {
//				if (getUsedMdSet().contentEquals("") || ((String) element).contentEquals(usedMdSet)) {
//					//logger.debug(" = usedMdSet: " + usedMdSet);
//					return BLACK;
//				}
//				return GRAY;
//
//			}
//		};
//		nameCol.setLabelProvider(nameColLP);

//		availableTagsTv.refresh(true);
//		availableTagsTv.getTable().pack();
		
	}


	private void initPropertyTable(Composite parent) {
		// Composite propsContainer = new Composite(parent,
		// ExpandableComposite.COMPACT);
		Composite propsContainer = new Composite(parent, 0);
		propsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		propsContainer.setLayout(new GridLayout(1, false));

		propsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Label headerLbl = new Label(propsContainer, 0);
		headerLbl.setText("Properties: Name/Value Pairs");
		Fonts.setBoldFont(headerLbl);
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Composite btnsContainer = new Composite(propsContainer, 0);
		btnsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnsContainer.setLayout(new GridLayout(4, false));

		addPropertyBtn = new Button(btnsContainer, SWT.PUSH);
		addPropertyBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		addPropertyBtn.setText("Add property name...");
		addPropertyBtn.setImage(Images.ADD);
		addPropertyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tn = getSelectedAvailableTagsName();

				logger.debug("debug md selected: " + getSelectedAvailableTagsName());
				if (tn == null)
					return;

				if (customMetadata.get(tn) == null) {
					customMetadata.put(tn, new HashMap<String, String>());
				}

				CreateTagNameDialog d = new CreateTagNameDialog(getShell(), "Specify key for '" + tn + "' md set",
						false);
				if (d.open() == Window.OK) {
					try {
						String name = d.getName();

						if (customMetadata.get(tn) != null && customMetadata.get(tn).containsKey(name)) {
							DialogUtil.showErrorMessageBox(getShell(), "Cannot add property",
									"Property already exists!");
							return;
						}

						if (name != null) {
							customMetadata.get(tn).put(name, null);
							saveCustomMetadataInConfig();
						}

						updatePropertiesForSelectedTag();

					} catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error adding property", ex.getMessage(),
								ex);
					}
				}
			}
		});

		deletePropertyButton = new Button(btnsContainer, SWT.PUSH);
		deletePropertyButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		deletePropertyButton.setText("Delete selected property");
		deletePropertyButton.setImage(Images.DELETE);
		deletePropertyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				String selMdSet = getSelectedAvailableTagsName();
				String propName = getSelectedProperty();
				
				if (propName == null || !customMetadata.get(selMdSet).containsKey(propName)) {
					logger.debug("nothing to delete: no value set");
					DialogUtil.createAndShowBalloonToolTip(propsContainer.getShell(), 0, "Nothing to do", "nothing to delete: no property selected", propsContainer.getLocation().x, propsContainer.getLocation().y, true);
					return;
				}
				else {
					//dialog mit Seitenauswahl - siehe Canvas tools
					logger.debug("delete property from pages: "+propName);
					
					if (updatePageMd(false, true, propName)) {
						customMetadata.get(selMdSet).remove(propName);
						try {
							Storage.getInstance().setLatestTranscriptAsCurrent();
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						saveCustomMetadataInConfig();
						updatePropertiesForSelectedTag();
					}
					
				}

			}
		});
		
		copyProperty2OtherPages = new Button(btnsContainer, SWT.PUSH);
		copyProperty2OtherPages.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		copyProperty2OtherPages.setText("Copy selected value...");
		copyProperty2OtherPages.setImage(Images.PAGE_COPY);
		copyProperty2OtherPages.setToolTipText("Copy this property value to other pages");
		copyProperty2OtherPages.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// toDo implement this method
				String selMdSet = getSelectedAvailableTagsName();
				String propName = getSelectedProperty();
				
				if (propName == null) {
					DialogUtil.createAndShowBalloonToolTip(propsContainer.getShell(), 0, "Nothing to do", "nothing to copy: no property selected", propsContainer.getLocation().x, propsContainer.getLocation().y, true);
					return;
				}
				
				if (customMetadata.get(selMdSet).get(propName) == null || customMetadata.get(selMdSet).get(propName) == "") {
					logger.debug("nothing to copy: no value set");
					DialogUtil.createAndShowBalloonToolTip(propsContainer.getShell(), 0, "Nothing to do", "nothing to copy: no value inserted", propsContainer.getLocation().x, propsContainer.getLocation().y, true);
					return;
				}
				else {
					//dialog mit Seitenauswahl - siehe Canvas tools
					logger.debug("copy the metadata values to the other selected pages ");
					updatePageMd(true, false, propName);
				}
			}
		});

		deleteAllValuesButton = new Button(btnsContainer, SWT.PUSH);
		deleteAllValuesButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		deleteAllValuesButton.setText("Delete all md values");
		deleteAllValuesButton.setImage(Images.DELETE);
		deleteAllValuesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ( getSelectedAvailableTagsName()!=null) {
					setTranscriptEdited();
				}
				clearEntries(true);
				//setUsedMdSet("");
				//updateVisibilityOfMdSets();
			}
		});

		Table table = new Table(propsContainer, SWT.NO_FOCUS | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		propsTable = new TableViewer(table);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 4, 4);

		gd.heightHint = 1200;
		propsTable.getTable().setLayoutData(gd);
		propsTable.setContentProvider(new ArrayContentProvider());
		propsTable.getTable().setHeaderVisible(false);
		propsTable.getTable().setLinesVisible(true);

		TableViewerColumn nameCol;
		TableViewerColumn valueCol;
		nameCol = TableViewerUtils.createTableViewerColumn(propsTable, SWT.LEFT, "Property", 150);
		valueCol = TableViewerUtils.createTableViewerColumn(propsTable, SWT.LEFT, "Value", 250);

		ColumnLabelProvider nameColLP = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return (String) element;
			}

		};

		// LABEL PROVIDERS:
		nameCol.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String name = "";
				String tn = getSelectedAvailableTagsName();
				if (tn != null) {
					name = (String) cell.getElement();
				}
				cell.setText(name);
			}
		});

		valueCol.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String tn = getSelectedAvailableTagsName();
				if (tn == null) {
					return;
				}

				cell.setBackground(Colors.getSystemColor(SWT.COLOR_WHITE));
				cell.setText("");

			}
		});
		propsTable.setContentProvider(ArrayContentProvider.getInstance());

		// initTraverseStuff();

		propsTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				logger.debug("selection changed: chekced??");
				deletePropertyButton.setEnabled(getSelectedAvailableTagsName() != null);
				// updatePropertiesForSelectedTag();
			}
		});

		propsTable.refresh(true);
		propsTable.getTable().pack();

		propsContainer.layout(true);
	}
	
	private boolean areValuesSet() {
		// check if values are set in this md set
		if (customMetadata.get(usedMdSet) == null) {
			return false;
		}
		
		Collection<String> values = customMetadata.get(usedMdSet).values();
		boolean valueFound = false;
		for (String v : values) {
			if (v != null && v != "") {
				logger.debug("value " + v);
				valueFound = true;
			}
		}
		
		return valueFound;
	}

	/*
	 * clear property values in the properties
	 * -> if all property values should be deleted
	 */
	private void clearEntries(boolean withUpdate) {
		String selMdSet = getSelectedAvailableTagsName();
		
		logger.debug("is it null if deleting? " + selMdSet);

		if (selMdSet == null) {
			updateMetadataNamesInDropDown();
			return;
		}

		try {

			for (String k : customMetadata.get(selMdSet).keySet()) {
				customMetadata.get(selMdSet).put(k, null);
			}
			
			if (withUpdate) {
				//setUsedMdSet("");
				//updateVisibilityOfMdSets();
				updateVisibilityOfButtons();
				saveCustomMetadataInConfig();
				updatePropertiesForSelectedTag();
			}

		} catch (Exception ex) {
			DialogUtil.showDetailedErrorMessageBox(getShell(), "Error deleting metadata values ", ex.getMessage(), ex);
		}

	}
	
	private void clearProperties(String selMdSet) {
		logger.debug("clear property values");
		if (selMdSet != null && customMetadata.get(selMdSet) != null) {
			for (String k : customMetadata.get(selMdSet).keySet()) {
				customMetadata.get(selMdSet).put(k, null);
			}			
		}
	}

	public String getSelectedAvailableTagsName() {
		if (shapeTypeCombo.getSelectionIndex() >= 0) {
			return shapeTypeCombo.getItem(shapeTypeCombo.getSelectionIndex());
		}
		else {
			logger.error("idx < 0; no md set selected in combo box - should not happen");
		}
//		if (availableTagsTv == null || availableTagsTv.getSelection().isEmpty())
//			return null;
//		else
//			return (String) ((IStructuredSelection) availableTagsTv.getSelection()).getFirstElement();
		return null;
	}

	public String getSelectedProperty() {
		if (propsTable.getSelection().isEmpty())
			return null;
		else
			return (String) ((IStructuredSelection) propsTable.getSelection()).getFirstElement();
	}

	public boolean isAvailableTagSelected() {
		return getSelectedAvailableTagsName() != null;
	}

	/*
	 * customMetadata is defined and stored as json: {mdSet1 : {key1:value1,
	 * key2:value2, key3:value3,..}, mdSet2 : {key1:value1, key2:value2,
	 * key3:value3,..}
	 * 
	 */
	private void updateMetadataNamesInTable() {
		if (SWTUtil.isDisposed(this) || SWTUtil.isDisposed(availableTagsSf)) {
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
//			updateAvailableTags();
				if (availableTagsTv != null && !availableTagsTv.getTable().isDisposed()) {
					logger.debug("update the available tags tv");
					availableTagsTv.setInput(customMetadata.keySet());
					//availableTagsTv.getTable().pack();
					availableTagsTv.refresh(true);
				}

			}
		});
	}
	
	private void updateMetadataNamesInDropDown() {
		if (SWTUtil.isDisposed(this) || SWTUtil.isDisposed(availableTagsSf)) {
			return;
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
//			updateAvailableTags();
				if (shapeTypeCombo != null && !shapeTypeCombo.isDisposed()) {
					logger.debug("update the available metadata sets");
					shapeTypeCombo.removeAll();
					String[] stringArray = Arrays.copyOf(customMetadata.keySet().toArray(), customMetadata.keySet().toArray().length, String[].class);
					shapeTypeCombo.setItems(stringArray);
//					if (!customMetadata.keySet().isEmpty()) {
//						shapeTypeCombo.select(0);
//					}
					shapeTypeCombo.update();
					shapeTypeCombo.redraw();
				}

			}
		});
	}

	private void updatePropertiesForSelectedTag() {
		
		String tn = getUsedMdSet();
		logger.debug("selected metadata set: " + tn);
//		if (getUsedMdSet() != null) {
//			setInput(getUsedMdSet());
//			return;
//		}
		
		if (tn == null || propsTable == null) {
			logger.debug("no update of properties: " + propsTable);
			// setInput();
			// propsTable.update();
			return;
		}
		
		try {
			if (!propsTable.getTable().isDisposed()) {
				logger.debug("propsTable set input: ");
				setInput();
				// propsTable.refresh(true);
			}

//			Display.getDefault().asyncExec(new Runnable() {
//				@Override
//				public void run() {
////					updateAvailableTags();
//
//
//				}
//			});

			// ToDo_ try to select first property
//			propsTable.setSelection(selection, reveal);.getSelection()).getFirstElement();
//			propsTable.selectFirstAttribute();

			selectAttribute(0);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
	}

	public HashMap<String, String> getCurrentAttributes() {
		HashMap<String, String> props = new HashMap<>();
		final String tn = getSelectedAvailableTagsName();
		if (tn != null) {
			return customMetadata.get(tn);
		}

		return props;
	}

	private void saveCustomMetadataInConfig() {

		JSONObject json = writeCustomMetadataToJsonObject();

		TrpConfig.getTrpSettings().setCustomMetadata(json.toString());
	}

	public void setInput() {
		try {
			String tn = getSelectedAvailableTagsName();
			logger.debug("available tags name " + tn);
			if (tn == null) {
				logger.debug("selectFirstMdSetInDropDown");
				selectFirstMdSetInDropDown();
				tn = getSelectedAvailableTagsName();
//				propsTable.setInput(null);
//				return;
			}

			if (customMetadata.get(tn) != null) {
				logger.debug("propsTable.setInput " + customMetadata.get(tn).keySet());
				propsTable.setInput(customMetadata.get(tn).keySet());
				createEditors();
				propsTable.refresh(); // needed?
			}
			else {
				logger.debug("propsTable.setInput " + null);
				propsTable.setInput(null);
			}
		} catch (Exception e) {
			logger.debug(e.getLocalizedMessage());
			TrpMainWidget.getInstance().onError("Unable to set input for text tag property editor!", e.getMessage(), e);
		}
	}

	public void setInput(String mdSet) {
		try {
			if (mdSet == null) {
				propsTable.setInput(null);
				// clearEditors();
				return;
			}

			if (customMetadata.get(mdSet) != null) {
				propsTable.setInput(customMetadata.get(mdSet).keySet());
				createEditors();
				propsTable.refresh(); // needed?
			}
		} catch (Exception e) {
			TrpMainWidget.getInstance().onError("Unable to set input for text tag property editor!", e.getMessage(), e);
		}
	}

	public void selectAttribute(int index) {
		int N = propsTable.getTable().getItemCount();
		String tn = getSelectedProperty();
		if (tn != null && index >= 0 && index < N) {
			propsTable.editElement(propsTable.getElementAt(index), 1);
		}
	}

//	public void selectFirstMdSet() {
//		if (availableTagsTv == null) {
//			return;
//		}
//		
//		int N = availableTagsTv.getTable().getItemCount();
//		
//		if (N>0) {
//			logger.debug("select item:" + availableTagsTv.getTable().getItem(0));
//			availableTagsTv.setSelection(new StructuredSelection(availableTagsTv.getElementAt(0)),true);
//		}
//
//	}
	
	public void selectFirstMdSetInDropDown() {
		
		int N = shapeTypeCombo.getItemCount();
		
		if (N>0) {
			logger.debug("select item:" + shapeTypeCombo.getItem(0));
			shapeTypeCombo.select(0);
			setUsedMdSet(shapeTypeCombo.getItem(0));
			shapeTypeCombo.update();			
			updatePropertiesForSelectedTag();
			updateVisibilityOfButtons();
			shapeTypeCombo.redraw();
		}

	}	

	public void selectFirstAttribute() {
		selectAttribute(0);
	}

	List<TableEditor> editors = new ArrayList<>();
//	DelayedTask setValueDelayedTask = new DelayedTask(task, isGuiTask);

	private void clearEditors() {
		for (TableEditor e : editors) {
			TaggingWidgetUtils.deleteEditor(e);
		}
		editors.clear();
	}

	private void createEditors() {
		clearEditors();

		for (TableItem item : propsTable.getTable().getItems()) {
			if (!item.isDisposed()) {
				Object element = item.getData();

				TableEditor editor = new TableEditor(propsTable.getTable());
				Control ctrl = createEditor(element);
				editor.grabHorizontal = true;
//			    editor.minimumWidth = 100;
				editor.grabVertical = true;
				editor.setEditor(ctrl, item, 1);

				editors.add(editor);
			}

		}

	}

	private Control createEditor(Object element) {
		Control ctrl;

		String tn = getSelectedAvailableTagsName();

		if (tn == null) {
			return null;
		}

		String value = customMetadata.get(tn).get(element);

//		logger.debug("creating a textfield!");
//		logger.debug("vaalue selected: " + value);
		Text text = new Text(propsTable.getTable(), SWT.NONE);
		text.setText(value == null ? "" : String.valueOf(value));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				logger.debug("text modified: " + text.getText() + " in " + (String) element);
				customMetadata.get(tn).put((String) element, text.getText());

				//setUsedMdSet(noValuesSet ? "" : tn);

				if (arePropValuesSet()) {
					logger.debug("prop values set");
					setTranscriptEdited();
				}

				//updateVisibilityOfMdSets();
				updateVisibilityOfButtons();

			}

		});
		ctrl = text;

		// TODO: add validators depending on type! (int, float ...)

		return ctrl;
	}
	
	private boolean arePropValuesSet() {
		boolean valuesSet = false;
		String tn = getSelectedAvailableTagsName();
		if (tn == null || customMetadata.get(tn) == null) {
			return false;
		}
		for (String propKey : customMetadata.get(tn).keySet()) {
			//logger.debug("prop: " + customMetadata.get(tn).get(propKey));
			if (customMetadata.get(tn).get(propKey) != null && customMetadata.get(tn).get(propKey) != "") {
				valuesSet = true;
			}
		}
		return valuesSet;
	}
	
	private void setTranscriptEdited() {
		Storage.getInstance().setCurrentTranscriptEdited(true);
	}

	protected void updateVisibilityOfButtons() {
		createTagBtn.setEnabled(shapeTypeCombo != null && !shapeTypeCombo.isDisposed());
		deleteTagDefBtn.setEnabled(getSelectedAvailableTagsName() != null);
		copyMdOtherPages.setEnabled(arePropValuesSet());

		//addPropertyBtn.setEnabled(availableTagsTv != null && !availableTagsTv.getSelection().isEmpty());
		addPropertyBtn.setEnabled(getSelectedAvailableTagsName() != null);
		deletePropertyButton.setEnabled(!propsTable.getSelection().isEmpty());
		deleteAllValuesButton.setEnabled(arePropValuesSet());

	}

	public JSONObject writeCustomMetadataToJsonObject() {
		logger.debug("start json object builder ");
		JSONObject mainObject = new JSONObject();

		for (String key : customMetadata.keySet()) {
			JSONArray arrb = new JSONArray();
			if (customMetadata.get(key) != null) {
				for (String propKey : customMetadata.get(key).keySet()) {
					arrb.put(propKey);
				}
			}

			// JsonArray tmp = arrb.build();
			//logger.debug("json: array " + arrb);
			mainObject.put(key, arrb);
			// mainObject..addAll(builder);
		}

		logger.debug("json: " + mainObject);

		return mainObject;

	}

	public void readCustomMetadataFromJsonObject() {
		try {
			HashMap<String, HashMap<String, String>> custom = new HashMap<String, HashMap<String, String>>();
			String customMetadata = TrpConfig.getTrpSettings().getCustomMetadata();
			
			if (!StringUtils.isEmpty(customMetadata)) {
				logger.info("customMetadata = "+customMetadata);
				JSONObject jsonObject = new JSONObject(customMetadata);
				Iterator<String> keys = jsonObject.keys();
				while (keys.hasNext()) {
					HashMap<String, String> attributes = new HashMap<>();
					String key = keys.next();
					if (jsonObject.get(key) instanceof JSONObject) {
						// do something with jsonObject here
					} else if (jsonObject.get(key) instanceof JSONArray) {
						JSONArray array = (JSONArray) jsonObject.get(key);
		
						for (int i = 0; i < array.length(); i++) {
							attributes.put(array.getString(i), null);
						}
		
						logger.debug("array to string " + array.toString());
					}
					custom.put(key, attributes);
		
				}
		
				setPageMetadata(custom);
				logger.debug("custom md_" + customMetadata);
			}
		} catch (Exception e) {
			logger.error("Error reading custom metadata: "+e.getMessage(), e);
		}
	}

	// call on page load -> ToDo implement event handler
	private void onTranscriptLoad(TranskribusMetadataType md) {
		
//		if (!(md == null || md.getProperty() == null))
//			logger.debug("md at load time" + md.getProperty());
		//clearEntries(false);
		
		if (md == null || md.getProperty() == null || md.getProperty().isEmpty()) {
			logger.debug("no properties in TranskribusMetadata true of false?");
			//setUsedMdSet("");
			if (getUsedMdSet()==null) {
				selectFirstMdSetInDropDown();
			}
			clearProperties(getUsedMdSet());
			updatePropertiesForSelectedTag();
			return;
		}
		
		updatePropertiesForSelectedTag();
		clearProperties(getUsedMdSet());
		String usedMdSet = null;
		HashMap<String, String> properties = new HashMap<String, String>();
		
		/*
		 * handle case if no mdSet is available in the loaded md properties -> use a default mdSet
		 */
		for (PropertyType prop : md.getProperty()) {
			prop.getKey().equalsIgnoreCase("mdSetName");
			
		}
		
		Stream<PropertyType> stream = md.getProperty().stream().filter(prop -> prop.getKey().equalsIgnoreCase("mdSetName"));
		if (stream.count()==0) {
			logger.debug("use default md set -> no mdSetName set was available in the TranskribusMetadata properties");
			usedMdSet = "defaultMdSet";
		}

		
		for (PropertyType prop : md.getProperty()) {
		
			if (prop.getKey().equalsIgnoreCase("mdSetName")) {
				if (customMetadata.containsKey(prop.getValue())) {
					properties = customMetadata.get(prop.getValue());
					if (properties == null || properties.isEmpty()) {
						properties = new HashMap<String, String>();
						customMetadata.put(usedMdSet, properties);	
					}
				} else {
					customMetadata.put(prop.getValue(), properties);
				}
				//logger.debug("used md set foundL: " + prop.getValue());
				usedMdSet = prop.getValue();
			} else {
//				logger.debug("uprop.getKey(): " + prop.getKey());
//				logger.debug("prop.getValue(): " + prop.getValue());
//				String value2Insert = prop.getValue();
//				if (value2Insert == null) {
//					value2Insert = "";
//				}
				if (properties == null || properties.isEmpty()) {
					properties = new HashMap<String, String>();
					customMetadata.put(usedMdSet, properties);	
				}
				properties.put(prop.getKey(), prop.getValue());

				if (prop.getValue() != null && usedMdSet != null) {
					logger.debug("used md set - loaded from PAGE XML: " + usedMdSet);
				}
			}
		}	
		if (usedMdSet != null) {

			setUsedMdSet(usedMdSet);
			String[] items = shapeTypeCombo.getItems();
			if (shapeTypeCombo.indexOf(usedMdSet) == -1) {
				logger.debug("add the usedMdSet to combo");
				shapeTypeCombo.add(usedMdSet);
			}
			shapeTypeCombo.select(shapeTypeCombo.indexOf(usedMdSet));
			
			selectMdSetInDropdown(usedMdSet);
			updatePropertiesForSelectedTag();
			saveCustomMetadataInConfig();
			//selectMdSet(usedMdSet);
		}
	}


	// call beforchange -> ToDo implement event handler
	private TranskribusMetadataType onTranscriptSave(TranskribusMetadataType md, int pageIdx) {
		
		HashMap<String, String> props = customMetadata.get(getUsedMdSet());

		Storage storage = Storage.getInstance();

		if (md == null) {
			md = new TranskribusMetadataType();
			md.setDocId(storage.getDocId());
			md.setPageId(storage.getPage().getPageId());
			md.setPageNr(pageIdx+1);
			logger.debug("is null?? " + md.getProperty().size());
		}
		
		List<PropertyType> propList = md.getProperty();		
		propList.clear();

		//logger.debug("props = null ?? " + props);
		//no props or all props without values
		if(props==null || props.isEmpty() || !areValuesSet()) {
			if (md.getProperty() != null) {
				logger.debug("clear the page related md");
				md.getProperty().clear();
			}
			logger.debug("props not set");
			return md;
		}
		
		logger.debug("store the page related md for  " + getUsedMdSet());

		// first property is the name of the md set
		PropertyType pt = new PropertyType();
		pt.setKey("mdSetName");
		pt.setValue(getUsedMdSet());

		propList.add(pt);

		// this action stores each key/value pair into a PropertyType and stores to the
		// TranskribusMetadata
		BiConsumer<String, String> action = new MyBiConsumer(propList);
		props.forEach(action);

		return md;

		// ToDo: main widget, Storage update the TranskribusMetadataType

	}
	
	// call beforchange -> ToDo implement event handler
	private TranskribusMetadataType onTranscriptUpdate(TranskribusMetadataType md, String propName) {
		
		HashMap<String, String> props = customMetadata.get(getUsedMdSet());
		
		List<PropertyType> propList = md.getProperty();
		Iterator<PropertyType> propIterator = propList.iterator();
		
		boolean hasChanged = false;

		/*
		 * get the property with the wanted prop name and set the copied value
		 */
		while (propIterator.hasNext()) {
			PropertyType prop = propIterator.next();
			if (prop.getKey().contentEquals(propName)) {
				prop.setValue(props.get(prop.getKey()));
				hasChanged = true;
			}
		}
		
		if (hasChanged) {
			return md;
		}
		
		//this property don't exist so far -> create new one
		PropertyType newProp = new PropertyType();
		newProp.setKey(propName);
		newProp.setValue(props.get(propName));
		
		propList.add(newProp);
		md.getProperty().addAll(propList);
		
		return md;

	}
	
	// call beforchange -> ToDo implement event handler
	private TranskribusMetadataType onPropertyDelete(TranskribusMetadataType md, String propName) {
		
		HashMap<String, String> props = customMetadata.get(getUsedMdSet());
		
		List<PropertyType> propList = md.getProperty();
		List<PropertyType> propRemove = new ArrayList<PropertyType>();
		Iterator<PropertyType> propIterator = propList.iterator();

		/*
		 * get the property with the wanted prop name and set the copied value
		 */
		boolean hasChanged = false;
		while (propIterator.hasNext()) {
			PropertyType prop = propIterator.next();
			if (prop.getKey().contentEquals(propName)) {
				propRemove.add(prop);
				//propList.remove(prop);
				hasChanged = true;
			}
		}
		
		if (hasChanged) {
			propList.removeAll(propRemove);
			return md;
		}
		
		return null;

	}

//	private boolean isMdSetUsed() {
//		// TODO Auto-generated method stub
//		return (!usedMdSet.contentEquals(""));
//	}

	// Defining Our Action in MyBiConsumer class
	class MyBiConsumer implements BiConsumer<String, String> {

		List<PropertyType> list;

		public MyBiConsumer(List<PropertyType> propList) {
			this.list = propList;
		}

		@Override
		public void accept(String k, String v) {
			PropertyType pt = new PropertyType();
			pt.setKey(k);
			pt.setValue(v);
			
			logger.debug("created property key: " + k);

			list.add(pt);

		}
	}

//	private void updateVisibilityOfMdSets() {
//		if (availableTagsTv != null)
//			availableTagsTv.refresh(true);
//	}

	public HashMap<String, HashMap<String, String>> getPageMetadata() {
		return customMetadata;
	}

	public void setPageMetadata(HashMap<String, HashMap<String, String>> pageMetadata) {
		if (pageMetadata==null) {
			pageMetadata = new HashMap<String, HashMap<String,String>>();
		}
		this.customMetadata = pageMetadata;
	}

	
	public void selectMdSetInDropdown(String mdSet) {
		int idx = 0;

		for (String item : shapeTypeCombo.getItems()) {
			//logger.debug("text 1 of table item to select: " + item.getText());
			if (item.contentEquals(mdSet)){
				//logger.debug("text 2 of table item to select: " + item.getText());
				shapeTypeCombo.select(idx);
				shapeTypeCombo.update();
				setUsedMdSet(shapeTypeCombo.getItem(idx));
				updatePropertiesForSelectedTag();
				updateVisibilityOfButtons();
				shapeTypeCombo.redraw();
				return;
			}
			idx++;
			
		}
	}

//	public boolean isMdAvailable() {
//		return (!usedMdSet.contentEquals(""));
//	}

	public String getUsedMdSet() {
		return usedMdSet;
		//return shapeTypeCombo.getItem(shapeTypeCombo.getSelectionIndex());
	}

//	public void setUsedMdSet(String usedMdSet) {
//		logger.debug("setUsedMdSet " + usedMdSet);
//		this.usedMdSet = usedMdSet;
//		updateVisibilityOfMdSets();
//		
//	}
	
	
	public boolean updatePageMd(boolean update, boolean delete, String propName) {
		try {
			logger.debug("copy/delete MdToOtherPagesInLoadedDoc!");

			if (!Storage.getInstance().isDocLoaded()) {
				DialogUtil.showErrorMessageBox(getShell(), "Error", "No document loaded!");
				return false;
			}
			CopyPageMdConfDialog d = new CopyPageMdConfDialog(getShell(), delete);
			if (d.open() != IDialogConstants.OK_ID) {
				return false;
			}
			
			Set<Integer> pageIndices = d.getPageIndices();

			class Result {
				public int nPagesTotal=0;
				public int nPagesChanged=0;
				public String msg;
				public List<Integer> affectedPageIndices=new ArrayList<>();
			}
			final Result res = new Result();

			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						logger.debug(delete?"deleteMdOnOtherPagesInLoadedDoc":"copyMdToOtherPagesInLoadedDoc");
						TrpDoc doc = Storage.getInstance().getDoc();
						int worked=0;
						int N = pageIndices==null ? doc.getNPages() : pageIndices.size();
						res.nPagesTotal = N;
						
						MonitorUtil.beginTask(monitor, delete ? "Delete property on nr. of pages = ": "Copy page metadata to nr. of pages = ", N);
						
						int idx = Storage.getInstance().getPageIndex();
						
						for (int i=0; i<doc.getNPages(); ++i) {

							if (pageIndices!=null && !pageIndices.contains(i) || (i==idx&&!delete)) {
								continue;
							}
							
							if (MonitorUtil.isCanceled(monitor)) {
								return;
							}
							MonitorUtil.subTask(monitor, "Processing page "+(worked+1)+" / "+N);
							
							TrpPage currP = doc.getPages().get(i);
							TrpTranscriptMetadata currMd = currP.getCurrentTranscript();
							
							JAXBPageTranscript currTr = new JAXBPageTranscript(currMd);
							currTr.build();

							PcGtsType pc = currP.unmarshallCurrentTranscript();
							
							if (pc == null) {
								logger.debug("pc is null");
								continue;
							}
							TranskribusMetadataType value = null;
							
							//update single property value for selected pages
							if (delete) {
								value = onPropertyDelete(pc.getMetadata().getTranskribusMetadata(), propName);
							}
							else if (update) {
								if (pc.getMetadata().getTranskribusMetadata() == null) {
									DialogUtil.showErrorMessageBox(getShell(), "No page metadata set on this page", "Update of property not possible because there is no page related metadata stored for page " + i+1);
								}
								value = onTranscriptUpdate(pc.getMetadata().getTranskribusMetadata(), propName);
							}
							else {
								value = onTranscriptSave(pc.getMetadata().getTranskribusMetadata(), i);
							}
							
							if (value != null) {
								logger.debug("value != null" + value);
								pc.getMetadata().setTranskribusMetadata(value);
								currTr.setPageData(pc);
								Storage.getInstance().saveTranscript(Storage.getInstance().getCurrentDocumentCollectionId(), currTr, "updated page md");	
							}
							else {
								logger.debug("value == null");
							}

							++res.nPagesChanged;
								
							MonitorUtil.worked(monitor, ++worked);
						}
						
						res.msg = "Page metadata updated on "+ res.nPagesChanged+" page(s)." ;
						logger.info(res.msg);
					} catch (Exception e) {
						throw new InvocationTargetException(e, e.getMessage());
					}
				}
			}, "Editing page metadata", true);
			
			DialogUtil.showInfoMessageBox(getShell(), "Editing page metadata", res.msg);
			
		} catch (Throwable e) {
			logger.error("Error", e.getMessage(), e);
		}
		return true;		
	}
	
	public void setUsedMdSet(String usedMdSet) {
		this.usedMdSet = usedMdSet;
	}
	
	public boolean handleMdSave() {
		logger.debug("page related md changed");
		//next line gives 'Invalid thread access' during save: maybe not needed
		//updatePropertiesForSelectedTag();
		
		if ( Storage.getInstance().getTranscript().getPageData().getMetadata() != null) {

			/*
			 * if Transkribus Metadata is null -> will be created
			 */
			TranskribusMetadataType value = onTranscriptSave(
					Storage.getInstance().getTranscript().getPageData().getMetadata().getTranskribusMetadata(), Storage.getInstance().getPageIndex());
			if (value != null) {
				logger.debug("value != null" + value.getProperty().size());
				Storage.getInstance().getTranscript().getPageData().getMetadata().setTranskribusMetadata(value);
				return true;
			}
		}

		return false;
	}

}
