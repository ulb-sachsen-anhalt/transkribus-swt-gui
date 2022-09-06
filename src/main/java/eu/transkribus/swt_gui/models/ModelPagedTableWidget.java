package eu.transkribus.swt_gui.models;

import java.util.Arrays;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.htr.treeviewer.ModelPageLoader;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.util.DelayedTask;

public class ModelPagedTableWidget extends ATableWidgetPagination<TrpModelMetadata> {
	private static final Logger logger = LoggerFactory.getLogger(ModelPagedTableWidget.class);
	
	public final static String[] providerValues = ModelUtil.ALL_PROVIDER;
	
	public static final String MODEL_NAME_COL = "Name";
	public static final String MODEL_LANG_COL = "Language";
	public static final String MODEL_CREATOR_COL = "Curator";
	public static final String MODEL_TECH_COL = "Technology";
	public static final String MODEL_DATE_COL = "Created";
	public static final String MODEL_ID_COL = "ID";
	public static final String MODEL_WORDS_COL = "nrOfWords";
	public static final String MODEL_CER_TRAIN_COL = "CER Train";
	public static final String MODEL_CER_VAL_COL = "CER Validation ";
	public static final String MODEL_RATING_COL = "Rating";
	
	// filter:
	ModelFilterWithProviderWidget filterComposite;
	private final String providerFilter;
	private final String typeFilter;
	private ModelPageLoader pl;
	
	public ModelPagedTableWidget(Composite parent, int style, String typeFilter, String providerFilter) {
		super(parent, style, 25, null, true);
		
		if(providerFilter != null && !Arrays.stream(providerValues).anyMatch(s -> s.equals(providerFilter))) {
			throw new IllegalArgumentException("Invalid providerFilter value");
		}
		
		this.typeFilter = typeFilter;
		this.providerFilter = providerFilter;
		this.setLayout(new GridLayout(1, false));
		
		addFilter();
		setPageLoader();
	}
	
	@Override
	public void addListener(int eventType, Listener listener) {
		super.addListener(eventType, listener);
		filterComposite.addListener(eventType, listener);
	}
	
	private void addFilter() {
		filterComposite = new ModelFilterWithProviderWidget(this, getTableViewer(), typeFilter, providerFilter, SWT.NONE) {
			@Override
			protected void refreshViewer() {
				logger.debug("refreshing viewer...");
				refreshPage(true);
			}
			@Override
			protected void attachFilter() {
			}
		};
		filterComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterComposite.getFilterText().setParent(SWTUtil.dummyShell);
		filterComposite.layout();
		filterComposite.moveAbove(null);
		
		// NOTE: ModifyListener may not be a good idea, since setting new values in the combo programmatically also triggers this listener and could cause infinite loop -> using SelectionListener instead
//		filterComposite.getProviderCombo().addModifyListener(new ModifyListener() {
//			@Override
//			public void modifyText(ModifyEvent arg0) {
//				refreshPage(true);
//			}
//		});
		
		SelectionAdapter refreshPageSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refreshPage(true);
			}
		};
		filterComposite.getProviderCombo().addSelectionListener(refreshPageSelectionAdapter);
		filterComposite.getLangFilterCombo().addSelectionListener(refreshPageSelectionAdapter);
		
		ModifyListener filterModifyListener = new ModifyListener() {
			DelayedTask dt = new DelayedTask(() -> { 
				refreshPage(true);
			}, true);
			@Override public void modifyText(ModifyEvent e) {
				dt.start();
			}
		};
		filter.addModifyListener(filterModifyListener);
		filter.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_RETURN) {
					refreshPage(true);
				}
			}
		});
	}
	
	void resetProviderFilter() {
		filterComposite.resetProviderFilter();
	}
	
	public String getProviderComboValue() {
		Combo providerCombo = filterComposite.getProviderCombo();
		return (String) providerCombo.getData(providerCombo.getText());
	}
	
	public TrpModelMetadata getSelectedModel() {
		return getFirstSelected();
	}

	public void refreshList() {
		logger.debug("refreshList");
		refreshPage(true);
	}

	public void setSelection(int modelId) {
		// TODO
	}

	@Override
	protected void setPageLoader() {
		pl = new ModelPageLoader(pageableTable.getController(), filter, filterComposite);
		pageableTable.setPageLoader(pl);
	}

	@Override
	protected void createColumns() {
		ModelViewerLabelProvider lp = new ModelViewerLabelProvider(tv);
		//createDefaultColumn(MODEL_NAME_COL, 210, "name", true);
		createColumn(MODEL_NAME_COL, 210, "name", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setImage(lp.getColumnImage((TrpModelMetadata)cell.getElement(), cell.getColumnIndex()));
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_NAME_COL));	
				}
			}
		});
		
		createColumn(MODEL_LANG_COL, 100, "language", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					TrpModelMetadata m = (TrpModelMetadata) cell.getElement();
					String txt = "";
					if (ModelUtil.hasTypeLanguage(m.getType())) {
						txt = lp.getColumnText(m, MODEL_LANG_COL);	
					}
					cell.setText(txt);
				}
			}
		});
		
		createColumn(MODEL_CREATOR_COL, 120, "userName", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_CREATOR_COL));	
				}
			}
		});
		createColumn(MODEL_TECH_COL, 80, "provider", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_TECH_COL));	
				}
			}
		});
		createColumn(MODEL_DATE_COL, 60, "created", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_DATE_COL));
				}
			}
		});
		createDefaultColumn(MODEL_WORDS_COL, 80, "nrOfWords", true);

		//DefaultColumn using BeanUtils will set some Double values in scientific notation. Use formatting as in ModelDetailsWidget
		createColumn(MODEL_CER_TRAIN_COL, 80, "finalCer", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_CER_TRAIN_COL));
				}
			}
		});
		createColumn(MODEL_CER_VAL_COL, 80, "finalValidationCer", new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell) {
				if (cell.getElement() instanceof TrpModelMetadata) {
					cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_CER_VAL_COL));
				}
			}
		});
		if (Storage.getInstance()!=null && Storage.getInstance().isAdminLoggedIn()) {
			createColumn(MODEL_RATING_COL, 80, "internalRating", new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					if (cell.getElement() instanceof TrpModelMetadata) {
						cell.setText(lp.getColumnText((TrpModelMetadata)cell.getElement(), MODEL_RATING_COL));
					}
				}
			});
		}
		createDefaultColumn(MODEL_ID_COL, 50, "modelId", true);
	}	
}