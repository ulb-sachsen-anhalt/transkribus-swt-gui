package eu.transkribus.swt_gui.models;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.TrpModelMetadata;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class ModelsComposite extends Composite implements IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(ModelsComposite.class);

	Storage store = Storage.getInstance();

	ModelPagedTableWidget mtw;
	
	ModelDetailsWidget mdw;

	MenuItem shareToCollectionItem, removeFromCollectionItem, deleteItem;
	
	TrpModelMetadata selectedModel;

	public ModelsComposite(Composite parent, final String typeFilter, final String providerFilter, int flags) {
		super(parent, flags);
		this.setLayout(new GridLayout(1, false));
		
		SashForm sashForm = new SashForm(this, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sashForm.setLayout(new GridLayout(2, false));
		mtw = new ModelPagedTableWidget(sashForm, SWT.BORDER, typeFilter, providerFilter);

		Menu menu = new Menu(mtw.getTableViewer().getTable());
		mtw.getTableViewer().getTable().setMenu(menu);

		shareToCollectionItem = new MenuItem(menu, SWT.NONE);
		shareToCollectionItem.setText("Share model...");

		//use ShareHtrDialog
//		removeFromCollectionItem = new MenuItem(menu, SWT.NONE);
//		removeFromCollectionItem.setText("Remove model from collection");
		
		deleteItem = new MenuItem(menu, SWT.NONE);
		deleteItem.setText("Delete model...");

		Group detailGrp = new Group(sashForm, SWT.BORDER);
		detailGrp.setText("Details");
		detailGrp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailGrp.setLayout(new GridLayout(1, false));

		mdw = new ModelDetailsWidget(detailGrp, SWT.VERTICAL);
		mdw.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mdw.setLayout(new GridLayout(2, false));

		sashForm.setWeights(new int[] { 66, 34 });
		
		//shows the first page in the paged HTR view
		mtw.refreshPage(true);
		
		addListeners();
	}
	
	public ModelDetailsWidget getModelDetailsWidget() {
		return mdw;
	}
	
	public ModelPagedTableWidget getModelTableWidget() {
		return mtw;
	}
	
	private void addListeners() {
		
		// fix for missing tooltip in chart after resize. Still does not work always...
		this.getShell().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				logger.trace("Resizing...");
				if(getShell().getMaximized()) {
					logger.trace("To MAX!");
				}
				
				mdw.triggerChartUpdate();
			}
		});
		
		mtw.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				BusyIndicator.showWhile(ModelsComposite.this.getDisplay(), new Runnable() {
					@Override public void run() {
						updateDetails(mtw.getSelectedModel());
					}
				});
			}
		});
		
		Listener filterModifyListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateHtrs(mtw.getProviderComboValue());
				updateDetails(mtw.getSelectedModel());
			}
		};
		mtw.addListener(SWT.Modify, filterModifyListener);
		
		SWTUtil.onSelectionEvent(mtw.getReloadButton(), e -> {
			mtw.refreshPage(false);
		});
		
		shareToCollectionItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//openCollectionChooserDialog(e);
				ShareModelMetadataDialog diag = new ShareModelMetadataDialog(getShell(), mtw.getSelectedModel());
				int ret = diag.open();
				logger.debug("ShareHtrDialog closed with return code: {}", ret);
			}
		});

		SelectionAdapter removeItemListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TrpModelMetadata model = mtw.getSelectedModel();
				try {
					store.removeModelFromCurrentCollection(model);
					mtw.refreshPage(false);
					clearTableSelection();
				} catch (SessionExpiredException | ServerErrorException | ClientErrorException | NoConnectionException e1) {
					logger.debug("Could not remove HTR from collection!", e1);
					DialogUtil.showErrorMessageBox(getShell(), "Error removing HTR",
							"The selected HTR could not be removed from this collection.");
				}
				super.widgetSelected(e);
			}
		};
		if(removeFromCollectionItem != null) {
			removeFromCollectionItem.addSelectionListener(removeItemListener);
		}
		
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//menu item is only enabled if delete action is allowed. See menu detect listener
				TrpModelMetadata model = getSelectedModel();
				final String msg = "You are about to delete the HTR model '" + model.getName() 
					+ "'.\nDo you really want to do this?";
				final int response = DialogUtil.showYesNoDialog(getShell(), "Delete HTR model?", msg);
				
				if(response != SWT.YES) {
					logger.debug("User canceled deletion of HTR with ID = {}", model.getModelId());
					return;
				}
				
				try {
					store.getConnection().getModelCalls().setModelDeleted(model);
//					//if that worked update the list in Storage
					mtw.refreshPage(false);
					clearTableSelection();
				} catch (SessionExpiredException | ServerErrorException | ClientErrorException e1) {
					logger.error("Could not delete HTR!", e1);
					DialogUtil.showErrorMessageBox(getShell(), "Error removing HTR",
							"The selected HTR could not be removed from this collection.");
				}
				super.widgetSelected(e);
			}
		});

		mtw.getTableViewer().getTable().addListener(SWT.MenuDetect, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (mtw.getTableViewer().getTable().getSelectionCount() <= 0) {
					event.doit = false;
					return;
				} 
				deleteItem.setEnabled(isOwnerOfSelectedHtr());
			}
		});
		
		//listen to ModelListLoadEvents
		store.addListener(this);
		
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				logger.debug("Detaching HtrModelsComposite IStorageListener from Storage");
				store.removeListener(ModelsComposite.this);
			}
		});
	}
	
	public ModelsComposite(Composite parent, int flags) {
		this(parent, null, null, flags);
	}
	
	public void setSelection(int modelId) {
		logger.trace("Setting selection to modelId = {}", modelId);
		mtw.setSelection(modelId);
	}
	
	public TrpModelMetadata getSelectedModel() {
		return selectedModel;
	}
	
	public boolean isOwnerOfSelectedHtr() {
		if(getSelectedModel() == null) {
			return false;
		}
		if(!store.isLoggedIn()) {
			return false;
		}
		if(store.isAdminLoggedIn()) {
			return true;
		}
		return getSelectedModel().getUserId() == store.getUserId();
	}
	
	void updateDetails(TrpModelMetadata selectedHtr) {
		mdw.checkForUnsavedChanges();
		mdw.updateDetails(selectedHtr);
		this.selectedModel = mdw.getModel(); // retrieve model from ModelDetailsWidget to get full data
	}

	private void updateHtrs(final String providerFilter) {
		//all models are paged now - omit to get all models
//		List<TrpHtr> uroHtrs = store.getHtrs(providerFilter);
//		htw.refreshList(uroHtrs);
		
		mtw.refreshPage(false);
	}
	
//	@Override
//	public void handleHtrListLoadEvent(HtrListLoadEvent e) {
//		htw.resetProviderFilter();
//		htw.refreshList(e.models.getList());
//	}
	
	/*
	 * private void reloadHtrsFromServer() { //reload HTRs and show busy indicator
	 * in the meantime. ReloadHtrListRunnable reloadRunnable = new
	 * ReloadHtrListRunnable(); BusyIndicator.showWhile(getDisplay(),
	 * reloadRunnable);
	 * 
	 * if(reloadRunnable.hasError()) { logger.error("Reload of HTR models failed!",
	 * reloadRunnable.getError());
	 * DialogUtil.showDetailedErrorMessageBox(getShell(),
	 * "Error loading HTR models",
	 * "Could not reload the HTR model list from the server.",
	 * reloadRunnable.getError()); } }
	 */
	
	private void clearTableSelection() {
		//remove any selection in table
		setSelection(-1);
		//clear any data from HtrDetailsWidget
		updateDetails(mtw.getSelectedModel());
	}
	
	/**
	 * Helper class for reloading the HTR list as task with a BusyIndicator.
	 * Any error can be retrieved from it and handled after BusyIndicator.showWhile() completes.
	 * Opening dialogs within the Runnable would block the BusyIndicator from completing.
	 */
	/*
	 * private class ReloadHtrListRunnable implements Runnable { private Throwable
	 * error;
	 * 
	 * ReloadHtrListRunnable() { error = null; }
	 * 
	 * public void run() { //update of the view is done in the
	 * handleHtrListLoadEvent method Future<TrpHtrList> future = store.reloadHtrs();
	 * try { //after 60 seconds we can assume that something is wrong future.get(60,
	 * TimeUnit.SECONDS); } catch(ExecutionException e) { //extract the exception
	 * thrown within the future's task error = e.getCause(); } catch(Exception e) {
	 * error = e; } }
	 * 
	 * boolean hasError() { return error != null; }
	 * 
	 * Throwable getError() { return error; } }
	 */
}
