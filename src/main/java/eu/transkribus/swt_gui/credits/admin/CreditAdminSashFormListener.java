package eu.transkribus.swt_gui.credits.admin;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.credits.CreditPackageDetailsDialog;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class CreditAdminSashFormListener {
	Logger logger = LoggerFactory.getLogger(CreditAdminSashFormListener.class);
	
	private CreditAdminSashForm view;
	private Storage store;

	public CreditAdminSashFormListener(CreditAdminSashForm view) {
		this.view = view;
		this.store = Storage.getInstance();
		view.userTable.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				BusyIndicator.showWhile(view.getShell().getDisplay(), new Runnable() {
					@Override
					public void run() {
						view.refreshUserAdminCreditsTable(true);
					}
				});
			}
		});
		
		SWTUtil.onSelectionEvent(view.userAdminCreditsTable.getCreatePackageBtn(), e -> {
			openCreatePackageDialog();
		});
		
		view.userAdminCreditsTable.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				view.userAdminCreditsTable.getDetailsBtn().setEnabled(!CollectionUtils.isEmpty(view.userAdminCreditsTable.getSelected()));
			}
		});
		
		SelectionAdapter showDetailsListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<TrpCreditPackage> packages = view.userAdminCreditsTable.getSelected();
				if(packages.isEmpty()) {
					return;
				}
				CreditPackageDetailsDialog d = new CreditPackageDetailsDialog(view.getShell(), packages);
				int ret = d.open();
				logger.debug("CreditPackageDetailsDialog return code = {}", ret);
				if(d.isPackageUpdated()) {
					logger.debug("Package was updated => trigger package table update.");
					view.refreshUserAdminCreditsTable(false);
				}
			}
		};
		view.userAdminCreditsTable.getDetailsBtn().addSelectionListener(showDetailsListener);
		view.showUserPackageDetailsItem.addSelectionListener(showDetailsListener);
		
		SWTUtil.onSelectionEvent(view.userAdminCreditsTable.getShowBalanceDetailsButton(), (e) -> {
			CreditPackageDetailsDialog d = new CreditPackageDetailsDialog(view.getShell(), 
					view.userAdminCreditsTable.getCurrentBalance());
			if (d.open() == IDialogConstants.OK_ID) {
				//we don't need feedback here. do nothing
			}
		});
	}

	private void openCreatePackageDialog() {
		CreateCreditPackageDialog d = new CreateCreditPackageDialog(view.getShell(), view.getSelectedUser());
		if (d.open() == IDialogConstants.OK_ID) {
			TrpCreditPackage newPackage = d.getPackageToCreate();
			try {
				TrpCreditPackage createdPackage = store.getConnection().getCreditCalls().createCredit(newPackage);
				DialogUtil.showInfoBalloonToolTip(view.userAdminCreditsTable.getCreatePackageBtn(), "Done",
						"Package created: '" + createdPackage.getProduct().getLabel() + "'" + "\nOwner: "
								+ createdPackage.getUserName());
				view.userAdminCreditsTable.refreshPage(false);
			} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e1) {
				DialogUtil.showErrorMessageBox2(view.getShell(), "Error", "Package could not be created.", e1);
			}
		}
	}
}
