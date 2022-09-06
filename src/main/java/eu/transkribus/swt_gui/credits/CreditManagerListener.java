package eu.transkribus.swt_gui.credits;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.IStorageListener;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CreditManagerListener implements IStorageListener {
	private static final Logger logger = LoggerFactory.getLogger(CreditManagerListener.class);
	
	Storage store;
	private final CreditManagerDialog view;
	
	CreditManagerListener(CreditManagerDialog view) {
		this.view = view;
		this.store = Storage.getInstance();
		
		addListeners(view);
//		addDndSupport(view);
	}
	
	private void addListeners(CreditManagerDialog view) {
		//enable this to trigger a refresh on tab switch
//		SWTUtil.onSelectionEvent(view.tabFolder, (e) -> { 
//			view.updateUI(false);
//		});
		SWTUtil.setTabFolderBoldOnItemSelection(view.tabFolder);
		SWTUtil.onSelectionEvent(view.getCollectionCreditWidget().addToCollectionBtn, (e) -> {
			List<TrpCreditPackage> packageList = view.getCollectionCreditWidget().userCreditsTable.getSelected();
			assignPackagesToCollection(view.getCollection(), packageList);
		});
		
		SWTUtil.onSelectionEvent(view.getCollectionCreditWidget().removeFromCollectionBtn, (e) -> {
			List<TrpCreditPackage> packageList = view.getCollectionCreditWidget().collectionCreditsTable.getSelected();
			removePackagesFromCollection(view.getCollection().getColId(), packageList);
		});
		
		//register as storage listener for handling collection changes (DocListLoadEvent). Deregister on dialog close.
		store.addListener(this);
		view.dialogArea.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				Storage.getInstance().removeListener(CreditManagerListener.this);
			}
		});
		
		view.getCollectionCreditWidget().userCreditsTable.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				logger.trace("Selection changed: {}", arg0);
				List<TrpCreditPackage> selection = view.getCollectionCreditWidget().userCreditsTable.getSelected();
				boolean enableSplitPackageItem = !CollectionUtils.isEmpty(selection)
						&& selection.size() == 1
						//allow split only for shareable package
						&& selection.get(0).getProduct().getShareable();
				boolean enableShowDetailsItem = !CollectionUtils.isEmpty(selection);
				view.getCollectionCreditWidget().showUserPackageDetailsItem.setEnabled(enableShowDetailsItem);
				view.getCollectionCreditWidget().splitUserPackageItem.setEnabled(enableSplitPackageItem);
			}
		});
		
		SelectionAdapter showDetailsListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openPackageDetailsDialog(view.getCollectionCreditWidget().userCreditsTable.getSelected());
			}
		};
		SelectionAdapter splitPackageListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openPackageManagerDialog(view.getCollectionCreditWidget().userCreditsTable.getSelectedPackage());
			}
		};
		view.getCollectionCreditWidget().showUserPackageDetailsItem.addSelectionListener(showDetailsListener);
		view.getCollectionCreditWidget().splitUserPackageItem.addSelectionListener(splitPackageListener);
		
		SWTUtil.onSelectionEvent(view.getCollectionCreditWidget().userCreditsTable.getShowBalanceDetailsButton(), (e) -> {
			openPackageDetailsDialog(null, view.getCollectionCreditWidget().userCreditsTable.getCurrentBalance());
		});
		
		SWTUtil.onSelectionEvent(view.getCollectionCreditWidget().collectionCreditsTable.getShowBalanceDetailsButton(), (e) -> {
			openPackageDetailsDialog(null, view.getCollectionCreditWidget().collectionCreditsTable.getCurrentBalance());
		});
	}

	private void assignPackagesToCollection(TrpCollection collection, List<TrpCreditPackage> packageList) {
		if(CollectionUtils.isEmpty(packageList)) {
			return;
		}
		
		List<TrpCreditPackage> packagesToShare = packageList.stream()
				.filter(p -> p.getProduct().getShareable())
				.collect(Collectors.toList());
		

		// error if packagesToShare is empty
		if(packagesToShare.isEmpty()) {
			view.dialogArea.getDisplay().asyncExec(new Runnable() {
				public void run() {
					DialogUtil.showBalloonToolTip(view.getCollectionCreditWidget().userCreditsTable, null, "No packages assigned", "Your selection does not contain any shareable packages.");
				}
			});
			return;
		}
		
		//show confirmation dialog & also inform about non-shareable packages in the selection
		String confirmMsg = "Do you really want to share " + packagesToShare.size() + " package(s) in the collection '" + collection.getColName() + "'?";
		
		//generate message about skipped packages
		final int nrOfSkippedPackages = packageList.size() - packagesToShare.size();
		if(nrOfSkippedPackages > 0) {
			confirmMsg += "\n\n(" + nrOfSkippedPackages + " non-shareable package(s) skipped)";
		}
		
		int answer = DialogUtil.showYesNoDialog(view.getShell(), "Please confirm your selection", confirmMsg);
		if(answer != SWT.YES) {
			logger.debug("User declined sharing packages.");
			return;
		}
			
		// go on with packagesToShare
		int addCount = 0;
		int notModifiedCount = 0;
		final List<Exception> fails = new ArrayList<>(0);
		
		for(TrpCreditPackage p : packagesToShare) {
			try {
				store.getConnection().getCreditCalls().addCreditPackageToCollection(collection.getColId(), p.getPackageId());
				addCount++;
			} catch (IllegalStateException ise) {
				//Client currently maps "304 - Not modified" to an IllegalStateException. The package was already assigned to this collection.
				logger.debug("Package is already assigned to this collection.");
				notModifiedCount++;
			} catch (SessionExpiredException e1) {
				//TODO abort and show login dialog
			} catch (TrpServerErrorException | TrpClientErrorException e2) {
				logger.error(e2.getMessageToUser());
				fails.add(e2);
			}
		}
		final String balloonMsgTitle = addCount + "/" + packagesToShare.size() + " packages assigned";
		String msg = "";
		if(notModifiedCount > 0) {
			msg += notModifiedCount + " already assigned\n";
		}
		// the user is now informed about skipped packages in the confirmation dialog. Activate this to add a message also in the balloon tip.
//		if(nrOfSkippedPackages > 0) {
//			msg += nrOfSkippedPackages + " packages in the selection are not shareable.\n";
//		}
		if(fails.size() > 0) {
			msg += fails.size() + " errors:\n";
			msg += fails.stream()
					.map(e -> e.getMessage())
					.collect(Collectors.joining("\n"));
		}
		final String balloonMsg = msg;
		
		view.dialogArea.getDisplay().asyncExec(new Runnable() {
			public void run() {
				view.getCollectionCreditWidget().collectionCreditsTable.refreshPage(false);
				DialogUtil.showBalloonToolTip(view.getCollectionCreditWidget().collectionCreditsTable, null, balloonMsgTitle, balloonMsg.trim());
			}
		});
	}

	private void removePackagesFromCollection(int collId, List<TrpCreditPackage> packageList) {
		if(CollectionUtils.isEmpty(packageList)) {
			return;
		}
		int addCount = 0;
		final Set<String> fails = new HashSet<>();
		for(TrpCreditPackage p : packageList) {
			try {
				store.getConnection().getCreditCalls().removeCreditPackageFromCollection(view.getCollection().getColId(), p.getPackageId());
				addCount++;
			} catch (SessionExpiredException e1) {
				//TODO abort and show login dialog
			} catch (TrpServerErrorException | TrpClientErrorException e2) {
				fails.add(e2.getMessageToUser());
			}
		}
		final int successCount = addCount;
		String errorMsg = fails.stream().collect(Collectors.joining("\n"));
		view.dialogArea.getDisplay().asyncExec(new Runnable() {
			public void run() {
				view.getCollectionCreditWidget().collectionCreditsTable.refreshPage(false);
				DialogUtil.showBalloonToolTip(view.getCollectionCreditWidget().collectionCreditsTable, null, successCount + " packages removed", errorMsg);
			}
		});
	}
	
	private void openPackageDetailsDialog(List<TrpCreditPackage> packages) {
		if(CollectionUtils.isEmpty(packages)) {
			return;
		}
		openPackageDetailsDialog(null, packages);
	}
	
	private void openPackageDetailsDialog(String msg, double creditBalance) {
		CreditPackageDetailsDialog d = new CreditPackageDetailsDialog(view.getShell(), creditBalance);
		if (d.open() == IDialogConstants.OK_ID) {
			//we don't need feedback here. do nothing
		}
	}
	
	private void openPackageDetailsDialog(String msg, List<TrpCreditPackage> packages) {
		CreditPackageDetailsDialog d = new CreditPackageDetailsDialog(view.getShell(), packages);
		if (d.open() == IDialogConstants.OK_ID) {
			//we don't need feedback here. do nothing
		}
	}
	
	private void openPackageManagerDialog(TrpCreditPackage creditPackage) {
		if(creditPackage == null || creditPackage.getBalance() <= 0.0) {
			return;
		}
		logger.debug("Opening package split dialog for package {}", creditPackage);
		CreditPackageManagerDialog d = new CreditPackageManagerDialog(view.getShell(), creditPackage);
		if (d.open() == IDialogConstants.OK_ID) {
			int numPackages = d.getNumPackages();
			double creditValue = d.getCreditValue();
			//currently the package can't be changed within the dialog so using creditPackage would work as well.
			TrpCreditPackage sourcePackage = d.getCreditPackage();
			ProgressBarDialog pbd = new ProgressBarDialog(view.getShell());
			
			IRunnableWithProgress r = new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Splitting package...", numPackages);
					//check selected Dirs on server
					for(int i = 0; i < numPackages; i++) {
						monitor.worked(i);
						try {
							store.getConnection().getCreditCalls().splitCreditPackage(sourcePackage, creditValue);
						} catch (Exception e) {
							logger.error("Error while splitting package", e);
							Runnable r = new Runnable() {
								@Override
								public void run() {
									DialogUtil.showErrorMessageBox(new Shell(Display.getDefault()), "Error", e.getMessage());
								}
							};
							Display.getDefault().syncExec(r);
						}
					}
					logger.debug("Finished splitting package.");
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							view.getCollectionCreditWidget().userCreditsTable.refreshPage(false);
						}
					});
				}
			};
			
			try {
				pbd.open(r, "Splitting package...", true);
			} catch (Throwable e) {
				DialogUtil.showErrorMessageBox(view.getShell(), "Error", e.getMessage());
				logger.error("Error in ProgressMonitorDialog", e);
			}
		}
	}

	/**
	 * Does not work as required.<br>
	 * Drag and drop should only work from one table to another, not within the same table.
	 * <br><br>
	 * I wanted to use DND.DROP_LINK and DND.DROP_MOVE to link the respective dragSourceListener and dropTargetAdapter
	 * but only with the DND.DROP_MOVE operation the event will trigger the dragAccepted method of the listener and execute the drop.
	 * The reason may be found in the source code of jface structured viewer or SWT but I can't find it for the specific version used...
	 * <br><br>
	 * Using classic buttons instead for now.
	 * @deprecated
	 */
	@SuppressWarnings("unused")
	private void addDndSupport(CreditManagerDialog view) {
		CreditPackageDragSourceListener userDragSourceListener, collectionDragSourceListener;
		CreditPackageDropTargetListener collectionDropTargetListener, userDropTargetListener;
		Transfer[] transferTypes = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		
		//listeners for adding credits to a collection
		final int addOps = DND.DROP_LINK;
		userDragSourceListener = new CreditPackageDragSourceListener(view.getCollectionCreditWidget().userCreditsTable.getTableViewer());
		collectionDropTargetListener = new CreditPackageDropTargetListener(view.getCollectionCreditWidget().collectionCreditsTable.getTableViewer()) {
			@Override
			protected void performDropAction(DropTargetEvent event) {
				// add credit package to collection
				super.performDropAction(event);
			}
		};

		view.getCollectionCreditWidget().userCreditsTable.getTableViewer().addDragSupport(addOps, transferTypes, userDragSourceListener);
		view.getCollectionCreditWidget().collectionCreditsTable.getTableViewer().addDropSupport(addOps, transferTypes, collectionDropTargetListener);
		
		//listeners for removing credits from a collection
		final int removeOps = DND.DROP_MOVE;
		collectionDragSourceListener = new CreditPackageDragSourceListener(view.getCollectionCreditWidget().collectionCreditsTable.getTableViewer());
		userDropTargetListener = new CreditPackageDropTargetListener(view.getCollectionCreditWidget().userCreditsTable.getTableViewer()) {
			@Override
			protected void performDropAction(DropTargetEvent event) {
				// remove credit package from collection
				super.performDropAction(event);
			}
		};

		view.getCollectionCreditWidget().collectionCreditsTable.getTableViewer().addDragSupport(removeOps, transferTypes, collectionDragSourceListener);
		view.getCollectionCreditWidget().userCreditsTable.getTableViewer().addDropSupport(removeOps, transferTypes, userDropTargetListener);
	}
	
	@Override
	public void handleDocListLoadEvent(DocListLoadEvent e) {
		if(e.isCollectionChange) {
			view.updateUI(true);
		}
	}
	
	class CreditPackageDragSourceListener implements DragSourceListener {

		private final Viewer viewer;

		public CreditPackageDragSourceListener(Viewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void dragStart(DragSourceEvent event) {
			logger.debug("DragStart: " + event);
			
			ISelection selection = viewer.getSelection();
			logger.debug("Setting selection = {}", selection);
			// the controller can retrieve the selection in the end.
			LocalSelectionTransfer.getTransfer().setSelection(selection);
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			logger.debug("DragSetData: " + event);
			event.data = (IStructuredSelection) viewer.getSelection();
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
			logger.debug("DragFinished: " + event);
		}
	}

	class CreditPackageDropTargetListener implements DropTargetListener { //extends ViewerDropAdapter {

		Viewer viewer;
		
		protected CreditPackageDropTargetListener(Viewer viewer) {
			this.viewer = viewer;
		}
		
//		protected CreditPackageDropTargetListener(Viewer viewer) {
//			super(viewer);
//		}

		@Override
		public void drop(DropTargetEvent event) {
			logger.debug("Drop event: " + event);
			Runnable r = new Runnable() {
				@Override
				public void run() {
					performDropAction(event);
				}
			};
			BusyIndicator.showWhile(view.dialogArea.getDisplay(), r);
		}
		
		protected void performDropAction(DropTargetEvent event) {
			logger.debug("Perform drop action here.");
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			logger.debug("Transfer selection = {}", selection);
		}

		@Override
		public void dragEnter(DropTargetEvent event) {
			logger.debug("DropEnter: " + event);
		}

		@Override
		public void dragLeave(DropTargetEvent event) {
			logger.debug("DropLeave: " + event);
		}

		@Override
		public void dragOperationChanged(DropTargetEvent event) {
			logger.debug("DropOperationChanged: " + event);
		}

		@Override
		public void dragOver(DropTargetEvent event) {
			logger.trace("DragOver: " + event);
		}

		@Override
		public void dropAccept(DropTargetEvent event) {
			logger.debug("DropAccept: " + event);
			// in some cases we want to block the drop action. set event.detail = DND.DROP_NONE to do this
//			event.detail = DND.DROP_NONE;
		}

//		@Override
//		public boolean performDrop(Object data) {
//			logger.debug("Performing drop. Data = {}", data);
//			return false;
//		}
//
//		@Override
//		public boolean validateDrop(Object target, int op, TransferData type) {
//			logger.debug("Validating drop:");
//			logger.debug("Target = {}", target);
//			logger.debug("op = {}", op);
//			logger.debug("type = {}", type);
//			logger.debug("Current target = {}", this.getCurrentTarget());
//			logger.debug("Selected object = {}", this.getSelectedObject());
//			return false;
//		}
	}
}
