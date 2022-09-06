package eu.transkribus.swt_gui.htr.treeviewer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.nebula.widgets.pagination.IPageLoader;
import org.eclipse.nebula.widgets.pagination.collections.PageResult;
import org.eclipse.nebula.widgets.pagination.tree.SortTreeColumnSelectionListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.GroundTruthSelectionDescriptor;
import eu.transkribus.core.model.beans.ReleaseLevel;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.core.model.beans.rest.TrpHtrList;
import eu.transkribus.core.model.beans.rest.TrpModelMetadataList;
import eu.transkribus.core.model.beans.searchresult.FacetRepresentation;
import eu.transkribus.core.model.beans.searchresult.FacetRepresentation.FacetValueRepresentation;
import eu.transkribus.core.util.DescriptorUtils.AGtDataSet;
import eu.transkribus.core.util.HtrCITlabUtils;
import eu.transkribus.core.util.HtrPyLaiaUtils;
import eu.transkribus.core.util.IsoLangUtils;
import eu.transkribus.core.util.ModelUtil;
import eu.transkribus.swt.pagination_table.ATreeWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.dialogs.ChooseCollectionDialog;
import eu.transkribus.swt_gui.htr.treeviewer.ModelGroundTruthContentProvider.ModelGtDataSet;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.mainwidget.storage.StorageUtil;
import eu.transkribus.swt_gui.models.ModelFilterWithProviderWidget;
import eu.transkribus.swt_gui.models.ShareModelDialog;
import eu.transkribus.swt_gui.models.ShareModelMetadataDialog;
import eu.transkribus.swt_gui.structure_tree.StructureTreeWidget.ColConfig;
import eu.transkribus.swt_gui.util.DelayedTask;

public class GroundTruthPagedTreeWidget extends ATreeWidgetPagination<TrpModelMetadata>  {
	private static final Logger logger = LoggerFactory.getLogger(GroundTruthPagedTreeWidget.class);
	
	public final static String[] providerValues = ModelUtil.ALL_PROVIDER;
	
	public final static ColConfig NAME_COL = new ColConfig("Name", 210, "name");
	public final static ColConfig SIZE_COL = new ColConfig("Size", 100, "size");
	public final static ColConfig CURATOR_COL = new ColConfig("Curator", 120, "userName");
	public final static ColConfig DATE_COL = new ColConfig("Date", 80, "created");
	public final static ColConfig ID_COL = new ColConfig("Model ID", 70, "modelId");
	public final static ColConfig WORD_COL = new ColConfig("nrOfWords", 80, "nrOfWords");

	public final static ColConfig[] COLUMNS = new ColConfig[] { NAME_COL, SIZE_COL, CURATOR_COL, ID_COL, WORD_COL, DATE_COL };
	
	// filter:
	ModelFilterWithProviderWidget filterComposite;
	private final String providerFilter;
	private final String typeFilter;
	
	Menu contextMenu;
	
	private PropertyChangeSupport retrainModelProperty;
	boolean withStartPylaiaInContextMenu = false;
	ModelPageLoader pl;
	
	public void setWithStartPylaiaInContextMenu(boolean withStartPylaiaInContextMenu) {
		this.withStartPylaiaInContextMenu = withStartPylaiaInContextMenu;
	}


	public GroundTruthPagedTreeWidget(Composite parent, int style, String typeFilter, String providerFilter, ITreeContentProvider contentProvider, CellLabelProvider labelProvider) {
		super(parent, style, 40, null, true, contentProvider, labelProvider);
//		super(parent, style, 40, contentProvider, labelProvider);
		
		if(providerFilter != null && !Arrays.stream(providerValues).anyMatch(s -> s.equals(providerFilter))) {
			throw new IllegalArgumentException("Invalid providerFilter value");
		}
		
		retrainModelProperty = new PropertyChangeSupport(this);
		
		createColumns();
		
		this.typeFilter = typeFilter;
		this.providerFilter = providerFilter;
		this.setLayout(new GridLayout(1, false));

		contextMenu = new Menu(tv.getTree());
		tv.getTree().setMenu(contextMenu);
		
		
		addFilter();
		
//		Listener filterModifyListener = new Listener() {
//			@Override
//			public void handleEvent(Event event) {
//				loadFirstPage();
//			}
//		};
//		this.addListener(SWT.Modify, filterModifyListener);

		initListener();
		setPageLoader();
	}
	
	
	@Override
	public void addListener(int eventType, Listener listener) {
		super.addListener(eventType, listener);
		filterComposite.addListener(eventType, listener);
	}
	
	private void addFilter() {
		filterComposite = new ModelFilterWithProviderWidget(this, getTreeViewer(), typeFilter, providerFilter, SWT.NONE) {
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
				loadFirstPage();
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
					loadFirstPage();
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

	public void refreshList(List<TrpModelMetadata> htrs) {
		// TODO: htrs are reloaded using the IPageLoadMethod created in setPageLoader method
		// --> no need to set them here
		
		logger.debug("refreshList");
		refreshPage(true);
	}
	
	public void loadFirstPage() {
		logger.debug("load first page of htr paged tree");
		refreshPage(true);
	}

	public void setSelection(int htrId) {
		// TODO
		
//		List<TrpHtr> htrs = (List<TrpHtr>)htrTv.getInput();
//		TrpHtr htr = null;
//		for(int i = 0; i < htrs.size(); i++){
//			final TrpHtr curr = htrs.get(i);
//			if(curr.getHtrId() == htrId){
//				logger.trace("Found htrId {}", htrId);
//				htr = curr;
//				break;
//			}
//		}
//		logger.trace("Selecting HTR in table viewer: {}", htr);
//		if(htr != null) { //if model has been removed from this collection it is not in the list.
//			htrTv.setSelection(new StructuredSelection(htr), true);
//		} else {
//			htrTv.setSelection(null);
//		}
	}

	@Override
	protected void setPageLoader() {
//		IPageLoadMethod<TrpModelMetadataList, TrpModelMetadata> plm = new IPageLoadMethod<TrpModelMetadataList, TrpModelMetadata>() {
//			Storage store = Storage.getInstance();
//			TrpModelMetadataList l;
//			
//			private TrpModelMetadataList load(int fromIndex, int toIndex, String sortPropertyName, String sortDirection) {
//				TrpModelMetadataList l = null;
//				if (store != null && store.isLoggedIn()) {
//					try {
//						Integer collId = store.getCollId();
//						String htrRelease = "All";
//						String langSelectionLabel = null;
//						List<String> isoLangFilter = null;
//						if(filterComposite != null) {
//							htrRelease = filterComposite.getLinkageFilterComboText();
//							langSelectionLabel = (String) filterComposite.getLangFilterCombo().getData(filterComposite.getLangFilterCombo().getText());
//						}
//						
//						Integer releaseLevel = htrRelease.contains("All") ? null : htrRelease.contains("Public") ? -1 : 0;
//						if (store.isAdminLoggedIn() && releaseLevel == null){
//							collId = null;
//						} else if (releaseLevel != null && releaseLevel == -1) {
//							//request for public models only
//							collId = null;
//						}
//					
//						if(langSelectionLabel != null) {
//							isoLangFilter = Arrays.asList(langSelectionLabel);
//						}
//						
//						String filterTxt = filter.getText();
//						logger.debug("load HTRs from DB with filter: " + filterTxt);
//						logger.debug("In collection: {}", collId);
//						logger.debug("providerFilter: {}", getProviderComboValue());
//						logger.debug("linkage filter: {}", htrRelease);
//						logger.debug("htr release is : {}", releaseLevel);
//						logger.debug("isoLangFilter: {}", isoLangFilter);
//						
//						
//						
//						l = store.getConnection().models().getModels("text", collId, releaseLevel, filterTxt, getProviderComboValue(), 
//								null, isoLangFilter, null, null, null, null, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
//								
//								//("text", collId, getProviderComboValue(), filterTxt, releaseLevel, fromIndex, toIndex-fromIndex, sortPropertyName, sortDirection);
//						if (l.getList()== null){
//							logger.debug("the result list is null - no htr match the search string");
//							//if we set not this the old entries persist in the table!!
//							l = new TrpModelMetadataList();
//							l.setList(new ArrayList<>());
//							l.setIndex(0);
//							l.setnValues(0);
//							l.setTotal(0);
//						}
//						
//						if(filterComposite != null) {
//							//process facets
//							filterComposite.getLangFilterCombo().removeAll();
//							final String allLangs = "All Languages";
//							filterComposite.getLangFilterCombo().add(allLangs);
//							filterComposite.getLangFilterCombo().setData(allLangs, null);
//							if(l.getFacets() != null) {
//								for(FacetRepresentation f : l.getFacets()) {
//									if(f.getName().equals("isoLanguages")) {
//										Integer langSelectionIndex = 0;
//										for(int i = 0; i < f.getValues().size(); i++) {
//											FacetValueRepresentation v = f.getValues().get(i);
//											String label = String.format("%s (%d)", 
//													IsoLangUtils.resolveLabelFromCode(v.getValue()), v.getCount());
//											String data = v.getValue();
//											filterComposite.getLangFilterCombo().add(label);
//											filterComposite.getLangFilterCombo().setData(label, data);
//											if(label.equals(langSelectionLabel)) {
//												langSelectionIndex = i;
//											}
//										}
//										filterComposite.getLangFilterCombo().select(langSelectionIndex);
//									}
//								}	
//							}
//						}
//					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
//						TrpMainWidget.getInstance().onError("Error loading HTRs", e.getMessage(), e);
//					}
//				}
//				if(l == null) {
//					l = new TrpModelMetadataList();
//					l.setList(new ArrayList<>());
//					l.setIndex(0);
//					l.setnValues(0);
//					l.setTotal(0);
//				}
//				return l;
//			}		
//			
//			@Override
//			public TrpModelMetadataList loadPage(int fromIndex, int toIndex, String sortPropertyName,
//					String sortDirection) {
//				//pageableTable.refreshPage();
//				l = load(fromIndex, toIndex, sortPropertyName, sortDirection);
//				//applyFilter();
//				return l;
//			}
//		};
//		final IPageLoader<PageResult<TrpModelMetadata>> pl = new RemotePageLoaderSingleRequest<>(pageableTree.getController(), plm);
//		pageableTree.setPageLoader(pl);			
		pl = new ModelPageLoader(pageableTree.getController(), filter, filterComposite);
		pageableTree.setPageLoader(pl);
	}

	@Override
	protected void createColumns() {
		
		for (ColConfig cf : COLUMNS) {
			TreeViewerColumn column = new TreeViewerColumn(tv, SWT.MULTI);
			column.getColumn().setText(cf.name);
			column.getColumn().setWidth(cf.colSize);
			column.getColumn().addSelectionListener(new SortTreeColumnSelectionListener(cf.dbName));
			column.setLabelProvider(labelProvider);
		}
		
	}	
	
	void initListener() {
		
		
		tv.getTree().addMenuDetectListener(new MenuDetectListener() {
			
			@Override
			public void menuDetected(MenuDetectEvent event) {
				if (tv.getTree().getSelectionCount() != 1) {
					event.doit = false;
					return;
				}
				//clear all options
				for(MenuItem item : contextMenu.getItems()) {
					item.dispose();
				}
				
				TreeItem selection = tv.getTree().getSelection()[0];
				Object selectionData = selection.getData();
				
				logger.debug("Menu detected on tree item of type: {}", selectionData.getClass());
				
				if(selectionData instanceof ModelGtDataSet) {
					ModelGtDataSet gtSet = (ModelGtDataSet) selectionData;
					TrpModelMetadata htr = gtSet.getModel();
					boolean isDataSetAccessible = htr.isGtAccessible();
					
					if(!Storage.getInstance().isAdminLoggedIn() && !isDataSetAccessible) {
						logger.debug("Data set not accessible for this user.");
						event.doit = false;
						return;
					}
					
					if (!StorageUtil.canDuplicate(Storage.getInstance().getCollId())) {
						logger.debug("User not privileged to manage collection.");
						event.doit = false;
						return;
					}
					MenuItem copyGtSetToDocItem = new MenuItem(contextMenu, SWT.NONE);
					copyGtSetToDocItem.setText("Copy data set to new document...");
					copyGtSetToDocItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							startCopyGtSetToDocumentAction();
							super.widgetSelected(e);
						}
					});
				} else if (selectionData instanceof TrpModelMetadata) {
					TrpModelMetadata model = (TrpModelMetadata) selectionData;					

					//easy retrain with Pylaia
					if ( (ModelUtil.PROVIDER_CITLAB_PLUS.equals(model.getProvider())
							|| ModelUtil.PROVIDER_PYLAIA.equals(model.getProvider()))
							&& withStartPylaiaInContextMenu) {
						MenuItem retrainItem = new MenuItem(contextMenu, SWT.NONE);
						retrainItem.setText("Retrain model...");
						retrainItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								retrainModelProperty.firePropertyChange("htr", null, model);
							}
						});
					}
					MenuItem showDetailsItem = new MenuItem(contextMenu, SWT.NONE);
					showDetailsItem.setText("Show details...");
					showDetailsItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							TrpMainWidget.getInstance().getUi().getServerWidget().showModelDetailsDialog(model);
						}
					});
					
					ReleaseLevel release = ReleaseLevel.fromString(model.getReleaseLevel());
					if(Storage.getInstance().isAdminLoggedIn() 
							//TODO add inUserCollections property on server side?
							|| (model.isGtAccessible() && release.getValue() == 0)
							|| (model.getUserId() == Storage.getInstance().getUserId())) {
						MenuItem shareModelItem = new MenuItem(contextMenu, SWT.NONE);
						shareModelItem.setText("Share model...");
						shareModelItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								ShareModelMetadataDialog diag = new ShareModelMetadataDialog(getShell(), model);
								//ShareModelDialog diag = new ShareModelDialog(getShell(), model);
								diag.open();
							}
						});
					}
				} else {
					event.doit = false;
				}
			}
		});
	}
	
	private void startCopyGtSetToDocumentAction() {
		ChooseCollectionDialog ccd = new ChooseCollectionDialog(getShell());
		
		@SuppressWarnings("unused")
		int ret = ccd.open();
		TrpCollection col = ccd.getSelectedCollection();
		
		if(col == null) {
			logger.debug("No collection was selected.");
			return;
		}
		
		//MenuDetectListener determined that this action is fine for the selection. Only HtrGtDataSet is allowed now
		TreeItem selection = tv.getTree().getSelection()[0];
		Object selectionData = selection.getData();
		
		if(selectionData == null) {
			logger.debug("Menu aborted without selection.");
		}
		
		AGtDataSet<?> gtSet = (AGtDataSet<?>) selectionData;

		final String title = "Copy of HTR " + gtSet.getDataSetType().getLabel() + " '" + gtSet.getName() + "'";
		
		GroundTruthSelectionDescriptor desc = new GroundTruthSelectionDescriptor(gtSet.getId(), gtSet.getDataSetType().toString());
		
		try {
			TrpMainWidget.getInstance().duplicateGtToDocument(Storage.getInstance().getCollId(), col, desc, title);
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException e1) {
			logger.debug("Could copy dataset to collection!", e1);
			String errorMsg = "The data set could not be copied to this collection.";
			if(!StringUtils.isEmpty(e1.getMessage())) {
				errorMsg += "\n" + e1.getMessage();
			}
			DialogUtil.showErrorMessageBox(getShell(), "Error while copying data set",
					errorMsg);
		}
	}
	
	public void expandTreeItem(Object o) {
		final ITreeContentProvider provider = (ITreeContentProvider) tv.getContentProvider();
		if(!provider.hasChildren(o)) {
			return;
		}
		if (tv.getExpandedState(o)) {
			tv.collapseToLevel(o, AbstractTreeViewer.ALL_LEVELS);
		} else {
			tv.expandToLevel(o, 1);
		}
	}
	
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        retrainModelProperty.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
    	retrainModelProperty.removePropertyChangeListener(pcl);
    }
    
    public ModelFilterWithProviderWidget getFilterComposite() {
    	return filterComposite;
    }

}