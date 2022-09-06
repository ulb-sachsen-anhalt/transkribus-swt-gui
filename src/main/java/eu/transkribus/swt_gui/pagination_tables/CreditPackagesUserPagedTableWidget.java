package eu.transkribus.swt_gui.pagination_tables;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.nebula.widgets.pagination.table.PageableTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCreditPackage;
import eu.transkribus.core.model.beans.rest.TrpCreditPackageList;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.credits.OverallBalanceComposite;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Page loader will retrieve credit packages
 *
 */
public class CreditPackagesUserPagedTableWidget extends ATableWidgetPagination<TrpCreditPackage> {
	
	public static final String PACKAGE_USER_NAME_COL = "Owner";
	public static final String PACKAGE_USER_ID_COL = "Owner ID";
	public static final String PACKAGE_NAME_COL = "Name";
	public static final String PACKAGE_BALANCE_COL = "Balance";
	public static final String PACKAGE_SHAREABLE_COL = "Shareable";	
	public static final String PACKAGE_TYPE_COL = "Type";
	public static final String PACKAGE_DATE_COL = "Created";
	public static final String PACKAGE_EXPIRATION_DATE_COL = "Expires";
	public static final String PACKAGE_ID_COL = "ID";
	
	protected final DateFormat dateFormat;
	
	RemotePageLoaderSingleRequest<TrpCreditPackageList, TrpCreditPackage> pageLoader;
	
	private Button showDisabledFilterBtn, showDepletedFilterBtn, showExpiredFilterBtn;
	
	protected OverallBalanceComposite overallBalanceComp;

	public CreditPackagesUserPagedTableWidget(Composite parent, int style) {
		super(parent, style, 25);
		this.setLayout(new GridLayout(1, false));
		dateFormat = CoreUtils.newDateFormatUserFriendly();
		addFilters(Storage.getInstance().isAdminLoggedIn());
		createOverallBalanceComposite(pageableTable);
		addListeners();
	}
	
	private void addListeners() {
		//add listeners related to the filter buttons within this composite
		SelectionAdapter listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshPage(false);
			}
		};
		if(showDisabledFilterBtn != null) {
			showDisabledFilterBtn.addSelectionListener(listener);
		}
		showDepletedFilterBtn.addSelectionListener(listener);
		showExpiredFilterBtn.addSelectionListener(listener);
	}

	protected void addFilters(boolean isAdmin) {
		final int numColumns = isAdmin ? 3 : 2;
		Composite filterComp = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterComp);
		GridLayoutFactory.fillDefaults().numColumns(numColumns).equalWidth(false).applyTo(filterComp);
		filterComp.moveAbove(null);
		if(isAdmin) {
			showDisabledFilterBtn = new Button(filterComp, SWT.CHECK);
			showDisabledFilterBtn.setText("Show disabled");
			showDisabledFilterBtn.setSelection(true);
		}
		showDepletedFilterBtn = new Button(filterComp, SWT.CHECK);
		showDepletedFilterBtn.setText("Show depleted");
		showExpiredFilterBtn = new Button(filterComp, SWT.CHECK);
		showExpiredFilterBtn.setText("Show expired");
	}

	protected void createOverallBalanceComposite(PageableTable pageableTable) {
		// Create the composite in the bottom right of the table widget
		Composite parent = pageableTable.getCompositeBottom();
		int layoutColsIncrement = 1;
		
		overallBalanceComp = new OverallBalanceComposite(parent, SWT.NONE);
		overallBalanceComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		//adjust layout of bottom
		GridLayout layout = (GridLayout) parent.getLayout();
		layout.numColumns += layoutColsIncrement;
		parent.pack();
	}

	public TrpCreditPackage getSelectedPackage() {
		return getFirstSelected();
	}

	public void setSelection(int packageId) {
		// TODO
	}
	
	@Override
	public void refreshPage(boolean resetToFirstPage) {
		//do the refresh
		super.refreshPage(resetToFirstPage);
		//retrieve previous page data from loader for further UI update
		TrpCreditPackageList currentData = pageLoader.getCurrentData();
		Double balance = null;
		if(currentData != null && currentData.getOverallBalance() != null) {
			balance = currentData.getOverallBalance();
		}
		overallBalanceComp.updateBalanceValue(balance);
	}

	@Override
	protected void createColumns() {
		createColumns(false);
	}

	protected void createColumns(boolean showOwnerColumn) {
		createColumn(PACKAGE_NAME_COL, 220, "label", new PackageColumnLabelProvider(p -> p.getProduct().getLabel()));
		createColumn(PACKAGE_BALANCE_COL, 80, "balance", new PackageColumnLabelProvider(p -> "" + p.getBalance()));
		if(showOwnerColumn) {
			createColumn(PACKAGE_USER_NAME_COL, 120, "userName", new PackageColumnLabelProvider(p -> p.getUserName()));
			//for now we don't need the userid
//			createDefaultColumn(PACKAGE_USER_ID_COL, 50, "userId", new PackageColumnLabelProvider(p -> "" + p.getUserId()));
		}
		createColumn(PACKAGE_SHAREABLE_COL, 70, "shareable", new PackageColumnLabelProvider(p -> "" + p.getProduct().getShareable()));
		createColumn(PACKAGE_DATE_COL, 120, "purchaseDate", new PackageColumnLabelProvider(p -> dateFormat.format(p.getPurchaseDate())));
		//hide credit type as the value is currently not used anyway
//		createColumn(PACKAGE_TYPE_COL, 100, "creditType", new PackageColumnLabelProvider(p -> "" + p.getCreditType()));
		createColumn(PACKAGE_EXPIRATION_DATE_COL, 120, "expirationDate", new PackageColumnLabelProvider(
				p -> p.getExpirationDate() == null ? "never" : dateFormat.format(p.getExpirationDate()))
			);
		createColumn(PACKAGE_ID_COL, 50, "packageId", new PackageColumnLabelProvider(p -> "" + p.getPackageId()));
		ColumnViewerToolTipSupport.enableFor(super.getTableViewer());
	}

	protected RemotePageLoaderSingleRequest<TrpCreditPackageList, TrpCreditPackage> createPageLoader() {
		IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage> plm = new IPageLoadMethod<TrpCreditPackageList, TrpCreditPackage>() {

			@Override
			public TrpCreditPackageList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				Storage store = Storage.getInstance();
				if (store.isLoggedIn()) {
					try {
						//do not show packages that are linked to any collection
//						Boolean isShared = false, shareable = null;
						//default: show all packages owned by logged in user
						Boolean isShared = null, shareable = null;
						return store.getConnection().getCreditCalls().getCreditPackagesByUser(
								!getShowDisabledFilterValue(), getShowExpiredFilterValue(), getMinBalanceFilterValue(),
								isShared, shareable,
								fromIndex, toIndex - fromIndex, sortPropertyName, sortDirection);
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading Credit Packages", e.getMessage(), e);
					}
				}
				return new TrpCreditPackageList(new ArrayList<>(), 0.0d, 0, 0, 0, null, null);
			}
		};
		return new RemotePageLoaderSingleRequest<>(pageableTable.getController(), plm);
	}
	
	@Override
	protected void setPageLoader() {
		//hold reference to page loader for later access
		pageLoader = createPageLoader();
		pageableTable.setPageLoader(pageLoader);
	}
	
	public Double getCurrentBalance() {
		return overallBalanceComp.getBalance();
	}
	
	/**
	 * @return the calculator button
	 */
	public Button getShowBalanceDetailsButton() {
		return overallBalanceComp.getShowDetailsBtn();
	}
	
	protected boolean getShowExpiredFilterValue() {
		return showExpiredFilterBtn.getSelection();
	}
	
	protected Double getMinBalanceFilterValue() {
		return showDepletedFilterBtn.getSelection() ? null : 0.001d;
	}
	
	protected boolean getShowDisabledFilterValue() {
		return showDisabledFilterBtn != null ? showDisabledFilterBtn.getSelection() : false;
	}
	
	protected static class PackageColumnLabelProvider extends ColumnLabelProvider {
		
		Function<TrpCreditPackage, String> getter;
		Date now;
		
		public PackageColumnLabelProvider(Function<TrpCreditPackage, String> getter) {
			super();
			this.getter = getter;
			this.now = new Date();
		}
		
		@Override
		public void update(ViewerCell cell) {
			if (!(cell.getElement() instanceof TrpCreditPackage)) {
				return;
			}
			TrpCreditPackage p = (TrpCreditPackage) cell.getElement();
			cell.setText(getter.apply(p));
			
			//show expired packages gray
			boolean greyOut = p.getExpirationDate() != null && now.after(p.getExpirationDate());
			//show depleted packages gray
			greyOut |= p.getBalance() != null && p.getBalance() <= 0.0d;
			//show disabled packages gray
			greyOut |= !p.isActive();
			
			if(greyOut) {
				cell.setForeground(Colors.getSystemColor(SWT.COLOR_DARK_GRAY));
			}
		}
		
		@Override
		public String getToolTipText(Object element) {
			if (!(element instanceof TrpCreditPackage)) {
				return super.getToolTipText(element);
			}
			TrpCreditPackage p = (TrpCreditPackage) element;
			List<String> hints = new ArrayList<>(2);
			if(p.getExpirationDate() != null && now.after(p.getExpirationDate())) {
				hints.add("expired");
			}
			if(p.getBalance() != null && p.getBalance() <= 0.0d) {
				hints.add("depleted");
			}
			if(!p.isActive()) {
				hints.add("disabled");
			}
			
			if(hints.isEmpty()) {
				return super.getToolTipText(element);
			}
			String msg = "Package is " + hints.get(0);
			if(hints.size() > 1) {
				msg += " and " + hints.get(1);
			}
			return msg;
		}
	}
}