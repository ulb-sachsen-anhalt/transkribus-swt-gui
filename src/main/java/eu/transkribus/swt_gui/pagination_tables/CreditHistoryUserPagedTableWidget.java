package eu.transkribus.swt_gui.pagination_tables;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.ibm.icu.math.BigDecimal;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpCreditHistoryEntry;
import eu.transkribus.core.model.beans.rest.TrpCreditHistoryList;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;
import eu.transkribus.swt.pagination_table.IPageLoadMethod;
import eu.transkribus.swt.pagination_table.RemotePageLoaderSingleRequest;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.credits.ABalanceComposite;
import eu.transkribus.swt_gui.credits.UserBalanceComposite;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

/**
 * Page loader will retrieve credit packages
 *
 */
public class CreditHistoryUserPagedTableWidget extends ATableWidgetPagination<TrpCreditHistoryEntry> {
	
	public static final String HIST_USER_NAME_COL = "User";
//	public static final String PACKAGE_USER_ID_COL = "Owner ID";
	public static final String HIST_DESC_COL = "Description";
	public static final String HIST_VALUE_COL = "Credits";
//	public static final String HIST_SOURCE_COL = "Source";	
//	public static final String HIST_TARGET_COL = "Target";
	public static final String HIST_DATE_COL = "Time";
	public static final String HIST_EXPIRATION_DATE_COL = "Expires";
	public static final String HIST_ID_COL = "ID";
	
	private int userId;
	
	protected final DateFormat dateFormat;
	protected final DecimalFormat numberFormat;
	
	RemotePageLoaderSingleRequest<TrpCreditHistoryList, TrpCreditHistoryEntry> pageLoader;
	
	private ABalanceComposite balanceComp;

	public CreditHistoryUserPagedTableWidget(Composite parent, int style) {
		super(parent, style, 25);
		this.setLayout(new GridLayout(1, false));
		dateFormat = CoreUtils.newDateFormatUserFriendly();
		numberFormat = new DecimalFormat("0.##");
		//round down explicitly, otherwise 0.9999996 would be shown as 1
		numberFormat.setRoundingMode(RoundingMode.DOWN);
		userId = Storage.getInstance().getUserId();
		addFilters(Storage.getInstance().isAdminLoggedIn());
		createBalanceComposite(pageableTable);
		addListeners();
	}
	
	public void setUserId(int userId) {
		this.userId = userId;
		refreshPage(true);
	}
	
	private void addListeners() {
		//add listeners related to the filter buttons within this composite
//		SelectionAdapter listener = new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				refreshPage(false);
//			}
//		};
//		if(showDisabledFilterBtn != null) {
//			showDisabledFilterBtn.addSelectionListener(listener);
//		}
//		showDepletedFilterBtn.addSelectionListener(listener);
//		showExpiredFilterBtn.addSelectionListener(listener);
	}

	protected void addFilters(boolean isAdmin) {
		final int numColumns = 2; //isAdmin ? 3 : 2;
		Composite filterComp = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterComp);
		GridLayoutFactory.fillDefaults().numColumns(numColumns).equalWidth(false).applyTo(filterComp);
		filterComp.moveAbove(null);
//		if(isAdmin) {
//			showDisabledFilterBtn = new Button(filterComp, SWT.CHECK);
//			showDisabledFilterBtn.setText("Show disabled");
//			showDisabledFilterBtn.setSelection(true);
//		}
//		showDepletedFilterBtn = new Button(filterComp, SWT.CHECK);
//		showDepletedFilterBtn.setText("Show depleted");
//		showExpiredFilterBtn = new Button(filterComp, SWT.CHECK);
//		showExpiredFilterBtn.setText("Show expired");
	}

	private void createBalanceComposite(PageableTable pageableTable) {
		// Create the composite in the bottom right of the table widget
		Composite parent = pageableTable.getCompositeBottom();
		int layoutColsIncrement = 1;
		
		balanceComp = createBalanceComposite(parent);
		balanceComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		//adjust layout of bottom
		GridLayout layout = (GridLayout) parent.getLayout();
		layout.numColumns += layoutColsIncrement;
		parent.pack();
	}

	protected ABalanceComposite createBalanceComposite(Composite parent) {
		return new UserBalanceComposite(parent, SWT.NONE);
	}

	public TrpCreditHistoryEntry getSelectedPackage() {
		return getFirstSelected();
	}

	public void setSelection(int packageId) {
		// TODO
	}
	
	@Override
	protected void onReloadButtonPressed() {
		super.onReloadButtonPressed();
		this.updateBalanceComposite();
	}
	
	@Override
	public void refreshPage(boolean resetToFirstPage) {
		//do the refresh
		super.refreshPage(resetToFirstPage);
		this.updateBalanceComposite();
	}

	protected void updateBalanceComposite() {
		//retrieve previous page data from loader for further UI update
		TrpCreditHistoryList currentData = pageLoader.getCurrentData();
		balanceComp.update(currentData);
	}
	
	@Override
	protected void createColumns() {
		createColumns(false);
	}

	protected void createColumns(boolean showOwnerColumn) {
		createColumn(HIST_DATE_COL, 120, "time", new HistoryColumnLabelProvider(p -> dateFormat.format(p.getTime())));
		createColumn(HIST_VALUE_COL, 80, "creditValue",  new HistoryColumnLabelProvider(p -> buildCreditValue(p)));
		if(showOwnerColumn) {
			createColumn(HIST_USER_NAME_COL, 120, "userName", new HistoryColumnLabelProvider(p -> p.getUserName()));
			//for now we don't need the userid
//			createDefaultColumn(PACKAGE_USER_ID_COL, 50, "userId", new PackageColumnLabelProvider(p -> "" + p.getUserId()));
		}
		createColumn(HIST_DESC_COL, 400, null, new HistoryColumnLabelProvider(p -> buildDescription(p)));
		
//		createColumn(HIST_SOURCE_COL, 120, "sourceName", new PackageColumnLabelProvider(p -> p.getSourceName()));
//		createColumn(HIST_TARGET_COL, 120, "targetName", new PackageColumnLabelProvider(p -> p.getTargetName()));
		createColumn(HIST_EXPIRATION_DATE_COL, 120, "packageExpirationDate", new HistoryColumnLabelProvider(
				p -> p.getPackageExpirationDate() == null ? "" : dateFormat.format(p.getPackageExpirationDate()))
			);
		createColumn(HIST_ID_COL, 50, "historyId", new HistoryColumnLabelProvider(p -> "" + p.getHistoryId()));
		ColumnViewerToolTipSupport.enableFor(super.getTableViewer());
	}

	protected String buildCreditValue(TrpCreditHistoryEntry e) {
		double value = e.getCreditValue();
		if(e.getSourceUserId() != null && e.getSourceUserId().intValue() == userId) {
			value = BigDecimal.valueOf(value).negate().doubleValue();
		}
		return numberFormat.format(value);
	}

	protected String buildDescription(TrpCreditHistoryEntry e) {
		if(!"Transfer".equals(e.getDescription())) {
			return e.getDescription();
		}
		String sourceType = e.getSourceUserId() != null ? "user" : "collection";
		String targetType = e.getTargetUserId() != null ? "user" : "collection";
		final String format = "Transfer from %s '%s' to %s '%s'";
		return String.format(format, sourceType, e.getSourceName(), targetType, e.getTargetName());
	}

	protected RemotePageLoaderSingleRequest<TrpCreditHistoryList, TrpCreditHistoryEntry> createPageLoader() {
		IPageLoadMethod<TrpCreditHistoryList, TrpCreditHistoryEntry> plm = new IPageLoadMethod<TrpCreditHistoryList, TrpCreditHistoryEntry>() {

			@Override
			public TrpCreditHistoryList loadPage(int fromIndex, int toIndex, String sortPropertyName,
					String sortDirection) {
				if(sortPropertyName == null) {
					sortPropertyName = "time";
					sortDirection = "DESC";
				}
				Storage store = Storage.getInstance();
				if (store.isLoggedIn()) {
					try {
						return store.getConnection().getCreditCalls().getCreditHistoryByUser(userId,
//								!getShowDisabledFilterValue(), getShowExpiredFilterValue(), getMinBalanceFilterValue(),
								fromIndex, toIndex - fromIndex, sortPropertyName, sortDirection);
					} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException e) {
						TrpMainWidget.getInstance().onError("Error loading Credit Packages", e.getMessage(), e);
					}
				}
				return new TrpCreditHistoryList(new ArrayList<>(), 0, 0.0d, 0, 0, null, null);
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
		TrpCreditHistoryList currentData = pageLoader.getCurrentData();
		if(currentData == null) {
			return null;
		}
		return currentData.getOverallBalance();
	}
	
	/**
	 * @return the calculator button
	 */
	public Button getShowBalanceDetailsButton() {
		return balanceComp.getShowDetailsBtn();
	}
	
//	protected boolean getShowExpiredFilterValue() {
//		return showExpiredFilterBtn.getSelection();
//	}
//	
//	protected Double getMinBalanceFilterValue() {
//		return showDepletedFilterBtn.getSelection() ? null : 0.001d;
//	}
//	
//	protected boolean getShowDisabledFilterValue() {
//		return showDisabledFilterBtn != null ? showDisabledFilterBtn.getSelection() : false;
//	}

	
	protected static class HistoryColumnLabelProvider extends ColumnLabelProvider {
		
		Function<TrpCreditHistoryEntry, String> getter;
		Date now;
		
		public HistoryColumnLabelProvider(Function<TrpCreditHistoryEntry, String> getter) {
			super();
			this.getter = getter;
			this.now = new Date();
		}
		
		@Override
		public void update(ViewerCell cell) {
			if (!(cell.getElement() instanceof TrpCreditHistoryEntry)) {
				return;
			}
			TrpCreditHistoryEntry p = (TrpCreditHistoryEntry) cell.getElement();
			cell.setText(getter.apply(p));
			
			//show expired packages gray
			boolean greyOut = p.getPackageExpirationDate() != null && now.after(p.getPackageExpirationDate());
			
			if(greyOut) {
				cell.setForeground(Colors.getSystemColor(SWT.COLOR_DARK_GRAY));
			}
		}
		
		@Override
		public String getToolTipText(Object element) {
			if (!(element instanceof TrpCreditHistoryEntry)) {
				return super.getToolTipText(element);
			}
			TrpCreditHistoryEntry p = (TrpCreditHistoryEntry) element;
			List<String> hints = new ArrayList<>(2);
			if(p.getPackageExpirationDate() != null && now.after(p.getPackageExpirationDate())) {
				hints.add("expired");
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