package eu.transkribus.swt_gui.pagination_tables;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

import eu.transkribus.core.model.beans.TrpCreditTransaction;
import eu.transkribus.swt.pagination_table.ATableWidgetPagination;

public abstract class ACreditTransactionsPagedTableWidget extends ATableWidgetPagination<TrpCreditTransaction> {
	public static final String TA_DESC_COL = "Description";
	public static final String TA_VALUE_COL = "Value";
	public static final String TA_COST_COL = "Cost Factor";
	public static final String TA_BALANCE_COL = "Balance";
	public static final String TA_USERNAME_COL = "User";
	public static final String TA_DATE_COL = "Time";
	public static final String TA_PACKAGE_ID_COL = "Package ID";

	// filter:
	Composite filterAndReloadComp;
	
	public ACreditTransactionsPagedTableWidget(Composite parent, int style) {
		super(parent, style, 25);
		this.setLayout(new GridLayout(1, false));
		addFilter();
	}
	
	@Override
	public void addListener(int eventType, Listener listener) {
		super.addListener(eventType, listener);
	}
	
	private void addFilter() {
		filterAndReloadComp = new Composite(this, SWT.NONE);
		filterAndReloadComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterAndReloadComp.setLayout(new GridLayout(2, false));
		filterAndReloadComp.moveAbove(null);
	}
	
	public TrpCreditTransaction getSelectedPackage() {
		return getFirstSelected();
	}

	public void setSelection(int packageId) {
		// TODO
	}
	
	@Override
	protected void createColumns() {
		createDefaultColumn(TA_DESC_COL, 220, "description", true);
		createDefaultColumn(TA_VALUE_COL, 50, "creditValue", true);
		createDefaultColumn(TA_COST_COL, 50, "costFactor", true);
		createDefaultColumn(TA_DATE_COL, 70, "time", true);
		createDefaultColumn(TA_BALANCE_COL, 50, "creditBalance", true);
		createDefaultColumn(TA_PACKAGE_ID_COL, 50, "packageId", true);
	}	
}