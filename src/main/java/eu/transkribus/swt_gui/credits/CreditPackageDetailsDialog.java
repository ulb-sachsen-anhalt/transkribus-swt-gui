package eu.transkribus.swt_gui.credits;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.client.util.TrpClientErrorException;
import eu.transkribus.client.util.TrpServerErrorException;
import eu.transkribus.core.model.beans.TrpCollection;
import eu.transkribus.core.model.beans.TrpCreditCosts;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt.util.Images;
import eu.transkribus.swt.util.SWTUtil;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.metadata.CollectionsTableWidget;
import eu.transkribus.swt_gui.pagination_tables.CreditTransactionsByPackagePagedTableWidget;

public class CreditPackageDetailsDialog extends Dialog {
	Logger logger = LoggerFactory.getLogger(CreditPackageDetailsDialog.class);
	protected Composite dialogArea;
	protected CreditCostsTable table;
	private CollectionsTableWidget collectionsTableWidget;
	private Button saveBtn;
	
	private List<TrpCreditPackage> packages;
	private double creditValue;
	private List<TrpCreditCosts> costs;
	
	/**
	 * This flag is set to true if a given package's properties have been updated
	 */
	private boolean isPackageUpdated = false;
	
	/**
	 * Create dialog that only shows the remaining pages with respect to current costs for the given balance
	 * 
	 * @param parent
	 * @param creditBalance
	 */
	public CreditPackageDetailsDialog(Shell parent, double creditBalance) {
		super(parent);
		packages = new ArrayList<>(0);
		this.creditValue = creditBalance;
		this.isPackageUpdated = false;
		costs = Storage.getInstance().getCreditCosts(null, false);
	}
	/**
	 * Create dialog that shows the remaining pages with respect to current costs for the sum of balances of all given packages.
	 * In case a single package is in the list, the booked transactions for this package will be displayed.
	 * 
	 * @param parent
	 * @param creditBalance
	 */
	public CreditPackageDetailsDialog(Shell parent, List<TrpCreditPackage> packages) {
		super(parent);
		if(packages == null) {
			packages = new ArrayList<>(0);
		}
		this.packages = packages;
		this.creditValue = sumBalances(packages);
		costs = Storage.getInstance().getCreditCosts(null, false);
	}
	
	/**
	 * Returns true after the saveBtn was clicked and the update was successful, i.e. any table in the parent dialog might need an update to show current data.
	 */
	public boolean isPackageUpdated() {
		return isPackageUpdated;
	}

	public void setVisible() {
		if (super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}
	
	/**
	 * sum credit values of all selected packages
	 * @param packages
	 * @return sum over all balances or 0.0 if list is null or empty
	 */
	private static double sumBalances(List<TrpCreditPackage> packages) {
		if(CollectionUtils.isEmpty(packages)) {
			return 0.0;
		}
		return packages.stream().collect(Collectors.summingDouble(TrpCreditPackage::getBalance));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		dialogArea = (Composite) super.createDialogArea(parent);
		dialogArea.setLayout(new GridLayout(1, true));
		dialogArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group remainingPagesGroup = new Group(dialogArea, SWT.NONE);
		remainingPagesGroup.setText("Remaining Pages");
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(remainingPagesGroup);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(remainingPagesGroup);
		table = new CreditCostsTable(remainingPagesGroup, SWT.BORDER | SWT.V_SCROLL, creditValue, costs);
		
		if(this.packages.size() == 1) {
			//Show transactions only if a single package is selected and the package owner is logged in
			TrpCreditPackage p = packages.get(0);
			Group transactionsGroup = new Group(dialogArea, SWT.NONE);
			transactionsGroup.setText("Transaction History of Package");
			Storage storage = Storage.getInstance();
			if(storage.isAdminLoggedIn() || storage.getUser().getUserId() == p.getUserId()) {
				GridLayoutFactory.fillDefaults().numColumns(1).applyTo(transactionsGroup);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(transactionsGroup);
				CreditTransactionsByPackagePagedTableWidget transactionsTable = new CreditTransactionsByPackagePagedTableWidget(transactionsGroup, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(transactionsTable);
				transactionsTable.setPackageId(p.getPackageId());
			} else {
				Label notAvailableLbl = new Label(transactionsGroup, SWT.NONE);
				notAvailableLbl.setText("Only the owner of this package can view the transaction history.");
			}
			if(p.getProduct().getShareable()) {
				Group collectionsGroup = new Group(dialogArea, SWT.NONE);
				collectionsGroup.setText("Linked collections");
				GridLayoutFactory.fillDefaults().numColumns(1).applyTo(collectionsGroup);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(collectionsGroup);
				collectionsTableWidget = new CollectionsTableWidget(collectionsGroup, SWT.BORDER);
				collectionsTableWidget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
				GridDataFactory.fillDefaults().grab(true, true).applyTo(collectionsTableWidget);
			
				List<TrpCollection> colls = new ArrayList<>(0);
				try {
					colls = storage.getConnection().getCreditCalls().getCollectionsByCreditPackage(p.getPackageId());
				} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
					DialogUtil.showErrorBalloonToolTip(collectionsTableWidget, "Error", "Could not fetch linked collections.");
					logger.error("Could not fetch linked collections.", e);
				}
				collectionsTableWidget.refreshList(colls);
			}
			if(storage.isAdminLoggedIn()) {
				//show the mutable package properties for admins
				//use copy constructor to set the data from the given package for editing. The given package object is not altered.
				createPackagePropsGroup(dialogArea, new TrpCreditPackage(p));				
			}
		}
		return dialogArea;
	}
	
	private void createPackagePropsGroup(Composite dialogArea, TrpCreditPackage creditPkg) {
		Group pkgPropsGroup = new Group(dialogArea, SWT.NONE);
		pkgPropsGroup.setText("Package Properties (Admin)");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(pkgPropsGroup);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(1).equalWidth(false).applyTo(pkgPropsGroup);
		CreditPackagePropsAdminWidget propsEditor = new CreditPackagePropsAdminWidget(pkgPropsGroup, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(propsEditor);
		propsEditor.setPackage(creditPkg);
		
		saveBtn = new Button(pkgPropsGroup, SWT.PUSH);
		saveBtn.setImage(Images.DISK);
		saveBtn.setText("Save");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(saveBtn);
		SWTUtil.onSelectionEvent(saveBtn, e -> {
			BusyIndicator.showWhile(this.getShell().getDisplay(), new Runnable() {
				@Override
				public void run() {
					try {
						Storage.getInstance().getConnection().getCreditCalls().updateCreditPackage(creditPkg);
						logger.info("Updated credit package properties: {}", creditPkg);
						isPackageUpdated = true;
						DialogUtil.showBalloonToolTip(saveBtn, SWT.ICON_INFORMATION, "", "Changes saved.");
					} catch (TrpServerErrorException | TrpClientErrorException | SessionExpiredException e) {
						logger.error(e.getMessage(), e);
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error while updating properties", e.getMessageToUser(), e);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						DialogUtil.showDetailedErrorMessageBox(getShell(), "Error while updating properties", e.getMessage(), e);
					}
				}
			});
		});
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// do not create button bar
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		final String titleTxt;
		if(packages.isEmpty()) {
			titleTxt = "Remaining Pages with " + creditValue + " Credits";
		} else if (packages.size() > 1){
			titleTxt = "Remaining Pages with " + creditValue + " Credits in " + packages.size() + " Packages";
		} else {
			titleTxt = "Details of Package '" + packages.get(0).getProduct().getLabel() + "'";
		}
		newShell.setText(titleTxt);
		newShell.setMinimumSize(600, computeHeight());
	}

	protected int computeHeight() {
		if(packages.isEmpty()) {
			return 400;
		} else if (packages.size() > 1){
			return 420;
		} else {
			if(Storage.getInstance().isAdminLoggedIn()) {
				//make room for the mutable package properties
				return 800;
			}
			return 600;
		}
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
	}
}
