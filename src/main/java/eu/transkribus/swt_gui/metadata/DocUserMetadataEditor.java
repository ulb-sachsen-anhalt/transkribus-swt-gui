package eu.transkribus.swt_gui.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpDoc;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.TrpEntityAttribute;
import eu.transkribus.core.model.beans.pagecontent.PropertyType;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Fonts;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.TableViewerUtils;
import eu.transkribus.swt_gui.TrpConfig;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class DocUserMetadataEditor extends Composite implements IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(DocUserMetadataEditor.class);

	HashMap<String, HashMap<String, String>> customMetadata = new HashMap<String, HashMap<String, String>>();
	final String usedMdSet = "USER_DEFINED_DOC_MD";

	TableViewer propsTable;
	//SashForm horizontalSf;

	Combo shapeTypeCombo;

	private Button addPropertyBtn, deletePropertyButton, deleteAllValuesButton, saveButton;

	public DocUserMetadataEditor(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new GridLayout());
//		horizontalSf = new SashForm(this, SWT.HORIZONTAL);
//
//		Composite leftWidget = new Composite(this, 0);
//		leftWidget.setLayout(new FillLayout());

		initPropertyTable(this);
		initDocMetadata(null);
		attach();

	}
	
	void attach() {
		Storage.getInstance().addListener(this);
	}
	
	void detach() {
		Storage.getInstance().removeListener(this);
	}

	private void initPropertyTable(Composite parent) {
		// Composite propsContainer = new Composite(parent,
		// ExpandableComposite.COMPACT);
		Composite propsContainer = new Composite(parent, 0);
		propsContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		propsContainer.setLayout(new GridLayout(1, false));

		propsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		Label headerLbl = new Label(propsContainer, 0);
		headerLbl.setText("User defined metadata - add name/value pairs");
		Fonts.setBoldFont(headerLbl);
		headerLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Composite btnsContainer = new Composite(propsContainer, 0);
		btnsContainer.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		btnsContainer.setLayout(new GridLayout(4, false));

		addPropertyBtn = new Button(btnsContainer, SWT.PUSH);
		addPropertyBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		addPropertyBtn.setText("Add name...");
		addPropertyBtn.setImage(Images.ADD);
		addPropertyBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String tn = usedMdSet;

				logger.debug("debug md selected: " + usedMdSet);
				if (tn == null)
					return;

				if (customMetadata.get(tn) == null) {
					customMetadata.put(tn, new HashMap<String, String>());
				}

				CreateTagNameDialog d = new CreateTagNameDialog(getShell(), "Specify new property name",
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
							
							saveCustomDocMetadataInConfig();
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
		deletePropertyButton.setText("Delete selected row");
		deletePropertyButton.setImage(Images.DELETE);
		deletePropertyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				String selMdSet = usedMdSet;
				String tn = getSelectedProperty();

				logger.debug("selected prop to delete: " + tn);
				if (selMdSet == null || tn == null) {
					return;
				}

				if (!StringUtils.isEmpty(tn)) {
					try {

//						if (!(customMetadata.get(selMdSet).get(tn) == null) && !(customMetadata.get(selMdSet).get(tn) == "")) {
//							
//						}

						customMetadata.get(selMdSet).remove(tn);
						saveCustomDocMetadataInConfig();
						updatePropertiesForSelectedTag();

						// check if values are set in this md set
						Collection<String> values = customMetadata.get(selMdSet).values();
						boolean valueFound = false;
						for (String v : values) {
							if (v != null && v != "") {
								// logger.debug("value " + v);
								valueFound = true;
							}
						}
						if (!valueFound) {
							logger.debug("no values set - reset used md set");
							// setUsedMdSet("");
							// updateVisibilityOfMdSets();
						}

					} catch (Exception ex) {
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error deleting metadata entry " + tn,
								ex.getMessage(), ex);
					}
				}
			}
		});

//		saveButton = new Button(btnsContainer, SWT.PUSH);
//		saveButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
//		saveButton.setText("Save..");
//		saveButton.setImage(Images.PAGE_COPY);
//		saveButton.setToolTipText("Save user defined document metadata");
//		saveButton.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				saveUserMd();
//			}
//
//		});

		deleteAllValuesButton = new Button(btnsContainer, SWT.PUSH);
		deleteAllValuesButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 1, 1));
		deleteAllValuesButton.setText("Delete all md values");
		deleteAllValuesButton.setImage(Images.DELETE);
		deleteAllValuesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				clearEntries(true);
				// setUsedMdSet("");
				// updateVisibilityOfMdSets();
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
				String tn = usedMdSet;
				if (tn != null) {
					name = (String) cell.getElement();
				}
				cell.setText(name);
			}
		});

		valueCol.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				String tn = usedMdSet;

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
				deletePropertyButton.setEnabled(usedMdSet != null);
				// updatePropertiesForSelectedTag();
			}
		});

		propsTable.refresh(true);
		propsTable.getTable().pack();
		
		updateVisibilityOfButtons();

		propsContainer.layout(true);
	}
	
	private void saveCustomDocMetadataInConfig() {

		JSONObject json = writeCustomDocMetadataToJsonObject();

		TrpConfig.getTrpSettings().setCustomMetadata(json.toString());
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
	
	public void saveUserMd() {
		Storage store = Storage.getInstance();
		//if no values are set and 'Save' is pressed then all entries in DB are deleted
		if (!store.isLocalDoc()) {
			store.updateDocMdAttributes(getAttributesList());
		}
		else {
			TrpDocMetadata docMd = store.getDoc().getMd();
			docMd.setAttributes(getAttributesList());
			try {
				store.saveDocMd(store.getCollId());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		
	}

	/*
	 * clear property values in the properties -> if all property values should be
	 * deleted
	 */
	private void clearEntries(boolean withUpdate) {
		String selMdSet = usedMdSet;

		logger.debug("is it null if deleting? " + selMdSet);

		if (selMdSet == null) {
			// updateMetadataNamesInDropDown();
			return;
		}

		try {

			for (String k : customMetadata.get(selMdSet).keySet()) {
				customMetadata.get(selMdSet).put(k, null);
			}

			if (withUpdate) {
				// setUsedMdSet("");
				// updateVisibilityOfMdSets();
				updateVisibilityOfButtons();
				// saveCustomMetadataInConfig();
				updatePropertiesForSelectedTag();
			}

		} catch (Exception ex) {
			DialogUtil.showDetailedErrorMessageBox(getShell(), "Error deleting metadata values ", ex.getMessage(), ex);
		}

	}

	private void clearProperties(String selMdSet) {
		if (selMdSet != null && customMetadata.get(selMdSet) != null) {
			for (String k : customMetadata.get(selMdSet).keySet()) {
				customMetadata.get(selMdSet).put(k, null);
			}
		}
	}

	public String getSelectedProperty() {
		if (propsTable.getSelection().isEmpty())
			return null;
		else
			return (String) ((IStructuredSelection) propsTable.getSelection()).getFirstElement();
	}

	public boolean isAvailableTagSelected() {
		return usedMdSet != null;
	}

	private void updatePropertiesForSelectedTag() {

		String tn = getUsedMdSet();
		logger.debug("selected metadata set: " + tn);
//		if (getUsedMdSet() != null) {
//			setInput(getUsedMdSet());
//			return;
//		}

		if (tn == null) {
			// setInput();
			// propsTable.update();
			return;
		}

		try {

			if (!propsTable.getTable().isDisposed()) {
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
	
	
	public List<TrpEntityAttribute> getAttributesList() {
		List<TrpEntityAttribute> attributes = new ArrayList<TrpEntityAttribute>();

		for (String k : customMetadata.get(usedMdSet).keySet()) {
			String v = customMetadata.get(usedMdSet).get(k);
			
			if (v == null || v.isEmpty()){
				continue;
			}
			
			TrpEntityAttribute att = new TrpEntityAttribute();
			att.setName(k);
			att.setValue(v);
			att.setType(usedMdSet);
			att.setEntityId(Storage.getInstance().getDocId());
			
			attributes.add(att);
		}
		
		return attributes;
		
	}
	
	public void setEntities(List<TrpEntityAttribute> entities) {
		
		if(customMetadata.get(usedMdSet) == null) {
			customMetadata.put(getUsedMdSet(), new HashMap<String, String>());
		}
		
		if (customMetadata.get(usedMdSet) != null) {
			logger.debug("used MD set != null");
			//if entity 
			if (entities == null || entities.isEmpty()) {
				logger.debug("entities MD set == null");
				clearEntries(true);
				return;
			}
			else {
				for (TrpEntityAttribute entity : entities) {
					customMetadata.get(usedMdSet).put(entity.getName(), entity.getValue());
				}
				
			}
		}
		else {
			logger.debug("used MD set is= null");
		}
	
	}

	public void setInput() {
		try {
			String tn = usedMdSet;

			if (customMetadata.get(tn) != null) {
				propsTable.setInput(customMetadata.get(tn).keySet());
				createEditors();
				propsTable.refresh(); // needed?
			} else {
				propsTable.setInput(null);
			}
		} catch (Exception e) {
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

		String tn = usedMdSet;

		String value = customMetadata.get(tn).get(element);

//		logger.debug("creating a textfield!");
//		logger.debug("vaalue selected: " + value);
		Text text = new Text(propsTable.getTable(), SWT.NONE);
		text.setText(value == null ? "" : String.valueOf(value));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				logger.debug("text modified: " + text.getText() + " in " + (String) element);
				if (customMetadata.get(tn) != null)
					customMetadata.get(tn).put((String) element, text.getText());

				// setUsedMdSet(noValuesSet ? "" : tn);

				// updateVisibilityOfMdSets();
				updateVisibilityOfButtons();

			}

		});
		ctrl = text;

		// TODO: add validators depending on type! (int, float ...)

		return ctrl;
	}

	private boolean arePropValuesSet() {
		boolean valuesSet = false;
		String tn = usedMdSet;
		if (tn == null || customMetadata.get(tn) == null) {
			return false;
		}
		for (String propKey : customMetadata.get(tn).keySet()) {
			// logger.debug("prop: " + customMetadata.get(tn).get(propKey));
			if (customMetadata.get(tn).get(propKey) != null && customMetadata.get(tn).get(propKey) != "") {
				valuesSet = true;
			}
		}
		return valuesSet;
	}

	protected void updateVisibilityOfButtons() {

		// addPropertyBtn.setEnabled(availableTagsTv != null &&
		// !availableTagsTv.getSelection().isEmpty());
		addPropertyBtn.setEnabled(usedMdSet != null);
		deletePropertyButton.setEnabled(!propsTable.getSelection().isEmpty());
		deleteAllValuesButton.setEnabled(arePropValuesSet());

	}

	public JSONObject writeCustomDocMetadataToJsonObject() {
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
			// logger.debug("json: array " + arrb);
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
				logger.info("customMetadata = " + customMetadata);
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

				initDocMetadata(custom);
				logger.debug("custom md_" + customMetadata);
			}
		} catch (Exception e) {
			logger.error("Error reading custom metadata: " + e.getMessage(), e);
		}
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

	public HashMap<String, HashMap<String, String>> getPageMetadata() {
		return customMetadata;
	}

	public void initDocMetadata(HashMap<String, HashMap<String, String>> pageMetadata) {
		if (pageMetadata == null) {
			pageMetadata = new HashMap<String, HashMap<String, String>>();
		}
		this.customMetadata = pageMetadata;
		customMetadata.put(getUsedMdSet(), null);
	}

//	public boolean isMdAvailable() {
//		return (!usedMdSet.contentEquals(""));
//	}

	public String getUsedMdSet() {
		return usedMdSet;
		// return shapeTypeCombo.getItem(shapeTypeCombo.getSelectionIndex());
	}
	
	@Override public void handleDocLoadEvent(DocLoadEvent dle) {	
		
		TrpDoc doc = dle.doc;
		if (doc != null && doc.getMd() != null) {
			logger.debug("load doc md attributes during doc load of doc " + doc.getMd().getTitle());
		}
		if (doc != null && doc.getMd() != null && doc.getMd().getAttributes() != null) {
			logger.debug("size of attributes: " + doc.getMd().getAttributes().size());
			setEntities(doc.getMd().getAttributes());
			setInput();
		}
		else {
			logger.debug("read the user defined md keys from the settings");
			readCustomMetadataFromJsonObject();
		}
		
		updateVisibilityOfButtons();
		
	}

//	public boolean handleMdSave() {
//		logger.debug("page related md changed");
//		updatePropertiesForSelectedTag();
//		TranskribusMetadataType value = onTranscriptSave(
//				Storage.getInstance().getTranscript().getPageData().getMetadata().getTranskribusMetadata());
//		if (value != null) {
//			logger.debug("value != null" + value);
//			Storage.getInstance().getTranscript().getPageData().getMetadata().setTranskribusMetadata(value);
//			return true;
//		}
//		return false;
//	}

}
